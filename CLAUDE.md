# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

Java SDK for [OpenPanel Analytics](https://openpanel.dev) — server-side event tracking for JVM backends (Java 11+). Published on Maven Central as `fr.idcapture:openpanel-java`.

## Build & Test Commands

```bash
mvn compile              # Compile
mvn test                 # Run all tests
mvn test -Dtest=OpenPanelTest              # Run a single test class
mvn test -Dtest=OpenPanelTest#options_defaults  # Run a single test method
mvn package              # Build JAR (includes tests)
mvn install              # Install to local Maven repo
mvn clean deploy -Prelease  # Publish to Maven Central (requires GPG + Central Portal token)
```

## Architecture

All events go through a single HTTP endpoint (`POST /track`) with a `type` discriminator.

**Public API layer:**
- `OpenPanel` — main entry point. Thread-safe facade that delegates to `HttpTracker`. Implements `AutoCloseable`. Created via `OpenPanel.create(options)` or `OpenPanel.builder()`.
- `OpenPanelOptions` — immutable config (builder pattern). Holds clientId, clientSecret, apiUrl, disabled flag, event filter predicate, timeouts, retry config, verbose flag.

**Internal layer:**
- `HttpTracker` — async HTTP client (OkHttp). Wraps every payload in `{"type": "...", "payload": {...}}` and POSTs to `{apiUrl}/track`. Returns `CompletableFuture<Void>`. Non-2xx responses throw `OpenPanelApiException`. Retries 5xx/network errors with exponential backoff (configurable). Sends `openpanel-sdk-name`/`openpanel-sdk-version` headers.

**Model layer (`fr.idcapture.openpanel.model`):**
- Immutable payload POJOs: `TrackPayload`, `IdentifyPayload`, `IncrementPayload`, `DecrementPayload`, `GroupPayload`, `AssignGroupPayload`. Each validates required fields in its constructor. Serialized by Jackson (nulls excluded via `@JsonInclude(NON_NULL)`).

**Key behaviors:**
- `disabled=true` short-circuits all methods to return completed futures (no HTTP).
- `filter` predicate on `track()` only — returns completed future if predicate rejects.
- Global properties merge into `track()` calls; caller properties win on conflict.
- Groups are NOT auto-attached to track events — must be passed explicitly each time.
- `revenue()` is a shorthand that tracks a "revenue" event with a `__revenue` property.
- `identify()` supports an optional `avatar` URL field.
- `verbose=true` enables debug logging via `java.util.logging`.
- Retry: 3 attempts by default (500ms → 1s → 2s), only on 5xx/network errors. 4xx are not retried.

## Kotlin Interop

All `Map<String, Object>` in the public API use JetBrains `@Nullable` type-use annotations (`Map<String, @Nullable Object>`) so Kotlin sees `Map<String, Any?>` natively — no `@Suppress("UNCHECKED_CAST")` needed. The `org.jetbrains:annotations` dependency is compile-scope (transitive).

## Dependencies

- **OkHttp 4.12** — HTTP client + MockWebServer for tests
- **Jackson 2.17** — JSON serialization
- **JetBrains Annotations 24.1** — `@Nullable` type-use annotations for Kotlin interop
- **JUnit 5** — test framework

## Test Structure

- `OpenPanelTest` — unit tests for options builder, disabled mode, filter, global properties, model validation
- `HttpTrackerTest` — integration tests using MockWebServer to verify HTTP requests (headers, path, JSON body structure, error handling, retry behavior)
