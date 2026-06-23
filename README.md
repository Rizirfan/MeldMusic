# MeldMusic

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Desktop-blue.svg)](https://nodejs.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[Download APK](https://github.com/Rizirfan/MeldMusic/blob/main/app-debug.apk?raw=true) or [Direct Download](https://raw.githubusercontent.com/Rizirfan/MeldMusic/main/app-debug.apk)

MeldMusic is a premium, minimalist Android music streaming player built using Jetpack Compose and Material 3. Featuring Xperia-inspired aesthetics, a smooth glassmorphic interface, and a dynamic drifting liquid blobs background, MeldMusic offers an immersive, modern listening experience directly integrated with live YouTube audio streams.

---

## Features

### Premium Visual Aesthetics
* **Dynamic Blobs Background**: Overlapping colorful liquid blobs (glowing purple, teal, and pink gradients) drift organic-like across the screen. Backed by a hardware-accelerated heavy blur (Modifier.blur), they create a gorgeous floating atmosphere.
* **Glassmorphic UI**: Floating bottom navigation bar, card components, panels, search bars, and the sliding MiniPlayer are styled as semi-transparent glass sheets (alpha values between 0.4f and 0.6f with subtle translucent borders).
* **Top Bar Theme Integration**: Automatic theme-dependent status bar and navigation bar icon color adjustments (light icons in dark mode, dark icons in light mode) for perfect readability.
* **Quick Theme Toggle**: Instantly switch between Light and Dark themes via a one-tap header switch button.

### Premium Controls & Navigation
* **2-Tab Floating Navigation**: Simplified, floating bottom navigation containing only Player and Search tabs.
* **Compact Player Deck**: Play, pause, skip, scrub tracks, and toggle shuffle/repeat directly from the home screen compact player.
* **Playlists Management**: 
  * View default curated playlists (Chill Lofi, Essential Pop, Electronic Focus, Workout, etc.) alongside your own custom playlists.
  * Add songs to playlists on-the-fly via a modern picker dialog.
  * Delete custom playlists and remove songs from your Liked list directly with inline trash actions.
* **Sleep Timer**: Schedule music termination (5, 15, 30, 60 minutes) to sleep peacefully.
* **Queue Panel**: Full overview of upcoming songs with simple track re-ordering and removal.

### Live Companion Audio
* **YouTube Streaming**: Live search and streaming powered by official YouTube audio streams.
* **Background Playback**: Employs a low-footprint, audio-only background companion playback engine to prevent system suspension and let your music play while screen-off.

---

## Project Structure

```
MeldMusic/
├── app/                  # Android Mobile Module
│   ├── src/main/
│   │   ├── java/com/example/walkmansh/
│   │   │   ├── data/     # Song model & Catalog Provider
│   │   │   ├── playback/ # Playback Manager
│   │   │   └── ui/       # Compose layouts (AppleMusicUi, PremiumDynamicBackground)
│   │   └── res/          # Drawables (app_logo) & styles
├── desktop/              # Electron Desktop Module
│   ├── index.html        # App layout and structure
│   ├── style.css         # Glassmorphic themes & background drift animations
│   ├── app.js            # Playback controller, search, storage persistence
│   ├── main.js           # Electron main process (frameless window config)
│   └── package.json      # Node packaging & dependencies
└── settings.gradle.kts
```

---

## Setup & Installation

### Android Mobile App
1. Prerequisites: Android SDK 34 (Target SDK), physical device or emulator running API 21+.
2. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Deploy to your connected device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Desktop Application (Windows, macOS, Linux)
1. Prerequisites: Node.js (v18+) and npm installed.
2. Navigate to the `desktop` directory:
   ```bash
   cd desktop
   ```
3. Install dependencies:
   ```bash
   npm install
   ```
4. Run the desktop application:
   ```bash
   npm start
   ```

---

## Technologies Used
* **Jetpack Compose**: Declarative UI development.
* **Material Design 3**: Modern, unified theme components.
* **Coil**: Premium asynchronous image loading.
* **Android-YouTube-Player**: Low-overhead companion rendering and audio streaming adapter.
* **Gson**: Local storage serialization and state persistence.
