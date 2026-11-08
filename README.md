# NextGalleryApp

[![CI](https://github.com/drehtuer/NextGalleryApp/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/drehtuer/NextGalleryApp/actions/workflows/ci.yml)
[![Security](https://github.com/drehtuer/NextGalleryApp/actions/workflows/security.yml/badge.svg?branch=main)](https://github.com/drehtuer/NextGalleryApp/actions/workflows/security.yml)
[![Documentation](https://github.com/drehtuer/NextGalleryApp/actions/workflows/pages.yml/badge.svg)](https://drehtuer.github.io/NextGalleryApp/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=drehtuer_NextGalleryApp&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=drehtuer_NextGalleryApp)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=drehtuer_NextGalleryApp&metric=coverage)](https://sonarcloud.io/summary/new_code?id=drehtuer_NextGalleryApp)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=drehtuer_NextGalleryApp&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=drehtuer_NextGalleryApp)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=drehtuer_NextGalleryApp&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=drehtuer_NextGalleryApp)
[![OpenSSF Scorecard](https://api.scorecard.dev/projects/github.com/drehtuer/NextGalleryApp/badge)](https://scorecard.dev/viewer/?uri=github.com/drehtuer/NextGalleryApp)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

A simple, fast Android gallery app with basic editing, multiple ways to browse your photos, and built-in Nextcloud integration.

> Status: all features are implemented; hardening for a first release is under way, and what needs a device is still open. See [STATUS.md](docs/STATUS.md).

## Documentation

- [Architecture](docs/ARCHITECTURE.md) – modules, layers, data model and key flows (Mermaid diagrams).
- [Design decisions & open questions](docs/DESIGN_DECISIONS.md) – what has been decided and what is still open.
- [Implementation TODO](docs/TODO.md) – ordered, step-by-step implementation plan.
- [Original design brief](docs/DESIGN_HANDOVER.md) – what Claude Design was asked for (historical).
- [Changelog](docs/CHANGELOG.md) – what changed in each version.
- [Status](docs/STATUS.md) – what is implemented, what is next, and what still needs checking on the device.
- [Design](docs/DESIGN.md) – the visual design: colours, type, dimensions, wording and states of every screen.
- [Security policy](docs/SECURITY.md) – how to report a vulnerability, and how the code is checked.

The documentation is also published as a website: <https://drehtuer.github.io/NextGalleryApp/>. It is rebuilt with every release.

## Goals

- **Simple** – a focused gallery, not a photo-management suite.
- **Fast** – smooth scrolling through tens of thousands of images, with a draggable fast-scroll handle.
- **Local first** – works fully offline; Nextcloud is optional.
- **No tracking, no Google Play Services dependency.**

## Features

### Browsing

| View | Description |
|---|---|
| **Folders** (default) | Images grouped by folder, shown as a thumbnail grid. |
| **Timeline** | Images grouped by month and day, newest first, with sticky month headers. |
| **Map** | Images with GPS location (EXIF) shown as clustered markers on a map, from the whole globe down to street level. It opens at your current location. Clusters are named after the nearest town ("near Bolzano"), looked up offline. Images without location data are skipped. |
| **Nextcloud** | Browse and view images stored on a Nextcloud server, without downloading them first. |

- **Your choice of folders** – add folders with Android's folder picker; only those and their subfolders are shown, and the phone is not scanned for other folders.
- **Virtual folders** – show the photos of several folders together, e.g. Camera, WhatsApp and Signal.
- **Appearance** – light or dark theme, or follow the system; 3, 4 or 5 grid columns; five accent colours.
- **Selection** – one selection across all views; a selection overview lists everything selected and greys out images the current view cannot show.
- **Fast scroll** – draggable scrollbar on the folder grid, the timeline and the map's cluster sheet; in the timeline it shows the date in the middle of the screen, with the dates before and after.
- **Thumbnails** – generated and cached on device; grids load lazily.
- **Full-screen viewer** – swipe between images, pinch to zoom, double-tap to zoom, image info (date, size, resolution, location, EXIF).

### Editing

- **Rotate** – 90° steps.
- **Crop** – free-form and fixed aspect ratios (1:1, 4:3, 16:9, original).
- **Scale** – resize by percentage or to a target width/height, keeping aspect ratio.
- **Pen** – freehand drawing with selectable colour and stroke width, with undo/redo.
- Editing is available for JPEG and PNG images; edits are saved in the same format.
- Images too large to edit within the available memory are refused with an explanation instead of risking a crash or silently lowering quality.
- Edits are saved as a **new copy** by default, with an option to overwrite the original after the system's confirmation. EXIF data is preserved where possible, with the orientation reset and the new dimensions.

### File actions

- **Delete** – single or multi-select; moves images to the system trash, so deletions can be undone.
- **Share** – send one or more images through the standard Android share sheet.

### Nextcloud integration

- **Built-in viewer** – browse folders on a Nextcloud server and view images full-screen. Thumbnails come from the server's preview API, so originals are only downloaded when opened.
- **Organise server photos** – long-press an image (or a selection) to move it to another server folder, share it or delete it to the Nextcloud trash bin, with undo.
- **Upload status** – while signed in, each local image shows whether it already exists on Nextcloud (badge on the thumbnail in the folder grid and the timeline).
- **Upload** – hands a single image or a selection to the official Nextcloud Android app, which does the actual upload (target folder, name conflicts, large files). Requires the Nextcloud app to be installed; images already on the server are left out.
- **Login** – via Nextcloud Login Flow v2 (browser-based sign-in, app password), so the account password is never stored in the app. Without a login, Nextcloud features are unavailable and the app shows a hint to log in.
- **Self-hosted servers** – self-signed or otherwise untrusted certificates can be accepted after a warning dialog showing the certificate details.

NextGalleryApp does not duplicate what the official Nextcloud Android app already does (uploading, offline files, …); it hands off to that app instead. The built-in viewer and moving or deleting server images are the deliberate exceptions.

#### How "already uploaded" is determined

1. Match by relative path and file size against the configured remote folder (cheap, one `PROPFIND` per folder).
2. If the server provides checksums (`oc:checksums`), compare a SHA-1/MD5 of the local file for an exact match. Only name-and-size matches are read, and only once per version of the file. Different content counts as not uploaded.
3. Results are cached locally and refreshed in the background, every 6 hours with a network, after a sign-in or a new remote folder, and on returning from the Nextcloud app, so the grid never waits on the network.
4. Files the Nextcloud app renamed during upload (e.g. on a name conflict) are not recognised and show as "not uploaded".

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Media access | `MediaStore` API (scoped storage), `ContentObserver` for changes |
| Index / cache | Room database (folders, dates, GPS, upload state) |
| Images | Coil (thumbnails, disk cache), Telephoto for zooming and subsampling large images in the viewer |
| EXIF | `androidx.exifinterface` |
| Map | osmdroid with OpenStreetMap tiles (configurable tile URL) – no API key needed |
| Nextcloud | WebDAV over OkHttp; preview API for remote thumbnails |
| Background work | WorkManager (indexing, upload-state sync) |
| Credentials | App password encrypted with Tink, key in the Android Keystore; signing in happens in the browser (Login Flow v2) |
| Dependency injection | Hilt |
| Navigation | Navigation Compose with type-safe routes (kotlinx.serialization) |
| Lists | Paging 3 over Room |
| Settings | DataStore |
| Licence list | AboutLibraries Gradle plugin (generated at build time) |
| UI design | Material 3 with the delivered design ([DESIGN.md](docs/DESIGN.md)): fixed neutral roles, five accent colours, no dynamic colour |
| SDK levels | Min, compile and target SDK 37 (Android 17); older Android versions are not supported |
| Reference device | Pixel 10a, Android 17 |

## Permissions

- `READ_MEDIA_IMAGES` – read photos; `READ_MEDIA_VISUAL_USER_SELECTED` – limited access to selected photos only. Only the folders you added are read. The folder picker only chooses a path and needs no permission of its own.
- `ACCESS_MEDIA_LOCATION` – read GPS data from EXIF for the map view.
- `ACCESS_COARSE_LOCATION` – optional; opens the map at your current location. Without it, the map opens where most of your photos are.
- `INTERNET` and `ACCESS_NETWORK_STATE` – map tiles and Nextcloud; traffic is https only.
- `ACCESS_LOCAL_NETWORK` – for a Nextcloud server on your own network; asked for when you sign in.
- Package visibility for `com.nextcloud.client` (`<queries>`) – to hand uploads to the Nextcloud app.
- Deleting (to the system trash, `MediaStore.createTrashRequest`) and overwriting originals (`createWriteRequest`) go through the system confirmation dialogs, so no broad storage permission is required.

## Roadmap

1. Project skeleton, permissions, MediaStore indexing into Room.
2. Folder view with thumbnail grid and fast scroll.
3. Full-screen viewer, delete, share.
4. Settings: choosing folders, virtual folders.
5. Timeline view (month/day).
6. Map view.
7. Editor: rotate, crop, scale, pen.
8. Nextcloud login and built-in remote viewer.
9. Upload status check and hand-off to the Nextcloud app.
10. Hardening and release: accessibility, release build, licences, benchmarks.

## Building

The build runs in a devcontainer ([.devcontainer/](.devcontainer/)) with JDK 21 and the Android SDK (API 37). CI uses the same image.

With the [Dev Containers CLI](https://github.com/devcontainers/cli) (or "Reopen in Container" in VS Code):

```bash
devcontainer up --workspace-folder .
devcontainer exec --workspace-folder . ./gradlew assembleDebug
```

Install on a connected device from the host (the container has no USB access):

```bash
adb install app/build/outputs/nextgallery/debug/NextGallery-debug-*.apk
```

Before opening a pull request, run the checks CI runs (`ktlintFormat` fixes what CI's `ktlintCheck` would report):

```bash
./gradlew ktlintFormat detekt lint test :build-logic:convention:test :build-logic:settings:test assembleDebug assembleDebugAndroidTest :app:assembleRelease :benchmark:compileBenchmarkReleaseKotlin
```

`./gradlew coverageReport` writes the JaCoCo line and branch coverage reports (`build/reports/` in each module).

CI sends the coverage and the code analysis to [SonarCloud](https://sonarcloud.io/project/overview?id=drehtuer_NextGalleryApp). This needs Automatic Analysis turned off in the SonarCloud project and a `SONAR_TOKEN` repository secret (see D41 in [DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)).

Two more workflows check security (D59, D60 in [DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)). [security.yml](.github/workflows/security.yml) runs CodeQL on the Kotlin code and the workflows, sends the Gradle dependencies to GitHub's dependency graph, and runs a dependency review on pull requests. [scorecard.yml](.github/workflows/scorecard.yml) runs OpenSSF Scorecard. Dependabot ([.github/dependabot.yml](.github/dependabot.yml)) proposes updates for Gradle, the actions and the devcontainer image every Monday. The results are under the repository's Security tab.

Instrumented tests do not run in CI. Run them on the host against an API 37 emulator or the Pixel 10a with `./gradlew connectedDebugAndroidTest` (this needs a local Android SDK).

Benchmarks and the Baseline Profile need a real device with photos and an unlocked screen, also from the host: `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest` and `./gradlew :app:generateBaselineProfile`. The second writes files under `app/src/release/generated/`, which are then committed.

### Release build

Builds name their APK after the version and write it to `app/build/outputs/nextgallery/<variant>/`, next to AGP's own output: `assembleDebug` writes `NextGallery-debug-<version>-<timestamp>.apk` (build time in UTC, `yyyyMMdd-HHmmss`), and `assembleRelease` writes `NextGallery-<version>.apk`. Only the latest APK of each variant is kept there.

`./gradlew :app:assembleRelease` builds a minified APK. Without a keystore it is unsigned (`NextGallery-<version>-unsigned.apk`) and Android won't install it. To sign it, create a keystore and put a `keystore.properties` next to `settings.gradle.kts`; Git ignores both, so never force-add them:

```properties
storeFile=/path/to/release.jks
storePassword=...
keyAlias=nextgallery
keyPassword=...
```

```bash
keytool -genkeypair -v -keystore /path/to/release.jks -alias nextgallery -keyalg RSA -keysize 4096 -validity 10000
./gradlew :app:assembleRelease
adb install app/build/outputs/nextgallery/release/NextGallery-*.apk
```

Keep the keystore safe: an installed app only accepts updates signed with the same key. A relative `storeFile` counts from the repository root, and the path must be visible inside the devcontainer.

The version comes from the nearest Git tag of the form `vMAJOR.MINOR.PATCH`: `v1.2.3` gives the name 1.2.3 and the code 1002003. Tags with a suffix such as `-rc1` stop the build, because their code would equal the final version's. Without a tag, or where Git can't run, the build calls itself 0.1.0; `-PversionTag=v1.2.3` overrides both. Record changes in [CHANGELOG.md](docs/CHANGELOG.md) before tagging.

### Publishing a release

Pushing a tag `vMAJOR.MINOR.PATCH` runs [.github/workflows/release.yml](.github/workflows/release.yml) (D61 in [DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)). In the devcontainer image it builds the release APK, signed with the key from the `release` environment, and checks the signature with `apksigner`. It then attaches the APK, R8's mapping file and a `SHA256SUMS` file to a draft release, using the changelog section of that version as the release notes. Publishing the release is the last step, and then the documentation site is deployed. Releases are immutable once published, so a mistake needs a new version: a published release can't be changed and its tag can't be moved.

1. Turn "Unreleased" in [CHANGELOG.md](docs/CHANGELOG.md) into a section `## <version>` and merge that to `main`.
2. Tag the merge commit and push the tag:

   ```bash
   git tag -a v0.1.0 -m "NextGallery 0.1.0"
   git push origin v0.1.0
   ```

The signing key is stored as secrets of the `release` environment (Settings → Environments → release), which only accepts `v*` tags:

| Secret | Value |
|---|---|
| `RELEASE_KEYSTORE_BASE64` | The keystore file, base64-encoded: `base64 -w0 release.jks` |
| `RELEASE_KEYSTORE_PASSWORD` | The keystore password (`storePassword`) |
| `RELEASE_KEY_ALIAS` | Optional: the key alias. Without it, the keystore must hold exactly one key, and that key is used. |
| `RELEASE_KEY_PASSWORD` | Optional: the key password. Without it, the keystore password is used, as a PKCS12 keystore (keytool's default) requires. |

The workflow opens the keystore before the build starts, so a wrong password or alias stops it at once.

If a run fails before publishing, fix the cause and re-run the workflow; it replaces the draft it left. If the fix needs a code change, delete the tag and push it again on the new commit. This only works while the release is still a draft.

### Documentation site

Publishing a GitHub release runs [.github/workflows/pages.yml](.github/workflows/pages.yml), which builds `README.md` and `docs/` with GitHub's Jekyll and deploys them to GitHub Pages; it can also be started by hand from the Actions tab. The Jekyll config is in [.github/pages/](.github/pages/). The repository's Pages source must be "GitHub Actions", and the `github-pages` environment must allow the release tags (`v*`) as well as `main` (D58 in [DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md)).

## License

GNU General Public License v3.0 – see [LICENSE](LICENSE) and [DESIGN_DECISIONS.md](docs/DESIGN_DECISIONS.md) (D27).
