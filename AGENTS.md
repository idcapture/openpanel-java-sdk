# AGENTS.md

This file provides guidance to AI coding agents (Claude Code, Codex, Gemini, Cursor, Windsurf, Copilot, etc.) working in this repository.

## Primary Guidance

Read [`CLAUDE.md`](CLAUDE.md) for comprehensive instructions covering:

- Project overview and Maven coordinates (`fr.idcapture:openpanel-java`)
- Build, test, and publish commands
- Architecture (public API, internal HTTP layer, model layer)
- Key behaviors (disabled mode, filtering, global properties, retry, verbose logging)
- Kotlin interop via `@Nullable` type-use annotations
- Dependencies and test structure

## Code Conventions

- **Java 11** language level — no records, sealed classes, or text blocks
- **Immutable models** with constructor validation and `@JsonInclude(NON_NULL)`
- **`@Nullable`** type-use annotations on all `Map<String, Object>` values for Kotlin interop
- **`CompletableFuture<Void>`** return type for all async public methods
- **Jackson** for JSON serialization, **OkHttp** for HTTP
- **JUnit 5** + **MockWebServer** for testing — no external services required

## Project Structure

| Path | Purpose |
|---|---|
| `src/main/java/fr/idcapture/openpanel/` | Public API (`OpenPanel`, `OpenPanelOptions`) |
| `src/main/java/fr/idcapture/openpanel/internal/` | Internal HTTP layer (`HttpTracker`) — not public API |
| `src/main/java/fr/idcapture/openpanel/model/` | Immutable payload POJOs |
| `src/test/java/fr/idcapture/openpanel/` | Unit and integration tests |
| `pom.xml` | Maven config with `release` profile for Central publishing |

## Publishing

Version must be updated in **two places** before a release:

1. `pom.xml` — `<version>` element
2. `HttpTracker.java` — `SDK_VERSION` constant

Deploy via `mvn clean deploy -Prelease` (requires GPG key + Central Portal token).
