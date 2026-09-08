# 2. Call Ollama through Spring AI, and ask for structured output

- **Status:** Accepted
- **Date:** 2026-09-08

## Context

The original integration hand-rolled the Ollama wire format: `OllamaRequest` and `OllamaResponse`
records, a `RestTemplate` built per call, and prompt text concatenated inline in Java. It asked the
model for a full HTML page and the controller recovered it with
`summary.substring(7, length - 3)` — stripping an assumed ```` ```html ```` fence by character
offset. When the model wrapped its reply differently, or the cached plain-text summary was returned
instead, that produced mangled output or a `StringIndexOutOfBoundsException`.

## Decision

Use Spring AI's `ChatClient`, and request a typed `NewsSummary` rather than text.

Spring AI derives a JSON schema from the record, appends it to the prompt, and parses the reply
back into an instance. Prompts move to `resources/prompts/*.st` as plain text.

## Consequences

- The fence-stripping hack is gone, along with the class of bug it represented: there is no prose
  to scrape, only data to bind.
- JSON and HTML render from the same typed value, so the two representations cannot disagree.
- Rendering through a Thymeleaf template escapes its inputs, removing the stored-XSS exposure of
  returning model-authored HTML verbatim.
- Roughly eighty lines of HTTP plumbing were deleted.
- Switching to a hosted provider becomes a dependency and a configuration change; `SummaryGenerator`
  does not move.
- The application now depends on Spring AI's release cadence, and structured output depends on the
  model honouring the schema. A smaller model may fail to comply — the fallback is free-text
  summaries behind the same port, which is why the port exists.
