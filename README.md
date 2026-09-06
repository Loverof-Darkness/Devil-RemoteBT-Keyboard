<div align="center">

<img src="Logo.svg" width="128" height="128" alt="Devil RemoteBT Keyboard Logo" />

# Devil RemoteBT Keyboard

A Bluetooth HID keyboard controller for Android, based on the open-source **Bluke** project by **Arnav Kumar (@arnav-kr)**.

[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![License: AGPL v3](https://img.shields.io/badge/License-AGPL%20v3-blue.svg?style=for-the-badge)](LICENSE)

</div>

---

## About

**Devil RemoteBT Keyboard** is a customized continuation of [Bluke](https://github.com/arnav-kr/Bluke), a native Android Bluetooth HID application that turns an Android device into a wireless keyboard/controller without requiring host-side companion software.

The project keeps the proven Bluetooth HID foundation and original application capabilities from Bluke, while adding a dedicated **native Android keyboard input experience** for people who want to type normally with their phone's system keyboard, including Gboard and other Android IMEs.

The goal is simple: keep the reliability and device compatibility of native Bluetooth HID while making text entry faster, more natural, and easier to use on a phone.

## Original Bluke Features

The underlying Bluke functionality is retained, including:

* **No host software required**: Uses Android's native Bluetooth HID profile to connect directly with Windows, macOS, Linux, ChromeOS, Android TV, and compatible game consoles.
* **Mechanical switch sound synthesis**: Includes real-time switch acoustics such as Cherry MX Brown, Holy Panda, Alpaca, Kailh Box Navy, Buckling Spring, and Topre.
* **Themes and case colors**: Includes the original built-in visual presets and selectable case colors.
* **System integration**: Supports system haptics, OLED black mode, and Material You dynamic colors.
* **Keyboard / controller functionality**: Preserves the original Bluke input architecture and HID behavior rather than replacing it with an unrelated implementation.

See the original project for the upstream implementation and complete history:

**https://github.com/arnav-kr/Bluke**

## What We Added / Changed

### 1. Native Android system-keyboard input

A dedicated text-entry field was added so users can type with the keyboard already installed on their Android phone instead of manually pressing individual on-screen HID keys.

This works with Android's normal IME system, including **Gboard and other system keyboards**.

**Benefits:**

* Much faster text entry for messages, commands, searches, and long text.
* Uses the familiar Android keyboard experience.
* No separate host application is required; the typed characters are converted to Bluetooth HID keyboard input and sent to the connected device.

### 2. Live input mode

**Live mode** sends newly typed text to the connected host as it is entered. Deletions at the end of the text are also translated into HID Backspace input.

**Benefits:**

* Very low-friction typing for interactive use.
* The computer/device receives the text while you type.
* Useful when the remote keyboard is being used as a direct replacement for physical keyboard input.

### 3. Buffer input mode

**Buffer mode** lets you compose the complete message in the Android text field first, then send it with the in-field Send button.

**Benefits:**

* Prevents partially composed messages from being transmitted.
* Makes longer messages easier to review before sending.
* Gives users an experience closer to a chat application's message composer.

### 4. Messenger-style Send button

The text field now includes a **Send button inside the field on the right**, similar to modern messaging applications.

Pressing Send behaves as the message submission action:

* **Live mode:** sends the Enter action to the host after the current text.
* **Buffer mode:** sends the composed text followed by Enter.
* After sending, the text field is **automatically cleared** so the previous message cannot be accidentally edited or confused with the next one.

**Why this matters:**

The field behaves like a true message composer instead of leaving already-sent text on screen. This reduces accidental duplicate sends, makes the next message immediately obvious, and keeps the interface clean during repeated use.

### 5. Dedicated Line Break / Shift+Enter action

The previous bottom Enter action was changed into a **Line break (Shift+Enter)** control.

* **Buffer mode:** inserts a newline into the message without sending it.
* **Live mode:** sends the HID equivalent of **Shift+Enter**, allowing a new line without triggering normal Enter submission behavior.

**Benefits:**

* Makes multi-line messages practical.
* Separates **Send/Enter** from **Line break/Shift+Enter** so the two actions are not confused.
* Matches the interaction pattern used by many modern messaging and text-entry interfaces.

### 6. More reliable native text transmission

Native text sending uses serialized HID text transmission with small pacing delays between key press/release reports.

This was added to avoid sending a burst of HID reports so quickly that some host systems could interpret the sequence incorrectly, which can otherwise result in repeated or apparently stuck characters.

**Benefits:**

* More dependable character delivery.
* Lower risk of repeated/stuck characters during fast text entry.
* Better behavior across hosts with different Bluetooth/HID processing speeds.

### 7. Application identity

The Android application identity was customized for this project as:

**DevilRemoteKeyboard**

This distinguishes the maintained application from the upstream Bluke app while clearly documenting that its core implementation originates from Bluke.

## Why These Changes Exist

Bluke already provides the important part: a native Android Bluetooth HID foundation that can communicate directly with supported host devices. The additions in this project focus specifically on **text-entry usability**.

A phone is already equipped with a capable software keyboard, predictive text, multilingual input, punctuation, emoji, and other IME features. Providing a native text composer lets the user take advantage of that existing input stack while still sending the final keyboard actions over Bluetooth HID.

The Live/Buffer split addresses two different workflows:

* **Live** is optimized for direct, continuous keyboard replacement.
* **Buffer** is optimized for composing and sending complete messages.

The Send/Line Break separation then makes those workflows less error-prone, while automatic clearing prevents stale sent text from remaining in the composer.

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

## License

This project remains licensed under the **[AGPL-3.0](LICENSE)** in accordance with the upstream Bluke project.

Because this project is based on Bluke, please retain the applicable upstream license and attribution information when redistributing or modifying the project.

## Requirements

* **Android 9 (API level 28) or higher**.
* A device with hardware support for Android's **Bluetooth HID Device** profile. Availability can vary by device chipset and manufacturer ROM.

## Build

### Prerequisites

* Android Studio Koala or newer.
* Android SDK 36.
* Gradle Wrapper included with the repository.

### Build the debug APK

```bash
./gradlew assembleDebug
```

### Build the release APK

```bash
./gradlew assembleRelease
```

## Upstream

This repository is maintained as a customized project derived from Bluke. The upstream project remains the authoritative source for the original Bluke implementation:

**https://github.com/arnav-kr/Bluke**
