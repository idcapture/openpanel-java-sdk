# Contributing

Thanks for your interest in contributing to the OpenPanel Java SDK!

## Prerequisites

- Java 11+
- Maven 3.8+

## Setup

```bash
git clone https://github.com/idcapture/openpanel-java-sdk.git
cd openpanel-java-sdk
mvn compile
```

## Running Tests

```bash
mvn test                                        # all tests
mvn test -Dtest=OpenPanelTest                   # single class
mvn test -Dtest=HttpTrackerTest#retry_4xx_noRetry  # single method
```

Tests use OkHttp MockWebServer — no external services needed.

## Making Changes

1. Fork the repo and create a branch from `main`
2. Write code and add tests for any new functionality
3. Run `mvn test` and make sure all tests pass
4. Run `mvn package` to verify the build (includes javadoc generation)
5. Open a pull request against `main`

## Code Style

- Java 11 language level (no records, no sealed classes)
- Immutable model classes with constructor validation
- `@JsonInclude(NON_NULL)` on payloads
- `@Nullable` type-use annotations on map values for Kotlin interop
- `CompletableFuture<Void>` for all async methods

## Releasing

Releases are published to Maven Central by maintainers:

```bash
# Update version in pom.xml and HttpTracker.SDK_VERSION
mvn clean deploy -Prelease
```

Requires GPG key and Central Portal token in `~/.m2/settings.xml`.

## License

By contributing, you agree that your contributions will be licensed under [AGPL-3.0](LICENSE).
