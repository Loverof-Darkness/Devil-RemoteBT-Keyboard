# Devil RemoteBT Keyboard

A simple Android Bluetooth HID remote keyboard for controlling Windows and Linux laptops/desktops from an Android phone.

> **Project status:** Android implementation is now present. Build/CI verification is required; real-device Windows/Linux qualification remains a manual hardware gate.

## Product goal

Turn an Android phone into a reliable Bluetooth HID keyboard for a selected Windows or Linux computer.

The product is intentionally focused:

- Bluetooth HID is the transport.
- The Android phone is the keyboard/remote.
- The user explicitly selects the target computer.
- Bluetooth ownership lives in a connected-device foreground service rather than the Activity.
- Text can be sent live or as a buffer.
- A focused special-key menu provides common keyboard controls.
- No desktop companion application, server, cloud service, Wi-Fi transport, or Internet connection is required for normal keyboard operation.

## Current architecture

```text
MainActivity
    │ commands + immutable snapshots
    ▼
KeyboardService (foreground connectedDevice service)
    │
    ▼
HidController (single HandlerThread / authoritative state machine)
    │
    ├── HID profile proxy + registration
    ├── explicit selected-host policy
    ├── connected-device evidence filtering
    ├── callback-driven connection state
    └── serialized keyboard report scheduler
             │
             ▼
        KeyCodec + EditPlan
```

The service owns the Bluetooth/HID session so normal Activity backgrounding, rotation, and screen-off do not intentionally unregister the HID application. A partial wake lock is used only while a key/report sequence is actively transmitting.

## Planned user flow

1. Pair/connect the Android phone and target computer using system Bluetooth.
2. Open Devil RemoteBT Keyboard.
3. Press **Search Devices**.
4. Show only observable connected computers, not merely paired/nearby devices.
5. Select one computer.
6. Press **Connect**; the app registers as a Bluetooth HID keyboard and connects only to the selected host.
7. Display **Connected** only after the HID callback reports an actual connected state.
8. Use the writing composer, Live Mode, Buffered Mode, Enter, emoji alternatives, text-file import, and special keys.
9. Use **Disconnect** to intentionally end the HID session.

## Reliability behavior

The controller is state-driven and treats HID/profile callbacks as authoritative. Connection requests are retained while the HID profile is initializing rather than being silently dropped. Old callback generations are ignored after a reset, unsolicited HID hosts are rejected, keyboard reports are serialized through one send path, and explicit Disconnect cancels pending input/reconnect intent.

Android cannot guarantee an immortal Bluetooth session: Bluetooth failures, force-stop, foreground-service termination, or aggressive vendor power management can still terminate a connection. The implementation is designed to avoid application-induced disconnects and to fail into an explicit state rather than pretending the host is connected.

## Input behavior

### Live Mode

The composer mirrors its current normalized US-ASCII text to the host using a common-prefix edit plan: delete the obsolete remote suffix with Backspace, then type the required suffix. Only one key stroke is in flight at a time; a newer edit causes the remaining plan to be recalculated against the completed remote state.

Live synchronization assumes a US keyboard layout and that the host caret remains at the end of the composing segment. HID cannot read the host caret, selection, focused application, or actual character insertion, so this is not a general remote document synchronization protocol.

### Buffered Mode

Text remains local until **Send Buffer** is pressed. The buffer is sent through the same serialized HID report path and is never interleaved with another HID command.

### Enter and line breaks

The Android text area is multi-line so the native mobile keyboard works normally. Local line breaks are normalized to spaces; they do **not** become remote Enter. The visible **Enter** button sends HID Enter and then starts a new local/remote composing segment.

### Special keys

The Keys menu includes Escape, Tab, Backspace, Delete, Enter, navigation keys, Insert, Caps Lock, F1–F12, Print Screen, Scroll Lock, Pause/Break, and one-shot modifier selection for Control, Shift, Alt, and Meta/Windows.

## Emoji and attachments

A standard keyboard HID connection is not a generic Unicode or binary file-transfer channel.

- **Emoji:** the app provides ASCII alternatives such as `:)`, `:D`, `;)`, `<3`, and `:'(` instead of pretending that arbitrary Unicode emoji can always be emitted by a keyboard HID report.
- **Attachment:** the paperclip imports a UTF-8 **text** file into the composer. It does not transfer images or arbitrary binary files to Windows/Linux.

These limitations are deliberate and documented rather than hidden behind a feature that cannot be delivered by standard HID alone.

## Permissions and network

Normal operation requires Bluetooth connection permission on modern Android, plus the permissions required for the connected-device foreground service and an optional notification. The application does not request Internet, location, Bluetooth scanning, or Bluetooth advertising permissions.

## Build

The project uses Android Gradle Plugin 8.7.3, Gradle 8.9, Java 17, compile/target SDK 35, and min SDK 28.

Local build with a system Gradle 8.9 installation:

```bash
gradle clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

GitHub Actions performs the same build/test/lint checks and publishes the APKs as workflow artifacts. Version tags beginning with `v` build a release APK and attach it to a GitHub Release.

## Hardware qualification

CI can verify compilation, unit tests, packaging, and linting, but it cannot prove Bluetooth HID interoperability. Final qualification must be performed on physical Android devices against at least one Windows host and one Linux host.

The manual matrix should include first connection, repeated connect, explicit disconnect, Bluetooth off/on, host loss, Activity recreation, app switching, screen-off/lock, rapid typing, Live Mode edits, Buffered Mode, Enter, modifiers, and unsupported Unicode/file-import behavior.

## Important platform limitations

Android does not expose a public general-purpose list of every currently ACL-connected Bluetooth device. The **Search Devices** implementation therefore uses observable connected ACL evidence plus connected devices exposed by accessible Bluetooth profiles and intentionally does not fall back to generic nearby discovery or bonded-device lists.

A paired computer is not necessarily an active Bluetooth connection. On some phone/computer combinations a host may not appear in the strict picker until the host establishes a qualifying connection. That is a platform/workflow limitation, not a hidden device-discovery failure.

## Verification record

See [`PLANNING.md`](PLANNING.md) for the implementation roadmap and [`ACHIEVEMENT.md`](ACHIEVEMENT.md) for the durable verification log.

## License

To be finalized with the first public release.
