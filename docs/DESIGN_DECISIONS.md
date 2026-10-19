# Design decisions and open questions

This document records the design gaps found in the [README](../README.md) and how they are resolved.
**Decided** items are the baseline for [ARCHITECTURE.md](ARCHITECTURE.md) and [TODO.md](TODO.md). They can still be
changed, but the change must be made in all three documents. **Open** items need an answer from the project owner
before the phase that depends on them starts.

## Decided (proposed defaults)

| # | Topic | Gap in README | Decision |
|---|---|---|---|
| D1 | Architecture pattern | Not specified | MVVM with unidirectional data flow. ViewModels expose `StateFlow<UiState>`; repositories are the single source of truth. |
| D2 | Dependency injection | Not specified | Hilt (incl. `HiltWorker` for WorkManager). |
| D3 | Modularisation | Not specified | Multi-module Gradle build (`:app`, `:core:*`, `:feature:*`) with a version catalog and convention plugins in `build-logic/`. |
| D4 | Navigation | Not specified | Navigation Compose with type-safe routes (kotlinx.serialization). |
| D5 | Grid loading | "Lazy loading" only | Paging 3 backed by Room `PagingSource`. |
| D6 | Index sync | Room *and* MediaStore, but no sync strategy | Full scan on first start, then incremental sync: `GENERATION_MODIFIED` on API 30+, `DATE_MODIFIED` on API 29. Triggered by a debounced `ContentObserver` and on app start. `.nomedia` is respected. |
| D7 | Timeline date source | Not specified | `DATE_TAKEN` → EXIF `DateTimeOriginal` → `DATE_MODIFIED`, grouped in the device's local time zone. |
| D8 | Media types | "Images" only | v1 supports images only (JPEG, PNG, WebP, HEIC/HEIF, static GIF). No video. |
| D9 | Viewer zoom / subsampling | "Subsampling" without a library | Telephoto (`zoomable-image-coil`), because it is Compose-native and uses Coil. |
| D10 | Delete on Android 10 | README promises trash, but `createTrashRequest` needs API 30 while min SDK is 29 | API 30+: system trash (undoable). API 29: permanent delete via the `RecoverableSecurityException` flow, after an in-app confirmation. |
| D11 | Credential storage | "Keystore-encrypted storage" (`EncryptedSharedPreferences` is deprecated) | DataStore encrypted with Tink AEAD, with the key held in the Android Keystore. |
| D12 | Editor model | Not specified | Non-destructive: a list of `EditOperation` with undo/redo. The preview renders on a downsampled bitmap; saving renders at full resolution. EXIF is copied, orientation is reset to normal and dimensions are updated. |
| D13 | Remote ↔ local mapping | "Relative path" is undefined | Each included local folder maps to `<remoteRoot>/<folder relative path>`. "Uploaded" means the same name and size exist there; checksums only confirm a match when the server provides them. |
| D14 | Nextcloud accounts | Not specified | One account in v1. |
| D15 | Android 14 partial access | Not mentioned | Support `READ_MEDIA_VISUAL_USER_SELECTED`: show a "limited access" banner with an option to grant more. |
| D16 | Location redaction | Not mentioned | Use `MediaStore.setRequireOriginal()` when reading EXIF GPS (requires `ACCESS_MEDIA_LOCATION`). |
| D17 | Testing stack | Not specified | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, in-memory Room, OkHttp MockWebServer, Compose UI tests. |
| D18 | Code style / CI | Not specified | ktlint (configured via `.editorconfig`) and detekt; GitHub Actions runs build, unit tests and lint. |

## Open questions

| # | Question | Why it matters | Suggested answer |
|---|---|---|---|
| O1 | **License?** (README says TBD) | Decides which dependencies can be used and whether F-Droid is possible. | GPL-3.0-or-later (fits a no-tracking, F-Droid-friendly app) or Apache-2.0. |
| O2 | **Map library:** osmdroid is no longer actively maintained. Keep it? | It is a long-term maintenance risk. Its View-based API also needs Compose interop. | Start with osmdroid behind a `MapRenderer` interface and switch to MapLibre if problems appear. |
| O3 | **Map tiles:** which tile server? | The OSM tile usage policy forbids heavy use and requires a proper User-Agent. Offline use is not covered. | Default to OSM tiles with a correct User-Agent and a configurable tile URL in settings. |
| O4 | **Distribution:** F-Droid, Play Store, or both? | Affects the build flavours, signing and the dependency audit (no proprietary libraries). | F-Droid first; Play Store optional. |
| O5 | **Coexistence with the official Nextcloud app's auto-upload.** Should the badge also detect files that the Nextcloud app uploaded under a different path or name (e.g. its "rename by date" option)? | Path + size matching misses renamed uploads, so the badge would show "not uploaded" for files that are on the server. | v1: path + size only, with a configurable remote root. Later: an optional checksum-only scan of a chosen remote folder. |
| O6 | **Upload conflicts:** a remote file has the same name but a different size. Skip, overwrite or rename? | Without a rule, uploads can overwrite or duplicate files. | Rename with a suffix (`IMG_1234 (1).jpg`) and never overwrite silently. |
| O7 | **Large uploads:** support Nextcloud chunked upload (v2)? | A single `PUT` fails for very large files or on flaky networks. | Single `PUT` in v1. Add chunking if needed. |
| O8 | **Remote viewer caching:** keep downloaded originals for offline use? | Affects storage use and the cache-eviction policy. | Size-limited disk cache only (e.g. 500 MB, LRU), with no "make available offline" option. |
| O9 | **Self-signed certificates / custom CAs** for self-hosted servers? | Many self-hosters use them, and Android rejects them by default. | Support user-installed CAs via the network security config. Don't build an in-app "trust this certificate" flow in v1. |
| O10 | **Overwrite original on API 29:** `createWriteRequest` needs API 30. | "Overwrite" cannot use the system dialog on Android 10. | Use `RecoverableSecurityException` on API 29, as for delete. |
| O11 | **Editing formats:** how to save edits of HEIC, PNG and GIF? | Android cannot encode HEIC everywhere, and a GIF loses its animation. | Save JPEG for JPEG and HEIC sources and PNG for PNG sources. Offer no editing for GIF. |
| O12 | **Selection across views:** should a multi-selection survive switching between Folders and Timeline? | Affects where the selection state lives (per screen or app-wide). | Per screen. Clear the selection when leaving the screen. |
| O13 | **Localisation and theming:** which languages, and Material You dynamic colour? | Affects the resource setup and the theme. | English + German, dynamic colour on Android 12+ with a static fallback theme. |
| O14 | **Scope of "fast" in numbers:** a target device and dataset? | Needed for performance acceptance tests. | 50 000 images on a mid-range device, at 60 fps scrolling and with a warm start under 1 s to the first grid. |
