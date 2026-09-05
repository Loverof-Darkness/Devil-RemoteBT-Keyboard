# Devil RemoteBT Keyboard

A simple Android Bluetooth HID remote keyboard for controlling Windows and Linux laptops/desktops from an Android phone.

> **Project status:** Pre-implementation foundation. The application code will be built from a new architecture after the Astra 6.1 technical design is reviewed.

## Product goal

Turn an Android phone into a reliable Bluetooth HID keyboard for a selected Windows or Linux computer.

The product is intentionally focused:

- Bluetooth HID is the transport.
- The Android phone is the keyboard/remote.
- The user explicitly selects the target computer.
- The HID session should remain alive while the app is backgrounded or the screen is off.
- Text can be sent live or as a buffer.
- A focused special-key menu provides common keyboard controls.
- No desktop companion application, server, cloud service, Wi-Fi transport, or Internet connection is required for normal keyboard operation.

## Planned user flow

1. Pair/connect the Android phone with the target computer using system Bluetooth.
2. Open Devil RemoteBT Keyboard.
3. Press **Search Devices**.
4. Show only relevant Bluetooth devices that are actively connected to the Android phone.
5. Select one computer from the device menu.
6. Press **Connect**; the app becomes a Bluetooth HID keyboard for that selected host.
7. Display an explicit **Connected** state only after the HID connection is actually established.
8. Use the writing composer, Live Mode, Buffered Mode, Enter, emoji, attachments, and special keys as supported by the final technically validated design.
9. Disconnect explicitly from the device menu when desired.

## Reliability requirements

Connection reliability is the primary engineering goal.

The final implementation must be state-driven and must correctly handle asynchronous HID registration/connection, background operation, screen-off, Activity recreation, Bluetooth adapter changes, unexpected host loss, explicit disconnect, serialized HID reports, and recovery without connecting to the wrong device.

The final implementation must never silently discard a connection request merely because the HID service is still initializing.

## Current repository phase

This repository currently contains project documentation and CI/release infrastructure only. The implementation is intentionally deferred until the Astra 6.1 design output is reviewed.

See:

- [`PLANNING.md`](PLANNING.md) — authoritative implementation roadmap.
- [`ACHIEVEMENT.md`](ACHIEVEMENT.md) — milestone and verification log.
- [`.github/workflows/build.yml`](.github/workflows/build.yml) — build/test/artifact workflow.
- [`.github/workflows/release.yml`](.github/workflows/release.yml) — tagged-release deployment workflow.

## Build and artifacts

Once the Android project is present, pushes and pull requests will run the GitHub Actions build workflow. Successful Android builds publish the generated APK as a workflow artifact.

Version tags (`v*`, for example `v1.0.0`) are prepared for automatic GitHub Release deployment with the release APK attached.

## Implementation policy

Do not add features merely because they are convenient to implement. Every feature must be validated against the capabilities and limitations of standard Bluetooth HID and the Windows/Linux receiving environment.

Particularly, arbitrary binary file transfer is **not** assumed to be possible through a keyboard HID connection alone. Emoji and Unicode input must also be implemented only through technically valid input mechanisms; platform-specific limitations must be documented rather than hidden.

## License

To be finalized with the first implementation release.
