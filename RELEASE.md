# Release & Distribution Guide

## Canonical release location

Publish final user-facing APKs on the repository's GitHub Releases page:

https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/releases

A GitHub Release is the canonical distribution point for this project. Keep the APK, matching source archive, checksums, and release notes together on the same release page.

## Release workflow

1. Confirm the `main` branch is green in GitHub Actions.
2. Confirm the version in `app/build.gradle.kts` is the intended release version.
3. Create and push a semantic version tag, for example:

```bash
git tag v1.0.0
git push origin v1.0.0
```

4. The `Release Android APK` workflow builds the release APK with JDK 17 and Gradle 9.4.1.
5. The workflow creates a source archive from the exact release tag.
6. The workflow creates a SHA-256 checksum file for the APK and source archive.
7. The workflow publishes those files to the GitHub Release automatically.

## Release assets

Each release should contain at least:

- `Devil-RemoteBT-Keyboard-vX.Y.Z-release.apk` — the final Android release APK.
- `Devil-RemoteBT-Keyboard-vX.Y.Z-source.zip` — source corresponding to the exact release tag.
- `SHA256SUMS.txt` — SHA-256 hashes for the published release files.

GitHub also exposes the tagged source through the release/tag page.

## Current feature set documented for release

The current project includes the upstream Bluke Bluetooth HID keyboard/controller foundation plus these maintained changes:

- Native Android system-keyboard input using the phone's normal IME, including Gboard and other Android keyboards.
- Live and Buffer typing modes.
- Messenger-style in-field Send action.
- Automatic clearing after message submission.
- Dedicated Line Break / Shift+Enter behavior.
- Dedicated Backspace control with press-and-hold repeat.
- Serialized native text HID transmission with pacing delays.
- Hellfire red-accented visual theme while retaining upstream palette choices.
- Customized application identity and presentation for Devil RemoteBT Keyboard.
- Updated About, Credits, Licenses, and release documentation with explicit upstream attribution.

## Licensing and corresponding source

Devil RemoteBT Keyboard is a modified version of Bluke and remains distributed under AGPL-3.0. The repository keeps the upstream license text and explicitly identifies Bluke and its original author in the README and About screen.

The release workflow publishes the built APK together with source corresponding to the exact release tag. This makes the final binary and its corresponding project source available from the same public release location.

The original Bluke project remains credited here:

https://github.com/arnav-kr/Bluke

## Build verification

The latest verified CI build before this release-documentation update completed successfully on 2026-09-06 at commit `56294b164c27e83f6b94bbd4cdd8f4a95d4e3cf0`.

That run completed the Android build and APK artifact upload successfully.
