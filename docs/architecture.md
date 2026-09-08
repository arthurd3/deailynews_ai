# Architecture

## Shape

One deployable artifact, four modules with enforced boundaries. Each module is a direct sub-package
of `com.arthur.newsbrief`; anything under a module's `internal` package is invisible to its
siblings. Spring Modulith checks that at build time in `ModularityTests`, so a violation is a
failing test rather than a review comment someone might miss.

```
com.arthur.newsbrief
├── shared/           open module — everyone may use it
├── news/             port: NewsProvider
├── summarization/    port: SummaryGenerator
└── brief/            composes the two, publishes the API
```

`news` and `summarization` have no edge between them. The summarizer takes its own
`SourceDocument` type instead of `news.Article`, which keeps them independent siblings rather
than a chain. `brief` maps between the two. That mapping is a few lines and buys the freedom to
replace either upstream without touching the other.

The generated C4 diagram in [`modules/components.puml`](modules) confirms this: two edges out of
`brief`, none between `news` and `summarization`.

## Request walkthrough

`GET /api/v1/briefs/daily`

1. **`NewsBriefController`** validates `country` and `category`, then resolves the configured
   default country. The query is fully specified from this point on — which matters, because it
   becomes a cache key.
2. **`NewsBriefService`** checks the `briefs` cache. A hit returns immediately.
3. **`NewsApiNewsProvider`** checks the `headlines` cache, then calls NewsAPI through a declarative
   `@HttpExchange` client. It maps the wire format to `Article`, drops withdrawn `[Removed]`
   placeholders, and translates failures into `NewsUnavailableException` with a transient/terminal
   flag.
4. **`SpringAiSummaryGenerator`** caps the article list, renders the prompt template, and asks the
   model for a `NewsSummary` — as typed structured output, not free text.
5. **`NewsBriefService`** assembles a `DailyBrief` including provenance and a timestamp.
6. The controller returns it as JSON, or hands it to a Thymeleaf template if the caller asked for
   `text/html`.

Failures anywhere become RFC 9457 problem details via `ApiExceptionHandler`.

## Interception order

Three proxies wrap the adapter methods, and the order is deliberate:

```
@Retryable  →  @Cacheable  →  @CircuitBreaker  →  the actual call
```

- **Caching ahead of the breaker.** A cache hit must not travel through the circuit breaker,
  because an open circuit would then suppress results the application already holds. Serving cached
  data during an outage is the entire point. `HeadlinesCircuitBreakerTest` pins this.
- **Retry outermost.** Spring's retry interceptor is installed by a `BeanPostProcessor`, so it wraps
  the advisor chain rather than joining it. This is harmless — a cache hit does not throw, so there
  is nothing for retry to act on.

## Resilience

| Failure | Response |
| --- | --- |
| Connect/read timeout | Retried twice with exponential backoff and jitter |
| NewsAPI 5xx or 429 | Retried |
| NewsAPI 401/403 or malformed request | **Not** retried — repeating it only burns quota |
| Repeated failures | Circuit opens; further calls fail fast with a 503 |
| Model unreachable or off-schema | `SummarizationException` → 503 |

Timeouts are set explicitly on the NewsAPI client. An unbounded HTTP client will hold a request
thread for as long as the upstream cares to stall, which is the failure mode that takes a service
down under load.

Virtual threads are enabled because nearly all wall-clock time here is spent waiting on two network
calls.

## Caching

Two caches with different economics:

| Cache | TTL | Why |
| --- | --- | --- |
| `headlines` | 5 min | One cheap HTTP call, but news moves |
| `briefs` | 30 min | Several seconds of local inference — worth holding |

Both are keyed on the fully resolved `HeadlinesQuery` record, which gets correct `equals`/`hashCode`
for free.

## Security notes

- The NewsAPI key is sent as an `X-Api-Key` header, never as a query parameter, so it cannot reach
  an access log.
- The catch-all exception handler logs the cause and returns a generic message. An unexpected stack
  trace can carry connection strings.
- The HTML representation renders a typed `DailyBrief` through a Thymeleaf template, which escapes
  its inputs. Injecting model-authored HTML directly into a response would be a stored-XSS vector.
- `.env` is git-ignored and untracked. Deployments use real environment variables.

## Testing strategy

Ports make most tests cheap. `NewsBriefServiceTest` uses two hand-written fakes and needs no Spring
context at all. Only the adapters — the classes that actually speak HTTP — need WireMock. The build
runs the whole suite offline.
