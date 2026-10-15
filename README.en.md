<div align="center">

# Click Counter

**Mechanical feel. One screen. Nothing extra.**

A minimal Android counter with a dark metal interface.

[Русский](README.md) · [English](README.en.md)

![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-native-F59E0B?style=flat-square)
![Offline](https://img.shields.io/badge/Offline-100%25-64748B?style=flat-square)
[![Version 1.0.0](https://img.shields.io/badge/Version-1.0.0-8B5CF6?style=flat-square)](https://github.com/Renamekk/Click-Counter/releases/tag/v1.0.0)
[![License: MIT](https://img.shields.io/badge/License-MIT-0EA5E9?style=flat-square)](LICENSE)

### [Download APK](https://github.com/Renamekk/Click-Counter/releases/latest)

Android 8.0 or newer · No ads · No account · No permissions

</div>

<div align="center">

<img src="docs/images/app.png" alt="Click Counter UI concept" width="280">

<sub>UI concept</sub>

</div>

## Just a counter

Four mechanical digit drums, soft lighting and animated presses. Every action is available on a single screen:

| Button | Action |
| :---: | --- |
| **+** | Add one |
| **-** | Subtract one |
| **RESET** | Reset to 0000 |

The range is **0000-9999**. The value stays within these limits. Your current count is saved on the device and restored the next time you open the app.

No history, statistics or extra screens. No network connection or server is needed.

## Install

1. Open the [latest release](https://github.com/Renamekk/Click-Counter/releases/latest) and download the `.apk` file.
2. Open it on your phone. If Android asks, allow installation from your selected browser or file manager.
3. Install the app and start counting.

Each release also includes a `.sha256` file to check the APK's integrity.

## Build from source

A native Java app with no third-party UI libraries. The project uses **Gradle 9.6.1**, **Android Gradle Plugin 9.0.0**, **Android SDK Platform 36** and **Build Tools 36.0.0**. The build was verified with **JDK 24**; the source code targets Java 17.

On Windows with JDK and Android SDK installed:

```powershell
.\build-apk.ps1
```

If dependencies are already cached:

```powershell
.\build-apk.ps1 -Offline
```

The script locates the SDK through `ANDROID_HOME`, `ANDROID_SDK_ROOT` or `%LOCALAPPDATA%\Android\Sdk`, runs the release build and lint, verifies the signature and saves the output:

```text
artifacts/
  Click-Counter-1.0.0.apk
  Click-Counter-1.0.0.apk.sha256
```

A local signing key is created on the first build. **Keep `.signing/` and `signing.properties` safe**: you need them to publish updates with the same signature. These files are excluded from Git.

## Version 1.0.0 checks

`assembleRelease` and `lintRelease` passed: 0 lint errors and 13 warnings. The APK signature and ZIP alignment were verified. On-device interface and behavior testing is still pending.

Main screen: [`MainActivity.java`](app/src/main/java/app/clickcounter/MainActivity.java).

## License

[MIT](LICENSE) · Copyright (c) 2026 Renamekk.
