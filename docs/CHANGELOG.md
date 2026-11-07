# Changelog

All notable changes to NextGallery. The version comes from a Git tag `vMAJOR.MINOR.PATCH`, see
[README.md](../README.md#release-build). Add to "Unreleased" with each change, and turn it into a version section when
tagging.

## Unreleased

No version is tagged yet. Builds call themselves 0.1.0.

### Added

- Browsing by folder, by timeline with a date rail, on a map and on a Nextcloud server.
- A full-screen viewer, and an editor with rotate, crop, scale and pen.
- One selection across all views, with share, delete and upload through the Nextcloud app.
- Settings for folders, appearance and the tile server, and About with a licence list generated from the dependencies.
- Screen reader support: names and dates on thumbnails, states on badges and selection, and scrollbars that work
  without dragging.
- A release build: minified, signed with a local keystore if there is one, with the version from a Git tag.
- Virtual folders: show the photos of several folders together, for example Camera, WhatsApp and Signal.

### Changed

- You now choose which folders are shown, with Android's folder picker. Only added folders and their subfolders are
  shown, and the phone is no longer scanned for all folders with images. This replaces folder rules and exclude
  patterns. They are not carried over, so after updating the app starts with no folders.

### Fixed

- Nextcloud sign-in opens in the default browser and checks for success as soon as you return to the app. A brief
  network failure no longer ends it, and it survives the app being killed in the background.
- A right swipe from an image opened from the map's cluster sheet no longer gets stuck on that sheet.
- Sharing more images than the system can take now shows a message instead of ending the app.
- In the remote browser a screen reader now finds an image's name and actions in one place.
