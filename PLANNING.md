# Devil RemoteBT Keyboard — Planning

This document is the working roadmap for rebuilding the application from scratch. It is intentionally written so implementation progress can be tracked without relying on chat history.

## Phase 0 — Foundation ✅

- [x] Create clean repository baseline.
- [x] Add product scope and technical principles to README.
- [x] Add planning and achievement tracking.
- [x] Add continuous build/test workflow that will publish Android artifacts once the project exists.
- [x] Add tagged-release workflow for automatic APK deployment through GitHub Releases.
- [ ] Review Astra 6.1 architecture/design output.

## Phase 1 — Technical design

- [ ] Validate Android API level and Bluetooth HID Device API strategy.
- [ ] Define HID registration and SDP descriptor.
- [ ] Define connection state machine.
- [ ] Define background/foreground-service lifecycle.
- [ ] Define selected-device filtering and connection policy.
- [ ] Define HID report serialization.
- [ ] Define text-to-HID mapping for standard US keyboard input.
- [ ] Define Live Mode synchronization algorithm.
- [ ] Define Buffered Mode transmission model.
- [ ] Define special-key and modifier semantics.
- [ ] Define emoji feasibility and platform-specific behavior.
- [ ] Define attachment feasibility and limitations.
- [ ] Define recovery/error strategy.
- [ ] Define automated and manual test matrix.

## Phase 2 — Android application foundation

- [ ] Create Gradle/Android project.
- [ ] Configure SDK, min/target SDK, Java/Kotlin versions, and dependencies.
- [ ] Implement permission handling by Android version.
- [ ] Implement application lifecycle/state model.
- [ ] Add test infrastructure.

## Phase 3 — Bluetooth HID core

- [ ] Implement HID profile proxy lifecycle.
- [ ] Implement HID application registration.
- [ ] Implement selected-host connection.
- [ ] Implement actual connection-state observation.
- [ ] Implement explicit disconnect.
- [ ] Implement unexpected-disconnect handling.
- [ ] Implement Bluetooth adapter off/on handling.
- [ ] Implement initialization race protection.
- [ ] Implement background-safe ownership of the HID session.
- [ ] Implement serialized report sender.
- [ ] Verify no concurrent `sendReport()` calls can reorder input.

## Phase 4 — Main keyboard experience

- [ ] Device search/selection UI.
- [ ] Connect/disconnect controls within the device selector.
- [ ] Accurate Connecting/Connected/Disconnected states.
- [ ] Native Android text field.
- [ ] Remote Enter button.
- [ ] Explicit Backspace/Tab/Escape and navigation controls.
- [ ] Special-key menu.
- [ ] Modifier handling with guaranteed release behavior.

## Phase 5 — Input modes

### Live Mode

- [ ] Mirror current Android text to the remote host.
- [ ] Correctly handle insertion, deletion, replacement, and clearing.
- [ ] Handle rapid edits without losing ordering.
- [ ] Keep local and remote text models coherent.

### Buffered Mode

- [ ] Keep composition local.
- [ ] Add Send Buffer operation.
- [ ] Send buffer atomically through the serialized HID queue.
- [ ] Preserve ordering and failure state.

## Phase 6 — Extended composer behavior

- [ ] Emoji UI.
- [ ] Implement only technically valid emoji transmission paths.
- [ ] Document OS/layout limitations.
- [ ] Attachment UI.
- [ ] Do not claim arbitrary file transfer through HID unless a technically valid mechanism exists under the no-companion-app requirement.

## Phase 7 — Reliability hardening

- [ ] Activity recreation does not terminate the HID session.
- [ ] Screen-off test.
- [ ] Screen-lock test.
- [ ] App-switching test.
- [ ] Bluetooth off/on test.
- [ ] Host disconnect/reconnect test.
- [ ] Explicit disconnect cannot be overridden by auto-reconnect.
- [ ] No stale Connected state.
- [ ] No silent connection-request loss.
- [ ] No stuck modifiers/keys.
- [ ] No report ordering corruption.

## Phase 8 — Verification and release

- [ ] Unit tests pass.
- [ ] Static/build checks pass.
- [ ] Debug APK builds.
- [ ] Release APK builds.
- [ ] CI artifact generated.
- [ ] Manual Windows verification.
- [ ] Manual Linux verification.
- [ ] Release notes/documentation updated.
- [ ] Version tag created.
- [ ] GitHub Release generated with APK asset.

## Change control

When Astra's design is supplied, update this roadmap with concrete architecture decisions before implementation. Completed items must be checked only after the corresponding code, tests, and verification evidence exist.
