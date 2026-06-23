# MeldMusic

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[Download APK](https://github.com/Rizirfan/MeldMusic/raw/main/app-debug.apk)

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
walkmansh_app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/walkmansh/
│   │   │   │   ├── data/                 # Data model (Song, Playlist) & Catalog Providers
│   │   │   │   ├── playback/             # Media player playback manager and state machine
│   │   │   │   ├── theme/                # Xperia colors, typography and theme wrapper
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/       # AppleMusicUi layout & PremiumDynamicBackground
│   │   │   │   │   ├── main/             # WalkmanShViewModel (persistence and search flow)
│   │   │   │   │   └── player/           # CompactPlayerCard and FullPlayerScreen
│   │   │   │   └── MainActivity.kt       # Launcher Activity and YouTube companion component
│   │   │   └── res/                      # Drawables (including app_logo), layout resources, etc.
│   │   └── build.gradle.kts              # Application build config
└── settings.gradle.kts
```

---

## Setup & Installation

### Prerequisites
* Android Studio Koala+ or latest build tools.
* Android SDK 34 (Target SDK).
* Physical device or Emulator running Android API 21+ (Drifting blur optimizations require API 31+).

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/Rizirfan/MeldMusic.git
   cd MeldMusic
   ```
2. Build the project using Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Deploy to your connected device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

---

## Technologies Used
* **Jetpack Compose**: Declarative UI development.
* **Material Design 3**: Modern, unified theme components.
* **Coil**: Premium asynchronous image loading.
* **Android-YouTube-Player**: Low-overhead companion rendering and audio streaming adapter.
* **Gson**: Local storage serialization and state persistence.
