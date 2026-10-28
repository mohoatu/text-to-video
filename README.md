# Videogen — Local-First Vietnamese AI Text-to-Video

Turns a Vietnamese prompt (or a long document) into a short narrated video: the system plans a
scene breakdown, synthesizes Vietnamese narration, generates an illustration per scene, and
assembles everything into a subtitled MP4 — running entirely on local/self-hosted AI, no cloud AI
provider required.

> **Note on scope vs. the original plan**: `docs/` holds the project's original planning
> documents, which described a hybrid AWS architecture (API Gateway, SQS, S3, DynamoDB) for a
> future commercial product. What's actually implemented (below) is the local-first MVP from that
> plan — no AWS: an in-process queue/worker and local disk storage stand in for SQS/S3/DynamoDB.
> See `docs/architecture-decision.md` for why, and `app/videogen/TASKS.md` for what's deliberately
> out of scope for now.

## How it works

```
POST /api/jobs  ──►  [PENDING] ──► background worker picks it up
                                          │
                                          ▼
                              1. planning  — local LLM (Ollama) breaks the prompt/document
                                             into scenes: narration (Vietnamese) + a visual
                                             description (English, for the image model)
                                          ▼
                              2. tts       — piper synthesizes each scene's narration
                                          ▼
                              3. visual    — ComfyUI (Stable Diffusion) illustrates each
                                             scene in the configured drawing style
                                          ▼
                              4. subtitle  — builds a timed .srt from the narration
                                          ▼
                              5. assembly  — ffmpeg combines image+audio per scene, burns
                                             in subtitles, fades out at the end
                                          ▼
                              6. storage   — final MP4 + job metadata saved to disk
                                          ▼
                                     [COMPLETED] ──► GET /api/jobs/{id}/output
```

Every AI-backed stage degrades gracefully: if Ollama/piper/ComfyUI/ffmpeg aren't reachable, the
pipeline either retries, falls back (visuals fall back to a plain placeholder image), or fails
that one job with a clear `failedStage`/`errorMessage` — it never leaves the app itself in a bad
state.

## Quick start

### Docker (recommended for deployment)

```bash
docker compose up -d --build
docker compose exec ollama ollama pull qwen2.5:14b          # one-time, ~9GB
docker compose run --rm comfyui-checkpoint-setup             # one-time, ~4.3GB
# download the Vietnamese piper voice (see "Voice model" below) into app/videogen/models/piper/

curl -X POST localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"prompt": "Ke cau chuyen Thach Sanh trong 30 giay"}'
```

GPU note: Ollama and ComfyUI benefit heavily from GPU acceleration. On Linux with an NVIDIA GPU,
install the NVIDIA Container Toolkit and uncomment the GPU reservation blocks in
`docker-compose.yml`. **Docker Desktop on macOS cannot pass through Apple's Metal/MPS GPU to a
container at all** — on a Mac, for real speed, run Ollama and ComfyUI natively on the host (see
`app/videogen/TASKS.md`) and point the containerized app at them instead of starting those two
services:

```bash
docker compose up -d --build app   # app only; Ollama/ComfyUI run natively on the host
```
```bash
# app/videogen (native overrides, since `app` is inside Docker but Ollama/ComfyUI are on the host)
VIDEOGEN_LLM_OLLAMA_BASE_URL=http://host.docker.internal:11434
VIDEOGEN_VISUAL_COMFYUI_BASE_URL=http://host.docker.internal:8188
```

### Native (fastest on Apple Silicon)

See `app/videogen/TASKS.md` for the full native setup (Ollama, piper-tts, a full ffmpeg build with
subtitle support, ComfyUI) — this is how the system was actually built and verified, and is
meaningfully faster than Docker on a Mac since it can use the GPU.

```bash
cd app/videogen
mvn spring-boot:run                                   # real profile — needs the stack above
mvn spring-boot:run -Dspring-boot.run.profiles=stub    # offline profile — no external tools needed
```

### Voice model

The Vietnamese piper voice isn't bundled (large binary, fetched per-machine):

```bash
mkdir -p app/videogen/models/piper && cd app/videogen/models/piper
curl -LO https://huggingface.co/rhasspy/piper-voices/resolve/main/vi/vi_VN/vais1000/medium/vi_VN-vais1000-medium.onnx
curl -LO https://huggingface.co/rhasspy/piper-voices/resolve/main/vi/vi_VN/vais1000/medium/vi_VN-vais1000-medium.onnx.json
```

## API

| Endpoint | Description |
|---|---|
| `POST /api/jobs` | Create a job from an inline `prompt` (short topic or a full long-form document, up to 50,000 characters) |
| `POST /api/jobs/upload` | Create a job from one or more uploaded `.txt` files (multipart `files`) — a long article, or a "folder" of chapter files, concatenated in filename order |
| `GET /api/jobs/{id}` | Job status (`PENDING`/`PROCESSING`/`COMPLETED`/`FAILED`) and metadata |
| `GET /api/jobs/{id}/output` | Output info once `COMPLETED` |

Both create endpoints accept:
- `style` *(optional)* — drawing style for scene visuals: a preset key (`ink-wash` — the default;
  also `anime`, `flat-comic`), or any free-form English style description, applied per request.
  See `VisualStylePreset`.
- `durationTargetSeconds` *(optional)* — 5–180.

```bash
curl -X POST localhost:8080/api/jobs \
  -H 'Content-Type: application/json' \
  -d '{"prompt": "Ke cau chuyen Thach Sanh trong 30 giay", "style": "anime"}'

curl -X POST localhost:8080/api/jobs/upload \
  -F "files=@chapter1.txt" -F "files=@chapter2.txt" -F "style=ink-wash"
```

## Tech stack

- **Backend**: Java 21, Spring Boot 3.3, in-process queue + background worker (no message broker)
- **LLM**: Ollama (`qwen2.5:14b` by default) — scene planning via structured JSON output
- **TTS**: piper (`vi_VN-vais1000-medium` Vietnamese voice)
- **Visual generation**: ComfyUI running a standard Stable Diffusion 1.5 txt2img workflow
- **Assembly**: ffmpeg (requires a build with `libass` for subtitle burn-in)
- **Storage**: local disk (`app/videogen/data/`) — no cloud dependency

## Project layout

```
app/videogen/            the application (see app/videogen/TASKS.md for implementation details)
  src/main/java/...       controller → service (orchestration/planning/tts/visual/subtitle/assembly/storage) → model
  Dockerfile               app image (bundles ffmpeg + piper; Ollama/ComfyUI run as separate services)
  TASKS.md                 task-by-task build log, native local-stack setup, and design notes
docker/comfyui/           ComfyUI container image definition
docker-compose.yml        full local stack: app + ollama + comfyui
docs/                     original project planning documents (business/technical requirements,
                          architecture options, the hybrid-AWS plan this MVP intentionally departs from)
```

For implementation history, configuration reference, and native setup instructions, see
**[app/videogen/TASKS.md](app/videogen/TASKS.md)**.
