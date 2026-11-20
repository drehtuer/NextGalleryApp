# Changelog

All notable changes to NextGallery. The version comes from a Git tag `vMAJOR.MINOR.PATCH`, see
[README.md](../README.md#release-build). Add to "Unreleased" with each change, and turn it into a version section when
tagging. The release workflow uses that section as the release notes, so it must exist before the tag is pushed.

## Unreleased

- Build: the Gradle wrapper retries a failed download of the Gradle distribution.
- Faster: the second indexing pass reads photo locations four files at a time; on 50 000 photos the whole first sync
  took 77 s instead of 6 minutes.
- Faster: opening a folder for the first time no longer sets up the Nextcloud connection on the main thread, which took
  over 300 ms; it is set up with the first request to the server.
- Build: the app ships a Baseline Profile, generated on the Pixel 10a.
- Fixed: the selected Theme and Grid columns, the chosen crop ratio and the pen-width slider showed in the warning
  colour (brown) instead of the accent.
- Fixed: with very large text, the "Nextcloud" tab label broke over two lines.

## 0.1.1

The first published release. 0.1.0 was tagged, but its release build failed and it was never published, so this
release contains everything listed under 0.1.0 in the
[changelog](https://github.com/drehtuer/NextGalleryApp/blob/main/docs/CHANGELOG.md#010), plus these build changes:

- Build: KSP back to 2.3.12, because 2.3.13 makes Hilt code generation fail or go missing at random
  (google/ksp#3286).
- Build: libraries with known vulnerabilities that the build tools pull in are raised to fixed versions; all GitHub
  Actions and the devcontainer base image are pinned to a commit or digest. The APK does not change.

## 0.1.0

The first release. It runs on Android 17 (API 37) only.

### Added

- Browsing by folder, by timeline with a date rail, on a map and on a Nextcloud server.
- You choose which folders are shown, with Android's folder picker; the phone is not scanned for other folders.
- Virtual folders: show the photos of several folders together, for example Camera, WhatsApp and Signal.
- A full-screen viewer, and an editor with rotate, crop, scale and pen.
- One selection across all views, with share, delete and upload through the Nextcloud app.
- Nextcloud sign-in in the default browser (Login Flow v2), with moving, sharing and deleting server images. Deleting
  asks first when the server's trash bin is off, because the images can't be restored then.
- Settings for folders, appearance and the tile server, and About with a licence list generated from the dependencies.
- Screen reader support: names and dates on thumbnails, states on badges and selection, and scrollbars that work
  without dragging.
- The documentation is published as a website on GitHub Pages with each release.

### Known limitations

- Several checks on a real device and with a real Nextcloud server are still open; see
  [STATUS.md](https://github.com/drehtuer/NextGalleryApp/blob/main/docs/STATUS.md).
- Map markers, crop handles and the pen canvas cannot be used with TalkBack.
