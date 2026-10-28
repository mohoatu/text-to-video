# Prompt 12: Observability and Reliability

Implement observability and reliability improvements for the application.

Requirements:
- Add structured logging around each pipeline stage.
- Capture errors with clear context and stage names.
- Add retry logic for temporary failures where appropriate.
- Add a dead-letter or failure state handling strategy.
- Expose health and readiness endpoints for monitoring.

Deliverables:
- logging and error handling improvements
- reliability patterns
- monitoring-friendly health endpoints

Constraints:
- Keep the implementation practical for MVP deployment.
- Make logs easy to read and useful during debugging.
