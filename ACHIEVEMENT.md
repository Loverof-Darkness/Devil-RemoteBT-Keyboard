# Devil RemoteBT Keyboard — Achievement Log

Use this file as the durable record of what has actually been completed and verified.

## 2026-09-05 — Android implementation milestone

### Completed in source

- Added the Android application project targeting API 35 with min SDK 28 and Java 17.
- Added a keyboard-only Bluetooth HID descriptor with standard 8-byte boot-compatible input reports and LED output handling.
- Added a `KeyboardService` foreground connected-device service so the HID session is not Activity-owned.
- Added a single-threaded `HidController` state machine for profile acquisition, registration, selected-host connection, disconnect, Bluetooth adapter changes, and serialized report transmission.
- Added strict connected-device filtering without nearby discovery, bonded-device fallback, Internet, or desktop companion software.
- Added explicit device selection, Connect, Disconnect, callback-driven Connected state, and connection timeout handling.
- Added native Android composer input with Live Mode and Buffered Mode.
- Added common-prefix Live Mode synchronization for append, deletion, insertion, replacement, and clearing under the documented host-caret contract.
- Added serialized special-key and one-shot modifier handling with neutral release reports.
- Added Enter behavior that sends HID Enter and starts a new composing segment.
- Added emoji ASCII alternatives and UTF-8 text-file import while documenting that Unicode emoji and arbitrary binary attachment transfer are not provided by standard keyboard HID.
- Added unit tests for ASCII mapping, Caps Lock compensation, newline normalization, unsupported Unicode, release reports, and randomized edit-plan behavior.
- Updated CI/release workflows to use a pinned Gradle 8.9 toolchain with Java 17.

### Verification status

The source implementation is present and the repository workflows now contain the build/test/lint commands. At this milestone, CI and physical Bluetooth hardware qualification have not yet been observed after the implementation commit.

### Known limitations

- Android cannot expose every previously-established ACL connection through a single public general-purpose API, so Search Devices only lists observable connected computer evidence.
- Pairing is not equivalent to an active connection; a paired-only computer may intentionally be absent from the strict picker.
- Keyboard HID cannot read the host caret, selection, focus, or actual text insertion acknowledgement, so Live Mode is a composing-segment projection rather than arbitrary remote document synchronization.
- Android cannot guarantee an immortal Bluetooth session against Bluetooth stack failure, force-stop, foreground-service termination, or vendor-specific power management.
- Universal Unicode emoji and arbitrary binary file transfer are outside standard keyboard HID semantics.

## Recording rule

Every future milestone should record:

1. What changed.
2. Which files/areas changed.
3. What tests were run.
4. Whether CI passed.
5. Any known limitations or unresolved issues.

Do not mark a feature complete merely because code exists. Completion requires verification evidence.
