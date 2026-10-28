# Implementation Plan for Local AI Text-to-Video Platform

## 1. Objective
Build a Vietnamese AI text-to-video pipeline that can:
- accept a user prompt
- generate a structured scene plan
- create narration in Vietnamese
- generate or retrieve visuals for each scene
- assemble a final MP4 video with subtitles
- expose the workflow through a simple API and background processing

The implementation should follow a hybrid architecture:
- cloud services for orchestration, API, storage, queueing, and job tracking
- local workstation for heavy AI and media rendering tasks

## 2. Success Criteria
The first version should be able to:
- accept a prompt in Vietnamese
- produce a JSON scene plan
- generate speech audio
- generate or retrieve visuals for scenes
- combine assets into a short video
- deliver the final output and status information

## 3. Guiding Principles
- Prefer a simple MVP over a complex system.
- Keep the architecture modular so each component can be improved separately.
- Favor local-first inference to reduce cost.
- Make the workflow asynchronous to avoid blocking user requests.
- Use clear input/output contracts for each pipeline stage.
- Expect the system to evolve from local MVP to commercial SaaS later.

## 4. System Components

### 4.1 API Layer
Responsibilities:
- accept video generation requests
- validate input payloads
- create a new job record
- return a job ID and initial status

Suggested implementation:
- Spring Boot REST controller
- request DTOs and response DTOs
- validation rules

### 4.2 Job Orchestrator
Responsibilities:
- receive the job request
- enqueue work items
- manage job state transitions
- trigger downstream services in the correct order

Suggested implementation:
- service class that coordinates pipeline steps
- state machine with statuses: PENDING, PROCESSING, COMPLETED, FAILED

### 4.3 Prompt Planning Service
Responsibilities:
- transform a user prompt into a scene-based plan
- return structured JSON containing narration text and visual prompts

Suggested implementation:
- Spring AI service using a local LLM via Ollama or LM Studio
- prompt template in Vietnamese
- output schema validation

### 4.4 TTS Service
Responsibilities:
- convert scene narration text into speech audio
- support Vietnamese pronunciation and punctuation

Suggested implementation:
- local TTS engine such as Piper or Coqui TTS
- optional fallback to a cloud TTS provider later

### 4.5 Visual Generation Service
Responsibilities:
- generate or retrieve visual assets for each scene
- support local image/video generation pipelines

Suggested implementation:
- ComfyUI or Draw Things integration
- local HTTP client wrapper
- fallback to placeholder or pre-generated assets if needed

### 4.6 Video Assembly Service
Responsibilities:
- combine audio, visuals, subtitles, and timing information
- render a final MP4 file

Suggested implementation:
- FFmpeg-based assembly workflow
- subtitle burn-in support
- duration alignment logic

### 4.7 Storage and Metadata Layer
Responsibilities:
- save input payloads, job state, generated assets, and final output files

Suggested implementation:
- DynamoDB for metadata
- S3 for media files
- clear file naming convention

## 5. Pipeline Workflow

### Step 1: Receive Request
Input:
- prompt text
- optional style parameters
- optional duration target

Output:
- job ID
- status = PENDING

### Step 2: Generate Scene Plan
Input:
- prompt text

Output:
- list of scenes
- each scene contains:
  - scene_id
  - narration_text
  - visual_prompt
  - duration_hint
  - voice_style_hint

### Step 3: Generate Audio
For each scene:
- convert narration_text to audio
- save as MP3 or WAV

### Step 4: Generate Visuals
For each scene:
- generate or retrieve visual asset
- save as image or short video clip

### Step 5: Create Subtitle Content
Generate subtitle lines for each scene based on narration text.

### Step 6: Assemble Video
Combine:
- audio tracks
- visual clips
- subtitle overlay
- timing offsets

Output:
- final MP4 file

### Step 7: Persist and Notify
- store output file in S3
- update job status to COMPLETED
- return URL or metadata to the user

## 6. Implementation Tasks

### Phase 1 – Project Bootstrap
1. Create the Spring Boot project structure.
2. Add dependencies for:
   - web
   - validation
   - Lombok
   - Spring AI
   - optional OpenAPI
3. Configure application properties for:
   - local LLM endpoint
   - TTS engine path
   - storage bucket name
   - queue name
4. Add basic health endpoints.

### Phase 2 – Core API
1. Create request DTOs.
2. Create response DTOs.
3. Implement a POST endpoint for video generation requests.
4. Implement a GET endpoint for job status.
5. Implement a GET endpoint for retrieving output metadata.

### Phase 3 – Job Processing
1. Define a job model with status and timestamps.
2. Implement a queue producer.
3. Implement a worker that processes one job at a time.
4. Add retry logic for transient failures.
5. Add dead-letter handling for repeated failures.

### Phase 4 – Scene Planning
1. Create a prompt template in Vietnamese.
2. Configure the local LLM connector.
3. Implement a planner service that calls the LLM.
4. Validate the LLM response against a strict JSON schema.
5. Map the JSON into Java domain objects.

### Phase 5 – TTS Integration
1. Configure the local TTS engine.
2. Implement a service that converts narration text to audio.
3. Save the generated audio to a local temp folder and then to storage.
4. Validate sample Vietnamese speech output.

### Phase 6 – Visual Generation
1. Connect to ComfyUI or Draw Things.
2. Implement a service that sends prompt payloads to the local renderer.
3. Save generated visuals to disk and storage.
4. Handle errors and fallbacks gracefully.

### Phase 7 – Video Assembly
1. Implement an FFmpeg command builder.
2. Concatenate clips into a final sequence.
3. Mix audio with the visual timeline.
4. Burn subtitles onto the output video.
5. Export an MP4 file.

### Phase 8 – Storage and Notification
1. Upload all intermediate and final files to S3.
2. Store metadata in DynamoDB.
3. Notify the client of completion using a response callback or polling mechanism.
4. Add logs and error reporting.

## 7. Data Models

### Job
- id
- status
- prompt
- created_at
- updated_at
- output_url
- error_message

### Scene
- id
- job_id
- scene_index
- narration_text
- visual_prompt
- duration_hint
- audio_file_path
- visual_file_path

### GenerationResult
- success
- output_path
- metadata
- error_message

## 8. Error Handling Strategy
- If the LLM response is invalid, retry with a stricter prompt.
- If TTS fails, retry once and then mark the job failed.
- If a visual generation call fails, use a placeholder asset.
- If the video assembly fails, preserve intermediate assets for debugging.
- Always log the failing stage.

## 9. Recommended Folder Structure
```text
src/main/java/com/example/videogen/
  controller/
  dto/
  model/
  service/
    orchestration/
    planning/
    tts/
    visual/
    assembly/
    storage/
  client/
  config/
  util/
```

## 10. Delivery Order
Implement in this order:
1. API and job model
2. queue and worker
3. local LLM planning
4. TTS
5. visual generation
6. FFmpeg assembly
7. storage and notifications

## 11. Implementation Notes for AI Agents
When implementing this plan, follow these rules:
- Keep each service focused on one responsibility.
- Design interfaces first, then implement concrete classes.
- Use DTOs for API boundaries.
- Keep prompts and model schemas explicit.
- Prefer deterministic JSON output from the model.
- Make the pipeline observable through structured logs.
- Do not mix orchestration logic into media rendering code.

## 12. Expected Outcome
After implementation, the system should be able to take a Vietnamese prompt and generate a short video with a clear workflow, modular services, and a practical local-first architecture.
