# OpenPanel Java SDK

[![Maven Central](https://img.shields.io/maven-central/v/fr.idcapture/openpanel-java)](https://central.sonatype.com/artifact/fr.idcapture/openpanel-java)
[![Javadoc](https://javadoc.io/badge2/fr.idcapture/openpanel-java/javadoc.svg)](https://javadoc.io/doc/fr.idcapture/openpanel-java)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL--3.0-blue.svg)](https://www.gnu.org/licenses/agpl-3.0)

Java SDK for [OpenPanel Analytics](https://openpanel.dev) — server-side tracking for JVM backends.

This SDK targets **Java 11+** and is designed for backend applications (Spring Boot, Quarkus, Micronaut, plain Java, etc.).

> For Android/Kotlin applications, see the official [kotlin-sdk](https://github.com/Openpanel-dev/kotlin-sdk).

## Installation

### Maven

```xml
<dependency>
    <groupId>fr.idcapture</groupId>
    <artifactId>openpanel-java</artifactId>
    <version>0.3.0</version>
</dependency>
```

### Gradle (Groovy)

```groovy
implementation 'fr.idcapture:openpanel-java:0.3.0'
```

### Gradle (Kotlin DSL)

```kotlin
implementation("fr.idcapture:openpanel-java:0.3.0")
```

## Quick Start

```java
import fr.idcapture.openpanel.OpenPanel;
import fr.idcapture.openpanel.OpenPanelOptions;

OpenPanel op = OpenPanel.create(
    OpenPanelOptions.builder()
        .clientId("YOUR_CLIENT_ID")
        .clientSecret("YOUR_CLIENT_SECRET") // required for server-side calls
        .build()
);

// Track an event (fire-and-forget)
op.track("user_signed_up", Map.of("plan", "pro"));

// Shutdown cleanly on app stop
op.close();
```

All methods return `CompletableFuture<Void>`. You can ignore the future (fire-and-forget) or chain callbacks:

```java
op.track("purchase", Map.of("amount", 99.99))
  .exceptionally(e -> { logger.error("Tracking failed", e); return null; });
```

## API Reference

### Initialization

```java
OpenPanel op = OpenPanel.create(
    OpenPanelOptions.builder()
        .clientId("YOUR_CLIENT_ID")           // required
        .clientSecret("YOUR_CLIENT_SECRET")   // optional, but required server-side
        .apiUrl("https://api.openpanel.dev")  // optional, default shown
        .disabled(false)                       // optional, set true to mute all events
        .filter(name -> !name.startsWith("debug_")) // optional event filter
        .connectTimeoutSeconds(10)             // optional, default 10
        .readTimeoutSeconds(30)                // optional, default 30
        .build()
);
```

### Track

```java
// Minimal
op.track("page_viewed");

// With properties
op.track("button_clicked", Map.of("button_id", "submit_form"));

// With user
op.track("checkout_started", Map.of("cart_value", 149.99), "user123");

// With user + groups
op.track("report_exported",
    Map.of("format", "pdf"),
    "user123",
    List.of("org_acme")
);
```

### Identify

```java
// With custom properties only
op.identify("user123", Map.of("plan", "enterprise"));

// With standard fields + custom properties
op.identify("user123", "John", "Doe", "john@example.com", Map.of("tier", "premium"));
```

### Increment / Decrement

```java
op.increment("user123", "login_count", 1);
op.decrement("user123", "credits", 5);
```

### Groups

```java
// Create or update a group
op.group("org_acme", "company", "Acme Inc", Map.of("plan", "enterprise", "seats", 25));

// Assign a user to groups
op.assignGroup("user123", List.of("org_acme"));
```

Note: groups are **not** automatically attached to track events. Pass them explicitly on each `track()` call where needed.

### Global Properties

Properties set here are merged into every `track()` call. Caller properties take precedence over global ones.

```java
op.setGlobalProperties(Map.of("app_version", "2.1.0", "env", "production"));

// All subsequent track() calls will include app_version and env automatically
op.track("feature_used", Map.of("feature", "export"));
```

### Shutdown

```java
// Explicit
op.close();

// Or via try-with-resources
try (OpenPanel op = OpenPanel.create(options)) {
    op.track("startup");
}
```

## Self-Hosted OpenPanel

```java
OpenPanelOptions.builder()
    .clientId("YOUR_CLIENT_ID")
    .apiUrl("https://your-openpanel-instance.com")
    .build();
```

## Disabling Tracking

Useful for local development or testing — no HTTP calls are made:

```java
OpenPanelOptions.builder()
    .clientId("id")
    .disabled(true)
    .build();
```

## Filtering Events

```java
OpenPanelOptions.builder()
    .clientId("id")
    .filter(eventName -> !eventName.startsWith("internal_"))
    .build();
```

## Error Handling

On API errors (4xx/5xx), the `CompletableFuture` is completed exceptionally with an `HttpTracker.OpenPanelApiException` containing the HTTP status code:

```java
op.track("event")
  .exceptionally(e -> {
      if (e.getCause() instanceof HttpTracker.OpenPanelApiException apiEx) {
          logger.warn("OpenPanel returned {}", apiEx.getStatusCode());
      }
      return null;
  });
```

## Kotlin Interop

All property maps use JetBrains `@Nullable` type-use annotations (`Map<String, @Nullable Object>`), so Kotlin sees `Map<String, Any?>` natively — no `@Suppress("UNCHECKED_CAST")` needed.

```kotlin
op.track("checkout", mapOf("amount" to 49.99, "coupon" to null))
```

## Dependencies

| Dependency | Version | Scope |
|---|---|---|
| `com.squareup.okhttp3:okhttp` | 4.12.0 | compile |
| `com.fasterxml.jackson.core:jackson-databind` | 2.17.x | compile |
| `org.jetbrains:annotations` | 24.1.0 | compile |
| `org.junit.jupiter:junit-jupiter` | 5.10.x | test |
| `com.squareup.okhttp3:mockwebserver` | 4.12.0 | test |

## Java / Kotlin SDK Comparison

|  | [kotlin-sdk](https://github.com/Openpanel-dev/kotlin-sdk) | openpanel-java-sdk (this) |
|---|---|---|
| Target | Android (Kotlin) | JVM backends (Java 11+) |
| Android Context | Required | N/A |
| Device/screen info | Auto-collected | N/A |
| App lifecycle | Auto-tracked | N/A |
| Async | Coroutines | `CompletableFuture` |
| Nullable map values | N/A | `@Nullable` type-use annotations |
| Maven artifact | `dev.openpanel:openpanel` | `fr.idcapture:openpanel-java` |

## License

[AGPL-3.0](LICENSE)
