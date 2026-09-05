# Devil RemoteBT Keyboard — Planning

This document is the durable implementation roadmap. Items are marked complete only when the corresponding code and available verification evidence exist.

## Phase 0 — Foundation ✅

- [x] Create clean repository baseline.
- [x] Add product scope and technical principles to README.
- [x] Add planning and achievement tracking.
- [x] Add GitHub Actions build/artifact workflow.
- [x] Add tagged-release workflow for APK deployment.
- [x] Review Astra 6.1 architecture/design output.

## Phase 1 — Technical design ✅

- [x] Validate Android API level and Bluetooth HID Device API strategy.
- [x] Define keyboard-only HID registration and SDP descriptor.
- [x] Define connection state machine.
- [x] Define connected-device filtering and explicit selected-host policy.
- [x] Define connected-device foreground-service ownership.
- [x] Define serialized HID report delivery.
- [x] Define US-ASCII text-to-HID mapping.
- [x] Define Live Mode common-prefix synchronization.
- [x] Define Buffered Mode snapshot transmission.
- [x] Define special-key and one-shot modifier semantics.
- [x] Define emoji and attachment limitations honestly.
- [x] Define recovery and error behavior.
- [x] Define unit/manual verification matrix.

## Phase 2 — Android application foundation ✅

- [x] Create Gradle/Android project.
- [x] Configure SDK, min/target SDK, Java version, and minimal dependencies.
- [x] Implement Android version-aware Bluetooth permission handling.
- [x] Implement service-owned application state model.
- [x] Add unit-test infrastructure.

## Phase 3 — Bluetooth HID core ✅

- [x] Implement HID profile proxy lifecycle.
- [x] Implement HID application registration.
- [x] Implement explicit selected-host connection.
- [x] Implement callback-driven connection-state observation.
- [x] Implement explicit disconnect.
- [x] Implement unexpected-disconnect handling.
- [x] Implement Bluetooth adapter off/on handling.
- [x] Prevent the initialization race from silently discarding Connect.
- [x] Keep the HID session outside Activity lifecycle.
- [x] Implement one serialized HID report sender.
- [x] Prevent concurrent `sendReport()` calls.

## Phase 4 — Main keyboard experience ✅

- [x] Device search/selection UI.
- [x] Explicit Connect/Disconnect controls.
- [x] Accurate Connecting/Connected/Disconnected states.
- [x] Native Android multi-line text field.
- [x] Remote Enter button.
- [x] Special-key menu.
- [x] One-shot modifier handling with release-safe key taps.

## Phase 5 — Input modes ✅

### Live Mode

- [x] Mirror current normalized composer text to the remote host.
- [x] Handle append, delete, replacement, insertion, and clearing under the documented host-caret contract.
- [x] Recalculate edits from completed HID strokes.
- [x] Keep HID input serialized during rapid revisions.

### Buffered Mode

- [x] Keep composition local.
- [x] Add Send Buffer operation.
- [x] Send buffer through the serialized HID queue.
- [x] Preserve ordering and fail cleanly when transmission is rejected.

## Phase 6 — Extended composer behavior ✅

- [x] Emoji UI with honest ASCII alternatives.
- [x] Reject unsupported Unicode rather than silently corrupting input.
- [x] Attachment UI.
- [x] Import UTF-8 text files into the composer.
- [x] Document that arbitrary file transfer is not provided by keyboard HID.

## Phase 7 — Reliability hardening — hardware gate pending

- [x] Activity does not own/unregister the HID session.
- [x] Activity stop/start only detaches observers and clears UI-local modifier state.
- [x] Screen-off behavior designed around a connected-device foreground service.
- [x] Stale Connected state is rejected by callback-driven state handling.
- [x] Explicit Disconnect cancels user intent and does not auto-reconnect.
- [x] Stuck modifiers/keys are cleared on disconnect/error.
- [ ] Physical screen-off/lock qualification.
- [ ] Physical app-switch qualification.
- [ ] Physical Windows qualification.
- [ ] Physical Linux qualification.
- [ ] Vendor-specific power-management qualification.

## Phase 8 — Verification and release — pending CI/hardware evidence

- [x] Unit tests added.
- [ ] Unit tests pass in GitHub Actions.
- [ ] Static/lint checks pass in GitHub Actions.
- [ ] Debug APK verified in CI.
- [ ] Release APK verified in CI.
- [ ] Windows manual qualification recorded.
- [ ] Linux manual qualification recorded.
- [ ] Release notes updated after qualification.
- [ ] Version tag created.
- [ ] GitHub Release generated with qualified APK asset.

## Change control

Every milestone must record:

1. What changed.
2. Which files/areas changed.
3. What tests were run.
4. Whether CI passed.
5. Known limitations or unresolved issues.

Do not mark hardware behavior complete merely because source code exists. Bluetooth HID reliability requires physical-device evidence.
