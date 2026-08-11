# Prompt 07: Visual Generation Service

Implement the visual generation service for scene assets.

Requirements:
- Create a service that accepts a visual prompt and produces an image or short video clip.
- Integrate a local tool such as ComfyUI or Draw Things if possible.
- Save the generated output to a local working directory.
- Return metadata including file path and generation status.
- Provide a fallback strategy if generation fails.

Deliverables:
- visual generation service
- generated asset handling
- basic fallback logic

Constraints:
- Keep the interface simple and modular.
- Avoid hard-coding environment-specific details where possible.
