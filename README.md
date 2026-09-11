# Lyra Save

Lyra Save is an advanced, production-grade Android utility for viewing, preserving, and sharing WhatsApp and WhatsApp Business media statuses. Built with modern Android development best practices, it combines a sleek Material 3 dark interface, Scoped Storage compliance, high-performance background processing, and edge-to-edge window inset management.

---

## Features

- **Automated Status Detection**: Instant background discovery of cached statuses across standard WhatsApp, WhatsApp Business, and dual-app / clone directories without repetitive folder navigation.
- **Storage Access Framework (SAF) Fallback**: Granular, persistable folder permissions ensuring full compliance on Android 11 through Android 15.
- **One-Time Onboarding**: Remembers permissions and setup permanently so subsequent app launches jump straight into your status feeds.
- **Skeleton Shimmer Loading**: Elegant, responsive placeholder cards that animate smoothly while statuses are being indexed.
- **High-Definition Media Player**: Full-screen immersive viewer powered by Media3 ExoPlayer for videos and Glide for high-resolution photo statuses.
- **Direct-to-Gallery Export**: High-speed, Scoped Storage compliant media saving directly to Pictures/LyraSave and Movies/LyraSave with immediate MediaScanner gallery synchronization.
- **In-App Update Notifications**: Integrated GitHub release checker alerting users when newer versions or bug-fix updates are available.
- **Native Sharing Sheet**: Direct sharing to WhatsApp, Telegram, Instagram, and other applications through secure FileProvider content URIs.
- **Developer Profile & Hub**: Built-in settings screen featuring developer socials, custom folder overrides, storage usage details, and cache clearing tools.

---

## Architecture and Technologies

- **Architecture**: MVVM (Model-View-ViewModel) paired with the Repository pattern.
- **Language**: Kotlin with Coroutines and Flow for non-blocking asynchronous I/O.
- **UI Toolkit**: Android Jetpack, Material Design 3, ViewBinding, SwipeRefreshLayout, Edge-to-Edge WindowInsetsCompat.
- **Media Engine**: AndroidX Media3 ExoPlayer for video rendering; Bumptech Glide for hardware-accelerated image caching and decoding.
- **Storage Systems**: Scoped Storage with MediaStore APIs, Storage Access Framework (SAF) DocumentFile, and AndroidX FileProvider.
- **Distribution**: Universal release APK compatible with ARMv7, ARM64, x86, and x86_64 devices running Android 7.0 (API 24) to Android 15 (API 35).

---

## Getting Started

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 35
- JDK 17
- Gradle 8.11+

### Build from Source

Clone the repository:
```bash
git clone https://github.com/shnwazdeveloper/LyraSave.git
cd LyraSave
```

Build the release APK:
```bash
./gradlew assembleRelease
```

The compiled release APK will be located at:
`app/build/outputs/apk/release/app-release.apk`

---

## Developer and Maintainer

- **Developer**: SHNWAZ
- **GitHub**: https://github.com/shnwazdeveloper
- **X / Twitter**: https://x.com/shnwazdev
- **Instagram**: https://instagram.com/shnwazxc
- **Website**: https://shnwaz.dev
- **Organization**: Saya Project (https://sayaproject.org)

---

## License

This project is licensed under the Apache License 2.0. See the LICENSE file for details.
