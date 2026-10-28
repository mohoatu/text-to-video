# Prompt 02: API Layer

Implement the initial API layer for the video generation platform.

Requirements:
- Create DTO classes for request and response payloads.
- Add a controller with a POST endpoint for creating a new video generation job.
- Add a GET endpoint for retrieving job status by ID.
- Add a GET endpoint for retrieving output information for a completed job.
- Use simple in-memory or stubbed service logic first if needed.
- Return clear JSON responses.

Deliverables:
- controller class
- request/response DTOs
- working endpoints for create and retrieve

Constraints:
- Keep the API simple and easy to test.
- Use validation annotations where appropriate.
- Do not integrate the full pipeline yet.
