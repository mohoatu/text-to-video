# Prompt 11: Queue and Background Processing

Implement asynchronous processing for long-running video generation jobs.

Requirements:
- Add support for background processing so the API can return quickly.
- Implement a queue abstraction or simple worker pattern for jobs.
- Ensure job status updates from PENDING to PROCESSING to COMPLETED or FAILED.
- Keep the processing loop simple and reliable for MVP usage.

Deliverables:
- background worker or queue integration
- async job processing flow

Constraints:
- Avoid over-engineering.
- Keep the worker separate from the API controller logic.
