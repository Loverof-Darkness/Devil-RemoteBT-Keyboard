# Devil RemoteBT Keyboard

An Android Bluetooth HID keyboard focused on using the phone's own keyboard/IME (for example Gboard) to enter text on a paired desktop.

## Architecture

The Bluetooth HID transport and on-screen keyboard structure are based on [Bluke](https://github.com/arnav-kr/Bluke), with the application package, branding, and UI simplified for Devil RemoteBT Keyboard. Bluke is licensed under AGPL-3.0; this project retains that licensing and attribution.

The app uses Android's public `BluetoothHidDevice` API for the keyboard connection and a normal Android Compose text field for native IME input. That means Gboard remains the phone's normal keyboard: it can provide suggestions, voice input, multilingual composition, and other IME features, while the app converts supported committed text into Bluetooth HID keyboard reports for the paired computer.

## Flow

1. Pair the computer with the phone in Android Bluetooth settings.
2. Open Devil RemoteBT Keyboard and select the paired computer.
3. Connect as a Bluetooth keyboard.
4. Focus **Native keyboard input** to bring up the phone's configured IME (such as Gboard).
5. Press **Send to laptop** to transmit the text as HID keyboard input.
6. The built-in compact keyboard remains available for direct key presses and modifier/function keys.

## Host text support

Bluetooth HID carries keyboard usages rather than Android Unicode text. The native IME can therefore accept much richer input locally than a generic HID keyboard can encode on every desktop. The transport currently sends the standard printable US-ASCII range plus Enter. Unsupported Unicode characters are kept in the local text field instead of being silently transformed into incorrect characters.
