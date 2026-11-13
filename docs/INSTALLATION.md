# Installation and recovery

These instructions describe the general ARM64 A/B GSI workflow. They do not replace a device-specific guide, and no physical-device boot result is claimed for the initial release. POCO C85 (`dew`) is the device of interest; confirm its actual firmware and partition requirements before proceeding.

## Prerequisites

- A Treble-capable ARM64 device compatible with system-as-root/A/B-style GSIs.
- An unlocked bootloader. Use the manufacturer's supported unlock process; unlocking normally erases data.
- A complete data backup, sufficient charge, a reliable USB connection, and current [Android SDK Platform-Tools](https://developer.android.com/tools/releases/platform-tools).
- Stock firmware and a known recovery method for your exact model and firmware revision.
- Enough space in the device's logical `system` partition for the decompressed image. Compressed download size is not the required partition size.

Check from the running stock system:

```sh
adb devices
adb shell getprop ro.treble.enabled
adb shell getprop ro.product.cpu.abi
adb shell getprop ro.product.cpu.abilist
adb shell getprop ro.boot.slot_suffix
```

Treble should report `true`, and the primary ABI should be `arm64-v8a`. A missing slot suffix alone is not a complete compatibility test: GSI partition naming conventions and physical slot support must be checked for the device. Do not use these images on an ARM32-only or x86 device.

## 1. Download, verify, and extract

Choose **one** image from the [release](https://github.com/0xra0/ProjectInfinity-X-GSI/releases/tag/v4.2-android17-20261113). Download `SHA256SUMS` too.

For example, for GApps:

```sh
sha256sum --ignore-missing -c SHA256SUMS
unxz -k ProjectInfinity-X-4.2-Android17-20261113-arm64-ab-gapps.img.xz
```

Require an `OK` result for the downloaded file. On Windows, use `Get-FileHash FILE -Algorithm SHA256` to compare against the checksum file, and extract XZ with a compatible archive utility. Keep the extracted `.img`; the `.xz` is a transport archive, not a directly flashable image.

`release-metadata.json` records original image sizes and SHA-256 values as well as compressed sizes and hashes. `SHA256SUMS` covers the downloadable image archives; `SHA256SUMS-images` covers the decompressed `.img` files.

## 2. Prepare AVB only as required by your device

AVB and chained vbmeta handling are device-specific. Keep the bootloader unlocked. Use only vbmeta images from the matching stock firmware for this exact device; none are supplied by this project.

Where the device's documented GSI procedure requires a patched/verification-disabled stock vbmeta, this is the usual command shape from bootloader mode:

```sh
adb reboot bootloader
fastboot devices
# Only when required by your device's procedure and using its matching stock image:
fastboot --disable-verity --disable-verification flash vbmeta vbmeta.img
```

Some devices have additional chained verification partitions; follow the model-specific procedure for partitions that actually exist. Do not invent partition names or use another device's vbmeta. Do not flash empty placeholder vbmeta files or erase verification partitions as a generic workaround.

## 3. Enter fastbootd for logical partitions

On devices using dynamic partitions, use **userspace fastboot (fastbootd)** for `system`, rather than assuming bootloader fastboot can write it:

```sh
fastboot reboot fastboot
fastboot getvar is-userspace
```

The expected result is `yes`. From a running Android system, `adb reboot fastboot` is another entry point supported by Android's fastbootd workflow. If the device does not provide fastbootd or reports a different partition arrangement, stop and use its documented GSI procedure.

## 4. Flash the image and perform a clean installation

Example for GApps, from fastbootd:

```sh
fastboot flash system ProjectInfinity-X-4.2-Android17-20261113-arm64-ab-gapps.img
# Destructive: wipes user data. Required here for the initial clean installation.
fastboot -w
fastboot reboot
```

For vanilla, substitute the filename ending in `vanilla.img`. Flash **one variant only**. Do not flash the system image to `boot`, `vendor`, `vbmeta`, or `super`.

A clean data format is strongly recommended when changing ROMs or switching between vanilla and GApps. A device may require its recovery's factory-reset/format-data procedure instead of, or in addition to, a successful `fastboot -w`; follow its documentation. Do not issue an automatic active-slot change or flash both slots without a device-specific reason.

First boot can take several minutes. The GApps image includes Setup Wizard and Google services; do not add another GApps package on top of it. ADB still requires host authorization when available.

## 5. Verify the running system

After boot and ADB authorization:

```sh
adb shell getprop ro.infinity.version
adb shell getprop ro.infinity.android.version
adb shell getprop ro.build.version.security_patch
adb shell getprop ro.product.device
```

Expected build values are `4.2`, `17`, `2026-11-01`, and `tdgsi_arm64_ab`. On GApps, you can also check package presence:

```sh
adb shell pm path com.android.vending
adb shell pm path com.google.android.gsf
adb shell pm path com.google.android.gms
```

Check cellular service, emergency-call behavior, Wi-Fi, Bluetooth, camera, fingerprint, audio, encryption, suspend, charging, and the display before relying on the device. A successful boot is only the beginning of hardware validation.

## Troubleshooting and rollback

| Symptom | Next check |
|---|---|
| `Partition not found` / cannot flash `system` | Verify fastbootd and the device's logical partition names |
| Image too large / insufficient space | Check decompressed image size and dynamic partition capacity; consider vanilla. Do not delete `product`, `vendor`, or unrelated partitions as a generic fix |
| Verification error / boot loop | Recheck stock firmware compatibility, the documented AVB procedure, clean data format, and correct ARM64 layout |
| Encryption/data mount failure | Use the exact device recovery's documented format-data procedure and stock recovery path |
| Black screen, fingerprint, camera, or IMS failure | Treat as a vendor compatibility issue; collect redacted logs and report the exact firmware/device |
| ADB unauthorized / `adb root` unavailable | These images retain authenticated ADB and report `user` properties; authorize on the device and do not assume root |

To roll back, restore the complete matching stock firmware using the manufacturer's supported method. Restoring only a stock `system` image may not reverse AVB or data-format changes. Relocking should only be considered after a complete, verified stock restoration according to the manufacturer; never relock while running this image.

## Upstream reference

- [Android Developers: GSI compatibility and installation](https://developer.android.com/topic/generic-system-image)
- [AOSP: fastbootd and logical partition commands](https://source.android.com/docs/core/architecture/bootloader/fastbootd)

The commands here are a manual guide. This repository does not automatically unlock, wipe, flash, or reboot a connected device.
