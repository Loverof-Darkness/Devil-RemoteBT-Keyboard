<div align="center">

<img src="Logo.svg" width="128" height="128" alt="Devil RemoteBT Keyboard Logo" />

# Devil RemoteBT Keyboard

A Bluetooth HID keyboard/controller for Android, based on the open-source **Bluke** project by **Arnav Kumar (@arnav-kr)**.

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?style=for-the-badge)](LICENSE)

</div>

---

## Download

### Latest release

**[Download the latest Devil RemoteBT Keyboard APK →](https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/releases/latest)**

The link above always opens the **latest published GitHub Release**, where the final APK and its release files are hosted. This is the recommended download location for end users.

For release history, older versions, source archives, and checksums, visit the [full Releases page](https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/releases).

---

## About

**Devil RemoteBT Keyboard** is a customized, maintained derivative of [Bluke](https://github.com/arnav-kr/Bluke), a native Android Bluetooth HID application that turns an Android device into a wireless keyboard/controller without requiring host-side companion software.

The project retains the upstream Bluetooth HID foundation and original application capabilities while adding a dedicated **native Android keyboard input experience** for normal phone typing, including Gboard and other Android IMEs.

The goal is to keep the reliability and compatibility of native Bluetooth HID while making text entry faster, more natural, and easier to use on a phone.

> **Modified version notice:** Devil RemoteBT Keyboard is a modified version of Bluke. Modifications are maintained in this repository and include the native system-keyboard input workflow, Live/Buffer text entry, messenger-style sending, Line Break/Shift+Enter handling, more reliable native text transmission, Hellfire theme work, application identity changes, and related usability fixes.
>
> **Modification date:** 2026-09-06
>
> **License:** This entire project is distributed under the **GNU Affero General Public License, version 3 (AGPL-3.0)**. See [LICENSE](LICENSE).

## Original Bluke Features

The underlying Bluke functionality is retained, including:

* **No host software required:** Uses Android's native Bluetooth HID profile to connect directly with Windows, macOS, Linux, ChromeOS, Android TV, and compatible game consoles.
* **Mechanical switch sound synthesis:** Includes real-time switch acoustics and the original keyboard sound assets used by Bluke.
* **Themes and case colors:** Retains the original built-in visual presets and selectable case colors.
* **System integration:** Supports system haptics, OLED black mode, and Material You dynamic colors.
* **Keyboard / controller functionality:** Preserves the original Bluke input architecture and HID behavior rather than replacing it with an unrelated implementation.

See the original project for the upstream implementation and complete history:

**https://github.com/arnav-kr/Bluke**

## What We Added / Changed

### Native Android system-keyboard input

A dedicated text-entry field lets users type with the keyboard already installed on their Android phone instead of manually pressing individual on-screen HID keys.

This works with Android's normal IME system, including **Gboard and other system keyboards**. Typed characters are converted to Bluetooth HID keyboard input and sent to the connected device; no separate host application is required.

### Live input mode

**Live mode** sends newly typed text to the connected host as it is entered. End-of-text deletions are translated into HID Backspace input.

### Buffer input mode

**Buffer mode** lets users compose a complete message in the Android text field before sending it with the in-field Send button.

### Messenger-style Send button

The text field includes a **Send** button on the right, similar to a modern messaging composer.

* **Live mode:** sends the Enter action to the host after the current text.
* **Buffer mode:** sends the composed text followed by Enter.
* After sending, the text field is **automatically cleared**.

### Dedicated Line Break / Shift+Enter action

The previous bottom Enter action is now a **Line break (Shift+Enter)** control.

* **Buffer mode:** inserts a newline without sending the message.
* **Live mode:** sends the HID equivalent of **Shift+Enter**.

### Direct Backspace control

A dedicated Backspace control is available alongside the Line Break action. It can send Backspace directly to the host and supports press-and-hold repeat behavior without depending on the Android IME's deletion callback.

### Reliable native text transmission

Native text sending serializes HID text transmission and uses short pacing delays between key press/release reports. This reduces the risk of repeated or apparently stuck characters on hosts that cannot process a burst of HID reports reliably.

### Hellfire visual theme

A dedicated **Hellfire** palette was added as the project's default visual direction, while the existing upstream palette choices remain available for compatibility. The Hellfire palette uses a red-accented dark/light color system and does not rely on device wallpaper colors while active.

### Application identity and attribution

The Android application identity and presentation are customized for **Devil RemoteBT Keyboard**. The project does not present itself as the original Bluke application; the upstream project and original author remain explicitly credited.

## Why These Changes Exist

Bluke provides the important native Android Bluetooth HID foundation. The additions in this project focus primarily on text-entry usability and a more polished standalone application experience.

The Live/Buffer split serves two workflows:

* **Live** is optimized for direct, continuous keyboard replacement.
* **Buffer** is optimized for composing and sending complete messages.

The Send/Line Break separation makes those workflows less error-prone, while automatic clearing prevents stale sent text from remaining in the composer.

## Credits & Attribution

### Original Project — Bluke

This project is **based on and derived from [Bluke](https://github.com/arnav-kr/Bluke)** by:

**Arnav Kumar (@arnav-kr)**

Original repository:
**https://github.com/arnav-kr/Bluke**

The original Bluke project provides the core Bluetooth HID application architecture, keyboard/controller functionality, UI foundation, Android implementation, and much of the functionality on which this project builds.

Please give the original author credit for the foundational work and refer to the upstream repository for the original project's history and development.

### kbsim

The original Bluke project credits **[kbsim](https://github.com/tplai/kbsim)** for inspiration for the web keyboard simulator UI and for the mechanical switch audio assets used by Bluke.

## Legal Notice

**Copyright and modifications:** Copyright in the original Bluke work remains with its respective copyright holders. Modifications and original additions in Devil RemoteBT Keyboard are maintained by Loverof-Darkness.

**License:** Devil RemoteBT Keyboard is distributed under the **GNU Affero General Public License v3 (AGPL-3.0)**. The complete license text is included in [LICENSE](LICENSE).

**No warranty:** The software is provided without warranty to the extent permitted by the AGPL-3.0.

**Source:** The complete corresponding source for the distributed application is available in this public repository. Release pages provide the corresponding tagged source and the built APK for that release.

## Release Downloads

The canonical place for user-facing builds is the repository's **GitHub Releases** page:

**https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/releases**

Every tagged release is intended to publish:

* the signed release APK produced by the release workflow;
* a matching source archive for the exact release tag;
* SHA-256 checksums for published release files;
* generated GitHub release notes.

This keeps the final APK and the corresponding source together in one place.

## Requirements

* **Android 9 (API level 28) or higher**.
* A device with hardware support for Android's **Bluetooth HID Device** profile. Availability can vary by device chipset and manufacturer ROM.

## Build

### Prerequisites

* Android Studio Koala or newer.
* Android SDK 36.
* JDK 17.
* Gradle Wrapper included in the repository.

### Build the debug APK

```bash
./gradlew assembleDebug
```

### Build the release APK

```bash
./gradlew clean lintDebug assembleRelease
```

The release build is minified/shrunk using the project's existing R8 configuration.

## Automated Build

GitHub Actions builds the project on pushes to `main` and uploads the generated APKs as workflow artifacts. A successful build is required before treating a commit as a release candidate.

The latest verified build before the current documentation hardening completed successfully on **2026-09-06** at commit `56294b164c27e83f6b94bbd4cdd8f4a95d4e3cf0`.

That build produced the Android APK artifact and completed all build/upload steps successfully.

## Creating a Release

Create and push a version tag using the format `vMAJOR.MINOR.PATCH`:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The release workflow will then build the release APK, create the source archive and checksums, and publish everything to the GitHub Releases page for that tag.

## Upstream

This repository is maintained as a customized project derived from Bluke. The upstream project remains the authoritative source for the original Bluke implementation:

**https://github.com/arnav-kr/Bluke**
