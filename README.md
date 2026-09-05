# Devil RemoteBT Keyboard

A small Android Bluetooth HID keyboard for Windows and Linux desktops.

## Build

The GitHub Actions workflow provisions JDK 17 and Gradle 8.9. Locally, use the same toolchain and Android SDK Platform 35:

```bash
gradle clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

## How it works

1. Pair/connect the Android phone with the target computer using the normal Bluetooth settings.
2. Open the app and press **Search Devices**.
3. The app lists only observable connected computer devices; pairing alone is not enough for the strict picker.
4. Select the computer and press **Connect**.
5. Wait for the explicit **Connected** state.
6. Type in the composer, use Live Mode or Buffered Mode, or open the keyboard key menu.

The HID session is owned by a foreground connected-device service rather than the Activity, so switching apps or rotating the UI does not intentionally tear down the session.

## Limitations

Ordinary keyboard HID cannot transfer arbitrary binary files or universally encode Unicode emoji. The app therefore does not pretend those features work. Live Mode is a composing-segment projection: HID cannot read the remote caret, selection, focus, or delivery acknowledgement. Use a US keyboard layout on Windows/Linux for the supported ASCII mapping.
