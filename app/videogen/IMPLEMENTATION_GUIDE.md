# Parent Implementation Guide

## Purpose
This file is the main execution blueprint for building the local-first AI text-to-video platform. It is written in a loop-oriented style so that an AI coding agent can implement the system step by step in a consistent and incremental way.

## Core Loop
The application should follow this continuous loop:

1. Receive a user prompt.
2. Validate the request.
3. Create a workflow job.
4. Generate a structured scene plan.
5. Convert narration to audio.
6. Generate or retrieve visuals.
7. Assemble the final video.
8. Store outputs and update job status.
9. Return the result to the user.

This loop should be implemented as a pipeline with clear stages and explicit data contracts.

## Required Implementation Tasks

### 1. Project Foundation
- Create a Spring Boot application entry point.
- Configure the application properties.
- Add health and readiness endpoints.
- Prepare the package structure for controllers, services, DTOs, models, and config.

### 2. API Layer
- Define request DTOs for prompt input.
- Define response DTOs for job submission and status.
- Implement a POST endpoint for creating a video generation job.
- Implement a GET endpoint for retrieving job status.
- Implement a GET endpoint for retrieving output details.

### 3. Domain Model
- Create a Job model with fields such as id, status, prompt, timestamps, and output URL.
- Create a Scene model for each planned scene.
- Create a GenerationResult model to hold output paths and errors.

### 4. Orchestration Layer
- Implement a service that manages the end-to-end workflow.
- Track job lifecycle from PENDING to PROCESSING to COMPLETED or FAILED.
- Ensure each stage is executed in sequence with clear error handling.

### 5. Prompt Planning Service
- Create a service that sends the prompt to a local LLM.
- Use a Vietnamese system prompt.
- Require the LLM to return strict JSON with scene structure.
- Validate the response and map it into Java objects.

### 6. TTS Service
- Integrate a local Vietnamese TTS engine.
- Convert each narration text into an audio file.
- Save the output in a predictable location.
- Handle failures gracefully.

### 7. Visual Generation Service
- Integrate a local image or video generation tool.
- Send scene prompts to the rendering tool.
- Save generated assets and link them to the specific scene.
- Support fallback behavior when generation fails.

### 8. Subtitle Service
- Generate subtitle lines from each narration segment.
- Format the text for FFmpeg overlay rendering.
- Align subtitle timing to the scene duration.

### 9. Video Assembly Service
- Implement FFmpeg-based assembly.
- Combine audio, visual clips, and subtitles.
- Synchronize timings and export the final MP4 file.
- Add logging for each step.

### 10. Storage and Metadata Layer
- Store generated media in a local temp directory first.
- Upload final artifacts to S3-compatible storage if configured.
- Store metadata in a structured format for later retrieval.

### 11. Queue and Background Processing
- Add asynchronous processing support for long-running tasks.
- Use a queue abstraction or simple background worker pattern.
- Ensure the API can return immediately while processing continues.

### 12. Observability and Reliability
- Add structured logs for each pipeline stage.
- Capture errors and retry logic.
- Add a dead-letter fallback or failure state.
- Expose health endpoints for monitoring.

## Suggested Implementation Order
1. Build the API endpoints.
2. Create the job and scene models.
3. Implement the orchestration service.
4. Implement the planning service.
5. Implement TTS.
6. Implement visual generation.
7. Implement video assembly.
8. Add storage and notifications.

## Implementation Rules
- Keep each service focused on one responsibility.
- Avoid mixing orchestration logic into rendering code.
- Use DTOs at API boundaries.
- Use explicit JSON schemas for model output.
- Ensure logs clearly identify which stage fails.
- Prefer simple, reliable implementation over premature optimization.

## Expected Result
When completed, the system should be able to accept a Vietnamese prompt, generate a structured plan, create audio and visuals, assemble a final video, and return a usable output to the user.
