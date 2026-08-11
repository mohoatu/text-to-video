# Prompt 03: Domain Model

Implement the core domain models for the video generation workflow.

Requirements:
- Create a Job model with fields such as id, status, prompt, createdAt, updatedAt, outputUrl, and errorMessage.
- Create a Scene model with fields such as id, jobId, sceneIndex, narrationText, visualPrompt, durationHint, audioFilePath, and visualFilePath.
- Create a GenerationResult model containing success flag, outputPath, metadata, and errorMessage.
- Use Lombok annotations if appropriate.
- Make the models simple, serializable, and suitable for future persistence.

Deliverables:
- Job model
- Scene model
- GenerationResult model

Constraints:
- Keep the models minimal and consistent.
- Avoid unnecessary complexity.
