# MeldMusic

[![Platform](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)
[![Platform](https://img.shields.io/badge/Platform-Desktop-blue.svg)](https://nodejs.org)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)

[![Download APK](https://img.shields.io/badge/Android-Download-brightgreen?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Rizirfan/MeldMusic/blob/main/MeldMusic.apk?raw=true)

MeldMusic is a premium, minimalist Android music streaming player built using Jetpack Compose and Material 3. Featuring an Apple Music-inspired aesthetic with Spotify-inspired dark theme, background playback with notification controls, and an animated waveform progress bar, MeldMusic offers an immersive, modern listening experience directly integrated with live YouTube audio streams.

---

## Features

### Apple Music-Inspired UI
* **3-Tab Navigation**: Home, Search, and Library tabs with floating bottom navigation bar.
* **MiniPlayer**: Persistent mini player with album art, playback controls, and progress bar — shows gradient colors extracted from the album art.
* **Full Player Screen**: Swipe-to-dismiss player with animated waveform progress bar, album art pulse animation, queue panel, sleep timer, and add-to-playlist dialog.

### Background Playback
* **Foreground Service**: MediaSession integration with lock screen controls, audio focus handling, and notification with album art.
* **Notification Controls**: Play/Pause, Next, Previous, and stop actions directly from the notification.

### Playlists & Library
* **Custom Playlists**: Create, play, and delete your own playlists.
* **Liked Songs**: Save favorite songs with one tap.
* **Search History & Recently Played**: Automatic tracking for quick access.

### Settings
* **Theme**: System, Light, or Dark mode with pure white / pure black backgrounds.
* **Song Quality**: Auto, High, Medium, or Low streaming quality.
* **Fade In/Out**: Crossfade between songs toggle.
* **Voice Search**: Search music using voice recognition.

---

## Project Structure

```
MeldMusic/
├── app/                  # Android Mobile Module
│   ├── src/main/
│   │   ├── java/com/example/walkmansh/
│   │   │   ├── data/     # Song model, DataRepository, API client
│   │   │   ├── playback/ # PlaybackManager & MusicService (foreground service)
│   │   │   ├── theme/    # Color, Type, Theme (Apple Music/Spotify palette)
│   │   │   └── ui/       # Compose screens (AppleMusicUi, FullPlayerScreen, etc.)
│   │   └── res/          # Drawables (vector icons, app_logo, splash)
└── settings.gradle.kts
```

---

## Setup & Installation

### Android Mobile App
1. Prerequisites: Android SDK 36 (Target SDK), physical device or emulator running API 24+.
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
