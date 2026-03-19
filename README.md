# Yarn & Thread — Android App

Port of the Knitting Pattern Reader web app to native Android (Java).

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 26+ (minSdk 26 = Android 8.0)
- Java 8

---

## How to open in Android Studio

1. Unzip this folder
2. Open Android Studio → **File > Open**
3. Select the `YarnAndThread` root folder
4. Wait for Gradle sync to complete
5. Connect a device or start an emulator (API 26+)
6. Press **Run ▶**

---

## Project structure

```
YarnAndThread/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/yarnandthread/app/
│   │   ├── model/
│   │   │   ├── Project.java          — Project data (name, page, zoom, annotations, counters)
│   │   │   ├── Annotation.java       — Highlight or note annotation (normalized coords)
│   │   │   └── Counter.java          — Row/stitch counter with optional max & link
│   │   ├── database/
│   │   │   ├── AppDatabase.java      — Room database singleton
│   │   │   ├── ProjectDao.java       — CRUD queries
│   │   │   ├── ProjectRepository.java— Repository pattern wrapper
│   │   │   └── Converters.java       — Gson-based type converters for Room
│   │   ├── ui/
│   │   │   ├── MainActivity.java     — Project list screen
│   │   │   ├── ProjectViewModel.java — ViewModel for both screens
│   │   │   └── ProjectReaderActivity.java — PDF reader, counters, annotations
│   │   ├── adapter/
│   │   │   ├── ProjectAdapter.java   — RecyclerView adapter for project list
│   │   │   └── CounterAdapter.java   — Horizontal RecyclerView for counters
│   │   └── util/
│   │       ├── PdfRenderer2Helper.java — PDF rendering + annotation overlay drawing
│   │       └── PdfStorage.java         — Copy/read/delete PDF files in private storage
│   └── res/
│       ├── layout/
│       │   ├── activity_main.xml
│       │   ├── activity_project_reader.xml
│       │   ├── item_project.xml
│       │   ├── item_counter.xml
│       │   ├── dialog_new_project.xml
│       │   └── dialog_add_counter.xml
│       ├── drawable/          — Color swatches, button states, chip background
│       ├── values/
│       │   ├── colors.xml     — Full color palette matching the web app
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── xml/
│           └── file_paths.xml — FileProvider paths
```

---

## Feature mapping (HTML → Android)

| Web feature                        | Android implementation                              |
|------------------------------------|-----------------------------------------------------|
| localStorage projects              | Room database (SQLite)                              |
| localStorage PDF (base64)          | App-private file storage (`/files/pdfs/`)           |
| pdf.js rendering                   | Android `PdfRenderer` (built-in, API 21+)           |
| Annotation layer (div overlay)     | Custom `View` (`AnnotationOverlayView`) drawn on Canvas |
| Highlight tool (drag rect)         | `onTouchEvent` → `ACTION_DOWN/MOVE/UP` → draw rect  |
| Note tool (tap + dialog)           | Tap → `AlertDialog` → draw note box on canvas       |
| Eraser tool                        | Tap to find & remove annotation by bounds           |
| Counters bar                       | Horizontal `RecyclerView` with custom chip layout   |
| Counter increment/decrement        | `CounterAdapter` callbacks → `ProjectReaderActivity`|
| Counter max + auto-reset           | `incrementCounter()` recursive method               |
| Counter linking (chain)            | `linkedTo` index + recursive `incrementCounter()`   |
| Pinch-zoom                         | `ZoomIn`/`ZoomOut` buttons (re-render at new zoom)  |
| Page navigation                    | `ScrollView` scroll + page observer                 |
| Project sidebar                    | `MainActivity` with `RecyclerView`                  |

---

## Optional: Custom fonts

The web app uses **Courier Prime**, **Special Elite**, and **Playfair Display**.
To add them:

1. Download from [fonts.google.com](https://fonts.google.com)
2. Place `.ttf` files in `app/src/main/res/font/`
3. In `themes.xml`, set:
   ```xml
   <item name="android:fontFamily">@font/courier_prime</item>
   ```

Without the font files the app compiles and runs fine using system fonts.

---

## Notes

- PDFs are stored in app-private storage — no external storage permission needed on API 29+.
- Annotations are stored as normalized coordinates (0.0–1.0) so they survive zoom changes.
- Counter linking supports chains (counter A resets → increments B → B resets → increments C, etc.) with a depth guard of 20 to prevent infinite loops.
- The `AnnotationOverlayView` is a custom `View` placed as an overlay on top of each rendered PDF page `ImageView` inside a `FrameLayout`.
