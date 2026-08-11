# Prompt 05: Prompt Planning Service

Implement the scene planning service that turns a user prompt into a structured scene plan.

Requirements:
- Create a service that accepts a prompt and returns a list of scenes.
- Use a Vietnamese system prompt instructing the model to produce structured JSON.
- Require the output to contain fields such as sceneIndex, narrationText, visualPrompt, and durationHint.
- Validate the LLM response and map it into Java objects.
- Support a local LLM integration path using Ollama or LM Studio.

Deliverables:
- planning service
- prompt template
- JSON schema validation logic

Constraints:
- Keep the model output deterministic and structured.
- Make the implementation easy to replace later with a different model provider.
