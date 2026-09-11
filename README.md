<p align="center">
  <img src="assets/logo.png" width="110" height="110" alt="Lyra Save Logo" style="border-radius: 50%;" />
</p>

<h1 align="center">Lyra Save</h1>

<p align="center">
  <b>Modern, High-Performance WhatsApp & WhatsApp Business Status Saver for Android</b>
</p>

<p align="center">
  <a href="https://github.com/shnwazdeveloper/LyraSave/releases/latest">
    <img src="https://img.shields.io/github/v/release/shnwazdeveloper/LyraSave?style=for-the-badge&color=6750A4&logo=github&label=Release" alt="Latest Release" />
  </a>
  <a href="https://github.com/shnwazdeveloper/LyraSave/releases/download/v1.0/LyraSave-universal-release.apk">
    <img src="https://img.shields.io/badge/Download-Universal_APK-25D366?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
  <img src="https://img.shields.io/badge/Android-7.0%2B_%28API_24--35%29-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Compatibility" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/License-Apache_2.0-007ACC?style=for-the-badge" alt="License" />
</p>

---

## Overview

**Lyra Save** is an advanced, privacy-first Android application designed to discover, preview, save, and share statuses from WhatsApp and WhatsApp Business. Built with modern Android development standards, it features Scoped Storage compliance (Android 10 to 15), background status auto-detection, skeleton shimmer loading, an in-app update notification system, and an edge-to-edge Material 3 dark interface.

---

## App Interface

<p align="center">
  <img src="assets/screenshot_permission.png" width="310" alt="Lyra Save Onboarding Screen" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="assets/screenshot_preview.png" width="310" alt="Lyra Save Media Preview Screen" />
</p>

---

## Key Features

| Feature | UI Indicator | Description |
| :--- | :--- | :--- |
| **Instant Auto-Detection** | ![Auto Detect](https://img.shields.io/badge/Auto--Detect-Direct_Scan-25D366?style=flat-square&logo=whatsapp&logoColor=white) | One-tap status discovery across regular WhatsApp, WhatsApp Business, and Dual Apps / Parallel Space directories without manual browsing. |
| **Permanent Onboarding** | ![Persistence](https://img.shields.io/badge/Access-One--Time_Setup-6750A4?style=flat-square&logo=googlecloud&logoColor=white) | Remembers access permanently so subsequent app launches bypass the onboarding screen and load statuses immediately. |
| **Skeleton Shimmer Loading** | ![Shimmer](https://img.shields.io/badge/UI-Skeleton_Shimmer-blueviolet?style=flat-square) | Elegant placeholder card animations provide immediate visual feedback while indexing media files. |
| **HD Video & Image Player** | ![ExoPlayer](https://img.shields.io/badge/Engine-ExoPlayer_%2B_Glide-red?style=flat-square&logo=youtube&logoColor=white) | Immersive full-screen media viewer powered by Media3 ExoPlayer for videos and Glide for high-resolution images. |
| **Direct-to-Gallery Export** | ![MediaStore](https://img.shields.io/badge/Storage-MediaStore_API-blue?style=flat-square&logo=android&logoColor=white) | Saves directly to `Pictures/LyraSave` and `Movies/LyraSave` with instant system MediaScanner synchronization. |
| **In-App Update Notifications** | ![Update Check](https://img.shields.io/badge/Sync-GitHub_Releases-success?style=flat-square&logo=github&logoColor=white) | Background release checker alerts users when updates are published, with one-tap direct APK downloading. |
| **Native Sharing Sheet** | ![FileProvider](https://img.shields.io/badge/Share-FileProvider-informational?style=flat-square) | Share photos and videos to social platforms seamlessly using secure Android content URIs. |

---

## Technology Stack

```
Lyra Save
├── Presentation Layer     : Material 3, ViewBinding, SwipeRefreshLayout, Edge-to-Edge Insets
├── Architecture Pattern   : MVVM (Model-View-ViewModel) + Repository Pattern
├── Asynchronous Flow      : Kotlin Coroutines & Flow
├── Media Processing       : AndroidX Media3 ExoPlayer, Bumptech Glide 4.16
├── Storage & File Access  : Scoped Storage (MediaStore API), SAF (DocumentFile), FileProvider
└── Build System           : Gradle 8.11, Android Gradle Plugin 8.7, JDK 17
```

| Component | Badge | Version / Specification |
| :--- | :--- | :--- |
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white) | 2.0.21 |
| **Target SDK** | ![Android SDK](https://img.shields.io/badge/Target_SDK-35_%28Android_15%29-3DDC84?style=flat-square&logo=android&logoColor=white) | Android 15 |
| **Min SDK** | ![Min SDK](https://img.shields.io/badge/Min_SDK-24_%28Android_7.0%29-3DDC84?style=flat-square&logo=android&logoColor=white) | Android 7.0 |
| **Video Engine** | ![Media3](https://img.shields.io/badge/AndroidX-Media3_ExoPlayer_1.5.1-red?style=flat-square) | 1.5.1 |
| **Image Loading** | ![Glide](https://img.shields.io/badge/Bumptech-Glide_4.16.0-orange?style=flat-square) | 4.16.0 |
| **Packaging** | ![Universal APK](https://img.shields.io/badge/Package-Universal_APK-000000?style=flat-square&logo=android&logoColor=white) | All ABIs (ARMv7, ARM64, x86, x86_64) |

---

## Installation & Download

### Direct APK Download
Grab the latest signed universal release directly from GitHub:

[![Download Latest Release](https://img.shields.io/badge/Download-Lyra_Save_v1.0_APK-25D366?style=for-the-badge&logo=android&logoColor=white)](https://github.com/shnwazdeveloper/LyraSave/releases/download/v1.0/LyraSave-universal-release.apk)

### Build From Source

```bash
# Clone the repository
git clone https://github.com/shnwazdeveloper/LyraSave.git

# Navigate into the project
cd LyraSave

# Build universal release APK
./gradlew assembleRelease
```

The compiled APK will be generated at:
```
app/build/outputs/apk/release/app-release.apk
```

---

## Developer

<p align="left">
  <img src="assets/avatar.png" width="70" height="70" alt="SHNWAZ Avatar" style="border-radius: 50%;" />
</p>

**SHNWAZ**  
*Web Developer & UI/UX Designer*  
*Founder @SayaProject*

[![GitHub](https://img.shields.io/badge/GitHub-shnwazdeveloper-181717?style=flat-square&logo=github&logoColor=white)](https://github.com/shnwazdeveloper)
[![X](https://img.shields.io/badge/X-@shnwazdev-000000?style=flat-square&logo=x&logoColor=white)](https://x.com/shnwazdev)
[![Instagram](https://img.shields.io/badge/Instagram-@shnwazxc-E4405F?style=flat-square&logo=instagram&logoColor=white)](https://instagram.com/shnwazxc)
[![Website](https://img.shields.io/badge/Website-shnwaz.dev-007ACC?style=flat-square&logo=googlechrome&logoColor=white)](https://shnwaz.dev)
[![Saya Project](https://img.shields.io/badge/Organization-Saya_Project-333333?style=flat-square)](https://sayaproject.org)

---

## License

```
Copyright 2026 SHNWAZ (Saya Project)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
