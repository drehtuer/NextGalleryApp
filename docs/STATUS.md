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
| 4 | Settings (folder rules, replaced by D55) | Done in code (PR #15), CI green. Open: checks on the device. |
| 5 | Timeline view | Done in code (PR #16), CI green. Open: checks on the device. |
| 6 | Map view | Done in code (PR #19), CI green. Open: checks on the device. |
| 7 | Editor | Done in code (PR #17), CI green. Open: checks on the device. |
| 8 | Nextcloud login and remote viewer | Done in code (PR #18), CI green. Open: checks on the device with a real server. |
| 9 | Upload status and hand-off to the Nextcloud app | Done in code (PR #20), CI green. Open: checks on the device with a real server and the Nextcloud app. |
| – | Login in the default browser (D54) | Done in code (PR #23), CI green. Open: checks on the device with a real server. |
| – | Added folders and virtual folders (D55) | Done in code (PR #24), CI green. Open: checks on the device. |
| 10 | Hardening and release | What could be done without a person is done. Open: benchmarks, profile, accessibility on the device, a signed build and the full manual pass. |

## What works now

- The app asks for photo access (full or "select photos"), shows a limited-access banner for partial access, and offers the app settings if access was denied.
- With access, it indexes the images of the folders you added from MediaStore into Room in the background; without an added folder it reads nothing (D55). It uses a full scan the first time, afterwards incremental syncs via `GENERATION_MODIFIED`, triggered on app start, on MediaStore changes (debounced 2 s) and after the access changed.
- Dates are resolved as `DATE_TAKEN` → EXIF `DateTimeOriginal` → `DATE_MODIFIED`; folders starting with a dot below an added folder are left out, such as WhatsApp's `.Statuses`.
- The Folders tab lists virtual folders first, then the folders with cover image and image count; with no folder added it offers "Add folder". Opening a folder shows a paged thumbnail grid, newest first, with a draggable fast scrollbar.
- Tapping an image opens the full-screen viewer: swipe between images, pinch or double-tap to zoom, tap to show or hide the controls, and an info sheet with the date, size, resolution, location and camera values.
- Long-press selects images, which works across folders. The selection bar shows the total and offers select all, clear, share, delete and an overview of the selection, where images the current folder cannot show are greyed out.
- Delete moves images to the system trash after the system's confirmation, with an undo option. Share uses the Android share sheet.
- Other apps can open an image in NextGallery ("Open with"). This shows only the zoomable image.
- The Timeline tab shows all images, newest first, grouped by day under month headers. The month header stays at the top and day headers scroll ("Today · Friday 6 November"). A date rail on the right shows the day in the middle of the screen with the days above and below, and its thumb can be dragged to jump.
- Tapping a timeline image opens the viewer, which swipes through the whole timeline. Selecting works as in the folders, and the selection is shared with them.
- The Map tab shows geotagged images as clusters that split on zoom, on OpenStreetMap tiles over HTTPS, with zoom 0–19 and a repeating world. It opens at the current location at zoom 8, or on the area with the most photos; without photo location access a card asks for it.
- Locations are read in a second pass after indexing. Tapping a cluster opens a sheet named by the bundled GeoNames list ("Near Bolzano and 7 more places"), and tapping an image opens the viewer on that cluster. The viewer's details show "near <town>, <country> · coordinates". Settings has a Map row for the tile server.
- Nextcloud: you sign in through the browser (Login Flow v2), so the app never sees your password; the app password is stored encrypted with Tink and the Android Keystore. The tab shows a login hint while logged out. Signed in, it browses folders with breadcrumbs and server previews, pull-to-refresh, an offline banner over the last loaded listing, and a session-ended banner.
- The remote viewer shows the preview first, then the original with "Loading full image · x of y MB". Long-press or Select offers Move to folder…, Share and Delete. Moving and deleting offer Undo; name conflicts ask per file. Share downloads to a temporary folder, then opens the share sheet.
- Unknown or changed certificates show a warning; accepted ones are pinned per host and can be removed in Settings.
- While signed in, the folder grid and timeline show an upload badge on each thumbnail: cloud with a check, outlined cloud, or cloud with a question mark. Badges come only from the local cache and never hold up the grid. The check runs every 6 hours with a network, after sign-in or a new remote root, and on returning from the Nextcloud app.
- Upload in the viewer, the selection bar and the selection overview hands images not yet on the server to the Nextcloud app. It is dimmed with an install hint if that app is missing, and hidden when signed out.
- Sharing more images than the system accepts shows a message instead of ending the app.
- Logging out is immediate and always ends logged out, wiping the credentials, listing cache, upload status, image cache and temporary shares; the server is told in the background. A new login starts with clean server data unless it is the same user. Traffic is https only (cleartext in debug builds only).
- The viewer offers Edit for JPEG and PNG. The black editor has Rotate, Crop (Free, Original, 1:1, 4:3, 16:9), Scale (percent, width, height) and Draw (six colours), with undo and redo.
- "Save as copy" creates `IMG_x_edited.jpg` in the same folder and opens it. "Overwrite original" asks the system first. Saves keep the source format and EXIF, with the orientation reset and new dimensions.
- Scales and saves that do not fit in memory are refused with the required and available memory. Closing with changes asks first.
- The Folders top bar opens Settings: Folders, Appearance, Map, Nextcloud ("Not connected") and About & licences.
- Settings > Folders adds folders with the system picker and removes them; a change re-indexes after about 2 s. It also creates, edits and deletes virtual folders, which show the images of several folders together. After updating from an earlier build the app starts with no folders.
- Appearance sets the theme mode, 3, 4 or 5 grid columns and the accent colour, applied live and kept across restarts.
- 2318 tests run with the CI command (plain JVM, Robolectric, Compose UI).
- About's licence list is generated from the dependencies, with GeoNames and OpenStreetMap kept. Screen readers get names and dates on thumbnails, selection and upload states, and scrollbars that work without dragging; touch targets are at least 48 dp.
- There is a minified release build, signed if `keystore.properties` exists, with its version from a Git tag. The build refuses proprietary libraries and licences outside the allowed list.

## To verify on the device

These need the Pixel 10a or an API 37 emulator (`./gradlew connectedDebugAndroidTest`, see [README](../README.md#building)):

- [ ] Install the debug build; grant full access → "No folders added"; add a folder with the system picker → it and its subfolders appear with the right image counts.
- [ ] Folder picker: check which folders Android really refuses (volume roots, the Download folder itself, `Android/data`, `Android/obb`; D55 says this from memory). An SD card folder and a subfolder of Download can be added and show images.
- [ ] Paths in the query work on the real MediaStore, including a folder name with `_` or `%` (`MediaStoreScannerTest` covers plain paths): `connectedDebugAndroidTest`.
- [ ] Removing a folder takes its images out of all tabs and touches no file; adding it again brings them back, and it comes back in the virtual folders it was in.
- [ ] Updating over an earlier build: the app starts with no folders and an empty timeline and map.
- [ ] Virtual folder: create one from Camera and another folder; images are in date order across both; the viewer swipes through all of them; Select All selects all of them; the overview greys out images of other folders. Deleting all members' images hides it in the list but not in Settings. The stacked-folder icon looks right.
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
- [ ] Settings persist after a force stop; forced Light and Dark look right, including the status bar icons; the viewer stays black.
- [ ] Columns change in the folder grid and selection overview, also in landscape; accent swatches look right in both themes.
- [ ] About: the source link opens the browser. Virtual folder editor: the keyboard does not cover the name field; a folder with a `.nomedia` file shows no images when added.
- [ ] Timeline: dates are correct, including images dated from EXIF or the modified date; "Today" shows on today's photos only; the month header switches correctly at month boundaries.
- [ ] Timeline: the rail is readable over photos in both themes; dragging it is smooth on 10k+ images, and the thumb does not jitter on small libraries; photos under the rail labels can still be tapped.
- [ ] Timeline: Back from the viewer returns to the same scroll position; delete with undo works; the overview opened from the timeline greys out nothing.
- [ ] Editor: a saved copy appears at once with correct orientation, dimensions and EXIF, including GPS. Overwrite: confirm and check that the viewer and thumbnail show the new image; decline and check that nothing changes.
- [ ] Editor: a photo rotated by EXIF comes out upright; a Display P3 photo keeps its colours; check how Ultra HDR photos look after saving, since their gain map is dropped.
- [ ] Editor: a very large scale shows the refusal message, with no crash; crop and draw gestures work, also in landscape.
- [ ] Nextcloud, needs a real server: login in the default browser, including a return to the app after it was killed meanwhile (D54), also behind a reverse proxy or on a custom port (a server reporting another address is refused); LAN IP, `.local` and VPN/Tailscale addresses, and how a blocked connection fails without `ACCESS_LOCAL_NETWORK` (D39).
- [ ] Nextcloud, needs a real server: an accepted self-signed certificate with Certificate Transparency on (D40); move, conflict, replace and undo, including whether a replaced file lands in the trash bin; trash-bin restore; read-only shares disabled through real `oc:permissions`; logout removes the password from the server's security settings.
- [ ] Nextcloud: the share sheet works with the temporary files; Telephoto zoom on originals with the progress bar; landscape.
- [ ] Map: tiles load; pinch, two-finger drag and the world repetition work; the attribution is visible; markers show cover, border and count pill at the right size; the location dot looks right, in both themes.
- [ ] Map: the first open shows the location prompt once; allowing photo locations fills the map, also with partial media access; without `ACCESS_MEDIA_LOCATION` no locations are read; a custom tile server applies.
- [ ] Map: Back from the viewer and the selection overview returns to the map with the camera kept, also after switching tabs; the location pass on a large library, interrupted and resumed.
- [ ] Upload: one and several images open the Nextcloud app's upload screen with read access; badges update after returning, including an upload that finishes later; the install hint with the app missing; "Select all" with a very large selection shows the message instead of crashing.
- [ ] Upload: a geotagged image handed off, then compared on a server that reports checksums (if the Nextcloud app lacks `ACCESS_MEDIA_LOCATION`, the system may blank GPS, and the image would stay "not uploaded"); the comparison against a real server; upgrading from an older database with existing data; the badge icons by eye.
- [ ] Release build, because R8 can break these with no build error (on `benchmarkRelease` or a signed release; only the start of the app has been tried): every screen opens, especially those with arguments (viewer, folder grid, editor, remote viewer, Nextcloud account, selection overview); the index builds and survives a restart, and `IndexWorker` and `UploadStateSyncWorker` run.
- [ ] Release build: sign in, close the app, still signed in, then log out; server browsing, previews, moving, with a CA certificate and a self-signed one; map tiles, clusters and zoom; place names; the licence list; edit and save, overwrite; share; upload hand-off; share with a very large selection shows the message.
- [ ] Phase 10: `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest` against D24; `./gradlew :app:generateBaselineProfile`, then commit the result; 50k indexing; the StrictMode log; TalkBack traversal; font scaling at 200 %; Accessibility Scanner; the full manual pass; a keystore and one real signed build.
- [ ] Design: the Select action starts a selection with "0 selected"; the limited access banner sits under the top bar.
- [x] `MainActivityTest` (`:app`), `MediaStoreScannerTest` (`:core:media`) and `MediaThumbnailFetcherTest` (`:core:ui`) pass on the Pixel 10a (2026-11-06).
- [x] Images in a folder with a `.nomedia` file are not returned by the scanner, after a media scan (D6; `MediaStoreScannerTest`).

## Known workarounds and technical debt

- The benchmark journeys (`Journeys.kt`) assume a populated folder list, but cannot drive the system folder picker. Add a folder with photos by hand before running them, and keep app data between runs; a build step that clears app data makes them fail with "Is a folder with photos added in Settings?". Not run since this change (D55).
- Overwriting an image is not atomic, because MediaStore has no atomic replace: if the process dies during the short write, the image can be incomplete. Failures inside the process restore the original from a backup.
- If photo location access is denied, GPS is lost from a saved image.
- The editor's history does not survive process death.
- Previews being downloaded during logout can still reach the image cache afterwards; a running move batch is not tied to one account; if the process dies right after logout, the password is not revoked on the server.
- With the server's trash bin disabled, Delete is permanent, although the design has no confirmation and Undo then reports "not found".
- An upload still running when you return from the Nextcloud app only shows at the next periodic check, up to 6 hours later.
- Map markers, crop handles and the pen canvas cannot be used with TalkBack (osmdroid overlays are not exposed to it).
- The licence screen shows licence names, not the full texts, which are in the generated file. Apache-2.0 asks for a copy of the licence, so you may want them shown.
- SonarCloud coverage only counts the tests CI runs, so adapters covered only by instrumented tests show as uncovered (D41).
- detekt 2.0 is an alpha release, because detekt 1.23 does not work with AGP 9 (D18).
- Espresso is pinned to 3.7.0 for tests; the version pulled in by Compose UI test calls an API removed in SDK 37.
- `failOnNoDiscoveredTests` is disabled, because Hilt generates test sources even in test tasks that have no tests.
- CI builds the devcontainer image on every run (about 6 minutes); caching the image would speed it up.
- `IndexWorker` does not report progress yet; there is no indexing indicator in the UI, so the folder list briefly says "No photos in your folders" during the first scan.
