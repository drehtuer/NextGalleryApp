## What and why

<!-- Short description of the change and the reason for it. -->

## Checklist

- [ ] Unit tests added for every new or changed function (one behaviour per test, no overlap between layers)
- [ ] Logic lives in the pure Kotlin/JVM modules; Android adapters only map data (D25)
- [ ] Docs updated (`README.md`, `docs/`), TODO items ticked
- [ ] `./gradlew ktlintCheck detekt lint test` passes
- [ ] Instrumented tests run locally (`./gradlew connectedDebugAndroidTest`) if an adapter changed
