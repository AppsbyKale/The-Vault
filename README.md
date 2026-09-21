# The Nobbery Vault

A local-first digital asset manager for artists, built with Kotlin and Jetpack Compose. It organizes project files, vector exports, and image variants into a searchable personal vault — with styling, tagging, linking, and full backup/restore.

## About

The Nobbery Vault is a portfolio tool for managing **Infinite Painter** project files (`.pntr`) alongside their derived assets:

- **Source project files** (`.pntr`)
- **Vector exports** (`.svg`)
- **Transparent-background images** (`.png`)
- **Solid-background images** (`.png`)

Each imported project becomes an **asset** with categorized file slots. Assets can be searched, filtered, tagged, grouped into collections, linked into parent/child lineages, merged, watermarked, and exported.

## Features

- **Asset library** — sort, search, and filter across categories, collections, and tags
- **Automatic file classification** — new imports are routed to the correct slot (source / vector / transparent / background) by inspecting the file
- **Asset profile** — manage files, metadata, classification, tags, collections, and notes per asset
- **Link & merge** — build parent/child lineages or merge assets with duplicate file sets
- **Watermark Studio** — position, scale, and apply a reusable watermark image
- **SVG converter** — convert image assets to vector SVG
- **Backup / restore** — export the entire vault (assets, files, and metadata) to a portable ZIP and restore it on any device
- **Bulk import** — import and organize many project files at once via background work

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Persistence | Room (SQLite) with Gson type converters |
| Architecture | MVVM (ViewModel + Room DAO data layer) |
| Async | Coroutines / Flow / WorkManager |
| Image loading | Coil |
| DI | Manual (planned: Hilt) |
| Logging | Timber |
| Min / Target SDK | 24 / 34 |

## Architecture

The app follows an **MVVM** structure:

```
MainActivity
   └─ VaultViewModel        (state + business logic, exposed as StateFlow)
        └─ AssetDao         (Room persistence)
             └─ VaultDatabase
Compose screens observe ViewModel flows and render state.
```

Business utilities live under `logic/` and interact with the data layer through the Room DAO. The codebase is organized by responsibility:

```
app/src/main/java/com/example/thenobbery/
├── data/           Room entities, DAOs, database + migrations
├── logic/          File classification, export, backup/restore, bitmap utilities
├── ui/
│   ├── components/ Shared Compose components
│   ├── screens/    Feature screens and dialogs
│   └── theme/      Material 3 theming
└── viewmodels/     VaultViewModel
```

## Building

Requirements: [Android SDK](https://developer.android.com/studio) 34, JDK 17.

```bash
./gradlew :app:assembleDebug
```

The build uses standard Gradle plugin management with version catalogs centralized in `settings.gradle.kts`.

## Roadmap

- Introduce a formal **Repository** layer and adopt **Hilt** for dependency injection
- Add unit tests for the file-classification and backup/restore utilities
- Replace manual `object` singletons with scoped, injectable dependencies

## License

Private portfolio project.
