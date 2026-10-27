# Implementation Tasklist

Tracks the 12 build-out tasks defined in `prompts/`, derived from `docs/implementation-plan.md`,
`docs/technical-requirements.md`, and `docs/technical-solution.md`. Each task below implements
the corresponding numbered prompt file 1:1.

- [x] **01 — Project Foundation** (`prompts/01-project-foundation.md`)
  Spring Boot 3.3.3 / Java 21 app, `/health` endpoint, package skeleton.

- [x] **02 — API Layer** (`prompts/02-api-layer.md`)
  `VideoJobController`: `POST /api/jobs`, `GET /api/jobs/{id}`, `GET /api/jobs/{id}/output`.
  `CreateJobRequest`/`JobResponse`/`JobOutputResponse` DTOs with bean validation,
  `GlobalExceptionHandler` for 400/404/409/500.

- [x] **03 — Domain Model** (`prompts/03-domain-model.md`)
  `Job`, `Scene`, `GenerationResult`, `JobStatus` in `model/`.

- [x] **04 — Orchestration Layer** (`prompts/04-orchestration-layer.md`)
  `VideoGenerationOrchestrator` drives PENDING → PROCESSING → COMPLETED/FAILED,
  calling planning → tts → visual → subtitle → assembly → storage in order,
  recording the failed stage + error message on failure.

- [x] **05 — Prompt Planning Service** (`prompts/05-prompt-planning-service.md`)
  `OllamaScenePlanningService` calls a local Ollama instance with a Vietnamese
  system prompt requesting strict JSON, validates/maps the response to `Scene`s.
  `StubScenePlanningService` (profile `stub`) is a deterministic offline fallback.

- [x] **06 — TTS Service** (`prompts/06-tts-service.md`)
  `PiperTtsService` shells out to the `piper` CLI per scene.
  `StubTtsService` (profile `stub`) writes a placeholder file for offline runs.

- [x] **07 — Visual Generation Service** (`prompts/07-visual-generation-service.md`)
  `ComfyUiVisualGenerationService` submits a standard SD1.5 txt2img workflow to a local ComfyUI
  instance when enabled/reachable (prompted toward a simple flat comic/storybook illustration
  style — see "Running the full local stack" below), otherwise (and on any failure) falls back
  to a generated placeholder image (`PlaceholderAssetGenerator`). `StubVisualGenerationService`
  always uses the placeholder.

- [x] **08 — Subtitle Service** (`prompts/08-subtitle-service.md`)
  `SrtSubtitleService` builds a standard `.srt` file with cumulative scene timing
  (falls back to a words-per-second estimate when `durationHint` is missing).

- [x] **09 — Video Assembly Service** (`prompts/09-video-assembly-service.md`)
  `FfmpegVideoAssemblyService` builds per-scene image+audio segments, concatenates
  them, and burns in the subtitle file via `ffmpeg` (`ProcessBuilder`), surfacing
  stderr on failure. `StubVideoAssemblyService` (profile `stub`) writes a placeholder MP4.

- [x] **10 — Storage and Metadata** (`prompts/10-storage-and-metadata.md`)
  `LocalStorageService` copies final output under `data/jobs/{id}/output.*` and
  writes a `job.json` metadata snapshot. No AWS dependency for the MVP.

- [x] **11 — Queue and Background Processing** (`prompts/11-queue-and-background-processing.md`)
  `JobQueue` (in-memory) + `JobWorker` (single background thread) so
  `POST /api/jobs` returns immediately while the pipeline runs asynchronously.

- [x] **12 — Observability and Reliability** (`prompts/12-observability-and-reliability.md`)
  MDC-scoped structured logs (`jobId`, `stage`) in the orchestrator, `RetryUtil`
  for transient planning/TTS failures, `failedStage`/`errorMessage` on `Job` as the
  dead-letter/failure record, Actuator health + readiness/liveness probes.

## How to run it

```bash
cd app/videogen
mvn spring-boot:run                                   # real profile: needs Ollama + piper + ffmpeg (+ ComfyUI optional)
mvn spring-boot:run -Dspring-boot.run.profiles=stub    # offline profile: no external tools required
```

```bash
curl -X POST localhost:8080/api/jobs -H 'Content-Type: application/json' \
  -d '{"prompt": "Ke cau chuyen Thach Sanh trong 30 giay"}'
curl localhost:8080/api/jobs/{id}
curl localhost:8080/api/jobs/{id}/output
```

## Known local dev gotcha

Lombok annotation processing (getters/setters/builders) silently no-ops on very new/preview JDKs
(observed on JDK 26 here). Build with JDK 21 — matching `pom.xml`'s `java.version` — via:

```bash
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12/libexec/openjdk.jdk/Contents/Home
mvn clean test
```

CI (`.github/workflows/ci.yml`) already pins JDK 21 via `actions/setup-java`, so this only affects
local builds on machines with a newer default JDK.

## Not yet implemented (deliberately out of MVP scope per docs)

These are called out in `docs/architecture-decision.md` / `docs/technical-solution.md` as
Phase 2/3 (optimization, commercialization) or cloud-hosted concerns, not part of the 12 prompts:
- Amazon SQS / S3 / DynamoDB integration (local queue + local storage stand in for the MVP)
- User authentication, billing/credits, hosted rendering, partner API access

## Running the full local stack (real profile)

Everything below runs entirely on-machine — no cloud AI provider required. Confirmed working
end-to-end on an M1 Pro / 32GB Mac: real Ollama scene planning, real Piper Vietnamese TTS, and
real ffmpeg subtitle burn-in all verified against actual `POST /api/jobs` runs.

### 1. LLM — Ollama (scene planning)
```bash
brew install ollama          # brew services starts it automatically
ollama pull qwen2.5:14b      # ~9GB; qwen2.5:7b also works, lower quality/faster
```
`OllamaClient` uses Ollama's structured-outputs feature (a JSON Schema passed as the `format`
field, not just `"format": "json"`) to reliably force an array of scenes — the plain `"json"`
format let the model return a single object instead of an array often enough to matter.

### 2. TTS — piper-tts (Vietnamese narration)
The prebuilt `piper` binary release (`piper_macos_aarch64.tar.gz`, tag `2023.11.14-2`) is missing
its bundled shared libraries on macOS and will not run. Use the `piper-tts` PyPI package instead:
```bash
brew install python@3.12
python3.12 -m venv ~/videogen-local-stack/piper-venv
source ~/videogen-local-stack/piper-venv/bin/activate && pip install piper-tts

# wrapper so PiperTtsService's plain `piper` ProcessBuilder call keeps working:
cat > /opt/homebrew/bin/piper <<'EOF'
#!/bin/bash
exec "$HOME/videogen-local-stack/piper-venv/bin/piper" "$@"
EOF
chmod +x /opt/homebrew/bin/piper
```
Vietnamese voice (best available quality — `medium`; `low` variants also exist):
```bash
mkdir -p app/videogen/models/piper && cd app/videogen/models/piper
curl -LO https://huggingface.co/rhasspy/piper-voices/resolve/main/vi/vi_VN/vais1000/medium/vi_VN-vais1000-medium.onnx
curl -LO https://huggingface.co/rhasspy/piper-voices/resolve/main/vi/vi_VN/vais1000/medium/vi_VN-vais1000-medium.onnx.json
```
(`app/videogen/models/` is gitignored — not committed, fetch it per-machine.)

### 3. ffmpeg — full build with subtitle support
Homebrew's default `ffmpeg` formula is built **without `libass`/`libfreetype`**, so the
`subtitles` filter used by `FfmpegVideoAssemblyService` doesn't exist and burn-in fails with a
confusing `No option name near '...srt'` parse error (not "unknown filter" — worth knowing when
debugging). Fix:
```bash
brew uninstall ffmpeg
brew tap homebrew-ffmpeg/ffmpeg
brew install homebrew-ffmpeg/ffmpeg/ffmpeg   # may compile from source (~a few min) if no bottle
                                              # exists yet for your macOS version
ffmpeg -filters | grep -i subtitle           # sanity check: should list `subtitles` and `ass`
```

### 4. Visual generation — ComfyUI, simple comic-style stills (optional)
The MVP falls back to a plain placeholder image per scene (`videogen.visual.enabled=false`) and
works fully without this.

```bash
git clone --depth 1 https://github.com/comfyanonymous/ComfyUI.git ~/videogen-local-stack/ComfyUI

python3.12 -m venv ~/videogen-local-stack/ComfyUI/venv
source ~/videogen-local-stack/ComfyUI/venv/bin/activate
pip install torch torchvision torchaudio   # MPS-accelerated build on Apple Silicon, confirmed
pip install -r ~/videogen-local-stack/ComfyUI/requirements.txt

mkdir -p ~/videogen-local-stack/ComfyUI/models/checkpoints
curl -L -o ~/videogen-local-stack/ComfyUI/models/checkpoints/v1-5-pruned-emaonly.safetensors \
  https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive/resolve/main/v1-5-pruned-emaonly.safetensors

# start it:
~/videogen-local-stack/ComfyUI/venv/bin/python ~/videogen-local-stack/ComfyUI/main.py --listen 127.0.0.1
```
Then set `videogen.visual.enabled=true` in `application.properties`.

`ComfyUiVisualGenerationService` submits a standard SD1.5 txt2img workflow (`CheckpointLoaderSimple
-> CLIPTextEncode -> KSampler -> VAEDecode -> SaveImage`; **no custom nodes needed**), polls
`/history/{promptId}`, and downloads the resulting PNG via `/view`. The scene's `visualPrompt` is
wrapped with `videogen.visual.style-prefix` (default: an anime/manga illustration style — "like
Naruto or Dragon Ball Z, cel shaded, bold ink outlines, vibrant anime colors" — the base SD1.5
checkpoint handles this surprisingly well with no dedicated anime checkpoint needed; swap the
prefix for something like "simple flat cartoon illustration, children's storybook art style" for
a softer look instead) so output stays illustrative rather than attempting photorealism. Takes
~30s/scene on M1 Pro MPS.

**An earlier version of this used ComfyUI's AnimateDiff custom node for real generated motion
(short animated clips instead of stills).** It worked (after patching a real MPS device-placement
bug in `comfyui-animatediff`'s `AnimateDiffSampler` — the motion module's weights never got moved
off the CPU offload device, see git history), but ran ~6-7 minutes *per scene* and the generated
content routinely didn't match the prompt (e.g. "Hạ Long Bay" produced wicker baskets and a person
on a motorbike). It was replaced by the still-image approach above, which is ~12x faster and far
more prompt-faithful. `FfmpegVideoAssemblyService` still has a code path for an animated `.gif`
scene visual (`-stream_loop -1`) if AnimateDiff generation is ever reintroduced.

**Scene visual prompts are generated in English, even though everything else (narration, subtitles)
is Vietnamese.** SD1.5's CLIP text encoder is trained overwhelmingly on English; sending it Vietnamese
prompts produced poorly-matched, sometimes unrelated images. `OllamaScenePlanningService`'s system
prompt now explicitly instructs the model to write `visualPrompt` in concise, concrete English
(narrationText stays Vietnamese) — this alone fixed most of the content-mismatch problem. The
placeholder-fallback caption (`PlaceholderAssetGenerator`) uses `narrationText`, not `visualPrompt`,
so it still displays Vietnamese.

### 5. Pacing: narration speed, scene timing, and the ending
- **Narration speed**: `videogen.tts.length-scale` is piper's `--length-scale` (1.0 = the voice's
  natural pace; 1.15 default here = ~15% slower, generally easier to follow for Vietnamese).
- **Scene timing**: the LLM's `durationHint` is only a starting guess. After TTS, each scene's
  duration is corrected to the *actual* synthesized audio length (via `MediaDurationProbe`, which
  shells out to `ffprobe`) plus `videogen.assembly.scene-padding-seconds` (0.4s breathing room),
  floored at `videogen.assembly.min-scene-seconds` (1.5s). This also fixed a latent bug: the
  per-scene ffmpeg command used to pass `-shortest`, which — combined with an LLM duration guess
  that was almost always longer than the real (now slower) audio — cut each scene's visual short
  the moment narration ended rather than holding it for the intended duration. `-shortest` was
  removed; `-t <corrected duration>` is now the sole authority on segment length, so narration
  always finishes playing and the image holds for the configured padding after.
- **Ending**: `videogen.outro.*` appends a fixed scene (default narration "Hết.") after planning,
  which runs through the normal pipeline like any other scene, plus `VideogenOrchestrator` applies
  a `fade=t=out`/`afade=t=out` over the last `videogen.outro.fade-out-seconds` (1.0s default) of
  the whole assembled video in `FfmpegVideoAssemblyService.finalizeVideo()`, so nothing just cuts
  off abruptly.
- **Gotcha**: don't put non-ASCII (Vietnamese) text directly in `application.properties` — unlike
  YAML, Spring Boot's `.properties` loader reads with `ISO-8859-1` by default, so `Hết.` written
  there came through as mojibake (`Háº¿t.`) at runtime. Put Vietnamese defaults in Java source
  (compiled as UTF-8) instead, as `VideogenProperties.Outro.narrationText`'s default does, or move
  the file to `application.yml` if it ever needs to be a property override.

### Gotcha: this connection was heavily bandwidth-constrained
Hugging Face downloads repeatedly stalled/dropped over HTTP/2 (`curl: (92) HTTP/2 stream ... was
not closed cleanly`). If a large model download fails partway, resume rather than restart:
```bash
curl -L -C - --retry 10 --retry-delay 5 --http1.1 -o <file> <url>
```
