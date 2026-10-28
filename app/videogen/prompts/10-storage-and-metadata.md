# Prompt 10: Storage and Metadata

Implement storage and metadata handling for generated assets and job records.

Requirements:
- Create a storage abstraction for saving intermediate and final assets.
- Store output files in a local working directory first.
- Optionally support an S3-compatible upload path if configuration is present.
- Persist job metadata in a structured way for later retrieval.
- Keep file naming consistent and predictable.

Deliverables:
- storage service
- metadata persistence support
- asset file handling

Constraints:
- Keep the code modular and easy to extend later.
- Avoid hard dependency on cloud services for the MVP.
