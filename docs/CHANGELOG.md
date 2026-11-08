# Changelog

All notable changes to NextGallery. The version comes from a Git tag `vMAJOR.MINOR.PATCH`, see
[README.md](../README.md#release-build). Add to "Unreleased" with each change, and turn it into a version section when
tagging. The release workflow uses that section as the release notes, so it must exist before the tag is pushed.

## Unreleased

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
