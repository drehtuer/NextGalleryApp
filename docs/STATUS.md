# Project status

Last updated: 2026-11-07

Where the implementation stands against the plan in [TODO.md](TODO.md). TODO.md has the detailed checklist; this page is the summary.

## Phases

| Phase | Scope | State |
| --- | --- | --- |
| 0 | Repository and build setup | Done (PR #3, #4). |
| 1 | Skeleton, permissions, MediaStore indexing into Room | Done in code (PR #5), CI green. Open: checks on the device (see below). |
| 2 | Folder view with thumbnail grid and fast scroll | Done in code (PR #6), CI green. Open: checks on the device and the performance check (2.6). |
| 3 | Full-screen viewer, delete, share | Done in code (PR #9, #10), CI green. Open: checks on the device. |
| – | Visual design from Claude Design (task 10.0) | Applied to the existing screens. Open: checks on the device. |
| 4 | Settings: folder include/exclude list | Done in code; the CI checks pass locally in the devcontainer. Open: checks on the device. |
| 5 | Timeline view | Done in code; the CI checks pass locally in the devcontainer. Open: checks on the device. |
| 6 | Map view | Next |
| 7 | Editor | Not started |
| 8 | Nextcloud login and remote viewer | Not started |
| 9 | Upload status and hand-off to the Nextcloud app | Not started |
| 10 | Hardening and release | Not started |

## What works now

- The app asks for photo access (full or "select photos"), shows a limited-access banner for partial access, and offers the app settings if access was denied.
- With access, it indexes all images from MediaStore into Room in the background: a full scan the first time, afterwards incremental syncs via `GENERATION_MODIFIED`, triggered on app start, on MediaStore changes (debounced 2 s) and after the access changed.
- Dates are resolved as `DATE_TAKEN` → EXIF `DateTimeOriginal` → `DATE_MODIFIED`; default folder rules exclude WhatsApp status images and `Android/data`.
- The Folders tab lists the folders with cover image and image count. Opening a folder shows a paged thumbnail grid, newest first, with a draggable fast scrollbar.
- Tapping an image opens the full-screen viewer: swipe between images, pinch or double-tap to zoom, tap to show or hide the controls, and an info sheet with the date, size, resolution, location and camera values.
- Long-press selects images, which works across folders. The selection bar shows the total and offers select all, clear, share, delete and an overview of the selection, where images the current folder cannot show are greyed out.
- Delete moves images to the system trash after the system's confirmation, with an undo option. Share uses the Android share sheet.
- Other apps can open an image in NextGallery ("Open with"). This shows only the zoomable image.
- The Timeline tab shows all images, newest first, grouped by day under month headers. The month header stays at the top and day headers scroll ("Today · Friday 6 November"). A date rail on the right shows the day in the middle of the screen with the days above and below, and its thumb can be dragged to jump.
- Tapping a timeline image opens the viewer, which swipes through the whole timeline. Selecting works as in the folders, and the selection is shared with them.
- The Map and Nextcloud tabs are still placeholders.
- The Folders top bar opens Settings: Folders, Appearance, Nextcloud ("Not connected") and About & licences.
- Folder rules offer Include, Default or Exclude per folder, plus exclude patterns; a change re-indexes after about 2 s.
- Appearance sets the theme mode, 3, 4 or 5 grid columns and the accent colour, applied live and kept across restarts.
- 587 tests run with the CI command (plain JVM, Robolectric, Compose UI).

## To verify on the device

These need the Pixel 10a or an API 37 emulator (`./gradlew connectedDebugAndroidTest`, see [README](../README.md#building)):

- [ ] Install the debug build; grant full access → the folders appear with the right image counts.
- [ ] Take or delete a photo → the folder's count changes within a few seconds.
- [ ] Grant "select photos" → only the selected photos are counted; "Select more" updates the count.
- [ ] Open a folder: thumbnails load, the orientation is correct, and an open grid updates after a change.
- [ ] Drag the fast scrollbar through a large folder → it jumps without visible delay (2.6: 10k+ images, no dropped frames).
- [ ] Long-press selects; tapping changes the selection; Back clears it first; the selection survives switching folders.
- [ ] Inside a folder the Folders tab stays selected; switching tabs and back returns to that folder.
- [ ] Viewer: opens on the tapped image, even far down a large folder; swiping, pinch and double-tap zoom work, also with very large images and HEIC; panning a zoomed image does not switch pages by accident.
- [ ] Viewer: tapping hides the controls and system bars; Back returns to the grid with the bars and the bottom navigation restored, in light and dark system theme.
- [ ] Info sheet: the values match the photo, and missing values are left out.
- [ ] Delete from the viewer and from a selection: the system dialog appears, cancelling changes nothing, confirming removes the images right away, and Undo brings them back after the next sync.
- [ ] Deleting the last image of a folder closes the viewer; an image deleted by another app disappears from an open viewer.
- [ ] Share one and several images, e.g. to a mail or messenger app.
- [ ] Selection overview: images of other folders are greyed out; tapping removes an image; actions apply to the whole selection.
- [ ] "Open with" from a file manager opens the image, also without media permission.
- [ ] Design: colours and text are readable in light and dark theme; the new icons look right; the selection ring and check are clear over light and dark photos.
- [ ] Settings: excluding a folder removes it from the folder list after the re-index; Default or Include brings it back; an Include under an excluded parent shows that subfolder.
- [ ] Settings persist after a force stop; forced Light and Dark look right, including the status bar icons; the viewer stays black.
- [ ] Columns change in the folder grid and selection overview, also in landscape; accent swatches look right in both themes.
- [ ] About: the source link opens the browser. Folder rules: the keyboard does not cover the pattern field; a folder with a `.nomedia` file is not listed.
- [ ] Timeline: dates are correct, including images dated from EXIF or the modified date; "Today" shows on today's photos only; the month header switches correctly at month boundaries.
- [ ] Timeline: the rail is readable over photos in both themes; dragging it is smooth on 10k+ images, and the thumb does not jitter on small libraries; photos under the rail labels can still be tapped.
- [ ] Timeline: Back from the viewer returns to the same scroll position; delete with undo works; the overview opened from the timeline greys out nothing.
- [ ] Design: the Select action starts a selection with "0 selected"; the limited access banner sits under the top bar.
- [x] `MainActivityTest` (`:app`), `MediaStoreScannerTest` (`:core:media`) and `MediaThumbnailFetcherTest` (`:core:ui`) pass on the Pixel 10a (2026-11-06).
- [x] Images in a folder with a `.nomedia` file are not returned by the scanner, after a media scan (D6; `MediaStoreScannerTest`).

## Known workarounds and technical debt

- The licence list is kept by hand until task 10.5 generates it.
- SonarCloud coverage only counts the tests CI runs, so adapters covered only by instrumented tests show as uncovered (D41).
- detekt 2.0 is an alpha release, because detekt 1.23 does not work with AGP 9 (D18).
- Espresso is pinned to 3.7.0 for tests; the version pulled in by Compose UI test calls an API removed in SDK 37.
- `failOnNoDiscoveredTests` is disabled, so modules without tests yet (features) pass.
- CI builds the devcontainer image on every run (about 6 minutes); caching the image would speed it up.
- `IndexWorker` does not report progress yet; there is no indexing indicator in the UI, so the folder list briefly says "No photos found" during the first scan.
