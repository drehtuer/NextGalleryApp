# Implementation TODO

Ordered checklist for building NextGalleryApp, from an empty repo to a release build.
Phases 1–9 follow the [README roadmap](../README.md#roadmap). Module layout, layering and data flow
are described in [ARCHITECTURE.md](ARCHITECTURE.md); rationale and open questions are tracked in
[DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).

## Conventions

- **Architecture:** MVVM + unidirectional data flow. Each screen has a `@HiltViewModel` exposing
  `StateFlow<XxxUiState>` and accepting events via functions (`onEvent(XxxEvent)`). Repositories are
  the single source of truth; Room is the source of truth for the local index. Coroutines + Flow.
- **Layering inside a phase:** model → domain logic and ports (`:core:data`, JVM) → adapters (`:core:database`,
  `:core:media`, `:core:platform`) → ViewModel → UI. Tasks below are ordered that way.
- **Android-independent core (D25):** logic goes into the pure JVM modules and is tested with plain JUnit.
  Adapters only map between the platform and model types; if an adapter needs an `if`, the decision
  probably belongs in `:core:data`.
- **Dependencies:** `:feature:*` modules depend only on `:core:*` modules. Cross-feature navigation
  is wired in `:app`.
- **Testing rule:** every function gets a unit test; tests are simple and do not overlap. Each task
  that adds code lists its own test items. Tooling: JUnit 4, MockK, Turbine,
  kotlinx-coroutines-test, Robolectric, in-memory Room, MockWebServer, Compose UI tests. CI runs all JVM tests
  (plain and Robolectric); instrumented tests run locally on an API 37 emulator or the Pixel 10a.
- **Phase exit:** every phase ends with "update docs to match code" and green CI.
- Test file naming: `FooTest.kt` next to `Foo.kt` in the module's `src/test` (JVM/Robolectric) or
  `src/androidTest` (instrumented, only where Robolectric is insufficient; see ARCHITECTURE.md §13.1).

---

## Phase 0 – Repository & build setup

**Goal:** a building, linting, CI-checked multi-module Gradle project with empty modules.

- [x] **0.1 Gradle basics**
  - [x] Add Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), pin a current stable Gradle.
  - [x] `settings.gradle.kts`: `pluginManagement` (`includeBuild("build-logic")`), `dependencyResolutionManagement` (google, mavenCentral; `FAIL_ON_PROJECT_REPOS`), `rootProject.name`, enable `TYPESAFE_PROJECT_ACCESSORS`.
  - [x] Root `build.gradle.kts` declaring plugins with `apply false`.
  - [x] `gradle.properties`: `org.gradle.jvmargs`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `kotlin.code.style=official`.
  - [x] `.gitignore` for Android/Gradle/IDE files; `local.properties` excluded.
- [x] **0.2 Version catalog** `gradle/libs.versions.toml`
  - [x] Versions/libraries: AGP, Kotlin, KSP, Compose BOM, Material 3, Activity Compose, Lifecycle (runtime/viewmodel compose), Navigation Compose, kotlinx.serialization, kotlinx.coroutines, Hilt (+ hilt-navigation-compose, hilt-work), Room (+ room-paging), Paging 3 (+ paging-compose), DataStore, WorkManager, Coil 3 (+ coil-network-okhttp), Telephoto `zoomable-image-coil`, ExifInterface, osmdroid, OkHttp, Tink, Browser (Custom Tabs).
  - [x] Test libraries: JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, androidx.test (core, ext-junit, runner), room-testing, work-testing, MockWebServer, compose-ui-test-junit4, compose-ui-test-manifest.
  - [x] Plugin aliases: android-application, android-library, kotlin-android, kotlin-serialization, compose-compiler, ksp, hilt, room, detekt, ktlint, baselineprofile. _Done; `kotlin-android` is not needed with AGP 9 built-in Kotlin, and `baselineprofile` is declared for Phase 10._
- [x] **0.3 Convention plugins** in `build-logic/convention`
  - [x] `build-logic/settings.gradle.kts` reusing the version catalog.
  - [x] `AndroidApplicationConventionPlugin` (`nextgallery.android.application`): minSdk/compileSdk/targetSdk 37 (Android 17), JVM toolchain 21.
  - [x] `AndroidLibraryConventionPlugin` (`nextgallery.android.library`).
  - [x] `AndroidComposeConventionPlugin` (`nextgallery.android.compose`): Compose compiler plugin, BOM, tooling. _Plugin id is `nextgallery.android.compose`._
  - [x] `AndroidHiltConventionPlugin` (`nextgallery.android.hilt`): Hilt + KSP.
  - [x] `AndroidRoomConventionPlugin` (`nextgallery.android.room`): Room plugin, KSP, `schemaDirectory("$projectDir/schemas")`.
  - [x] `AndroidFeatureConventionPlugin` (`nextgallery.android.feature`): library + compose + hilt + deps on `:core:ui`, `:core:model`, `:core:common`, `:core:data`.
  - [x] `JvmLibraryConventionPlugin` (`nextgallery.jvm.library`) for the pure Kotlin/JVM modules (`:core:model`, `:core:common`, `:core:data`, `:core:nextcloud`): `kotlin("jvm")`, toolchain 21, `javax.inject`, JVM test libraries.
  - [x] `AndroidTestConventionPlugin` / shared test config: add the standard test libraries, `testOptions.unitTests.isIncludeAndroidResources = true` for Robolectric 4.17+ (first release with SDK 37), Robolectric `--add-opens` JVM flags in `unitTests.all { jvmArgs(...) }`, `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` for on-device tests (ARCHITECTURE.md §13.1). Placeholder Robolectric test and placeholder instrumented test both pass (`test` on CI, `connectedDebugAndroidTest` locally on an API 37 emulator or the Pixel 10a). _Robolectric check: `NextGalleryThemeTest` (`:core:ui`) passes on SDK 37. Instrumented check: `MainActivityTest` (`:app`) passes on the Pixel 10a._
  - [x] `DetektConventionPlugin` and `KtlintConventionPlugin` applied to every module.
- [x] **0.4 Module skeletons** (each with `build.gradle.kts`, namespace `net.drehtuer.nextgallery.<module>` (D26), application ID `net.drehtuer.nextgallery`, empty `src/main`, a placeholder test so the test task runs) _Instead of placeholder tests, `failOnNoDiscoveredTests` is disabled so empty modules pass._
  - [x] `:app` (`MainActivity`, `NextGalleryApplication` with `@HiltAndroidApp`, empty Compose `NextGalleryTheme` call).
  - [x] JVM: `:core:model`, `:core:common`, `:core:data`, `:core:nextcloud`.
  - [x] Android: `:core:database`, `:core:media`, `:core:platform`, `:core:ui`.
  - [x] `:feature:folders`, `:feature:timeline`, `:feature:map`, `:feature:viewer`, `:feature:editor`, `:feature:remote`, `:feature:settings`.
  - [x] Add a module-graph check (e.g. a Gradle task or detekt rule) that fails if a `:feature:*` depends on another `:feature:*`. _`AndroidFeatureConventionPlugin` throws on a `:feature:*` → `:feature:*` dependency; rule unit-tested in `FeatureDependencyRuleTest` and verified manually._
  - [x] `./gradlew assembleDebug` succeeds and installs an empty-screen app. _Builds in the devcontainer; installing on the Pixel 10a is still to be done._
- [x] **0.5 Static analysis**
  - [x] `.editorconfig` with ktlint rules (`ktlint_code_style = android_studio`, Compose function naming allowed).
  - [x] `config/detekt/detekt.yml` (baseline from defaults, Compose rule set if used), `detekt-baseline.xml` empty. _detekt 2.0.0-alpha.6 (see D18); no Compose rule set yet._
  - [x] Android Lint config `lint.xml`, `warningsAsErrors` for new code; `abortOnError = true`. _`config/lint/lint.xml`, `warningsAsErrors` for all code._
- [x] **0.5a Platform behaviour review** – go through the Android 15–17 behaviour changes for apps targeting SDK 37 (edge-to-edge, predictive back, package visibility for the Nextcloud app hand-off, media permission changes) and record anything that changes the plan in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md). _Recorded as D39 (local network permission) and D40; follow-up tasks added to Phases 7, 8 and 10._
- [x] **0.6 CI** `.github/workflows/ci.yml`
  - [x] Trigger on push and pull_request; JDK 21 (Temurin); `gradle/actions/setup-gradle` for caching. _Builds inside the devcontainer image via `devcontainers/ci` (D38), so no separate JDK or Gradle cache setup; triggers on every branch push (incl. merges into `main`) and on pull requests; a newer push cancels the running job for the same branch/PR._
  - [x] Jobs/steps: `ktlintCheck`, `detekt`, `lint`, `test` (JVM modules + Android local unit tests incl. Robolectric), `assembleDebug`. No emulator job; instrumented tests run locally. _Also compiles the instrumented tests (`assembleDebugAndroidTest`) and runs the build-logic tests._
  - [x] Upload test and lint reports as artifacts on failure.
  - [x] Gradle wrapper validation action.
- [x] **0.7 Repo hygiene**
  - [x] `LICENSE` with the full GNU GPL v3 text (D27); license header note in README. Check every dependency in the version catalog is GPL-3.0-compatible. _All runtime dependencies are Apache-2.0 or similarly permissive and GPL-3.0-compatible; JUnit 4 (EPL-1.0) is test-only and not distributed._
  - [x] `CONTRIBUTING.md` section or README note on running `./gradlew ktlintFormat detekt testDebugUnitTest`. _README "Building" section; the command is `./gradlew ktlintFormat detekt lint test :build-logic:convention:test`._
  - [x] PR template with checklist "tests added for every new function", "docs updated".
- [x] **0.8 Update docs** – ARCHITECTURE.md module list and README "Building" section match the actual build.

**Definition of done:** CI is green on an empty multi-module project; all 16 modules exist; the four JVM modules build without the Android plugin; lint/detekt/ktlint run on every module; a feature→feature dependency fails the build.

---

## Phase 1 – Skeleton, permissions, MediaStore indexing into Room

**Goal:** the app asks for media permissions and keeps a Room index of all local images in sync with MediaStore.
_Depends on: Phase 0._

- [x] **1.1 `:core:common` (JVM)**
  - [x] `DispatcherProvider` interface + `DefaultDispatcherProvider` (`io`, `default`, `main`). Test: `TestDispatcherProvider` returns injected `StandardTestDispatcher`. _Only `io` and `default`; no `main` (ViewModels use `viewModelScope`)._
  - [x] `AppResult<T>` / `runCatchingCancellable {}` helper that rethrows `CancellationException`. Tests: success, failure, cancellation rethrown. _Plus `AppError.from(throwable)`._
  - [x] `Clock` abstraction (`SystemClock` impl) for testable timestamps. Test: returns injected instant. _`Clock` also provides the device time zone (`zone()`)._
  - [x] `Debouncer` / `Flow.debounceSignals()` helper. Test with virtual time: bursts collapse to one emission. _Not needed: `IndexScheduler` uses kotlinx `debounce` directly; covered by its burst test._
- [x] **1.2 `:core:model` (JVM)**
  - [x] `MediaId` (value class over MediaStore `_ID`), `MediaItem` (id, contentUri, displayName, relativePath, bucketId, bucketName, mimeType, sizeBytes, width, height, orientation, dateTakenMillis, dateModifiedSeconds, generationModified?, latitude?, longitude?, takenLocalDate). _Fields as in ARCHITECTURE.md §3.3._
  - [x] `Folder` (bucketId, name, relativePath, itemCount, coverItem).
  - [x] `SupportedMimeTypes` (`image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`, `image/gif`) with `isSupported(mime)` and `isEditable(mime)` (JPEG, PNG only; D29). Tests: each supported/unsupported type, each editable/non-editable type. _Includes `isEditable` (D29)._
  - [x] `FolderRule` (path, `Include`/`Exclude`) and `FolderRules.matches(relativePath)`; default excludes (`WhatsApp/Media/.Statuses`, `Android/data`). Tests: include match, exclude wins over include, prefix vs. sibling (`DCIM/Cam` vs `DCIM/Camera`), empty include list = all. _`FolderRule` is in `:core:model`; matching is `FolderRuleMatcher` and the defaults are `DefaultFolderRulesProvider`, both in `:core:data` (they need `RelativePaths` from `:core:common`)._
  - [x] `MediaAccess` enum (`Full`, `Partial`, `Denied`), `ScannedImage`, `RawExif`, `PlatformConfirmation` (opaque handle).
- [x] **1.3 `:core:data` (JVM) – ports and indexing logic**
  - [x] Ports: `MediaScanSource` (`fullScan()`, `changesSince(generation)`, `allIds()`, `mediaStoreVersion()`, `noMediaPaths()`), `MediaChangeSource`, `ExifSource`, `MediaPermissionSource`, `MediaIndexStore`, `SyncStateStore`, `WorkScheduler`. Plus hand-written fakes in `testFixtures`. _Per-volume API; no `noMediaPaths()` (see next item). Fakes live in the module's tests; they move to `testFixtures` once another module needs them._
  - [x] `NoMediaMatcher.isHidden(relativePath, noMediaPaths)`. Tests: folder with `.nomedia`, nested subfolder, unrelated sibling. _Dropped: MediaStore already hides folders with a `.nomedia` file (D6); confirmed by `MediaStoreScannerTest` on the Pixel 10a._
  - [x] `ExifDateParser.parse(dateTimeOriginal, offsetTimeOriginal, subSec)`. Tests: plain, with offset, with sub-seconds, malformed → null.
  - [x] `DateResolver.resolve(dateTaken, exifDate, dateModified, zone): ResolvedDate` (DATE_TAKEN → EXIF → DATE_MODIFIED, local date). Tests: each fallback branch, zone boundary at midnight.
  - [x] `MediaAccessResolver.resolve(grantedPermissions)` and `requiredPermissions()`. Tests: full, selected only, none. _Folded into `DefaultMediaAccessRepository` (`currentAccess()`, `requestedPermissions`)._
  - [x] `MediaIndexer` with `fullScan()` and `incrementalSync()` against fake ports:
    - [x] full scan: stream scan source, apply `FolderRules` + `NoMediaMatcher`, resolve taken date, upsert in batches, delete rows not seen. Tests: insert new, update changed, delete removed, excluded folder skipped, `.nomedia` skipped.
    - [x] incremental: `changesSince(lastGeneration)` plus id-diff for deletions; fall back to full scan when the MediaStore version changed. Tests: changed-only upsert, deleted item removed, version change → full scan. _Also removes images that moved into an excluded folder._
    - [x] persist sync state after success. Test: state written only on success.
  - [x] `IndexRepository.sync(mode): AppResult<IndexStats>` and `observeStatus()`. Tests: success, scan failure → `Failure`.
  - [x] `IndexScheduler`: `onAppStart()` → `WorkScheduler.enqueueIndex(FULL)` if never indexed, else `INCREMENTAL`; collects `MediaChangeSource` debounced (2 s) → incremental; media access change → full. Tests: first launch → full, later → incremental, burst of changes → one enqueue, access change → full.
  - [x] `MediaRepository` interface + `DefaultMediaRepository`: `observeIndexedCount()`, `getItem(id)`. Tests per method with fake `MediaIndexStore`. _`observeIndexedCount()` was removed in Phase 2, when the folder list replaced the count placeholder._
- [x] **1.4 `:core:database` (Android adapter)**
  - [x] `MediaEntity` (`media` table; PK `id`; indices on `bucket_id`, `taken_local_date`, `date_modified`, `relative_path`). _Also stores `bucket_name`; indices on `(bucket_id, date_taken_ms)`, `date_taken_ms`, `(local_year, local_month, local_day)`, `volume_name`._
  - [x] `SyncStateEntity` (`sync_state`: last full scan time, last `GENERATION_MODIFIED`, MediaStore version string).
  - [x] `MediaDao`: `upsertAll`, `deleteByIds`, `deleteNotIn(ids)` (chunked), `getAllIdsWithModified()`, `observeCount()`, `getById()`. Tests (in-memory Room, Robolectric): one per method. _`deleteNotIn` is computed in `MediaIndexer`; chunking in `RoomMediaIndexStore`._
  - [x] `SyncStateDao`: `get()`, `put()`. Tests: default empty, round-trip. _Plus `hasAny()`._
  - [x] `NextGalleryDatabase` (version 1, exported schema in `core/database/schemas`). Test: `MigrationTestHelper` setup ready for future migrations. _Schema v1 exported; the Room convention plugin adds the schemas to unit-test assets so `MigrationTestHelper` works under Robolectric._
  - [x] `RoomMediaIndexStore`, `RoomSyncStateStore` implementing the ports, with entity ↔ model mappers; `DatabaseModule` (Hilt) binding them. Tests: mapper round-trip; each store method delegates to the DAO (Robolectric, in-memory Room).
- [x] **1.5 `:core:media` (Android adapter)**
  - [x] `MediaStoreQuery`: projection, selection for supported mime types, `IS_PENDING = 0`, `IS_TRASHED = 0`. Test: generated selection/args (Robolectric). _Plain JVM test; MediaStore hides pending and trashed rows by default, so no extra conditions._
  - [x] `CursorMapper.toScannedImage(cursor)`. Test with `MatrixCursor` (Robolectric): all columns, nulls.
  - [x] `MediaStoreScanner` implementing `MediaScanSource` (batches of 500, `GENERATION_MODIFIED`, `.nomedia` paths from `MediaStore.Files`). Tests: instrumented (local emulator / Pixel 10a) with inserted test images, one per method (ARCHITECTURE.md §13.1). _Instrumented `MediaStoreScannerTest` passes on the Pixel 10a._
  - [x] `AndroidExifSource` implementing `ExifSource` (`androidx.exifinterface`, `MediaStore.setRequireOriginal` when `ACCESS_MEDIA_LOCATION` is granted); returns raw tag strings, no parsing. Tests: tag extraction from sample JPEGs in `src/test/resources` (Robolectric); `setRequireOriginal` with/without permission instrumented. _Only the date tags for now; GPS and `setRequireOriginal` come in Phase 6. EXIF test writes a real JPEG with Robolectric native graphics._
  - [x] `MediaStoreChangeObserver` implementing `MediaChangeSource` (`callbackFlow` over `ContentObserver`). Test: register/unregister on collect/cancel (Robolectric).
  - [x] `AndroidMediaPermissionSource` implementing `MediaPermissionSource`. Test: granted set reflects shadowed permissions (Robolectric).
  - [x] `MediaModule` (Hilt) binding the adapters.
- [x] **1.6 `:core:platform` (Android adapter and wiring)**
  - [x] `IndexWorker` (`@HiltWorker`, `CoroutineWorker`): input `mode`, calls `IndexRepository.sync(mode)`, maps `Success` → `success()`, `Failure` → `retry()`. Tests (`TestListenableWorkerBuilder`, Robolectric): success, failure → retry. _`PermissionDenied` → `failure()` (retrying cannot help), other failures → `retry()`._
  - [x] `WorkManagerScheduler` implementing `WorkScheduler`: unique work `index`, `KEEP` for app start, `APPEND_OR_REPLACE` for changes. Tests (`WorkManagerTestInitHelper`): enqueued request has the expected name, policy and input. _Every request uses `APPEND_OR_REPLACE`; full scans are expedited._
  - [x] `DispatchersModule`, `ApplicationScopeModule`, `RepositoryModule` (binds `:core:data` interfaces). WorkManager initialization via `Configuration.Provider` with `HiltWorkerFactory` in `NextGalleryApplication` (remove default initializer in manifest). _Implemented as `PlatformModule` and `RepositoryModule`; no application scope needed yet (the change observer runs in `MainViewModel`)._
- [x] **1.7 Permissions UI**
  - [x] `:app` manifest: `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `ACCESS_MEDIA_LOCATION`. (`INTERNET` added in Phase 8.)
  - [x] `:core:ui` `rememberMediaPermissionState()` + `PermissionRationale` composable (explain, request, "select more photos" for partial, open app settings when permanently denied). Compose UI tests: each state renders the correct action. _Stateless `MediaAccessRequest` and `LimitedAccessBanner` in `:core:ui` (Compose tests); the permission launcher is wired in `MainActivity` and not unit-tested (Android glue)._
- [x] **1.8 `:core:ui` foundation**
  - [x] `NextGalleryTheme` (Material 3, light/dark) with placeholder colour, typography, shape and spacing tokens; the final values come from Claude Design later (D22). Compose test: renders in light/dark. _Spacing and sizes were added in Phase 2 (`NextGalleryTheme.dimensions`)._
  - [x] Common composables: `LoadingIndicator`, `EmptyState`, `ErrorState`. Compose UI test each.
- [x] **1.9 `:app` shell**
  - [x] Type-safe routes (`@Serializable object FoldersRoute`, …) in `:app` `navigation/Routes.kt`; `NextGalleryNavHost`.
  - [x] Bottom navigation bar (Folders, Timeline, Map, Nextcloud) with placeholder screens. Compose UI test: tab switching. _Placeholder screens live in `:app` until the features replace them._
  - [x] `MainViewModel` exposing `MainUiState(mediaAccess, indexedCount)`; calls `IndexScheduler.onAppStart()`. JVM tests: state on permission granted/denied. _`AccessRequestFinished` event forces a rebuild after the permission dialog or photo selection closes; the change observer starts once access is granted._
  - [x] Debug-only screen/log showing indexed count to verify indexing. _Replaced in Phase 2: the folder list shows the number of images per folder._
- [x] **1.10 Update docs** – ARCHITECTURE.md (indexing flow, DB schema v1) and README match code.

**Definition of done:** on a device with photos, granting permission fills the `media` table; adding/deleting a photo updates the index within seconds; partial access indexes only selected photos; all new functions have tests; CI green.

---

## Phase 2 – Folder view with thumbnail grid and fast scroll

**Goal:** default Folders screen listing folders and a paged thumbnail grid per folder with a draggable scrollbar.
_Depends on: Phase 1._

- [x] **2.1 Database**
  - [x] `FolderDao.observeFolders(): Flow<List<FolderRow>>` (GROUP BY bucket, count, newest item as cover). Test: grouping, count, cover selection. _A plain query; no `@DatabaseView`, so the schema stays at version 1._
  - [x] `MediaDao.pagingByBucket(bucketId): PagingSource<Int, MediaEntity>` ordered by taken date desc. Test: `PagingSource.load` ordering and page size.
  - [x] `MediaDao.countByBucket(bucketId)`. Test. _Not needed: paging placeholders provide the count. Added `idsInBucket(bucketId)` for "select all" instead._
- [x] **2.2 Repository**
  - [x] `FolderRepository.observeFolders(): Flow<List<Folder>>`. Test with Turbine. _Plus `observeFolder(bucketId)` for the grid title._
  - [x] `MediaRepository.pagedFolderItems(bucketId): Flow<PagingData<MediaItem>>` (`Pager`, page size ~ 120, `enablePlaceholders = true` for fast scroll). Test with `asSnapshot()`. _The repository sets the `PagingConfig` (with a jump threshold for the scrollbar); `RoomMediaIndexStore` builds the `Pager`, because entities stay in the adapter. Plus `folderItemIds(bucketId)`._
- [x] **2.3 Thumbnails (`:core:ui` + `:app`)**
  - [x] `ImageLoaderModule` providing a singleton Coil 3 `ImageLoader` (memory cache %, disk cache dir/size, `crossfade` off for grids). Test: config values.
  - [x] `MediaThumbnailKeyer`/fetcher using `ContentResolver.loadThumbnail(uri, size)` with fallback decode. Tests: keyer output stable per (id, dateModified, size) (JVM); `loadThumbnail` at the requested size (instrumented). _Instrumented `MediaThumbnailFetcherTest` passes on the Pixel 10a._
  - [x] `ThumbnailCell` composable (fixed aspect, placeholder, content description, selection overlay slot, badge slot for Phase 9, greyed-out state). Compose UI test: placeholder, selected state, greyed-out state.
- [x] **2.4 Shared grid components (`:core:ui`)**
  - [x] `MediaGrid` (`LazyVerticalGrid`, adaptive columns, stable keys, `LazyPagingItems`). Compose test: renders N items, keys stable.
  - [x] `ScrollbarMath.indexForFraction()` / `fractionForIndex()` in `:core:data` (JVM tests), and the `FastScrollbar` composable (draggable thumb, optional `label: (index) -> String?` bubble). Compose test: drag scrolls grid. _`ScrollbarMath` is in `:core:common`, because `:core:ui` does not depend on `:core:data`. The folder grid shows no label; the timeline has its own date rail (Phase 5)._
  - [x] `SelectionRepository` in `:core:data` – app-wide in-memory selection (D33): `observe()`, toggle, range, select all, clear (JVM tests per function); `SelectionTopBar` in `:core:ui` (total count, actions slot). Unit tests per function; Compose test: long-press enters selection. _No gesture uses `selectRange` yet._
- [x] **2.5 `:feature:folders`**
  - [x] `FolderListViewModel` → `StateFlow<FolderListUiState>` (Loading/Empty/Content). Tests with Turbine: each state.
  - [x] `FolderListScreen` (grid of folder covers with name/count). Compose UI test: click emits `onFolderClick(bucketId)`.
  - [x] `FolderGridViewModel` (route passed via Hilt assisted injection, see ARCHITECTURE.md §7), exposes `PagingData` + app-wide selection state. JVM tests: selection events go to `SelectionRepository`, paging flow wired to the route's bucket.
  - [x] `FolderGridScreen` (MediaGrid + FastScrollbar + SelectionTopBar). Compose UI test: item click → `onOpenViewer(mediaId, source)`. _The click becomes `FolderGridEffect.OpenViewer(mediaId)`; `source` comes with `MediaSource` in Phase 3, and until then `:app` ignores the effect._
  - [x] Navigation entry points `folderListScreen()` / `folderGridScreen()` as `NavGraphBuilder` extensions; wired in `:app`. _As a nested graph, so the Folders tab stays selected inside a folder. The Hilt-backed wiring is covered by the build only; it runs for the first time on a device._
- [ ] **2.6 Performance check** – Macrobenchmark smoke (or manual profiling) scrolling 10k items; no dropped-frame regressions noted in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md) if any trade-offs made.
- [x] **2.7 Update docs** – ARCHITECTURE.md (paging, thumbnail pipeline) and README match code.

**Definition of done:** Folders is the start screen; opening a folder shows a smoothly scrolling grid; scrollbar drag jumps through 10k+ images; tests for every new function; CI green.

---

## Phase 3 – Full-screen viewer, delete, share

**Goal:** swipeable zoomable viewer with image info, plus delete and share for single and multi-selection.
_Depends on: Phase 2 (grid, selection)._

- [x] **3.1 Model & data**
  - [x] `MediaSource` sealed type (`Folder(bucketId)`, `Timeline`, `Map(bounds?)`) so the viewer pages over the same list as the caller. Test: serialization round-trip (used in routes). _`Folder` and, since Phase 5, `Timeline`; Phase 6 adds its own. `ViewerRoute` carries `mediaId` and `bucketId` as plain values, so nothing needs serializing._
  - [x] `MediaRepository.pagedItems(source)` and `indexOf(source, mediaId)`. Tests per source. _Replaces `pagedFolderItems`._
  - [x] `ImageDetails` model (date, size, resolution, mime, path, lat/long, camera make/model, exposure, ISO, focal length…). `ImageDetailsMapper.from(rawExif, item)` in `:core:data` (JVM tests per field, missing values); `AndroidExifSource` extended with the camera tags (Robolectric fixture test).
- [x] **3.2 Delete (`:core:media` + `:core:data`)**
  - [x] `MediaDeleteSource` port; `MediaStoreDeleter` adapter: `createTrashRequest` (default) / `createDeleteRequest` (permanent) wrapped in `PlatformConfirmation`. Tests (Robolectric): trash, permanent. _Instrumented instead (`MediaStoreDeleterTest`, passes on the Pixel 10a), because the requests need the real MediaStore (ARCHITECTURE.md §13.1). Plus a restore request for undo._
  - [x] `MediaActionsRepository.delete(ids): DeleteOutcome` (`NeedsUserConfirmation(intentSender)`, `Failed`) and `onDeleteConfirmed(ids)` removing rows from Room immediately. Tests per outcome. _`TrashFlow` holds the steps (request, confirmation, undo) for all screens. `delete` can delete permanently (D10), but no screen offers that yet._
  - [x] `:core:ui` `rememberConfirmationLauncher()` unwrapping `PlatformConfirmation` and launching it via `StartIntentSenderForResult`. Compose test: result forwarded.
- [x] **3.3 Share**
  - [x] `ShareSpec.from(items)` in `:core:data` (single vs multiple, common mime or `image/*`; JVM tests) and `ShareIntentFactory` in `:core:ui` turning it into `ACTION_SEND`/`ACTION_SEND_MULTIPLE` with `FLAG_GRANT_READ_URI_PERMISSION` and `ClipData` (Robolectric test). _`ShareSpec` is in `:core:model`, because `:core:ui` does not depend on `:core:data`._
- [x] **3.4 `:feature:viewer`**
  - [x] `ViewerRoute(source: MediaSource, mediaId: Long)`. _`ViewerRoute(mediaId, bucketId)`, see 3.1; a null `bucketId` means the timeline._
  - [x] `ViewerViewModel`: paging over source, current index, `ViewerUiState(currentItem, chromeVisible, details?, pendingDelete?)`; events `PageChanged`, `ToggleChrome`, `ShowInfo`, `Delete`, `Share`, `DeleteConfirmed`. Tests per event.
  - [x] `ViewerScreen`: `HorizontalPager` + Telephoto `ZoomableAsyncImage` (Coil, subsampling, double-tap zoom); system-bars hide/show; top bar (back, share, delete, edit placeholder, info). Compose UI tests: swipe changes page, tap toggles chrome, delete shows confirmation flow. _No edit placeholder; the button comes with the editor (Phase 7). The confirmation flow is tested in `TrashEffectHandlerTest`. Zooming itself has only been compiled; it needs the device checks._
  - [x] `ImageInfoSheet` (ModalBottomSheet with `ImageDetails`). Compose test: fields rendered, missing values hidden.
  - [x] Handle item disappearing (deleted externally) → move to neighbour or close. Test in ViewModel.
- [x] **3.5 Multi-select actions in grids**
  - [x] Add delete/share actions to `SelectionTopBar` in `FolderGridScreen`; `FolderGridViewModel` uses `MediaActionsRepository` / `ShareIntentFactory`. Actions apply to the whole app-wide selection. Tests: events → outcomes.
  - [x] Selection overview (D33): `SelectionRepresentability.canShow(item, context)` in `:core:data` (Folder: same bucket; Timeline: always; Map: has GPS – added in Phase 6). JVM tests per context. `SelectionRepository.overview(context)` → items with a `greyedOut` flag. JVM test. `SelectionOverviewRoute(context)` screen (grid, greyed-out items, same actions). ViewModel tests; Compose test: greyed-out item rendered. _`SelectionOverviewRoute(bucketId)`, where null means the timeline (Phase 5). Tapping an image removes it from the selection._
- [x] **3.6 `:app`** – route `ViewerRoute` from folder grid; handle `ACTION_VIEW` intent for `image/*` content URIs (open viewer for a single external item). Test: intent parsing. _`ExternalImageScreen` shows only the zoomable image: the file need not be in the index, so it has no info, share or delete._
- [x] **3.7 Update docs** – ARCHITECTURE.md (viewer, delete flow) and README match code.

**Definition of done:** open image from grid, swipe, pinch/double-tap zoom large images without OOM, view info, share 1..n images, delete 1..n images to the system trash (with undo) with grid updating; tests for every function; CI green.

---

## Phase 4 – Settings: folder include/exclude list

**Goal:** users choose which folders are indexed; changes re-index.
_Depends on: Phase 1 (FolderRules, IndexScheduler)._

- [x] **4.1 Settings storage (`:core:data`)**
  - [x] `UserSettings` model in `:core:model` (folderRules, gridColumns, theme mode, accent colour). `AccentColor` already exists. _Also `excludePatterns`, for the pattern field in DESIGN.md._
  - [x] Proto or Preferences DataStore `SettingsDataSource` (`settings.preferences_pb`), serializer for rules. Tests: read defaults, write/read each field, corrupt-file handler.
  - [x] `SettingsRepository`: `observeSettings()`, `addRule()`, `removeRule()`, `setGridColumns()`, `setThemeMode()`, `setAccentColor()`. Tests per function. _Also `setExcludePatterns()`._
- [x] **4.2 Rule application**
  - [x] `MediaIndexer` reads rules from `SettingsRepository` (replace Phase 1 defaults). Test: rules respected. _`FolderRulesProvider` is gone. `FolderRuleMatcher` changed to the meaning in DESIGN.md (D52): an include no longer hides all other folders._
  - [x] `RuleChangeObserver`: rules change → enqueue full scan (rule change can add items). Test: change → enqueue once. _Debounced by 2 s; started from `NextGalleryApplication`._
  - [x] `FolderRepository.observeAllDeviceFolders()` – folders from MediaStore regardless of rules (needed to pick includes). Test. _Built on the existing `MediaScanSource.fullScan` and `MediaChangeSource`, so no new adapter code._
- [x] **4.3 `:feature:settings`**
  - [x] `SettingsViewModel` / `SettingsScreen` (root list: Folders, Appearance, Nextcloud placeholder, About/licenses), opened from a settings icon in the Folders top bar. About needs `BuildConfig` enabled for the version. Tests: state mapping, navigation callbacks. _No `SettingsViewModel` yet: the root has no state until Phase 8 adds the account. `BuildConfig` is enabled in `:app`, which passes the version to the screen, because a library's `BuildConfig` has no version. The licence list is a hand-kept resource until task 10.5 generates it._
  - [x] `FolderRulesViewModel` / `FolderRulesScreen`: list of device folders with Include/Exclude/Default toggles, add custom exclude pattern, show `.nomedia` folders as excluded (read-only). Tests: each toggle event; Compose test: toggling updates UI. _`.nomedia` folders cannot be listed (D53); a note on the screen says so._
  - [x] Appearance: theme mode, grid columns (3, 4 or 5; D51), accent colour (the five accents in DESIGN.md; D34). Tests.
  - [x] Wire `NextGalleryTheme` and grids to settings. Test: `MainViewModel` emits theme. _The column count goes through `NextGalleryTheme.dimensions.gridColumns`, so every grid follows it; the folder list keeps its two covers (DESIGN.md)._
- [x] **4.4 Update docs** – ARCHITECTURE.md (settings storage) and README match code.

**Definition of done:** excluding a folder removes its items from all views after re-index; including restores them; settings persist across restarts; tests for every function; CI green.

---

## Phase 5 – Timeline view (year/month/day)

**Goal:** all indexed images grouped by year → month → day with sticky headers and a date-labelled fast scroll.
_Depends on: Phases 2 (grid), 3 (viewer)._

- [x] **5.1 Database**
  - [x] `MediaDao.pagingTimeline(): PagingSource<Int, MediaEntity>` ordered by `taken_local_date DESC, date_taken DESC`. Test. _In its own `TimelineDao` as `paging()`, behind the `TimelineStore` port. Uses the existing `local_year/month/day` columns; no schema change. Also `ids()` (select all) and `indexOf(id)` (viewer)._
  - [x] `MediaDao.observeDayCounts(): Flow<List<DayCount>>` (date, count) for headers and scrollbar map. Test. _In `TimelineDao`._
- [x] **5.2 Repository**
  - [x] `TimelineRepository.pagedTimeline(): Flow<PagingData<TimelineEntry>>` inserting `Header(year, month, day)` separators via `insertSeparators`. Tests: separators at day/month/year boundaries. _No separators: `insertSeparators` only sees loaded pages, so the row count and the position of every header would change while scrolling, and the date rail could not jump. `TimelineRepository.observeIndex()` gives a `TimelineIndex` that knows all rows from the day counts; the images are the plain `MediaRepository.pagedItems(MediaSource.Timeline)`, the same list the viewer pages through._
  - [x] `TimelineIndex` built from day counts: `labelForPosition(index): "MMMM yyyy"`, `positionForMonth(yearMonth)`, `dayRailLabels(centerIndex)` → previous, current and next day ("24 Oct 2026"; null at the ends) for the date rail (D43). Tests per function. _As `rowAt`, `isHeader`, `monthAt` and `dayRailAt`, which return dates; the feature module formats them. `positionForMonth` is left out, because nothing jumps to a month._
  - [ ] Grouping granularity enum (`Day`, `Month`, `Year`) for pinch-to-change density (optional, behind setting). Tests. _Not done: optional, and it would need its own layout per level and a setting._
- [x] **5.3 `:feature:timeline`**
  - [x] `TimelineViewModel` → `TimelineUiState` (paging flow, granularity, app-wide selection count). Tests per event. _The paging flow is separate from the state, as in the folder grid; no granularity (see 5.2). The state also has today's date, for "Today · …"._
  - [x] `TimelineScreen`: `LazyVerticalGrid` with full-span `stickyHeader`-style headers (Compose `LazyGrid` header items + overlay sticky header), date rail (D43: track, thumb, current day large in a pill, neighbouring days small; drawn above the sticky headers), multi-select delete/share. Compose UI tests: headers shown, rail shows the day in the middle of the screen and its neighbours. _Month headers stick; day headers scroll (DESIGN.md). The wording of the empty and failed states is new, modelled on the folder screens, because DESIGN.md has none for the timeline._
  - [x] Open viewer with `MediaSource.Timeline`. Test. _`ViewerRoute(mediaId, bucketId = null)`, and `SelectionOverviewRoute(bucketId = null)` for the overview. Both screens now sit outside the Folders graph, so the bottom navigation is hidden on the overview too._
- [x] **5.4 Update docs** – ARCHITECTURE.md (timeline date resolution, separators) and README match code.

**Definition of done:** timeline shows correct local dates (EXIF fallback verified), sticky headers, the date rail shows the current day and its neighbours (D43), smooth on 10k+ items; tests for every function; CI green.

---

## Phase 6 – Map view

**Goal:** images with GPS shown as clustered markers on an OpenStreetMap map.
_Depends on: Phases 1 (GPS in index, ACCESS_MEDIA_LOCATION), 3 (viewer)._

- [ ] **6.1 Location data**
  - [ ] `MediaIndexer` fills lat/long from `ExifSource` (`GpsParser` in `:core:data` turns raw GPS tags into `GeoPoint`); re-index when `ACCESS_MEDIA_LOCATION` is granted later. JVM tests: valid GPS, missing GPS, permission missing → null, permission granted → re-index.
  - [ ] Lazy GPS backfill: a `LocationBackfill` step in `:core:data` run after the main sync (via `IndexRepository`) for rows with `exif_scanned = false` (column exists since schema v1, see ARCHITECTURE.md §4.1). Tests: backfill only unscanned rows, flag set afterwards.
  - [ ] `MediaDao.observeGeotagged(bounds?)` (id, lat, lon). Test.
- [ ] **6.2 Clustering (`:core:data`, JVM)**
  - [ ] `GeoPoint`, `GeoBounds`, `Cluster(center, count, coverId, ids)` in `:core:model`.
  - [ ] `GridClusterer.cluster(points, bounds, zoom, cellPx)` (pure Kotlin, grid-based). Tests: single point, merge within cell, split at higher zoom, antimeridian handling.
  - [ ] `MapRepository.observeClusters(viewport: Flow<Viewport>)` (debounced, computed on `Dispatchers.Default`). Test with Turbine.
  - [ ] `PlaceNamer` (D45): loads the bundled GeoNames `cities15000` extract (name, country, lat, lon) into a spatial grid; `nearest(point, maxKm = 30)` → place or null; `clusterTitle(cluster)` → "near X", "near X and N more places" or null (count only). JVM tests: within/outside 30 km, merged cluster named after its largest place with a town, no town.
  - [ ] Build step that turns the GeoNames dump into the bundled asset (only the needed columns), plus the attribution in About. Test: asset parses and has the expected row count.
- [ ] **6.3 `:feature:map`**
  - [ ] `MapRenderer` interface (`setClusters`, `setViewport`, `onViewportChanged`, `onClusterClick`) + `OsmdroidMapRenderer` in `AndroidView` (user agent set, tile cache in app cache dir, no Play Services). Test: `FakeMapRenderer` used in ViewModel tests; osmdroid config (user agent, tile URL) in Robolectric; instrumented test (local) that the map renders clusters.
  - [ ] Tile source setting (default OSM tile server, configurable URL; D23) in `SettingsRepository`, read by `OsmdroidMapRenderer`. Tests: default value, custom URL applied.
  - [ ] Cluster marker drawable with thumbnail + count (Coil-loaded bitmap). Test: count text formatting (`999+`).
  - [ ] Viewport (D44): zoom 0–19, horizontal world repetition, multi-touch pinch and two-finger drag (osmdroid), zoom buttons and a "my location" button. Robolectric test: min/max zoom and repetition configured.
  - [ ] Current location (D44): `ACCESS_COARSE_LOCATION` in the manifest; `LocationProvider` port with a `LocationManager` adapter (last known or one fresh fix, no Play Services); start viewport = current location at zoom 8, else the bounds of the largest cluster. Tests: permission granted/denied → start viewport; Compose test: location dot shown.
  - [ ] `MapViewModel` → `MapUiState(clusters, viewport, permissionNeeded)`; events `ViewportChanged`, `ClusterClicked`. Tests per event.
  - [ ] `MapScreen`: map, permission prompts for `ACCESS_MEDIA_LOCATION` and `ACCESS_COARSE_LOCATION`, empty state "no geotagged images". Compose UI test: states.
  - [ ] Map rule in `SelectionRepresentability` (item has GPS) and selection overview entry from the map. JVM test: item without GPS greyed out for `ViewContext.Map`.
  - [ ] Cluster tap → bottom-sheet grid of cluster items (title from `PlaceNamer`, else count and coordinates; own fast scrollbar) or viewer with `MediaSource.Map(ids/bounds)`. Test.
- [ ] **6.4 Update docs** – ARCHITECTURE.md (MapRenderer abstraction, clustering) and README match code.

**Definition of done:** geotagged images appear as clusters that split on zoom; tapping opens images; images without GPS are absent; works without Play Services; tests for every function; CI green.

---

## Phase 7 – Editor: rotate, crop, scale, pen

**Goal:** non-destructive edit pipeline with undo/redo, saving a new copy or overwriting with EXIF preserved.
_Depends on: Phase 3 (viewer entry point, write/delete request helpers)._

- [x] **7.1 Model (`:core:model`)**
  - [x] `EditOperation` sealed interface: `Rotate(quarterTurns)`, `Crop(normalizedRect)`, `Scale(targetWidth, targetHeight)`, `PenStroke(points, colorArgb, widthPx-normalized)`. _In the package `core.model.edit`, with `ImageSize`, `NormalizedPoint`, `NormalizedRect`, `Matrix3` and `PreviewImage`._
  - [x] `EditHistory` (immutable list + cursor): `push`, `undo`, `redo`, `canUndo`, `canRedo`, `operations`. Tests per function, push after undo truncates redo.
  - [x] `AspectRatio` (`Free`, `Square`, `R4_3`, `R16_9`, `Original`) + `CropMath.constrain(rect, ratio, bounds)`. Tests per ratio. _Also `CropMath.handleAt` and `drag` for the crop frame. 4:3 and 16:9 follow the image, so an upright image gets 3:4 and 9:16._
  - [x] `ScaleMath.fromPercent()` / `fromTargetWidth()` / `fromTargetHeight()` keeping aspect. Tests.
  - [x] `GeometryMapper`: maps pen/crop coordinates through prior rotate/crop/scale so ops compose. Tests per op order. _Coordinates of an operation are relative to the image just before it, so a point is mapped through the operations that follow it._
- [x] **7.2 Rendering**
  - [x] `EditGeometry.steps(ops, sourceSize): RenderPlan` in `:core:data` (output size, 3×3 matrices, crop rects, stroke paths in pixels, orientation bake-in). JVM tests: one per op type + combinations. _All operations flatten into one matrix from source to output, whose size also does the clipping, so there are no crop rects and only the two bitmaps of D30. `ImageDecoder` delivers upright pixels, so the plan starts from the upright size and the bake-in is done by the decoder (tested in `BitmapImageRendererTest`)._
  - [x] `MemoryBudget.check(sourceSize, outputSize, availableBytes): Fits | Refused(requiredBytes, availableBytes)` in `:core:data` (D30). JVM tests: fits, exactly at the limit, too large → refused with both numbers.
  - [x] On the Pixel 10a, check that the memory estimate respects Android 17's per-app memory limit, not only `availMem` (D40); adjust `ActivityManagerMemoryInfo` if needed. _Checked on 2026-11-07: `adb shell am memory-limiter status` reports `disabled` on the Pixel 10a, so `availMem` is the right measure for now. Check again if a later Android update enables the limiter; Android offers no public API for its value._
  - [x] `MemoryInfoSource` port; `ActivityManagerMemoryInfo` in `:core:platform` (`ActivityManager.MemoryInfo.availMem` minus safety margin). Test (Robolectric): value from shadowed `MemoryInfo`, margin subtracted. _The margin is the system's low-memory threshold plus 200 MB._
  - [x] `sampleSizeFor(source, maxSide)` in `:core:data`. JVM tests.
  - [x] `ImageRenderer` port; `BitmapImageRenderer` adapter in `:core:media` (`ImageDecoder` decode, apply `RenderPlan` via `Matrix`/`Canvas`/`Path`, return `PreviewImage` handle). Tests (Robolectric, native graphics): applies a matrix, a crop and a stroke on a small bitmap. _An `OutOfMemoryError` becomes the same refusal as the check before it. A scaled-down save decodes only as many pixels as the output needs._
  - [x] Encoding in `BitmapImageRenderer`: JPEG (quality 95) and PNG, keeping the source format (D29). Tests per format (Robolectric).
  - [x] `ExifPolicy.forOutput(sourceTags, newWidth, newHeight)` in `:core:data` (which tags to copy, orientation normal, new dimensions; JVM tests) and `ExifWriter` in `:core:media` applying it (Robolectric fixture test). _`ExifWriter` is the port, `AndroidExifWriter` the adapter. Reading through `setRequireOriginal` keeps the location; on-device check below._
- [x] **7.3 Saving (`:core:data` + `:core:media`)**
  - [x] `CopyNamer.nameFor(displayName, existingNames)` in `:core:data` (e.g. `IMG_1234_edited.jpg`, numbered on collision). JVM tests.
  - [x] `MediaWriteSink` port; `MediaStoreWriter` adapter: `insertPending(displayName, relativePath, mime)`, `requestOverwrite(contentUri): PlatformConfirmation` (`createWriteRequest`), `publish()`, `cleanupPending()`. Tests (Robolectric): insert sets `IS_PENDING`, publish clears it; overwrite request built for the URI. _Robolectric tests run against a stand-in provider; `MediaStoreWriterTest` is the instrumented test against the real one, on entries it inserted itself. `cleanupPending` runs on app start._
  - [x] `EditRepository.save(sourceId, ops, mode = NewCopy|Overwrite): SaveOutcome` → triggers incremental index. Tests per mode/outcome. _Overwriting continues in `commitOverwrite` after the confirmation. It keeps a temporary backup and puts it back if writing fails. The index syncs inline, so the viewer can open the copy right away._
- [x] **7.4 `:feature:editor`**
  - [x] `EditorRoute(mediaId)`; `EditorViewModel` → `EditorUiState(previewBitmap, activeTool, history, cropRect, penColor, penWidth, saving)`; events per tool, `Undo`, `Redo`, `Save(mode)`, `Discard`. A refused scale or save (`InsufficientMemory`) leaves the image unchanged and shows a message with the required and available memory (D30, with the numbers). Tests per event, including both refusal cases; preview re-render runs on `Default` dispatcher. _The dispatcher is in `DefaultEditRepository` and tested there. The design has no apply button for a crop: it is applied on leaving the tool, on another action and on saving._
  - [x] `EditorScreen` with tool bar; `RotateTool`, `CropOverlay` (handles + aspect chips), `ScaleDialog` (percent / width / height), `PenCanvas` (pointer input, color & width pickers). Compose UI tests per tool.
  - [x] Unsaved-changes dialog on back; save mode dialog (new copy default, overwrite). Compose tests. _The save mode is the menu of DESIGN.md, not a dialog, with "Save as copy" first._
  - [x] Viewer "Edit" button shown only for editable formats (JPEG, PNG; D29) → `EditorRoute` in `:app`; after save, viewer navigates to the new copy. Test. _After an overwrite it returns to the same image, with Coil's memory cache cleared._
- [x] **7.5 Update docs** – ARCHITECTURE.md (edit pipeline) and README match code.

**Definition of done:** all four tools work with undo/redo; saved copies appear in the gallery with correct orientation, dimensions and preserved EXIF; overwrite goes through system confirmation; scales and saves that exceed available memory are refused with an explanatory message (D30), never an OOM; Edit is offered only for JPEG and PNG; tests for every function; CI green.

---

## Phase 8 – Nextcloud login and built-in remote viewer

**Goal:** sign in with Login Flow v2 and browse/view Nextcloud images via WebDAV and the preview API.
_Depends on: Phases 3 (viewer), 4 (settings)._

- [ ] **8.1 Model (`:core:model`)**
  - [ ] `NextcloudAccount(serverUrl, loginName, userId, appPassword)` (password never logged; `toString` redacted). Test: redaction.
  - [ ] `RemoteItem(path, name, isDirectory, size, mime, etag, lastModified, fileId, checksums?)`. 
- [ ] **8.2 Credentials**
  - [ ] `SecretCipher` port and `AccountStore` in `:core:data` (DataStore-core + `SecretCipher`): `observeAccount()`, `save()`, `clear()`. JVM tests per function with a fake cipher; undecryptable data → null.
  - [ ] `KeystoreSecretCipher` in `:core:platform`: Tink `AndroidKeysetManager` with Android Keystore master key (`android-keystore://nextgallery_master`). Test: encrypt/decrypt round-trip, instrumented (Robolectric has no `AndroidKeyStore`).
- [ ] **8.3 `:core:nextcloud` client**
  - [ ] `:app` manifest: `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_LOCAL_NETWORK` (D39). (`:core:nextcloud` is a JVM module and has no manifest.)
  - [ ] Request `ACCESS_LOCAL_NETWORK` at the start of the login with an explanation; map blocked LAN connections to a clear `AppError` instead of a timeout. Tests: permission state → request shown; blocked connection → error. On-device check: LAN IP, `.local` name, VPN/Tailscale address, login page in Custom Tabs (D39).
  - [ ] On-device check that an accepted self-signed certificate still works with Certificate Transparency on by default (D40).
  - [ ] XML parsing on the XmlPullParser API: `compileOnly` dependency, provided by Android at runtime; kxml2 on the JVM test classpath.
  - [ ] `NextcloudHttpModule`: base `OkHttpClient` (timeouts, user agent), `AuthInterceptor` (Basic auth from account, OCS-APIRequest header). Test with MockWebServer: header present.
  - [ ] `LoginFlowV2Client`: `start(serverUrl)` → `{loginUrl, pollEndpoint, token}`; `poll()` until 200 or timeout. Tests (MockWebServer): start, 404 pending, success, expiry.
  - [ ] `ServerUrlNormalizer` (add https, strip `/index.php`, trailing slash). Tests per case.
  - [ ] `PropfindRequestBuilder` (depth 1, props: resourcetype, getcontentlength, getcontenttype, getetag, getlastmodified, oc:fileid, oc:checksums, oc:permissions). Test: XML body.
  - [ ] `MultistatusParser` (XmlPullParser) → `List<RemoteItem>`. Tests with fixture XML: files, folders, missing props, URL-encoded names.
  - [ ] `WebDavClient`: `list(path)`, `download(path, sink)`, and (D46) `move(from, to, overwrite = false)`, `createFolder(path)`, `delete(path)`; trash-bin `restore(item)`. Uploading stays with the Nextcloud app (D31). Tests (MockWebServer) per method incl. 401/404, 412 on an existing target, 403 on a read-only folder.
  - [ ] `PinningTrustManager` + `HostnameVerifier` (D32): platform trust first, then pinned SHA-256 per host, else `UntrustedCertificateException(chain)`. JVM tests with `okhttp-tls` `HeldCertificate`: CA-signed accepted, unknown self-signed rejected, pinned accepted, pinned but changed rejected with `changedFromPinned`, pinned with hostname mismatch accepted.
  - [ ] `CertificateInfoMapper`. Test: fields and fingerprint of a fixture certificate.
  - [ ] `RemotePathMapper`: `davUrl(account, path)` (`/remote.php/dav/files/<userId>/…`, segment encoding), `previewUrl(fileId, size)` (`/index.php/core/preview?fileId=…&x=…&y=…&a=1`). Tests.
- [ ] **8.4 Coil integration (`:core:ui` / `:app`)**
  - [ ] Separate authenticated Coil `ImageLoader` (or `OkHttpNetworkFetcherFactory` with auth client) qualified `@RemoteImageLoader`; disk-cache key by fileId+etag+size. Tests: keyer.
- [ ] **8.5 Repository (`:core:data`)**
  - [ ] `NextcloudAccountRepository`: `login(serverUrl): Flow<LoginState>` (Started(loginUrl) → Polling → Success/Failed), `logout()` (revoke via `DELETE /ocs/v2.php/core/apppassword` best effort, clear store, remote cache and upload states; D35). Tests per state.
  - [ ] Logged-out guard: every remote call returns `AppError.NotLoggedIn` without network access. Test.
  - [ ] `TrustedCertificateRepository` + `TrustedCertificateStore` port (DataStore): `accept(info)`, `remove(host)`, `observe()`. JVM tests per function.
  - [ ] `RemoteBrowseRepository.list(path): Flow<RemoteListing>` (Room `remote_cache` per path, so offline browsing shows what was loaded before; filter supported image mimes + folders). Tests.
  - [ ] `RemoteShareRepository.prepare(items)` (D48): downloads to a temporary cache folder with progress, returns a `ShareSpec`; `FileProvider` limited to that folder; old files removed at the next share and at app start. Tests: progress, failure, offline, cleanup; Robolectric: URIs and read grants.
  - [ ] `RemoteFileOperations` (D46): `move(items, targetFolder, conflictPolicy)` → per-item result (moved, renamed, replaced, skipped, failed), `undoMove`, `delete(items)` / `undoDelete` via the trash bin, `canWrite(folder)` from `oc:permissions`, `nextFreeName("IMG_1.jpg")` → "IMG_1 (2).jpg". Tests per function and per conflict policy.
- [ ] **8.6 `:feature:remote` and settings**
  - [ ] `NextcloudSettingsViewModel` / `NextcloudSettingsScreen` in `:feature:settings`: server URL input, "Sign in" opens Custom Tab with `loginUrl`, shows polling/progress, account info, logout, remote root folder picker (used in Phase 9). Tests per event; Compose tests per state.
  - [ ] `RemoteBrowserViewModel` (current path, listing, breadcrumbs, `LoginRequired` state) / `RemoteBrowserScreen` (folder list + thumbnail grid with previews, pull-to-refresh, offline/error state; login-required hint with a login button, D35). Tests per event; Compose tests.
  - [ ] Remote selection and context menu (D47): Select action, selection bar (Select all, Move), long-press menu (Move to folder…, Share, Delete) for one image or the whole selection. ViewModel tests per event; Compose test: long-press opens the menu.
  - [ ] Move sheet (D46): folder picker with breadcrumbs, read-only folders disabled, "New folder", "Move here" disabled in the source folder; conflict dialog (keep both, replace, skip, apply to all); snackbars with Undo; offline hint. Tests per event; Compose tests per state.
  - [ ] `CertificateWarningDialog` in `:core:ui` and the accept-and-retry flow in the login and remote ViewModels (D32). Tests: untrusted → dialog effect, accept → pinned and retried, cancel → error state; Compose test: details shown.
  - [ ] `TrustedCertificatesScreen` in `:feature:settings` (list, remove). Tests.
  - [ ] `RemoteViewerViewModel` / `RemoteViewerScreen`: pager over folder's images, Telephoto zoom loading full image via authenticated loader (download to cache), info sheet from `RemoteItem`. Tests.
  - [ ] Nextcloud tab and settings entries wired in `:app`.
- [ ] **8.7 Update docs** – ARCHITECTURE.md (Nextcloud client, credential storage), README (login, permissions: INTERNET) match code.

**Definition of done:** user signs in via browser without entering password in-app; app password stored encrypted; remote folders browse with server previews; full image opens only on view; logout revokes the app password and leaves no remote access, with a login hint shown; a self-signed server can be used after accepting the warning; tests for every function; CI green.

---

## Phase 9 – Upload status check and hand-off to the Nextcloud app

**Goal:** show a per-thumbnail "on Nextcloud" badge and hand images that are not uploaded yet to the Nextcloud Android app (D31).
_Depends on: Phases 4 (settings), 8 (WebDAV client, account)._

- [ ] **9.1 Model & settings**
  - [ ] `UploadState` (`Unknown`, `NotUploaded`, `Uploaded(verifiedByChecksum)`).
  - [ ] `remoteRoot` in settings (should match the Nextcloud app's upload folder); `RemoteTargetResolver.remotePathFor(item, remoteRoot)` = `<remoteRoot>/<local relative path>/<displayName>`. Tests: nesting, trailing slashes, special characters.
- [ ] **9.2 Database (migration → next version)**
  - [ ] `UploadStateEntity` (`upload_state`: media_id PK, remote_path, local_size, remote_etag, checksum_verified, state, checked_at). Migration test.
  - [ ] `UploadStateDao`: `upsertAll`, `observeStatusFor(ids)`, `observeStatusByBucket(bucketId)`, `deleteForMissingMedia()`, `clear()`. Tests per method.
- [ ] **9.3 Status check (`:core:data` + `:core:platform`)**
  - [ ] `ChecksumCalculator.sha1(InputStream)` / `md5(InputStream)` (streaming, JVM); content opened through the `MediaContentSource` port (`ContentResolverContentSource` adapter, Robolectric test). Tests with fixture bytes.
  - [ ] `ChecksumParser.parse("SHA1:… MD5:…")`. Tests.
  - [ ] `UploadStateMatcher.match(localItems, remoteListing)` – path+size match; if remote has `oc:checksums`, confirm via local hash. Tests: match, size mismatch, missing remote, checksum confirm/mismatch.
  - [ ] `UploadStateRepository.refreshAll()`: one PROPFIND per local folder → matcher → upsert. JVM tests: success, offline → retryable failure, logged out → no-op.
  - [ ] `UploadStateSyncWorker` in `:core:platform` (periodic e.g. 6 h, network constraint; one-shot via `requestUploadStateSync()`), maps the result. Tests (Robolectric): success, retry.
- [ ] **9.4 Hand-off (`:core:data` + `:core:platform`)**
  - [ ] `NextcloudAppSource` port; `PackageManagerNextcloudAppSource` + manifest `<queries>` for `com.nextcloud.client`. Test (Robolectric): installed / not installed.
  - [ ] `UploadHandoffRepository.prepare(ids)`: drops `Uploaded` items, returns a `ShareSpec` targeted at `com.nextcloud.client`; `NothingToUpload` when all are uploaded; `NextcloudAppMissing` when not installed. `onReturnedFromHandoff()` → `requestUploadStateSync()`. JVM tests per outcome.
  - [ ] `ShareIntentFactory` honours a target package (explicit intent). Robolectric test.
- [ ] **9.5 UI**
  - [ ] `UploadBadge` composable in `:core:ui` (icon per `UploadState`, content description). Compose test per state.
  - [ ] Folder grid and timeline ViewModels combine paging items with `observeStatusFor`; badges shown only when logged in. Tests.
  - [ ] "Upload" action in viewer, `SelectionTopBar` and selection overview: hand-off of the not-yet-uploaded items; disabled with a hint when the Nextcloud app is missing; hidden when logged out. Tests per case.
  - [ ] Settings: remote root. Tests.
- [ ] **9.6 Update docs** – ARCHITECTURE.md (upload-state algorithm, hand-off) and README "How already uploaded is determined" match code.

**Definition of done:** badges reflect server state without blocking the grid; selected images that are not on the server open in the Nextcloud app's upload screen; badges update after returning; already-present files are not handed over again; tests for every function; CI green.

---

## Phase 10 – Hardening & release

**Goal:** fast, accessible release build for local installation. F-Droid distribution is deferred (D28).
_Depends on: Phases 1–9._

- [x] **10.0 Apply design** – replace the placeholder theme tokens with the values in [DESIGN.md](DESIGN.md) (delivered; can be done now): five accents × light/dark, fixed roles, always-dark viewer colours, type scale, shapes and the new dimension tokens. Adjust screens only where DESIGN.md changes the layout (selection ring, badges, Appearance, About with version and source link). Set a `versionName`. Compose tests: theme per accent and mode. _Done before Phase 4. Applied to all existing screens, with new wording, states, icons and the Select action. The rest comes with the screens that need it: settings icon, Appearance and About in 4.3, upload action and badges in Phase 9. `versionName` was already set._
- [ ] **10.1 Performance**
  - [ ] `:benchmark` (`com.android.test`) module with Macrobenchmark: cold start, folder grid scroll, timeline fast-scroll, viewer swipe. Run on the Pixel 10a against the D24 targets.
  - [ ] Baseline Profile generator (`androidx.baselineprofile` plugin) for `:app`; commit `baseline-prof.txt`. Verify startup improvement.
  - [ ] Profile indexing on 50k images (time, memory); tune batch size, Room transactions. Record results in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).
  - [ ] StrictMode in debug builds; fix disk/network on main thread.
- [ ] **10.2 Accessibility**
  - [ ] Content descriptions for thumbnails (date/name), badges, actions; TalkBack traversal of grids and viewer.
  - [ ] Touch targets ≥ 48 dp, contrast check, font scaling 200 %; scrollbar operable via accessibility actions.
  - [ ] Compose UI tests asserting semantics on key screens; Accessibility Scanner pass.
- [ ] **10.3 Strings**
  - [ ] All user strings in `strings.xml` (lint `HardcodedText` as error); plurals for counts.
  - [ ] English only (D34); no translations or per-app language support in v1.
- [ ] **10.4 Release build**
  - [ ] R8 full mode, `isMinifyEnabled`/`isShrinkResources`; keep rules for kotlinx.serialization routes, Tink, osmdroid, Room. Run instrumented smoke tests on `release`-like `benchmark` build type.
  - [ ] Check that all native libraries in the APK are 16 KB page aligned (D40).
  - [ ] Verify the dependency tree has no proprietary dependencies (GMS/Firebase) and only GPL-3.0-compatible licenses.
  - [ ] _Deferred until distribution (D28):_ reproducible build settings (no build timestamps, `dependenciesInfo { includeInApk = false }`), Fastlane metadata in `fastlane/metadata/android/`, F-Droid submission.
  - [ ] Release signing with a local keystore via `keystore.properties` (never committed); `signingConfigs.release`.
  - [ ] Versioning scheme (`versionCode` from tag), CHANGELOG.
  - [ ] _Deferred until distribution (D28):_ GitHub Actions `release.yml` building signed artifacts on tag.
- [ ] **10.5 Final checks**
  - [ ] Full manual pass on the reference Pixel 10a (Android 17); instrumented suite green on a local API 37 emulator and on the Pixel 10a.
  - [ ] Third-party license screen (`:feature:settings` About) generated from dependencies.
  - [ ] Privacy: confirm no analytics/tracking, network only to user's Nextcloud and OSM tiles.
- [ ] **10.6 Update docs** – README (status, building, installing the release build), ARCHITECTURE.md final state, DESIGN_DECISIONS.md resolved questions.

**Definition of done:** signed, minified release build passes benchmarks and accessibility checks, contains no proprietary dependencies and installs on the Pixel 10a.
