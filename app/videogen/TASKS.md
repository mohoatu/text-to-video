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
  `ComfyUiVisualGenerationService` calls a local ComfyUI instance when enabled/reachable,
  otherwise (and on any failure) falls back to a generated placeholder image
  (`PlaceholderAssetGenerator`). `StubVisualGenerationService` always uses the placeholder.

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

### 4. Visual/video generation — ComfyUI + AnimateDiff (optional)
The MVP falls back to a placeholder still image per scene (`videogen.visual.enabled=false`) and
works fully without this. To get real generated motion instead:
```bash
git clone --depth 1 https://github.com/comfyanonymous/ComfyUI.git ~/videogen-local-stack/ComfyUI
git clone --depth 1 https://github.com/ArtVentureX/comfyui-animatediff.git \
  ~/videogen-local-stack/ComfyUI/custom_nodes/comfyui-animatediff

python3.12 -m venv ~/videogen-local-stack/ComfyUI/venv
source ~/videogen-local-stack/ComfyUI/venv/bin/activate
pip install torch torchvision torchaudio   # MPS-accelerated build on Apple Silicon, confirmed
pip install -r ~/videogen-local-stack/ComfyUI/requirements.txt
pip install opencv-python                  # comfyui-animatediff's own requirement

mkdir -p ~/videogen-local-stack/ComfyUI/models/checkpoints
curl -L -o ~/videogen-local-stack/ComfyUI/models/checkpoints/v1-5-pruned-emaonly.safetensors \
  https://huggingface.co/Comfy-Org/stable-diffusion-v1-5-archive/resolve/main/v1-5-pruned-emaonly.safetensors
curl -L -o ~/videogen-local-stack/ComfyUI/custom_nodes/comfyui-animatediff/models/mm_sd_v15_v2.ckpt \
  https://huggingface.co/guoyww/animatediff/resolve/main/mm_sd_v15_v2.ckpt

# start it:
~/videogen-local-stack/ComfyUI/venv/bin/python ~/videogen-local-stack/ComfyUI/main.py --listen 127.0.0.1
```
Then set `videogen.visual.enabled=true` in `application.properties`. Note: `ComfyUiVisualGenerationService`
still only *establishes connectivity* to ComfyUI as of this writing — it does not yet submit the
AnimateDiff workflow graph itself (see the code comment in `callComfyUi()`); wiring the actual
API-format workflow (checkpoint → CLIPTextEncode → AnimateDiffSampler → AnimateDiffCombine as
GIF, which `FfmpegVideoAssemblyService` would then need to accept as a scene visual alongside
still images) is the remaining piece to get real generated video instead of the placeholder.

### Gotcha: this connection was heavily bandwidth-constrained
Hugging Face downloads repeatedly stalled/dropped over HTTP/2 (`curl: (92) HTTP/2 stream ... was
not closed cleanly`). If a large model download fails partway, resume rather than restart:
```bash
curl -L -C - --retry 10 --retry-delay 5 --http1.1 -o <file> <url>
```
