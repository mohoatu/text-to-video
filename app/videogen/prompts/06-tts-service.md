# Prompt 06: TTS Service

Implement a text-to-speech service for Vietnamese narration.

Requirements:
- Create a service that takes narration text and produces an audio file.
- Support a local TTS engine such as Piper or Coqui TTS.
- Save the output audio to a local working directory and then to storage if available.
- Keep the implementation simple and compatible with a later cloud fallback.
- Handle basic errors and return useful metadata.

Deliverables:
- TTS service class
- audio generation output
- simple error handling

Constraints:
- Focus on correctness and simplicity.
- Use a clear file naming convention.
