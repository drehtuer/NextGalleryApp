# NextGalleryApp

A simple, fast Android gallery app with basic editing, multiple ways to browse your photos, and built-in Nextcloud integration.

> Status: planning. Nothing is implemented yet; this README describes the intended scope.

## Documentation

- [Architecture](docs/ARCHITECTURE.md) – modules, layers, data model and key flows (Mermaid diagrams).
- [Design decisions & open questions](docs/DESIGN_DECISIONS.md) – what has been decided and what is still open.
- [Implementation TODO](docs/TODO.md) – ordered, step-by-step implementation plan.

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
| **Timeline** | Images grouped by year → month → day, with sticky date headers. |
| **Map** | Images with GPS location (EXIF) shown as clustered markers on a map. Images without location data are skipped. |
| **Nextcloud** | Browse and view images stored on a Nextcloud server, without downloading them first. |

- **Configurable sources** – choose which folders are scanned (include list), with optional exclusions (e.g. `WhatsApp/.Statuses`, app caches).
- **Fast scroll** – draggable scrollbar on every grid; in the timeline it shows the current year/month while dragging.
- **Thumbnails** – generated and cached on device; grids load lazily.
- **Full-screen viewer** – swipe between images, pinch to zoom, double-tap to zoom, image info (date, size, resolution, location, EXIF).

### Editing

- **Rotate** – 90° steps.
- **Crop** – free-form and fixed aspect ratios (1:1, 4:3, 16:9, original).
- **Scale** – resize by percentage or to a target width/height, keeping aspect ratio.
- **Pen** – freehand drawing with selectable colour and stroke width, with undo/redo.
- Edits are saved as a **new copy** by default, with an option to overwrite the original. EXIF data is preserved where possible.

### File actions

- **Delete** – single or multi-select; moves images to the system trash, so deletions can be undone.
- **Share** – send one or more images through the standard Android share sheet.

### Nextcloud integration

- **Built-in viewer** – browse folders on a Nextcloud server and view images full-screen. Thumbnails come from the server's preview API, so originals are only downloaded when opened.
- **Upload status** – each local image shows whether it already exists on Nextcloud (badge on the thumbnail).
- **Upload** – upload a single image or a selection of images that aren't on the server yet, into a configurable target folder.
- **Login** – via Nextcloud Login Flow v2 (browser-based sign-in, app password), so the account password is never stored in the app.

#### How "already uploaded" is determined

1. Match by relative path and file size against the configured remote folder (cheap, one `PROPFIND` per folder).
2. If the server provides checksums (`oc:checksums`), compare a SHA-1/MD5 of the local file for an exact match.
3. Results are cached locally and refreshed in the background, so the grid never waits on the network.

## Planned tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Media access | `MediaStore` API (scoped storage), `ContentObserver` for changes |
| Index / cache | Room database (folders, dates, GPS, upload state) |
| Images | Coil (thumbnails, disk cache), subsampling for large images in the viewer |
| EXIF | `androidx.exifinterface` |
| Map | osmdroid with OpenStreetMap tiles (configurable tile URL) – no API key needed |
| Nextcloud | WebDAV over OkHttp; preview API for remote thumbnails |
| Background work | WorkManager (indexing, upload queue, upload-state sync) |
| Credentials | Android Keystore–encrypted storage |
| UI design | Stock Material 3 with a placeholder theme until the design from Claude Design is delivered |
| SDK levels | Min, compile and target SDK 37 (Android 17); older Android versions are not supported |
| Reference device | Pixel 10a, Android 17 |

## Permissions

- `READ_MEDIA_IMAGES` – read photos; `READ_MEDIA_VISUAL_USER_SELECTED` – limited access to selected photos only.
- `ACCESS_MEDIA_LOCATION` – read GPS data from EXIF for the map view.
- `INTERNET` – Nextcloud only.
- Deleting and overwriting originals goes through the system confirmation dialogs (`MediaStore.createDeleteRequest` / `createWriteRequest`), so no broad storage permission is required.

## Roadmap

1. Project skeleton, permissions, MediaStore indexing into Room.
2. Folder view with thumbnail grid and fast scroll.
3. Full-screen viewer, delete, share.
4. Settings: folder include/exclude list.
5. Timeline view (year/month/day).
6. Map view.
7. Editor: rotate, crop, scale, pen.
8. Nextcloud login and built-in remote viewer.
9. Upload status check and upload queue.

## Building

Requirements: JDK 21+, Android SDK (via Android Studio or command-line tools).

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## License

TBD
