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

- [ ] **0.1 Gradle basics**
  - [ ] Add Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`), pin a current stable Gradle.
  - [ ] `settings.gradle.kts`: `pluginManagement` (`includeBuild("build-logic")`), `dependencyResolutionManagement` (google, mavenCentral; `FAIL_ON_PROJECT_REPOS`), `rootProject.name`, enable `TYPESAFE_PROJECT_ACCESSORS`.
  - [ ] Root `build.gradle.kts` declaring plugins with `apply false`.
  - [ ] `gradle.properties`: `org.gradle.jvmargs`, `org.gradle.caching=true`, `org.gradle.configuration-cache=true`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `kotlin.code.style=official`.
  - [ ] `.gitignore` for Android/Gradle/IDE files; `local.properties` excluded.
- [ ] **0.2 Version catalog** `gradle/libs.versions.toml`
  - [ ] Versions/libraries: AGP, Kotlin, KSP, Compose BOM, Material 3, Activity Compose, Lifecycle (runtime/viewmodel compose), Navigation Compose, kotlinx.serialization, kotlinx.coroutines, Hilt (+ hilt-navigation-compose, hilt-work), Room (+ room-paging), Paging 3 (+ paging-compose), DataStore, WorkManager, Coil 3 (+ coil-network-okhttp), Telephoto `zoomable-image-coil`, ExifInterface, osmdroid, OkHttp, Tink, Browser (Custom Tabs).
  - [ ] Test libraries: JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, androidx.test (core, ext-junit, runner), room-testing, work-testing, MockWebServer, compose-ui-test-junit4, compose-ui-test-manifest.
  - [ ] Plugin aliases: android-application, android-library, kotlin-android, kotlin-serialization, compose-compiler, ksp, hilt, room, detekt, ktlint, baselineprofile.
- [ ] **0.3 Convention plugins** in `build-logic/convention`
  - [ ] `build-logic/settings.gradle.kts` reusing the version catalog.
  - [ ] `AndroidApplicationConventionPlugin` (`nextgallery.android.application`): minSdk/compileSdk/targetSdk 37 (Android 17), JVM toolchain 21.
  - [ ] `AndroidLibraryConventionPlugin` (`nextgallery.android.library`).
  - [ ] `AndroidComposeConventionPlugin` (`nextgallery.android.compose`): Compose compiler plugin, BOM, tooling.
  - [ ] `AndroidHiltConventionPlugin` (`nextgallery.android.hilt`): Hilt + KSP.
  - [ ] `AndroidRoomConventionPlugin` (`nextgallery.android.room`): Room plugin, KSP, `schemaDirectory("$projectDir/schemas")`.
  - [ ] `AndroidFeatureConventionPlugin` (`nextgallery.android.feature`): library + compose + hilt + deps on `:core:ui`, `:core:model`, `:core:common`, `:core:data`.
  - [ ] `JvmLibraryConventionPlugin` (`nextgallery.jvm.library`) for the pure Kotlin/JVM modules (`:core:model`, `:core:common`, `:core:data`, `:core:nextcloud`): `kotlin("jvm")`, toolchain 21, `javax.inject`, JVM test libraries.
  - [ ] `AndroidTestConventionPlugin` / shared test config: add the standard test libraries, `testOptions.unitTests.isIncludeAndroidResources = true` for Robolectric 4.17+ (first release with SDK 37), Robolectric `--add-opens` JVM flags in `unitTests.all { jvmArgs(...) }`, `testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"` for on-device tests (ARCHITECTURE.md §13.1). Placeholder Robolectric test and placeholder instrumented test both pass (`test` on CI, `connectedDebugAndroidTest` locally on an API 37 emulator or the Pixel 10a).
  - [ ] `DetektConventionPlugin` and `KtlintConventionPlugin` applied to every module.
- [ ] **0.4 Module skeletons** (each with `build.gradle.kts`, namespace `de.drehtuer.nextgallery.<module>` (package name to be confirmed, see O15 in DESIGN_DECISIONS.md), empty `src/main`, a placeholder test so the test task runs)
  - [ ] `:app` (`MainActivity`, `NextGalleryApplication` with `@HiltAndroidApp`, empty Compose `NextGalleryTheme` call).
  - [ ] JVM: `:core:model`, `:core:common`, `:core:data`, `:core:nextcloud`.
  - [ ] Android: `:core:database`, `:core:media`, `:core:platform`, `:core:ui`.
  - [ ] `:feature:folders`, `:feature:timeline`, `:feature:map`, `:feature:viewer`, `:feature:editor`, `:feature:remote`, `:feature:settings`.
  - [ ] Add a module-graph check (e.g. a Gradle task or detekt rule) that fails if a `:feature:*` depends on another `:feature:*`.
  - [ ] `./gradlew assembleDebug` succeeds and installs an empty-screen app.
- [ ] **0.5 Static analysis**
  - [ ] `.editorconfig` with ktlint rules (`ktlint_code_style = android_studio`, Compose function naming allowed).
  - [ ] `config/detekt/detekt.yml` (baseline from defaults, Compose rule set if used), `detekt-baseline.xml` empty.
  - [ ] Android Lint config `lint.xml`, `warningsAsErrors` for new code; `abortOnError = true`.
- [ ] **0.5a Platform behaviour review** – go through the Android 15–17 behaviour changes for apps targeting SDK 37 (edge-to-edge, predictive back, foreground-service limits for `dataSync` uploads, media permission changes) and record anything that changes the plan in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).
- [ ] **0.6 CI** `.github/workflows/ci.yml`
  - [ ] Trigger on push and pull_request; JDK 21 (Temurin); `gradle/actions/setup-gradle` for caching.
  - [ ] Jobs/steps: `ktlintCheck`, `detekt`, `lint`, `test` (JVM modules + Android local unit tests incl. Robolectric), `assembleDebug`. No emulator job; instrumented tests run locally.
  - [ ] Upload test and lint reports as artifacts on failure.
  - [ ] Gradle wrapper validation action.
- [ ] **0.7 Repo hygiene**
  - [ ] `LICENSE` placeholder: record the license decision (e.g. GPL-3.0 vs Apache-2.0, constrained by osmdroid/Telephoto licenses and F-Droid) in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md); add the file once decided.
  - [ ] `CONTRIBUTING.md` section or README note on running `./gradlew ktlintFormat detekt testDebugUnitTest`.
  - [ ] PR template with checklist "tests added for every new function", "docs updated".
- [ ] **0.8 Update docs** – ARCHITECTURE.md module list and README "Building" section match the actual build.

**Definition of done:** CI is green on an empty multi-module project; all 16 modules exist; the four JVM modules build without the Android plugin; lint/detekt/ktlint run on every module; a feature→feature dependency fails the build.

---

## Phase 1 – Skeleton, permissions, MediaStore indexing into Room

**Goal:** the app asks for media permissions and keeps a Room index of all local images in sync with MediaStore.
_Depends on: Phase 0._

- [ ] **1.1 `:core:common` (JVM)**
  - [ ] `DispatcherProvider` interface + `DefaultDispatcherProvider` (`io`, `default`, `main`). Test: `TestDispatcherProvider` returns injected `StandardTestDispatcher`.
  - [ ] `AppResult<T>` / `runCatchingCancellable {}` helper that rethrows `CancellationException`. Tests: success, failure, cancellation rethrown.
  - [ ] `Clock` abstraction (`SystemClock` impl) for testable timestamps. Test: returns injected instant.
  - [ ] `Debouncer` / `Flow.debounceSignals()` helper. Test with virtual time: bursts collapse to one emission.
- [ ] **1.2 `:core:model` (JVM)**
  - [ ] `MediaId` (value class over MediaStore `_ID`), `MediaItem` (id, contentUri, displayName, relativePath, bucketId, bucketName, mimeType, sizeBytes, width, height, orientation, dateTakenMillis, dateModifiedSeconds, generationModified?, latitude?, longitude?, takenLocalDate).
  - [ ] `Folder` (bucketId, name, relativePath, itemCount, coverItem).
  - [ ] `SupportedMimeTypes` (`image/jpeg`, `image/png`, `image/webp`, `image/heic`, `image/heif`, `image/gif`) with `isSupported(mime)`. Test: each supported/unsupported type.
  - [ ] `FolderRule` (path, `Include`/`Exclude`) and `FolderRules.matches(relativePath)`; default excludes (`WhatsApp/Media/.Statuses`, `Android/data`). Tests: include match, exclude wins over include, prefix vs. sibling (`DCIM/Cam` vs `DCIM/Camera`), empty include list = all.
  - [ ] `MediaAccess` enum (`Full`, `Partial`, `Denied`), `ScannedImage`, `RawExif`, `PlatformConfirmation` (opaque handle).
- [ ] **1.3 `:core:data` (JVM) – ports and indexing logic**
  - [ ] Ports: `MediaScanSource` (`fullScan()`, `changesSince(generation)`, `allIds()`, `mediaStoreVersion()`, `noMediaPaths()`), `MediaChangeSource`, `ExifSource`, `MediaPermissionSource`, `MediaIndexStore`, `SyncStateStore`, `WorkScheduler`. Plus hand-written fakes in `testFixtures`.
  - [ ] `NoMediaMatcher.isHidden(relativePath, noMediaPaths)`. Tests: folder with `.nomedia`, nested subfolder, unrelated sibling.
  - [ ] `ExifDateParser.parse(dateTimeOriginal, offsetTimeOriginal, subSec)`. Tests: plain, with offset, with sub-seconds, malformed → null.
  - [ ] `DateResolver.resolve(dateTaken, exifDate, dateModified, zone): ResolvedDate` (DATE_TAKEN → EXIF → DATE_MODIFIED, local date). Tests: each fallback branch, zone boundary at midnight.
  - [ ] `MediaAccessResolver.resolve(grantedPermissions)` and `requiredPermissions()`. Tests: full, selected only, none.
  - [ ] `MediaIndexer` with `fullScan()` and `incrementalSync()` against fake ports:
    - [ ] full scan: stream scan source, apply `FolderRules` + `NoMediaMatcher`, resolve taken date, upsert in batches, delete rows not seen. Tests: insert new, update changed, delete removed, excluded folder skipped, `.nomedia` skipped.
    - [ ] incremental: `changesSince(lastGeneration)` plus id-diff for deletions; fall back to full scan when the MediaStore version changed. Tests: changed-only upsert, deleted item removed, version change → full scan.
    - [ ] persist sync state after success. Test: state written only on success.
  - [ ] `IndexRepository.sync(mode): AppResult<IndexStats>` and `observeStatus()`. Tests: success, scan failure → `Failure`.
  - [ ] `IndexScheduler`: `onAppStart()` → `WorkScheduler.enqueueIndex(FULL)` if never indexed, else `INCREMENTAL`; collects `MediaChangeSource` debounced (2 s) → incremental; media access change → full. Tests: first launch → full, later → incremental, burst of changes → one enqueue, access change → full.
  - [ ] `MediaRepository` interface + `DefaultMediaRepository`: `observeIndexedCount()`, `getItem(id)`. Tests per method with fake `MediaIndexStore`.
- [ ] **1.4 `:core:database` (Android adapter)**
  - [ ] `MediaEntity` (`media` table; PK `id`; indices on `bucket_id`, `taken_local_date`, `date_modified`, `relative_path`).
  - [ ] `SyncStateEntity` (`sync_state`: last full scan time, last `GENERATION_MODIFIED`, MediaStore version string).
  - [ ] `MediaDao`: `upsertAll`, `deleteByIds`, `deleteNotIn(ids)` (chunked), `getAllIdsWithModified()`, `observeCount()`, `getById()`. Tests (in-memory Room, Robolectric): one per method.
  - [ ] `SyncStateDao`: `get()`, `put()`. Tests: default empty, round-trip.
  - [ ] `NextGalleryDatabase` (version 1, exported schema in `core/database/schemas`). Test: `MigrationTestHelper` setup ready for future migrations.
  - [ ] `RoomMediaIndexStore`, `RoomSyncStateStore` implementing the ports, with entity ↔ model mappers; `DatabaseModule` (Hilt) binding them. Tests: mapper round-trip; each store method delegates to the DAO (Robolectric, in-memory Room).
- [ ] **1.5 `:core:media` (Android adapter)**
  - [ ] `MediaStoreQuery`: projection, selection for supported mime types, `IS_PENDING = 0`, `IS_TRASHED = 0`. Test: generated selection/args (Robolectric).
  - [ ] `CursorMapper.toScannedImage(cursor)`. Test with `MatrixCursor` (Robolectric): all columns, nulls.
  - [ ] `MediaStoreScanner` implementing `MediaScanSource` (batches of 500, `GENERATION_MODIFIED`, `.nomedia` paths from `MediaStore.Files`). Tests: instrumented (local emulator / Pixel 10a) with inserted test images, one per method (ARCHITECTURE.md §13.1).
  - [ ] `AndroidExifSource` implementing `ExifSource` (`androidx.exifinterface`, `MediaStore.setRequireOriginal` when `ACCESS_MEDIA_LOCATION` is granted); returns raw tag strings, no parsing. Tests: tag extraction from sample JPEGs in `src/test/resources` (Robolectric); `setRequireOriginal` with/without permission instrumented.
  - [ ] `MediaStoreChangeObserver` implementing `MediaChangeSource` (`callbackFlow` over `ContentObserver`). Test: register/unregister on collect/cancel (Robolectric).
  - [ ] `AndroidMediaPermissionSource` implementing `MediaPermissionSource`. Test: granted set reflects shadowed permissions (Robolectric).
  - [ ] `MediaModule` (Hilt) binding the adapters.
- [ ] **1.6 `:core:platform` (Android adapter and wiring)**
  - [ ] `IndexWorker` (`@HiltWorker`, `CoroutineWorker`): input `mode`, calls `IndexRepository.sync(mode)`, maps `Success` → `success()`, `Failure` → `retry()`. Tests (`TestListenableWorkerBuilder`, Robolectric): success, failure → retry.
  - [ ] `WorkManagerScheduler` implementing `WorkScheduler`: unique work `index`, `KEEP` for app start, `APPEND_OR_REPLACE` for changes. Tests (`WorkManagerTestInitHelper`): enqueued request has the expected name, policy and input.
  - [ ] `DispatchersModule`, `ApplicationScopeModule`, `RepositoryModule` (binds `:core:data` interfaces). WorkManager initialization via `Configuration.Provider` with `HiltWorkerFactory` in `NextGalleryApplication` (remove default initializer in manifest).
- [ ] **1.7 Permissions UI**
  - [ ] `:app` manifest: `READ_MEDIA_IMAGES`, `READ_MEDIA_VISUAL_USER_SELECTED`, `ACCESS_MEDIA_LOCATION`. (`INTERNET` added in Phase 8.)
  - [ ] `:core:ui` `rememberMediaPermissionState()` + `PermissionRationale` composable (explain, request, "select more photos" for partial, open app settings when permanently denied). Compose UI tests: each state renders the correct action.
- [ ] **1.8 `:core:ui` foundation**
  - [ ] `NextGalleryTheme` (Material 3, light/dark) with placeholder colour, typography, shape and spacing tokens; the final values come from Claude Design later (D22). Compose test: renders in light/dark.
  - [ ] Common composables: `LoadingIndicator`, `EmptyState`, `ErrorState`. Compose UI test each.
- [ ] **1.9 `:app` shell**
  - [ ] Type-safe routes (`@Serializable object FoldersRoute`, …) in `:app` `navigation/Routes.kt`; `NextGalleryNavHost`.
  - [ ] Bottom navigation bar (Folders, Timeline, Map, Nextcloud) with placeholder screens. Compose UI test: tab switching.
  - [ ] `MainViewModel` exposing `MainUiState(mediaAccess, indexedCount)`; calls `IndexScheduler.onAppStart()`. JVM tests: state on permission granted/denied.
  - [ ] Debug-only screen/log showing indexed count to verify indexing.
- [ ] **1.10 Update docs** – ARCHITECTURE.md (indexing flow, DB schema v1) and README match code.

**Definition of done:** on a device with photos, granting permission fills the `media` table; adding/deleting a photo updates the index within seconds; partial access indexes only selected photos; all new functions have tests; CI green.

---

## Phase 2 – Folder view with thumbnail grid and fast scroll

**Goal:** default Folders screen listing folders and a paged thumbnail grid per folder with a draggable scrollbar.
_Depends on: Phase 1._

- [ ] **2.1 Database**
  - [ ] `FolderDao.observeFolders(): Flow<List<FolderRow>>` (GROUP BY bucket, count, newest item as cover). Test: grouping, count, cover selection.
  - [ ] `MediaDao.pagingByBucket(bucketId): PagingSource<Int, MediaEntity>` ordered by taken date desc. Test: `PagingSource.load` ordering and page size.
  - [ ] `MediaDao.countByBucket(bucketId)`. Test.
- [ ] **2.2 Repository**
  - [ ] `FolderRepository.observeFolders(): Flow<List<Folder>>`. Test with Turbine.
  - [ ] `MediaRepository.pagedFolderItems(bucketId): Flow<PagingData<MediaItem>>` (`Pager`, page size ~ 120, `enablePlaceholders = true` for fast scroll). Test with `asSnapshot()`.
- [ ] **2.3 Thumbnails (`:core:ui` + `:app`)**
  - [ ] `ImageLoaderModule` providing a singleton Coil 3 `ImageLoader` (memory cache %, disk cache dir/size, `crossfade` off for grids). Test: config values.
  - [ ] `MediaThumbnailKeyer`/fetcher using `ContentResolver.loadThumbnail(uri, size)` with fallback decode. Tests: keyer output stable per (id, dateModified, size) (JVM); `loadThumbnail` at the requested size (instrumented).
  - [ ] `ThumbnailCell` composable (fixed aspect, placeholder, content description, selection overlay slot, badge slot for Phase 9). Compose UI test: placeholder, selected state.
- [ ] **2.4 Shared grid components (`:core:ui`)**
  - [ ] `MediaGrid` (`LazyVerticalGrid`, adaptive columns, stable keys, `LazyPagingItems`). Compose test: renders N items, keys stable.
  - [ ] `ScrollbarMath.indexForFraction()` / `fractionForIndex()` in `:core:data` (JVM tests), and the `FastScrollbar` composable (draggable thumb, optional `label: (index) -> String?` bubble). Compose test: drag scrolls grid.
  - [ ] `SelectionState` in `:core:data` (set of `MediaId`, toggle, range, clear; JVM tests) and `SelectionTopBar` in `:core:ui` (count, actions slot). Unit tests per function; Compose test: long-press enters selection.
- [ ] **2.5 `:feature:folders`**
  - [ ] `FolderListViewModel` → `StateFlow<FolderListUiState>` (Loading/Empty/Content). Tests with Turbine: each state.
  - [ ] `FolderListScreen` (grid of folder covers with name/count). Compose UI test: click emits `onFolderClick(bucketId)`.
  - [ ] `FolderGridViewModel` (route passed via Hilt assisted injection, see ARCHITECTURE.md §7), exposes `PagingData` + selection. JVM tests: selection events, paging flow wired to the route's bucket.
  - [ ] `FolderGridScreen` (MediaGrid + FastScrollbar + SelectionTopBar). Compose UI test: item click → `onOpenViewer(mediaId, source)`.
  - [ ] Navigation entry points `folderListScreen()` / `folderGridScreen()` as `NavGraphBuilder` extensions; wired in `:app`.
- [ ] **2.6 Performance check** – Macrobenchmark smoke (or manual profiling) scrolling 10k items; no dropped-frame regressions noted in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md) if any trade-offs made.
- [ ] **2.7 Update docs** – ARCHITECTURE.md (paging, thumbnail pipeline) and README match code.

**Definition of done:** Folders is the start screen; opening a folder shows a smoothly scrolling grid; scrollbar drag jumps through 10k+ images; tests for every new function; CI green.

---

## Phase 3 – Full-screen viewer, delete, share

**Goal:** swipeable zoomable viewer with image info, plus delete and share for single and multi-selection.
_Depends on: Phase 2 (grid, selection)._

- [ ] **3.1 Model & data**
  - [ ] `MediaSource` sealed type (`Folder(bucketId)`, `Timeline`, `Map(bounds?)`) so the viewer pages over the same list as the caller. Test: serialization round-trip (used in routes).
  - [ ] `MediaRepository.pagedItems(source)` and `indexOf(source, mediaId)`. Tests per source.
  - [ ] `ImageDetails` model (date, size, resolution, mime, path, lat/long, camera make/model, exposure, ISO, focal length…). `ImageDetailsMapper.from(rawExif, item)` in `:core:data` (JVM tests per field, missing values); `AndroidExifSource` extended with the camera tags (Robolectric fixture test).
- [ ] **3.2 Delete (`:core:media` + `:core:data`)**
  - [ ] `MediaDeleteSource` port; `MediaStoreDeleter` adapter: `createTrashRequest` (default) / `createDeleteRequest` (permanent) wrapped in `PlatformConfirmation`. Tests (Robolectric): trash, permanent.
  - [ ] `MediaActionsRepository.delete(ids): DeleteOutcome` (`NeedsUserConfirmation(intentSender)`, `Failed`) and `onDeleteConfirmed(ids)` removing rows from Room immediately. Tests per outcome.
  - [ ] `:core:ui` `rememberConfirmationLauncher()` unwrapping `PlatformConfirmation` and launching it via `StartIntentSenderForResult`. Compose test: result forwarded.
- [ ] **3.3 Share**
  - [ ] `ShareSpec.from(items)` in `:core:data` (single vs multiple, common mime or `image/*`; JVM tests) and `ShareIntentFactory` in `:core:ui` turning it into `ACTION_SEND`/`ACTION_SEND_MULTIPLE` with `FLAG_GRANT_READ_URI_PERMISSION` and `ClipData` (Robolectric test).
- [ ] **3.4 `:feature:viewer`**
  - [ ] `ViewerRoute(source: MediaSource, mediaId: Long)`.
  - [ ] `ViewerViewModel`: paging over source, current index, `ViewerUiState(currentItem, chromeVisible, details?, pendingDelete?)`; events `PageChanged`, `ToggleChrome`, `ShowInfo`, `Delete`, `Share`, `DeleteConfirmed`. Tests per event.
  - [ ] `ViewerScreen`: `HorizontalPager` + Telephoto `ZoomableAsyncImage` (Coil, subsampling, double-tap zoom); system-bars hide/show; top bar (back, share, delete, edit placeholder, info). Compose UI tests: swipe changes page, tap toggles chrome, delete shows confirmation flow.
  - [ ] `ImageInfoSheet` (ModalBottomSheet with `ImageDetails`). Compose test: fields rendered, missing values hidden.
  - [ ] Handle item disappearing (deleted externally) → move to neighbour or close. Test in ViewModel.
- [ ] **3.5 Multi-select actions in grids**
  - [ ] Add delete/share actions to `SelectionTopBar` in `FolderGridScreen`; `FolderGridViewModel` uses `MediaActionsRepository` / `ShareIntentFactory`. Tests: events → outcomes.
- [ ] **3.6 `:app`** – route `ViewerRoute` from folder grid; handle `ACTION_VIEW` intent for `image/*` content URIs (open viewer for a single external item). Test: intent parsing.
- [ ] **3.7 Update docs** – ARCHITECTURE.md (viewer, delete flow) and README match code.

**Definition of done:** open image from grid, swipe, pinch/double-tap zoom large images without OOM, view info, share 1..n images, delete 1..n images to the system trash (with undo) with grid updating; tests for every function; CI green.

---

## Phase 4 – Settings: folder include/exclude list

**Goal:** users choose which folders are indexed; changes re-index.
_Depends on: Phase 1 (FolderRules, IndexScheduler)._

- [ ] **4.1 Settings storage (`:core:data`)**
  - [ ] `UserSettings` model in `:core:model` (folderRules, gridColumns, theme mode, defaultView). 
  - [ ] Proto or Preferences DataStore `SettingsDataSource` (`settings.preferences_pb`), serializer for rules. Tests: read defaults, write/read each field, corrupt-file handler.
  - [ ] `SettingsRepository`: `observeSettings()`, `addRule()`, `removeRule()`, `setGridColumns()`, `setThemeMode()`. Tests per function.
- [ ] **4.2 Rule application**
  - [ ] `MediaIndexer` reads rules from `SettingsRepository` (replace Phase 1 defaults). Test: rules respected.
  - [ ] `RuleChangeObserver`: rules change → enqueue full scan (rule change can add items). Test: change → enqueue once.
  - [ ] `FolderRepository.observeAllDeviceFolders()` – folders from MediaStore regardless of rules (needed to pick includes). Test.
- [ ] **4.3 `:feature:settings`**
  - [ ] `SettingsViewModel` / `SettingsScreen` (root list: Folders, Appearance, Nextcloud placeholder, About/licenses). Tests: state mapping, navigation callbacks.
  - [ ] `FolderRulesViewModel` / `FolderRulesScreen`: list of device folders with Include/Exclude/Default toggles, add custom exclude pattern, show `.nomedia` folders as excluded (read-only). Tests: each toggle event; Compose test: toggling updates UI.
  - [ ] Appearance: theme mode, grid columns. Tests.
  - [ ] Wire `NextGalleryTheme` and grids to settings. Test: `MainViewModel` emits theme.
- [ ] **4.4 Update docs** – ARCHITECTURE.md (settings storage) and README match code.

**Definition of done:** excluding a folder removes its items from all views after re-index; including restores them; settings persist across restarts; tests for every function; CI green.

---

## Phase 5 – Timeline view (year/month/day)

**Goal:** all indexed images grouped by year → month → day with sticky headers and a date-labelled fast scroll.
_Depends on: Phases 2 (grid), 3 (viewer)._

- [ ] **5.1 Database**
  - [ ] `MediaDao.pagingTimeline(): PagingSource<Int, MediaEntity>` ordered by `taken_local_date DESC, date_taken DESC`. Test.
  - [ ] `MediaDao.observeDayCounts(): Flow<List<DayCount>>` (date, count) for headers and scrollbar map. Test.
- [ ] **5.2 Repository**
  - [ ] `TimelineRepository.pagedTimeline(): Flow<PagingData<TimelineEntry>>` inserting `Header(year, month, day)` separators via `insertSeparators`. Tests: separators at day/month/year boundaries.
  - [ ] `TimelineIndex` built from day counts: `labelForPosition(index): "MMMM yyyy"`, `positionForMonth(yearMonth)`. Tests per function.
  - [ ] Grouping granularity enum (`Day`, `Month`, `Year`) for pinch-to-change density (optional, behind setting). Tests.
- [ ] **5.3 `:feature:timeline`**
  - [ ] `TimelineViewModel` → `TimelineUiState` (paging flow, granularity, selection). Tests per event.
  - [ ] `TimelineScreen`: `LazyVerticalGrid` with full-span `stickyHeader`-style headers (Compose `LazyGrid` header items + overlay sticky header), `FastScrollbar` with month/year label, multi-select delete/share. Compose UI tests: headers shown, scrollbar label text.
  - [ ] Open viewer with `MediaSource.Timeline`. Test.
- [ ] **5.4 Update docs** – ARCHITECTURE.md (timeline date resolution, separators) and README match code.

**Definition of done:** timeline shows correct local dates (EXIF fallback verified), sticky headers, scrollbar shows year/month while dragging, smooth on 10k+ items; tests for every function; CI green.

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
- [ ] **6.3 `:feature:map`**
  - [ ] `MapRenderer` interface (`setClusters`, `setViewport`, `onViewportChanged`, `onClusterClick`) + `OsmdroidMapRenderer` in `AndroidView` (user agent set, tile cache in app cache dir, no Play Services). Test: `FakeMapRenderer` used in ViewModel tests; osmdroid config (user agent, tile URL) in Robolectric; instrumented test (local) that the map renders clusters.
  - [ ] Tile source setting (default OSM tile server, configurable URL; D23) in `SettingsRepository`, read by `OsmdroidMapRenderer`. Tests: default value, custom URL applied.
  - [ ] Cluster marker drawable with thumbnail + count (Coil-loaded bitmap). Test: count text formatting.
  - [ ] `MapViewModel` → `MapUiState(clusters, viewport, permissionNeeded)`; events `ViewportChanged`, `ClusterClicked`. Tests per event.
  - [ ] `MapScreen`: map, permission prompt for `ACCESS_MEDIA_LOCATION`, empty state "no geotagged images". Compose UI test: states.
  - [ ] Cluster tap → bottom-sheet grid of cluster items or viewer with `MediaSource.Map(ids/bounds)`. Test.
- [ ] **6.4 Update docs** – ARCHITECTURE.md (MapRenderer abstraction, clustering) and README match code.

**Definition of done:** geotagged images appear as clusters that split on zoom; tapping opens images; images without GPS are absent; works without Play Services; tests for every function; CI green.

---

## Phase 7 – Editor: rotate, crop, scale, pen

**Goal:** non-destructive edit pipeline with undo/redo, saving a new copy or overwriting with EXIF preserved.
_Depends on: Phase 3 (viewer entry point, write/delete request helpers)._

- [ ] **7.1 Model (`:core:model`)**
  - [ ] `EditOperation` sealed interface: `Rotate(quarterTurns)`, `Crop(normalizedRect)`, `Scale(targetWidth, targetHeight)`, `PenStroke(points, colorArgb, widthPx-normalized)`.
  - [ ] `EditHistory` (immutable list + cursor): `push`, `undo`, `redo`, `canUndo`, `canRedo`, `operations`. Tests per function, push after undo truncates redo.
  - [ ] `AspectRatio` (`Free`, `Square`, `R4_3`, `R16_9`, `Original`) + `CropMath.constrain(rect, ratio, bounds)`. Tests per ratio.
  - [ ] `ScaleMath.fromPercent()` / `fromTargetWidth()` / `fromTargetHeight()` keeping aspect. Tests.
  - [ ] `GeometryMapper`: maps pen/crop coordinates through prior rotate/crop/scale so ops compose. Tests per op order.
- [ ] **7.2 Rendering**
  - [ ] `EditGeometry.steps(ops, sourceSize): RenderPlan` in `:core:data` (output size, 3×3 matrices, crop rects, stroke paths in pixels, orientation bake-in). JVM tests: one per op type + combinations.
  - [ ] `MemoryBudget.maxPixels(maxMemory)` and the downscale decision for huge images (O17) in `:core:data`. JVM tests: fits, too large → scaled size.
  - [ ] `sampleSizeFor(source, maxSide)` in `:core:data`. JVM tests.
  - [ ] `ImageRenderer` port; `BitmapImageRenderer` adapter in `:core:media` (`ImageDecoder` decode, apply `RenderPlan` via `Matrix`/`Canvas`/`Path`, return `PreviewImage` handle). Tests (Robolectric, native graphics): applies a matrix, a crop and a stroke on a small bitmap.
  - [ ] `ImageEncoder.encode(bitmap, mime, quality)` (JPEG/PNG/WebP; HEIC saved as JPEG; GIF is not editable – see O11 in DESIGN_DECISIONS.md). Tests per mime (Robolectric); HEIC source decode → JPEG output as an instrumented test.
  - [ ] `ExifPolicy.forOutput(sourceTags, newWidth, newHeight)` in `:core:data` (which tags to copy, orientation normal, new dimensions; JVM tests) and `ExifWriter` in `:core:media` applying it (Robolectric fixture test).
- [ ] **7.3 Saving (`:core:data` + `:core:media`)**
  - [ ] `CopyNamer.nameFor(displayName, existingNames)` in `:core:data` (e.g. `IMG_1234_edited.jpg`, numbered on collision). JVM tests.
  - [ ] `MediaWriteSink` port; `MediaStoreWriter` adapter: `insertPending(displayName, relativePath, mime)`, `requestOverwrite(contentUri): PlatformConfirmation` (`createWriteRequest`), `publish()`, `cleanupPending()`. Tests (Robolectric): insert sets `IS_PENDING`, publish clears it; overwrite request built for the URI.
  - [ ] `EditRepository.save(sourceId, ops, mode = NewCopy|Overwrite): SaveOutcome` → triggers incremental index. Tests per mode/outcome.
- [ ] **7.4 `:feature:editor`**
  - [ ] `EditorRoute(mediaId)`; `EditorViewModel` → `EditorUiState(previewBitmap, activeTool, history, cropRect, penColor, penWidth, saving)`; events per tool, `Undo`, `Redo`, `Save(mode)`, `Discard`. Tests per event; preview re-render runs on `Default` dispatcher.
  - [ ] `EditorScreen` with tool bar; `RotateTool`, `CropOverlay` (handles + aspect chips), `ScaleDialog` (percent / width / height), `PenCanvas` (pointer input, color & width pickers). Compose UI tests per tool.
  - [ ] Unsaved-changes dialog on back; save mode dialog (new copy default, overwrite). Compose tests.
  - [ ] Viewer "Edit" button → `EditorRoute` in `:app`; after save, viewer navigates to the new copy. Test.
- [ ] **7.5 Update docs** – ARCHITECTURE.md (edit pipeline) and README match code.

**Definition of done:** all four tools work with undo/redo; saved copies appear in the gallery with correct orientation, dimensions and preserved EXIF; overwrite goes through system confirmation; no OOM on 50 MP images; tests for every function; CI green.

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
  - [ ] `:app` manifest: `INTERNET`, `ACCESS_NETWORK_STATE`. (`:core:nextcloud` is a JVM module and has no manifest.)
  - [ ] XML parsing on the XmlPullParser API: `compileOnly` dependency, provided by Android at runtime; kxml2 on the JVM test classpath.
  - [ ] `NextcloudHttpModule`: base `OkHttpClient` (timeouts, user agent), `AuthInterceptor` (Basic auth from account, OCS-APIRequest header). Test with MockWebServer: header present.
  - [ ] `LoginFlowV2Client`: `start(serverUrl)` → `{loginUrl, pollEndpoint, token}`; `poll()` until 200 or timeout. Tests (MockWebServer): start, 404 pending, success, expiry.
  - [ ] `ServerUrlNormalizer` (add https, strip `/index.php`, trailing slash). Tests per case.
  - [ ] `PropfindRequestBuilder` (depth 1, props: resourcetype, getcontentlength, getcontenttype, getetag, getlastmodified, oc:fileid, oc:checksums). Test: XML body.
  - [ ] `MultistatusParser` (XmlPullParser) → `List<RemoteItem>`. Tests with fixture XML: files, folders, missing props, URL-encoded names.
  - [ ] `WebDavClient`: `list(path)`, `download(path, sink)`, `upload(path, source, size)` (PUT, `X-OC-Mtime`), `mkcol(path)`. Tests (MockWebServer) per method incl. 401/404/507.
  - [ ] `RemotePathMapper`: `davUrl(account, path)` (`/remote.php/dav/files/<userId>/…`, segment encoding), `previewUrl(fileId, size)` (`/index.php/core/preview?fileId=…&x=…&y=…&a=1`). Tests.
- [ ] **8.4 Coil integration (`:core:ui` / `:app`)**
  - [ ] Separate authenticated Coil `ImageLoader` (or `OkHttpNetworkFetcherFactory` with auth client) qualified `@RemoteImageLoader`; disk-cache key by fileId+etag+size. Tests: keyer.
- [ ] **8.5 Repository (`:core:data`)**
  - [ ] `NextcloudAccountRepository`: `login(serverUrl): Flow<LoginState>` (Started(loginUrl) → Polling → Success/Failed), `logout()` (delete app password via `DELETE /ocs/v2.php/core/apppassword`, clear store). Tests per state.
  - [ ] `RemoteBrowseRepository.list(path): Flow<RemoteListing>` (in-memory cache per path, filter supported image mimes + folders). Tests.
- [ ] **8.6 `:feature:remote` and settings**
  - [ ] `NextcloudSettingsViewModel` / `NextcloudSettingsScreen` in `:feature:settings`: server URL input, "Sign in" opens Custom Tab with `loginUrl`, shows polling/progress, account info, logout, remote root folder picker (used in Phase 9). Tests per event; Compose tests per state.
  - [ ] `RemoteBrowserViewModel` (current path, listing, breadcrumbs, `NotSignedIn` state) / `RemoteBrowserScreen` (folder list + thumbnail grid with previews, pull-to-refresh, offline/error state). Tests per event; Compose tests.
  - [ ] `RemoteViewerViewModel` / `RemoteViewerScreen`: pager over folder's images, Telephoto zoom loading full image via authenticated loader (download to cache), info sheet from `RemoteItem`. Tests.
  - [ ] Nextcloud tab and settings entries wired in `:app`.
- [ ] **8.7 Update docs** – ARCHITECTURE.md (Nextcloud client, credential storage), README (login, permissions: INTERNET) match code.

**Definition of done:** user signs in via browser without entering password in-app; app password stored encrypted; remote folders browse with server previews; full image opens only on view; logout revokes the app password; tests for every function; CI green.

---

## Phase 9 – Upload status check and upload queue

**Goal:** show a per-thumbnail "on Nextcloud" badge and upload missing images through a durable queue.
_Depends on: Phases 4 (settings), 8 (WebDAV client, account)._

- [ ] **9.1 Model & settings**
  - [ ] `UploadStatus` enum (`Unknown`, `Uploaded`, `NotUploaded`, `Queued`, `Uploading`, `Failed`).
  - [ ] `FolderMapping(localRelativePath, enabled)` + `remoteRoot` in settings; `RemoteTargetResolver.remotePathFor(item, remoteRoot)` = `<remoteRoot>/<local relative path>/<displayName>`. Tests: nesting, trailing slashes, special characters.
- [ ] **9.2 Database (migration → next version)**
  - [ ] `UploadStateEntity` (`upload_state`: media_id PK, remote_path, local_size, remote_size, remote_etag, checksum_match?, status, checked_at). Migration test.
  - [ ] `UploadQueueEntity` (`upload_queue`: id, media_id, remote_path, state, attempts, last_error, enqueued_at). Migration test.
  - [ ] `UploadStateDao`: `upsertAll`, `observeStatusFor(ids)`, `observeStatusByBucket(bucketId)`, `deleteForMissingMedia()`. Tests per method.
  - [ ] `UploadQueueDao`: `enqueue`, `nextPending`, `markUploading/Done/Failed`, `observeQueue()`, `removeDone()`. Tests per method.
- [ ] **9.3 Status check (`:core:data`)**
  - [ ] `ChecksumCalculator.sha1(InputStream)` / `md5(InputStream)` (streaming, JVM); content opened through the `MediaContentSource` port (`ContentResolverContentSource` adapter, Robolectric test). Tests with fixture bytes.
  - [ ] `ChecksumParser.parse("SHA1:… MD5:…")`. Tests.
  - [ ] `UploadStateMatcher.match(localItems, remoteListing): List<UploadStateEntity>` – path+size match; if remote has `oc:checksums`, confirm via local hash. Tests: match, size mismatch, missing remote, checksum confirm/mismatch.
  - [ ] `UploadStateRepository.refreshAll()` in `:core:data`: one PROPFIND per mapped folder → matcher → upsert. JVM tests: success, offline → retryable failure, not signed in → no-op.
  - [ ] `UploadStateSyncWorker` in `:core:platform` (`@HiltWorker`, periodic e.g. 6 h, network constraint; also one-shot after login/mapping change via `WorkScheduler`), maps the result. Tests (Robolectric): success, retry.
- [ ] **9.4 Upload queue (`:core:data` + `:core:platform`)**
  - [ ] `UploadRepository.enqueue(ids)` (skip already `Uploaded`), `retry(id)`, `cancel(id)`, `observeQueue()`. Tests per function.
  - [ ] `UploadProcessor.drain(onProgress)` in `:core:data`: `mkcol` missing parents, PUT streaming from `MediaContentSource`, update `upload_state` to `Uploaded` on 201/204, conflict renaming (O6). JVM tests (MockWebServer + fakes): success, 401 → auth failure without retry, 5xx → retryable, parent creation, name conflict.
  - [ ] `UploadWorker` in `:core:platform` (`@HiltWorker`, unique work `upload`, `NetworkType.CONNECTED` / optional unmetered setting, exponential backoff, foreground notification with progress, `dataSync` type): calls `UploadProcessor`, maps the result. Tests (Robolectric): success, auth failure → failure, retryable → retry.
  - [ ] Notification channel `uploads` created in `:core:platform`. Test (Robolectric).
- [ ] **9.5 UI**
  - [ ] `UploadBadge` composable in `:core:ui` (icon per `UploadStatus`, content description). Compose test per status.
  - [ ] Folder grid and timeline ViewModels combine paging items with `observeStatusFor`; badge shown only when signed in and folder mapped. Tests.
  - [ ] "Upload" action in viewer and `SelectionTopBar` (only not-uploaded items). Tests: enqueue called with filtered ids.
  - [ ] `UploadQueueScreen` in `:feature:remote` (pending/running/failed, retry/cancel). ViewModel tests per event; Compose test.
  - [ ] Settings: folder mapping toggles, remote root, "upload only on Wi-Fi". Tests.
- [ ] **9.6 Update docs** – ARCHITECTURE.md (upload-state algorithm, workers) and README "How already uploaded is determined" match code.

**Definition of done:** badges reflect server state without blocking the grid; uploads survive process death and resume on network; already-present files are not re-uploaded; tests for every function; CI green.

---

## Phase 10 – Hardening & release

**Goal:** fast, accessible, localized, reproducible release build suitable for F-Droid.
_Depends on: Phases 1–9._

- [ ] **10.0 Apply design** – replace the placeholder theme tokens with the Claude Design delivery (can happen earlier, whenever the design arrives); adjust screens only where the design changes layout.
- [ ] **10.1 Performance**
  - [ ] `:benchmark` (`com.android.test`) module with Macrobenchmark: cold start, folder grid scroll, timeline fast-scroll, viewer swipe. Run on the Pixel 10a against the D24 targets.
  - [ ] Baseline Profile generator (`androidx.baselineprofile` plugin) for `:app`; commit `baseline-prof.txt`. Verify startup improvement.
  - [ ] Profile indexing on 50k images (time, memory); tune batch size, Room transactions. Record results in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).
  - [ ] StrictMode in debug builds; fix disk/network on main thread.
- [ ] **10.2 Accessibility**
  - [ ] Content descriptions for thumbnails (date/name), badges, actions; TalkBack traversal of grids and viewer.
  - [ ] Touch targets ≥ 48 dp, contrast check, font scaling 200 %; scrollbar operable via accessibility actions.
  - [ ] Compose UI tests asserting semantics on key screens; Accessibility Scanner pass.
- [ ] **10.3 Localization**
  - [ ] All user strings in `strings.xml` (lint `HardcodedText` as error); plurals for counts.
  - [ ] Locale-aware date formatting in timeline headers/labels; RTL layout check.
  - [ ] Initial translation (e.g. German); per-app language support (`locales_config.xml`).
- [ ] **10.4 Release build**
  - [ ] R8 full mode, `isMinifyEnabled`/`isShrinkResources`; keep rules for kotlinx.serialization routes, Tink, osmdroid, Room. Run instrumented smoke tests on `release`-like `benchmark` build type.
  - [ ] F-Droid-friendly build: no proprietary deps (verify dependency tree has no GMS/Firebase), reproducible build settings (no build timestamps, `dependenciesInfo { includeInApk = false }`), Fastlane metadata in `fastlane/metadata/android/`.
  - [ ] Release signing via `keystore.properties` / CI secrets (never committed); `signingConfigs.release`.
  - [ ] Versioning scheme (`versionCode` from tag), CHANGELOG.
  - [ ] GitHub Actions `release.yml`: on tag, build signed APK/AAB, attach to GitHub release.
- [ ] **10.5 Final checks**
  - [ ] Full manual pass on the reference Pixel 10a (Android 17); instrumented suite green on a local API 37 emulator and on the Pixel 10a.
  - [ ] Third-party license screen (`:feature:settings` About) generated from dependencies.
  - [ ] Privacy: confirm no analytics/tracking, network only to user's Nextcloud and OSM tiles.
- [ ] **10.6 Update docs** – README (status, building, license, F-Droid), ARCHITECTURE.md final state, DESIGN_DECISIONS.md resolved questions.

**Definition of done:** signed, minified release build passes benchmarks and accessibility checks, contains no proprietary dependencies, builds reproducibly, and is ready for F-Droid submission.
