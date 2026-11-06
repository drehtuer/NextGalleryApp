# NextGallery Design Handover

Everything Claude Design needs to create the visual design of NextGallery. The app works today with a placeholder look. Your delivery replaces it (decision D22 in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md)).

## The app

NextGallery is a simple, fast gallery for Android. It browses local photos by folder, timeline and map, does basic editing, and can view photos on a personal Nextcloud server. It is free software (GPL v3), works offline, and has no tracking.

**Who uses it:** one person with their own photo library, often tens of thousands of images, who may run their own Nextcloud server.

**What the design should feel like:** the photos are the content and the interface stays out of their way. Think of a calm, precise tool, not a social or photo-management app. Scrolling through 50,000 images must feel instant.

**Reference device:** Pixel 10a on Android 17. This is the only Android version the app supports.

## What to deliver

| Deliverable | Details |
| --- | --- |
| Colour concept | Light and dark colour schemes, with values for the Material 3 colour roles below |
| Accent colours | A small set of accents the user can choose from in settings (D34) |
| Typography | Material 3 type scale; font files if you do not use the system font |
| Shapes | Material 3 corner sizes |
| Spacing and sizes | Values for the dimension tokens below |
| Icons | Replacements for the placeholder icons below, plus those needed by the planned screens |
| App icon | Adaptive launcher icon: background, foreground and monochrome layers |
| Screen designs | The screens below, in light and dark, wherever the design differs from stock Material 3 |

Values are most useful as tables that map directly to the tokens below. Icons should be vector drawables on a 24 × 24 dp grid. Tell us about anything the tokens cannot express, because that will need code changes.

## Constraints

- **Material 3 on Jetpack Compose.** The design can restyle Material 3 components freely. Replacing a component with a custom one costs more, so reserve that for places where it matters.
- **All values come from one theme.** Screens only use theme tokens, so the design can be applied in one module. New tokens are fine.
- **Grids must stay simple.** A cell appears thousands of times during fast scrolling, so avoid per-thumbnail shadows, blur or animation. Thumbnails do not fade in.
- **No Google Play Services and no tracking.** Fonts must be bundled with the app, not downloaded at runtime.
- **Licences.** The app is GPL v3, so fonts and icons must have a compatible licence, such as OFL or Apache 2.0.
- **English only** in version 1 (D34). Allow for long folder and file names; both are truncated to one line today.
- **Rotation and large screens.** The UI must work in landscape and must not lock orientation (D40). The app draws edge to edge, behind the system bars.
- **Accessibility.** Touch targets of at least 48 dp, sufficient contrast in both themes, and text that still works at 200 % font size. These are checked before release.
- **System dialogs cannot be styled.** Confirming a delete and the photo permission dialogs belong to Android.

## Theme tokens

### Colour roles used so far

`primary`, `primaryContainer`, `secondaryContainer`, `surface`, `surfaceVariant`, `errorContainer`, `scrim`. Components also use their standard Material 3 roles. All values are stock Material 3 for now.

| Where a role has a specific job | Role |
| --- | --- |
| Thumbnail placeholder while an image loads | `surfaceVariant` |
| Thumbnail that failed to load | `errorContainer` |
| Selected thumbnail: border and check mark | `primary` |
| Fast scrollbar thumb and its date label | `primary`, `primaryContainer` |
| Viewer background | `scrim` |
| Viewer top bar, at 60 % opacity over the image | `surface` |

### Typography roles used so far

`headlineSmall`, `titleSmall`, `bodyLarge`, `bodyMedium`, `bodySmall`, `labelLarge`, `labelMedium`. All stock Material 3.

### Shapes used so far

`small` (scrollbar thumb), `medium` (folder covers, scrollbar label).

### Dimension tokens

| Token | Current value | Used for |
| --- | --- | --- |
| `spacingSmall` | 8 dp | General spacing |
| `spacingMedium` | 16 dp | General spacing |
| `gridGap` | 2 dp | Gap between thumbnails |
| `mediaCellMinSize` | 96 dp | Minimum thumbnail size; sets the column count |
| `folderCellMinSize` | 160 dp | Minimum folder cover size |
| `selectionBorderWidth` | 3 dp | Border of a selected thumbnail |
| `scrollbarThumbWidth` | 6 dp | Visible width of the fast scrollbar |
| `scrollbarThumbHeight` | 48 dp | Height of the fast scrollbar |
| `scrollbarTouchWidth` | 48 dp | Touch area of the fast scrollbar |

An opacity of 38 % for greyed-out thumbnails is also set in code.

## Screens that exist today

There are no screenshots in this handover, because the only device holds a private photo library. The sketches show the structure only. To see the real screens, ask the project owner for screenshots with test images.

<!--wireframes-->

| Screen | Contents | States |
| --- | --- | --- |
| Photo access request | Title, explanation, "Allow access", "Open app settings" | Shown until access is granted |
| Limited access banner | "NextGallery can only see the photos you selected." and "Select more" | Above the content when only some photos are shared |
| Bottom navigation | Folders, Timeline, Map, Nextcloud | Hidden in the viewer |
| Folder list | Top bar; grid of folder covers with name and image count | Loading, no photos, content |
| Folder grid | Top bar with back and folder name; thumbnail grid; fast scrollbar | Loading, empty, load failed, content, selecting |
| Selection bar | Replaces the top bar: clear, "3 selected", select all, show selection, share, delete | The count covers all folders (D33) |
| Selection overview | Top bar with back, share, delete; grid of all selected images | Loading, nothing selected, content; images the current folder cannot show are greyed out |
| Full-screen viewer | Swipeable, zoomable image; top bar with back, file name, share, delete, info | Controls visible or hidden; tap toggles them and the system bars |
| Image info sheet | Bottom sheet: name, date, size, resolution, type, folder, location, camera, exposure, aperture, ISO, focal length | Missing values are left out |
| Opened from another app | Only the zoomable image | No controls yet |
| Messages | Snackbar: "2 images moved to the trash" with Undo; "The images could not be moved to the trash" | |
| Timeline, Map, Nextcloud tabs | Placeholder text | Replaced in later phases |

## Screens still to come

These do not exist yet. Designing them now avoids rework. Details are in [TODO.md](TODO.md).

| Phase | Screen | Needs from the design |
| --- | --- | --- |
| 4 | Settings | Root list: folders, appearance, Nextcloud, about and licences |
| 4 | Folder rules | Device folders with include, exclude and default states; custom exclude pattern; `.nomedia` folders shown as read-only |
| 4 | Appearance | Theme mode, grid columns, accent colour |
| 5 | Timeline | Thumbnail grid with sticky year, month and day headers; scrollbar label such as "March 2024" |
| 6 | Map | OpenStreetMap with thumbnail cluster markers showing a count; permission prompt; "no geotagged images" state |
| 6 | Cluster contents | Bottom sheet with a grid of a cluster's images |
| 7 | Editor | Tool bar; crop overlay with handles and aspect ratios; scale dialog; pen with colour and width pickers; undo and redo; save as copy or overwrite; unsaved changes dialog; "not enough memory" message |
| 8 | Nextcloud login | Server address, browser sign-in progress, account details, logout |
| 8 | Remote browser | Folder list and thumbnails, breadcrumbs, pull to refresh, offline and login-required states |
| 8 | Remote viewer | Viewer for server images, with loading of the full image |
| 8 | Certificate warning | Dialog with certificate details, also for a changed certificate; list of accepted certificates |
| 9 | Upload badges | Small badge on thumbnails: unknown, not uploaded, uploaded. It must stay readable over any photo |
| 9 | Upload action | In the selection bar and viewer; disabled with a hint when the Nextcloud app is missing |

## Icons to replace

All current icons are simple placeholders.

| Group | Icons |
| --- | --- |
| Navigation bar | Folders, Timeline, Map, Nextcloud |
| Actions | Back (mirrors for right-to-left), Close, Share, Delete, Select all, Show selection, Image info |
| States | Selected check mark |
| App | Launcher icon (background, foreground, monochrome) |

## Decisions we would like your view on

1. **Viewer theme.** The viewer is always dark, even in the light theme, so that images stand out. Keep this?
2. **Selected thumbnails.** Today they get a 3 dp border and a check mark at the top right. Is that clear over both light and dark photos?
3. **Thumbnail gap and size.** A 2 dp gap and 96 dp minimum cell are placeholders, and they set how dense the grids feel.
4. **Accent colours.** How many, and do they need to work with both themes without further adjustment?
5. **Nextcloud tab icon.** Should it suggest a server or cloud generally? The app is not an official Nextcloud product, so it should not look like one.

## How your delivery is applied

Colours, typography, shapes and dimensions go into `NextGalleryTheme`, so that most of the design takes effect in one place. Screens only change where the design changes their layout (task 10.0 in [TODO.md](TODO.md)). This can happen at any time and does not have to wait for the later phases.
