# ProjectInfinity-X GSI — Android 17

**Unofficial ProjectInfinity-X 4.2 for ARM64 A/B Treble devices, maintained by [0xra0](https://github.com/0xra0).**

This is a personal GSI adaptation of ProjectInfinity-X with TrebleDroid integration, MediaTek-oriented display compatibility work, and a choice of vanilla or MindTheGapps images. It is an independent community build, not an official ProjectInfinity-X release.

[Download both variants](https://github.com/0xra0/ProjectInfinity-X-GSI/releases/tag/v4.2-android17-20261113) · [Installation](docs/INSTALLATION.md) · [Build guide](docs/BUILDING.md) · [Changes](docs/MODIFICATIONS.md) · [Report an issue](https://github.com/0xra0/ProjectInfinity-X-GSI/issues/new/choose)

## Release overview

| Item | Value |
|---|---|
| ROM | ProjectInfinity-X 4.2 |
| Android | 17 / API 37 |
| Platform build ID | `CP2A.260605.016` |
| Android security patch property | `2026-11-01` |
| Architecture / layout | ARM64, A/B / system-as-root; ARM32 compatibility components included |
| Product device | `tdgsi_arm64_ab` |
| Image format | Raw ext4 system filesystem, distributed as `.img.xz` |
| Variants | Vanilla; GApps with MindTheGapps `cinnamonbun` |
| Initial release | `v4.2-android17-20261113` (experimental prerelease) |
| Vanilla image build date | November 12, 2026 |
| GApps image build date | November 13, 2026 |
| Main device of interest | POCO C85 (`dew`); other devices require individual validation |
| Validation status | Both image builds succeeded; filesystem contents inspected; physical-device boot and hardware testing not established |

The version and security patch values above are read from the supplied images. They are build properties, not certification or a claim that every device's firmware has the same patch level.

## Choose an image

| Variant | What it contains | Pick it when |
|---|---|---|
| **Vanilla** | Infinity UI and Treble components, without the bundled Google package set | You want a system without preinstalled Google services |
| **GApps** | Infinity plus MindTheGapps, including Play Store, Google Services Framework, Setup Wizard, and the Google services APEX | You want Google account setup and Google services included |

Both images include their `product` and `system_ext` content inside `system.img`; this release does not provide a separate vendor image, boot image, recovery, or device-specific vbmeta images. The GApps variant does not need an additional GApps ZIP.

The console may label the GApps build `VANILLA` because Infinity's `WITH_GAPPS` switch is disabled to prevent its separate Pixel GMS package set from being included. Actual Google package presence was verified directly in the GApps filesystem. Select by the release asset's `gapps` / `vanilla` suffix.

## What is modified

- Infinity GSI product definitions for vanilla and MindTheGapps, with direct ARM64 GApps inheritance and exclusion of the competing Pixel GMS tree.
- TrebleDroid base, hardware overlays, TrebleApp integration, and available VNDK compatibility snapshots.
- SurfaceFlinger/HWC and MediaTek compatibility work: display power-on handling, GPU work fallback, configurable HWC backpressure, and a BLASTBufferQueue lock-order fix.
- Framework compatibility changes for legacy vendors, brightness curves, overlay matching, fingerprint cleanup behavior, and older device configurations.
- Android 17 build integration fixes, including APN/configuration conflicts, VNDK selection, policy adaptation, and duplicate Infinity property emission.
- The actual Android virtualization implementation is retained; incompatible replacement stubs from the original patch collection are excluded.

See [the modification notes](docs/MODIFICATIONS.md) for exact scope, upstream credits, and exclusions. The patch archive contains development inputs: **not every patch in it is applied to these images**. Hand-porting and later reversions are recorded in the [patch triage](docs/patch-triage.md).

## Install / use

Start with the [complete installation guide](docs/INSTALLATION.md). In brief:

1. Confirm ARM64 Treble compatibility, an unlocked bootloader, the expected partition layout, and a stock recovery path.
2. Download one `.img.xz` plus `SHA256SUMS` from the release.
3. Verify its checksum, then decompress it with `unxz -k IMAGE.img.xz`.
4. Follow your device's AVB and fastbootd procedure, flash the decompressed image to `system`, and wipe user data for the first installation.
5. Boot and complete setup. On GApps, sign in through the included Google setup flow if desired.

**Flashing and wiping can erase data and can leave an incompatible device unable to boot.** Back up first and use the device-specific instructions in the guide. These are experimental images; a successful build does not establish device compatibility.

Do not flash a compressed `.xz` directly, do not flash this system image to `boot` or `super`, and do not relock the bootloader with this unofficial image installed.

## Known limitations

- POCO C85 is the development target, not a verified working-device certification. Calls, mobile data, IMS/VoLTE, Wi-Fi, Bluetooth, cameras, fingerprint, encryption, display refresh rates, charging, and suspend must be tested on real hardware.
- The lunch target is `userdebug`, but both supplied images report `ro.build.type=user`, `ro.debuggable=0`, and `ro.adb.secure=1`. Do not assume `adb root` or automatic ADB authorization.
- `ro.infinity.maintainer` is `UNKNOWN` in these existing images; this repository is maintained by `0xra0`. The binaries were not rebuilt merely to change that property.
- Google certification, Play Integrity, banking applications, DRM levels, and device-specific vendor features are not guaranteed.
- GSI compatibility patches relax handling of some AVB/dm-verity failures. These images should not be treated as equivalent to a locked, stock verified-boot configuration.
- An OTA updater entry in the UI does not establish a supported OTA upgrade path for these releases. Use the release assets and installation guide.
- Android 16-era TrebleDroid repositories are used where the selected integration has no Android 17 counterpart. This is a custom Android 17 port, not a claim that every dependency is natively Android 17.

## Repository contents

| Directory | Purpose |
|---|---|
| `gsi/` | Product makefiles, patch driver, patch inputs, and excluded patch records |
| `manifests/` | Local manifest describing the GSI dependencies |
| `metadata/` | Properties extracted from each released image and public pinned source provenance |
| `docs/` | Installation, build, modification, and triage notes |
| `tools/install-products.sh` | Installs product definitions into an existing source checkout without applying patches or building |
| `source-overlays/` | Reference copies of current files touched by the published patch inputs; see its inventory and build guide |

Binary images are release assets, not Git source files. The image-specific source manifests record provenance; local commit IDs in them may not exist on upstream servers, so they are not standalone, directly syncable source manifests.

## Building and contributing

Read [BUILDING.md](docs/BUILDING.md). This repository is a configuration, patch, and release companion to the multi-repository Android tree; it is not the complete Android source checkout. Reconstructing a clean tree requires reviewing the recorded hand-ports and exclusions. A one-command, bit-for-bit rebuild is not claimed.

For reports, include the release variant, device model/codename, stock firmware version, kernel version, installation steps, and relevant redacted logs. Remove account details, phone numbers, IMEI/serial numbers, and other personal data before attaching logs.

## Credits and licenses

Thanks to [ProjectInfinity-X](https://github.com/ProjectInfinity-X), [AOSP](https://source.android.com/), [LineageOS](https://github.com/LineageOS), [TrebleDroid](https://github.com/TrebleDroid), [phhusson](https://github.com/phhusson), [Doze-off](https://github.com/Doze-off/patches), [MisterZtr](https://github.com/MisterZtr/LineageOS_gsi), [AndyCGYan](https://github.com/AndyCGYan), and [MindTheGapps](https://gitlab.com/MindTheGapps/vendor_gapps), along with the authors retained in individual patch headers.

This project does not claim authorship of upstream ROM features or third-party patches. Source files and patches retain their original copyright and license notices. Google applications are proprietary third-party components; their inclusion does not grant an open-source license to those applications. See [NOTICE.md](NOTICE.md) and the license/NOTICE material in the images.
