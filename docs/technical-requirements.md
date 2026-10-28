# Technical Requirements Document

## 1. Purpose
This document specifies the technical requirements for implementing a local-first, Vietnamese-language AI text-to-video generation platform. It defines the architecture, component responsibilities, integration points, and implementation constraints for the MVP.

## 2. Project Scope
The MVP will include:
- A backend service for receiving generation requests
- An orchestration layer for prompt processing and workflow execution
- Local AI inference for script and scene generation
- TTS conversion for Vietnamese narration
- Local media generation and video assembly
- Final MP4 export with subtitles

## 3. Proposed Architecture
The solution will use the following components:
- Java Spring Boot for backend services
- Spring AI for orchestration and model abstraction
- Ollama or LM Studio as the local LLM inference layer
- FFmpeg for editing, audio/video mixing, and subtitle rendering
- ComfyUI or Draw Things for local image/video generation
- S3-compatible storage for intermediate and final assets
- Optional queue-based asynchronous processing

## 4. Functional Requirements
### 4.1 Request Handling
- Expose a REST API for receiving video generation requests.
- Accept Vietnamese text prompts as input.
- Return a job identifier and initial status.

### 4.2 Pipeline Orchestration
The system must execute the following workflow:
1. Analyze the user prompt and generate a script.
2. Break the script into structured scenes in JSON format.
3. Convert narration text to Vietnamese audio.
4. Generate visuals or short clips for each scene.
5. Assemble the final video using FFmpeg.
6. Render subtitles and export the MP4 file.

### 4.3 Job Management
- Support asynchronous execution with job state tracking.
- Store metadata for each job, input prompt, assets, and output file.
- Provide endpoints for query status and retrieval of resulting assets.

## 5. AI and Language Model Requirements
- Local LLM inference must be supported through Ollama or LM Studio.
- The system must support configurable model selection through application settings.
- Recommended model categories include:
  - Script generation: Qwen, Llama, or Mistral-based models
  - Vietnamese understanding: multilingual instruction-tuned models

## 6. TTS Requirements
- Support Vietnamese speech synthesis.
- Output audio in MP3 or WAV format.
- Enable control over speed, punctuation handling, and voice selection.

## 7. Media and Video Requirements
- Support local image/video generation through ComfyUI or equivalent tools.
- Accept structured scene prompts produced by the orchestration layer.
- Support short-form video clips and basic fallback behavior on generation failure.

## 8. Editing and Export Requirements
- FFmpeg must concatenate clips, mix audio, and synchronize timing.
- Subtitles must be generated in Vietnamese and burned into the output video.
- The final output format must be MP4 with standard encoding settings.

## 9. Data and Storage Requirements
- Store requests, jobs, generated files, and metadata.
- Preserve intermediate assets for debugging and reprocessing.
- Use structured JSON for scene definitions and workflow state.

## 10. Non-Functional Requirements
- Deployable on local hardware with minimal cloud dependency
- Clear error handling, retries, and logging
- Modular architecture adaptable to future cloud integration
- Configurable endpoints and model settings

## 11. Deployment and Environment Requirements
- Support local development on macOS and Windows/Linux environments.
- Optional Docker packaging for reproducibility and portability.
- The system must operate without requiring a cloud AI provider for the MVP.

## 12. Deliverables
- Backend API service
- Local AI orchestration workflow
- TTS integration
- FFmpeg-based video assembly pipeline
- Basic web or API-based user entry point
