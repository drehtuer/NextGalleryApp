# Design decisions and open questions

This document records the design gaps found in the [README](../README.md) and how they are resolved.
**Decided** items are the baseline for [ARCHITECTURE.md](ARCHITECTURE.md) and [TODO.md](TODO.md). They can still be
changed, but the change must be made in all three documents. **Open** items need an answer from the project owner
before the phase that depends on them starts.

## Decided (proposed defaults)

| # | Topic | Gap in README | Decision |
| --- | --- | --- | --- |
| D1 | Architecture pattern | Not specified | MVVM with unidirectional data flow. ViewModels expose `StateFlow<UiState>`; repositories are the single source of truth. |
| D2 | Dependency injection | Not specified | Hilt (incl. `HiltWorker` for WorkManager). Pure JVM modules use only `javax.inject` annotations; their Hilt bindings live in Android modules (D25). |
| D3 | Modularisation | Not specified | Multi-module Gradle build (`:app`, `:core:*`, `:feature:*`) with a version catalog and convention plugins in `build-logic/`. `:core:model`, `:core:common`, `:core:data` and `:core:nextcloud` are pure Kotlin/JVM modules (D25). |
| D4 | Navigation | Not specified | Navigation Compose with type-safe routes (kotlinx.serialization). |
| D5 | Grid loading | "Lazy loading" only | Paging 3 backed by Room `PagingSource`. |
| D6 | Index sync | Room *and* MediaStore, but no sync strategy | Full scan on first start, then incremental sync: `GENERATION_MODIFIED` (deletions found by comparing ID sets). Triggered by a debounced `ContentObserver` and on app start. `.nomedia` is respected. |
| D7 | Timeline date source | Not specified | `DATE_TAKEN` → EXIF `DateTimeOriginal` → `DATE_MODIFIED`, grouped in the device's local time zone. |
| D8 | Media types | "Images" only | v1 supports images only (JPEG, PNG, WebP, HEIC/HEIF, static GIF). No video. |
| D9 | Viewer zoom / subsampling | "Subsampling" without a library | Telephoto (`zoomable-image-coil`), because it is Compose-native and uses Coil. |
| D10 | Delete and overwrite | README promises trash | Delete uses `createTrashRequest` (undoable); permanent delete via `createDeleteRequest` as a secondary action. Overwriting an original uses `createWriteRequest`. All go through the system confirmation dialog (was O10). |
| D11 | Credential storage | "Keystore-encrypted storage" (`EncryptedSharedPreferences` is deprecated) | DataStore encrypted with Tink AEAD, with the key held in the Android Keystore. |
| D12 | Editor model | Not specified | Non-destructive: a list of `EditOperation` with undo/redo. The preview renders on a downsampled bitmap; saving renders at full resolution. EXIF is copied, orientation is reset to normal and dimensions are updated. |
| D13 | Remote ↔ local mapping | "Relative path" is undefined | Each included local folder maps to `<remoteRoot>/<folder relative path>`. "Uploaded" means the same name and size exist there; checksums only confirm a match when the server provides them. |
| D14 | Nextcloud accounts | Not specified | One account in v1. |
| D15 | Partial media access | Not mentioned | Support `READ_MEDIA_VISUAL_USER_SELECTED`: show a "limited access" banner with an option to grant more. |
| D16 | Location redaction | Not mentioned | Use `MediaStore.setRequireOriginal()` when reading EXIF GPS (requires `ACCESS_MEDIA_LOCATION`). |
| D17 | Testing stack | Not specified | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric (4.17 or later, the first release with SDK 37 support; needs JDK 21), in-memory Room, OkHttp MockWebServer, Compose UI tests. Behaviour Robolectric cannot reproduce (real MediaStore, Android Keystore, HEIC decoding, system dialogs, map rendering) is tested with instrumented tests (`src/androidTest`). CI runs only JVM tests (plain JUnit and Robolectric); instrumented tests run locally on an API 37 emulator or the Pixel 10a. |
| D18 | Code style / CI | Not specified | ktlint (configured via `.editorconfig`) and detekt; GitHub Actions runs build, unit tests and lint. |
| D19 | Transport security | Not specified | HTTPS only (cleartext disallowed via `network_security_config.xml`; cleartext allowed in debug builds only). |
| D20 | Tech stack | Listed as "planned" | Confirmed by the project owner: Kotlin, Jetpack Compose (Material 3 components) and Room as the local database. |
| D21 | Target device and SDK levels | "Target the latest SDK" only | Reference device is a **Pixel 10a on Android 17**. `minSdk`, `compileSdk` and `targetSdk` are all 37 (Android 17); older Android versions are not supported. The platform APIs the app needs are therefore always available, so there are no API-level branches. Manual testing and performance acceptance run on the Pixel 10a. |
| D22 | Visual design | Not specified | The visual design will be delivered later from Claude Design. Until then screens use stock Material 3 with a placeholder theme. All colours, typography, shapes and spacing come from `NextGalleryTheme` in `:core:ui`; feature modules never hard-code visual values, so the design can be dropped in without touching features. |
| D23 | Map library and tiles | osmdroid named; tile source not specified (was O2, O3) | OpenStreetMap via osmdroid behind a `MapRenderer` interface (MapLibre stays the fallback if osmdroid causes problems). Default tile source is the OSM tile server with a proper User-Agent; the tile URL is configurable in settings. |
| D24 | Performance targets | "Fast" without numbers (was O14) | 50 000 images on the Pixel 10a: no dropped frames when scrolling or fast-scrolling at the display's refresh rate, warm start under 1 s to the first grid. |
| D25 | Android-independent core | Not specified | Business logic is kept free of Android so most tests run as plain JVM tests on CI. Repositories and all domain logic live in pure Kotlin/JVM modules (`:core:data`, `:core:nextcloud`, `:core:model`, `:core:common`). Android APIs (Room, MediaStore, EXIF, bitmaps, WorkManager, Keystore) sit behind interfaces (ports) declared in `:core:data` and implemented by thin adapter modules (`:core:database`, `:core:media`, `:core:platform`) that only map data. ViewModels use no `android.*` types: routes are passed through Hilt assisted injection, and system dialogs and rendered images cross the boundary as opaque handles (`PlatformConfirmation`, `PreviewImage`). The build enforces the split, because a JVM module cannot depend on an Android library. |

## Open questions

| # | Question | Why it matters | Suggested answer |
| --- | --- | --- | --- |
| O1 | **License?** (README says TBD) | Decides which dependencies can be used and whether F-Droid is possible. | GPL-3.0-or-later (fits a no-tracking, F-Droid-friendly app) or Apache-2.0. |
| O4 | **Distribution:** F-Droid, Play Store, or both? | Affects the build flavours, signing and the dependency audit (no proprietary libraries). | F-Droid first; Play Store optional. |
| O5 | **Coexistence with the official Nextcloud app's auto-upload.** Should the badge also detect files that the Nextcloud app uploaded under a different path or name (e.g. its "rename by date" option)? | Path + size matching misses renamed uploads, so the badge would show "not uploaded" for files that are on the server. | v1: path + size only, with a configurable remote root. Later: an optional checksum-only scan of a chosen remote folder. |
| O6 | **Upload conflicts:** a remote file has the same name but a different size. Skip, overwrite or rename? | Without a rule, uploads can overwrite or duplicate files. | Rename with a suffix (`IMG_1234 (1).jpg`) and never overwrite silently. |
| O7 | **Large uploads:** support Nextcloud chunked upload (v2)? | A single `PUT` fails for very large files or on flaky networks. | Single `PUT` in v1. Add chunking if needed. |
| O8 | **Remote viewer caching:** keep downloaded originals for offline use? | Affects storage use and the cache-eviction policy. | Size-limited disk cache only (e.g. 500 MB, LRU), with no "make available offline" option. |
| O9 | **Self-signed certificates / custom CAs** for self-hosted servers? | Many self-hosters use them, and Android rejects them by default. | Support user-installed CAs via the network security config. Don't build an in-app "trust this certificate" flow in v1. |
| O11 | **Editing formats:** how to save edits of HEIC, PNG and GIF? | Android cannot encode HEIC everywhere, and a GIF loses its animation. | Save JPEG for JPEG and HEIC sources and PNG for PNG sources. Offer no editing for GIF. |
| O12 | **Selection across views:** should a multi-selection survive switching between Folders and Timeline? | Affects where the selection state lives (per screen or app-wide). | Per screen. Clear the selection when leaving the screen. |
| O13 | **Localisation and theming:** which languages, and Material You dynamic colour? | Affects the resource setup and the theme. | English + German. Dynamic colour depends on the Claude Design delivery (D22): use it only if the design allows it, otherwise the design's fixed palette. |
| O15 | **Application ID / package name?** | Needed for module namespaces in Phase 0, and hard to change after release. | `de.drehtuer.nextgallery` (placeholder used in ARCHITECTURE.md and TODO.md). |
| O16 | **Logout behaviour:** revoke the app password on the server, or only forget it locally? | Revoking also removes the device from the user's Nextcloud security settings. | Revoke on the server (`DELETE /ocs/v2.php/core/apppassword`), then wipe local credentials and caches. |
| O17 | **Editor memory budget:** what happens when a full-resolution bitmap does not fit in memory (e.g. 100 MP images)? | Saving at full resolution can cause an OOM on low-memory devices. | Warn the user and save at the largest size within ~1/4 of `Runtime.maxMemory()`. Tiled rendering is a later option. |
