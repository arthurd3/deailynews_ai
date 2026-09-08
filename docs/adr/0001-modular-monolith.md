# 1. Enforce module boundaries with Spring Modulith

- **Status:** Accepted
- **Date:** 2026-09-08

## Context

The service began as a Spring Initializr scaffold organized by technical layer: `controller`,
`service`, `client`, `dto`. That arrangement scatters one feature across four packages and says
nothing about what may depend on what. Nothing prevented the controller from reaching into the
NewsAPI wire format, and in practice it did — it received a provider-shaped string and sliced
characters off it.

We considered three options:

1. Keep the layered packages and just fix the defects.
2. Split into a Maven multi-module build for compile-time isolation.
3. Package by feature, with boundaries enforced at build time.

## Decision

Option 3. Modules are direct sub-packages of the application package, each exposing a small public
API and hiding the rest under `internal`. Spring Modulith verifies the rules in `ModularityTests`.

A Maven multi-module build would give harder isolation, but for roughly fifteen classes the
ceremony and slower builds outweigh the benefit — and Modulith's rules can be tightened later, or
a module extracted into its own artifact, without rewriting anything.

## Consequences

- A cross-module reach into `internal` fails the build with the offending import named.
- C4 diagrams and module canvases are generated from the code, so documentation cannot drift.
- Feature work touches one directory instead of four.
- `shared` must be declared an open module, or the feature modules cannot use the cache and error
  infrastructure. Keeping anything feature-specific out of `shared` is a discipline the tooling
  does not enforce.
