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
- A real ComfyUI workflow graph (current integration establishes connectivity and falls back
  to a placeholder image — a working node graph is deployment-specific)
