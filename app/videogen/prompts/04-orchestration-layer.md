# Prompt 04: Orchestration Layer

Implement the workflow orchestration layer for the video generation pipeline.

Requirements:
- Create a service that coordinates the full sequence of operations for a video generation job.
- Manage state transitions for statuses such as PENDING, PROCESSING, COMPLETED, and FAILED.
- Call the downstream services in the correct order: planning -> TTS -> visuals -> assembly -> storage.
- Provide a simple orchestration contract that can later be expanded into a queue worker.
- Return a result object or update the job state after each stage.

Deliverables:
- orchestration service
- job lifecycle handling
- stage-by-stage workflow execution

Constraints:
- Keep the orchestration logic clear and modular.
- Do not embed media logic directly inside the controller.
