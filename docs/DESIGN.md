# Design

Status: delivered by Claude Design (D22) and applied to the screens that exist (task 10.0); later phases build their screens from it. This document is the handover for implementing the visual
design: every value `NextGalleryTheme` needs, and the layout, wording and states of each screen that differs from stock
Material 3. Behaviour and architecture stay in [ARCHITECTURE.md](ARCHITECTURE.md); the decisions behind the new
behaviour are D22, D34 and D43–D47 in [DESIGN_DECISIONS.md](DESIGN_DECISIONS.md).

Sources, for reference only (private claude.ai artifacts of the project owner; this document is complete without them):

- Design system **Cloudsync**, shared with NextAlarm: https://claude.ai/artifact/X3aFsDXLL1qTFopup2nsA4
- Clickable prototype with every state in its Tweaks panel: https://claude.ai/artifact/PMmofciZw4U9NXLNbH7fRa

## Decisions

| Topic | Decision | Why |
|-------|----------|-----|
| Direction | Neutral greys, one accent, flat surfaces. Photos are the only colourful thing on screen. | The handover asks for a calm, precise tool that stays out of the photos' way. |
| Themes | Light and dark follow the system (Appearance can force either). The viewer and the editor are always black. | Images stand out on black, and the editor's colours must not shift with the theme. |
| Dynamic colour | Off. The user picks one of five fixed accents instead (D34). | Accents are tested for contrast in both themes; wallpaper colours are not. |
| Selected thumbnail | 3 dp accent border plus a 24 dp disc at the top right: filled accent with a check when selected, an empty white ring on a dark backing while selecting. | Both cues read over light and dark photos; the ring tells the user they are in selection mode. |
| Grid density | 2 dp gap; 3 columns by default (D51); Appearance offers 3, 4 or 5. | 3 columns keep faces recognisable; 4–5 are for scanning large libraries. |
| Nextcloud tab icon | A plain cloud with an up arrow. | Generic; the app is not an official Nextcloud product, so no Nextcloud mark. |
| Timeline scrollbar | A date rail: the thumb plus three dates, the day in the middle of the screen large, the days before and after small (D43). | The user always sees where they are and what comes next. |
| Fonts | System font (Roboto) through `FontFamily.Default`; no font files ship. | Nothing to license or bundle. |

## Colour

### Fixed roles

| Compose role | Light | Dark | Used for |
|--------------|-------|------|----------|
| `surface` / `background` | `#F8F8F8` | `#121212` | Screen background, top bar |
| `surfaceContainer` | `#EDEDED` | `#1E1E1E` | Bottom navigation, sheets, limited-access banner |
| `surfaceContainerHigh` | `#E4E4E4` | `#2A2A2A` | Dialogs, menus, map controls |
| `surfaceVariant` | `#E2E2E2` | `#303030` | Thumbnail placeholder while loading (flat, no shimmer) |
| `onSurface` | `#1B1B1B` | `#E6E6E6` | Main text |
| `onSurfaceVariant` | `#474747` | `#C6C6C6` | Secondary text, inactive icons |
| `outline` | `#767676` | `#929292` | Field and segmented-button borders |
| `outlineVariant` | `#C6C6C6` | `#474747` | Dividers, progress tracks, date rail track |
| `inverseSurface` | `#303030` | `#E6E6E6` | Snackbar |
| `inverseOnSurface` | `#F1F1F1` | `#303030` | Snackbar text |
| `error` | `#A3161A` | `#FF8E84` | Error text, Delete in menus |
| `errorContainer` / `onErrorContainer` | `#FFDAD6` / `#410002` | `#93000A` / `#FFDAD6` | Failed thumbnail, "session ended" banner |
| `secondaryContainer` / `onSecondaryContainer` | `#FFDCBE` / `#2C1600` | `#693C00` / `#FFDCBE` | "You're offline" banner |
| `scrim` | `#000000` | `#000000` | Viewer background; sheets and dialogs at 32 % |

`inversePrimary` (snackbar action) is the accent's `primary` of the opposite theme.

### Accents (D34)

The chosen accent fills `primary`, `onPrimary`, `primaryContainer` and `onPrimaryContainer`. Blue is the default.

| Accent | Light: primary / onPrimary / container / onContainer | Dark: primary / onPrimary / container / onContainer |
|--------|-------------------------------------------------------|------------------------------------------------------|
| Blue | `#00639B` / `#FFFFFF` / `#CFE5FF` / `#001D33` | `#99CBFF` / `#003354` / `#004A76` / `#CFE5FF` |
| Teal | `#00696E` / `#FFFFFF` / `#9CF0F7` / `#002022` | `#80D4DA` / `#00363A` / `#004F53` / `#9CF0F7` |
| Plum | `#7B4E7F` / `#FFFFFF` / `#FFD6FD` / `#30063A` | `#EBB5ED` / `#4A2050` / `#613766` / `#FFD6FD` |
| Moss | `#4C6600` / `#FFFFFF` / `#CCF087` / `#141F00` | `#B1D46E` / `#253500` / `#384D00` / `#CCF087` |
| Coral | `#A23F2E` / `#FFFFFF` / `#FFDAD3` / `#3D0600` | `#FFB4A6` / `#5F150A` / `#822819` / `#FFDAD3` |

Uses: section headers, filled buttons, the selection bar (`primaryContainer`), selection border and check, the scrollbar
thumb and date-rail pill, the active navigation item, the map's location dot and cluster counts. Warning and error
colours never change with the accent.

### Always-dark colours (viewer, editor)

| Name | Value | Used for |
|------|-------|----------|
| `viewerBackground` | `#000000` | Viewer, editor, image opened from another app |
| `viewerBar` | `#121212` at 60 % | Viewer top bar over the image |
| `viewerOnSurface` | `#E6E6E6` | Text and icons on the viewer and editor |
| `viewerSheet` | `#1E1E1E` | Details sheet |
| `viewerDialog` | `#2A2A2A` | Editor dialogs and save menu |
| `viewerAccent` / `viewerOnAccent` | `#99CBFF` / `#003354` | Save button, active editor tool (container `#004A76` / `#CFE5FF`) |
| `badgeBacking` | `#000000` at 60 % | Disc behind upload badges and the selection ring |
| `badgeOn` | `#FFFFFF` | Badge icons |

### Map

| Name | Light | Dark |
|------|-------|------|
| Land | `#ECE9E1` | `#202326` |
| Water | `#AFCBDB` | `#1C3442` |

These only colour the empty map while tiles load and the attribution backing; tiles keep their own colours.

## Type

`FontFamily.Default`, sizes in `sp`:

| Role | Size / line | Weight | Used for |
|------|-------------|--------|----------|
| `headlineMedium` | 32 / 40 | 400 | Photo access title |
| `headlineSmall` | 24 / 32 | 400 | Dialog titles |
| `titleLarge` | 22 / 28 | 400 | Top bars, sheet titles, empty-state titles |
| `titleMedium` | 18 / 26 | 500 | Timeline month headers, viewer file name (400) |
| `bodyLarge` | 16 / 24 | 400 | List rows, folder names (500) |
| `bodyMedium` | 14 / 20 | 400 | Secondary text, banners, day headers |
| `bodySmall` | 13 / 18 | 400 | Image counts under folders, notes |
| `labelLarge` | 14 / 20 | 500 | Buttons, section headers, chips |
| `labelMedium` | 12 / 16 | 500 | Navigation labels, field labels, small date-rail dates (400) |

Counts and times use tabular figures (`FontFeatureSettings("tnum")`).

## Shapes and dimensions

| `NextGalleryTheme` token | Value | Notes |
|--------------------------|-------|-------|
| `shapes.small` | 8 dp | Chips, fields, breadcrumbs, scrollbar thumb |
| `shapes.medium` | 12 dp | Banners, menus, date-rail pill |
| `shapes.large` | 16 dp | Folder covers, editor tool pill |
| `shapes.extraLarge` | 28 dp | Dialogs, top of bottom sheets |
| `spacingSmall` | 8 dp | unchanged |
| `spacingMedium` | 16 dp | unchanged |
| `gridGap` | 2 dp | unchanged |
| `gridColumns` | 3 | replaces `mediaCellMinSize`; the default until Appearance sets it (D51) |
| `folderCellMinSize` | 160 dp | unchanged; folders are 2 columns on the phone |
| `selectionBorderWidth` | 3 dp | unchanged |
| `scrollbarThumbWidth` | 6 dp | unchanged |
| `scrollbarThumbHeight` | 48 dp | unchanged |
| `scrollbarTouchWidth` | 48 dp | unchanged |
| `selectionRingSize` (new) | 24 dp | inset 6 dp from the top right; 2 dp ring |
| `badgeSize` (new) | 24 dp | inset 6 dp from the bottom left; 16 dp icon |
| `clusterMarkerSize` (new) | 60 dp | 3 dp white border, count pill 22 dp high at the top right |
| `dateRailInset` (new) | 18 dp | distance from the right edge to the date labels |

The greyed-out opacity stays 38 %. Edge to edge: backgrounds run behind the system bars; content is padded with
`WindowInsets.safeDrawing`. Bottom navigation is 80 dp plus the gesture inset; the active item has a 64 × 32 dp pill.

## Screens

Text in quotes is the exact wording.

### Photo access request

Centred: a 72 dp `primaryContainer` tile with a photo icon, "See your photos", then "NextGallery shows the photos on this
phone. They stay on the phone: nothing is uploaded unless you choose to, and there is no tracking." and, smaller, "If
Android asks, you can allow all photos or pick some. You can add more later." At the bottom: a full-width filled
"Allow access" and a text button "Open app settings".

### Limited access banner

Under the top bar of Folders and the folder grid, a `surfaceContainer` card with 12 dp corners: "NextGallery can only
see the photos you selected." and a text button "Select more".

### Folder list

Top bar "Folders" with a settings icon. Two columns of square covers (16 dp corners), the name in `bodyLarge` 500 on one
line with an ellipsis, the count below ("1,204 images"). Loading: flat `surfaceVariant` blocks in the same layout. No
photos: photo icon, "No photos yet", "Photos you take or download appear here, sorted by folder."

### Folder grid

Top bar: back, the folder name, a Select icon (long-press also starts selecting, D49). The grid uses `gridGap`, square
cells and no rounding. Fast scrollbar on the right, no label. States:

- Loading: `surfaceVariant` cells.
- Empty: "This folder is empty" / "Its images were moved or deleted."
- Load failed: warning icon in `error`, "Couldn't load this folder", "Android didn't return its images. Try again; if it
  keeps failing, restart the phone.", filled "Try again".
- A thumbnail that fails: `errorContainer` cell with a broken-image icon.
- Upload badges (Phase 9) at the bottom left: cloud with a check (uploaded), outlined cloud (not uploaded), cloud with
  a question mark (unknown).

### Selection bar and overview

The bar replaces the top bar on a `primaryContainer` background: close, "5 selected", then select all, show selection,
share, upload, delete. Upload is drawn at 38 % when the Nextcloud app is missing; tapping it shows "Install the
Nextcloud app to upload photos." Delete ends with the snackbar "5 images moved to the trash" and Undo.

The overview has the top bar "Selection" with share and delete, a note "5 selected in all folders. Greyed out: 2 in
other folders, which this view can't open." and the grid with greyed-out items. Nothing selected: "Nothing selected" /
"Long-press a photo in any folder to start selecting."

### Timeline (D43)

Month headers ("March 2024", `titleMedium`) stick to the top on a `surface` background; day headers ("Saturday 16
March", "Today · Friday 6 November") scroll with the grid. On the right, a date rail that stays in place while the
photos scroll:

- A 2 dp `outlineVariant` track from top to bottom, and the 6 × 48 dp accent thumb at the scroll position.
- Next to the thumb, three labels stacked and right-aligned: the day above (`labelMedium` 400, `onSurfaceVariant`),
  the **current day** (the day in the vertical middle of the screen) at 16 sp 500 in a `primaryContainer` pill, and the
  day below. Format "24 Oct 2026". The small labels sit on `surface` at 85–90 % so they read over photos.
- At the start or end of the timeline the missing neighbour is left out.
- The rail is drawn above the sticky headers (a sibling after the list, not inside it), so headers never cover it.

### Map (D44, D45)

- Opens centred on the current location at zoom 8, with a location dot: a 20 dp accent disc with a 3 dp white ring,
  on a 48 dp accent halo at 20 %. Without location permission it opens on the area with the most photos.
- Zoom from 0 (the whole globe) to 19 (OpenStreetMap's maximum); the world repeats horizontally. One-finger drag
  moves; two fingers pinch to zoom and drag to move.
- Bottom right: a zoom column (+ and −, 48 dp each, 16 dp corners, `surfaceContainerHigh`) and a 56 dp "Show my
  location" button (`primaryContainer`). The attribution "© OpenStreetMap contributors" sits in the corner below.
- Cluster markers: 60 dp circle with the cover photo, 3 dp white border, count pill in the accent (`999+` above 999).
  Content description: "near Bolzano, 128 images".
- Tapping a cluster opens a bottom sheet (600 dp, 28 dp top corners): title "Near Bolzano" or, for a merged cluster,
  "Near Bolzano and 7 more places"; subtitle "128 images". Without a town: title "4 images", subtitle the coordinates
  "72.50° N, 40.00° W". Below, a 4-column grid with its own fast scrollbar.
- Location permission card (when photo locations are not allowed yet): "See where your photos were taken" / "Android
  hides the location in photos until you allow it. NextGallery only reads it to place photos on the map." with "Not
  now" and "Allow".
- No geotagged images: card at the top "No geotagged images" / "None of your photos has a location saved. Turn on
  location in your camera app to see new photos here."

### Viewer

Always black. The top bar (`viewerBar`, drawn behind the status bar) has back, the file name, share, upload, edit,
delete and details. A tap on the image hides the bar and the system bars. Details open a sheet on `viewerSheet` with
label/value rows; missing values are left out. Location reads "near Desenzano del Garda, Italy · 45.62° N, 10.68° E".
A remote image shows the server preview first, with a progress bar and "Loading full image · 2.4 of 6.1 MB". An image
opened from another app shows only the image.

### Editor

Black. Top bar: close, undo, redo (38 % when unavailable) and a filled "Save" that opens a menu: "Save as copy",
"Overwrite original". Tools at the bottom as icon + label: Rotate, Crop, Scale, Draw (D50); the active one has a `#004A76` pill.

- Rotate: each tap turns the image 90° clockwise. It is an action, so it does not stay active like the other tools.

- Crop: white 2 dp frame with thirds lines and 5 dp corner and edge handles; outside darkened to 60 %. Aspect chips:
  Free, Original, 1:1, 4:3, 16:9.
- Draw: six colour swatches (white, black, coral, orange, green, blue) and a width slider ("8 px").
- Scale: dialog "Scale image" with Percent, Width and Height fields (D50); changing one updates the others.
  "Proportions are kept. Original: 4000 × 3000 (12 MP), new: 3 MP.", Cancel / Scale.
- Closing with changes: "Discard your changes?" / "Your changes to IMG_2041.jpg haven't been saved and will be lost."
  with "Keep editing" / "Discard".
- Not enough memory (D30): "This image needs about 480 MB of memory, but only 300 MB are available. Close other apps
  and try again."

### Nextcloud

- Signed out: cloud tile, "Connect your Nextcloud", "Browse the photos on your own server. You sign in in the browser,
  so NextGallery never sees your password.", the "Server address" field and "Sign in".
- Signing in: the field filled, an indeterminate bar, "Waiting for you to sign in in the browser…" and "Cancel".
- Browser: breadcrumbs as 32 dp chips (the current one bold on `primaryContainer`), folder rows with counts, then the
  photo grid. Top bar actions: Select and Refresh.
- Offline: `secondaryContainer` banner "You're offline. Showing what was loaded before."; thumbnails at 60 %.
- Session ended (401): `errorContainer` banner "Your Nextcloud session ended. Sign in again to see new photos." with
  "Sign in".
- Certificate dialogs (D32): unknown: "Can't verify this server" with issued to, issued by, valid until and SHA-256;
  buttons "Trust this certificate" (in `error`) and "Cancel" (filled). Changed: "The server's certificate changed",
  "cloud.example.com now shows a different certificate from the one you accepted. If you didn't renew it yourself,
  someone may be intercepting the connection." with the accepted and the new fingerprint; "Trust the new one" and
  "Disconnect" (filled).

#### Context menu, moving and deleting (D46, D47)

- A long-press on a thumbnail opens a menu at the touch point (232 dp wide, `surfaceContainerHigh`): a header with the
  file name, then "Move to folder…", "Share", "Delete" (in `error`). There is no Properties entry; the viewer's details
  cover it.
- While images are selected, a long-press on any image adds it to the selection and opens the same menu for all of them
  (header "3 selected images"). The selection bar offers Select all and Move.
- "Move to folder…" opens a sheet: "Move 3 images", "Choose a folder on your Nextcloud. The images are moved on the
  server, without downloading them.", breadcrumbs, the subfolders (read-only shares at 38 % with "Read-only share: you
  can't add files here"), "New folder", and at the bottom the target ("To: Photos/2024/Holiday/Italy"), "Cancel" and
  "Move here". "Move here" is disabled in the images' own folder ("The images are already in this folder.").
- After moving: snackbar "3 images moved to Italy" with Undo (moves them back).
- Name conflict: "IMG_2524.jpg is already in Italy", the dates and sizes of both files, a checkbox "Do the same for the
  other conflict", and stacked buttons "Keep both" (filled), "Replace the one in Italy", "Skip this image".
- Share (D48): the images are downloaded first, with a progress bar and "Downloading 2 of 3…", then the Android share
  sheet opens. Offline, images that are not cached show "You're offline. Sharing needs a connection to your server."
- Delete: snackbar "1 image moved to the Nextcloud trash bin" with Undo (restores from the trash bin).
- Offline: Move shows "You're offline. Moving needs a connection to your server."

### Settings

Rows with an icon, title, secondary line and chevron: "Folders" (Include, exclude and hidden folders), "Appearance"
(Theme, grid columns and accent colour), "Nextcloud" (the account, or "Not connected"), "About & licences".

- Folder rules: "Default shows a folder if it has images. Include always shows it; Exclude always hides it." Each
  folder has a three-way segmented button Include / Default / Exclude. `.nomedia` folders show a lock chip "Hidden by a
  .nomedia file; can't be changed here". At the bottom the field "Also exclude folders matching" ("* matches any
  characters. One pattern per line.").
- Appearance: segmented Theme (System / Light / Dark) with "The photo viewer and the editor stay black in every
  theme.", segmented Grid columns (3 / 4 / 5) with a live two-row preview, and five 52 dp accent swatches (the chosen one
  with a check and a 3 dp ring), followed by "<Accent>. Colours buttons, selections and the scrollbar. Warning and error
  colours never change."
- About & licences: the app icon and "NextGallery", "Free software, GPL-3.0. No tracking.", "Version" with
  `BuildConfig.VERSION_NAME` and the build number, "Source code" linking to https://github.com/drehtuer/NextGalleryApp
  (opens the browser), "Open-source licences", and the note "Map data © OpenStreetMap contributors, available under the
  Open Database Licence. Place names from GeoNames (geonames.org), CC BY 4.0, looked up on the phone." The screen needs
  `BuildConfig` enabled; `versionName` is already set.

## Icons

24 dp grid, 2 dp stroke, round caps and joins, as vector drawables: folder, timeline (three tiles), map (folded map),
Nextcloud tab (cloud with an up arrow), settings (two sliders), select (checklist), select all, show selection (eye),
share, delete, details (i in a circle), upload (arrow onto a line), edit (pencil), refresh, move (folder with an arrow),
rotate, crop, scale, draw, palette, zoom in, zoom out, my location (crosshair), the three upload badges, broken image, lock.
Icon-only buttons have content descriptions.

## Implementation notes

- All values go into `NextGalleryTheme` in `:core:ui` (task 10.0): a light and a dark scheme per accent, the fixed
  roles above, the always-dark colours and the new dimension tokens. Feature modules keep reading only the theme.
- The viewer and editor wrap their content in a dark scheme built from the always-dark colours, whatever the setting.
- The accent and theme mode come from `AppSettings` (D34) once Phase 4 adds them; until then the theme follows the
  system with the blue accent. The column count comes from the Appearance setting, by default 3 (D51).
- The date rail and the map behaviour are specified in TODO Phases 5 and 6; moving and deleting on the server in
  Phase 8.
