# Infinity 4.2 / Android 17 — experimental ARM64 A/B GSI

Unofficial personal build maintained by **0xra0**, based on ProjectInfinity-X with TrebleDroid and local Android 17 / MediaTek compatibility work. This is not an official ProjectInfinity-X release.

## Downloads

| Variant | Asset | Compressed size | Decompressed size | Internal image build date |
|---|---|---|---|---|
| Vanilla | `ProjectInfinity-X-4.2-Android17-20261113-arm64-ab-vanilla.img.xz` | 1,267,906,048 bytes | 2,974,470,144 bytes | 12.11.2026 |
| Gapps | `ProjectInfinity-X-4.2-Android17-20261113-arm64-ab-gapps.img.xz` | 1,574,416,308 bytes | 3,679,248,384 bytes | 13.11.2026 |

Choose one image. **Vanilla** omits the bundled Google package set. **GApps** includes MindTheGapps `cinnamonbun`; Play Store, Google Services Framework, and the Google services APEX were verified inside the actual image. Do not install another GApps ZIP on top.

Also download `SHA256SUMS`, `SHA256SUMS-images`, and `release-metadata.json`. The first checksum file covers the XZ downloads; the second covers the extracted images. The JSON contains both sets of hashes, exact sizes, and properties read directly from each image.

## Platform and changes

- ProjectInfinity-X **4.2**, Android **17 / API 37**, build ID `CP2A.260605.016`.
- Android security patch property: **2026-11-01**.
- ARM64 A/B / system-as-root layout; raw ext4 system images distributed using XZ compression.
- Infinity GSI product configuration and direct MindTheGapps integration, with conflicting Pixel GMS modules excluded.
- TrebleDroid base, hardware overlays, TrebleApp integration, and available VNDK snapshots.
- Display power-on, BLASTBufferQueue lock-order, HWC backpressure, GPU-work fallback, and related vendor compatibility adaptations.
- Android 17 policy/build/API/configuration fixes; the real Android virtualization implementation is retained.

See [the README](https://github.com/0xra0/ProjectInfinity-X-GSI/blob/main/README.md), [modification notes](https://github.com/0xra0/ProjectInfinity-X-GSI/blob/main/docs/MODIFICATIONS.md), and patch triage for scope and exclusions. Patch-directory membership is not a claim that every input patch is applied.

## Installation

Read [INSTALLATION.md](https://github.com/0xra0/ProjectInfinity-X-GSI/blob/main/docs/INSTALLATION.md) before flashing. Confirm device compatibility, back up data, unlock the bootloader through the supported device procedure, verify the download, and decompress the XZ file. Use the device's documented AVB procedure and fastbootd workflow to flash **system**, then perform a clean data format for initial installation.

Flashing/wiping can erase data and can make an incompatible device fail to boot. No boot, vendor, recovery, or device-specific vbmeta image is supplied. Do not flash compressed XZ files directly and do not relock with this unofficial image installed.

## Validation and limitations

- Both image builds completed successfully; their filesystem metadata and key package presence/absence were inspected.
- Both XZ archives passed decompression integrity checking and reproduce the original image SHA-256 exactly. The original images were not modified or rebuilt for publication.
- **Physical-device boot and hardware validation remain unconfirmed.** POCO C85 (`dew`) is the development target, not a tested compatibility certification.
- Although the selected lunch target is `userdebug`, these images report `ro.build.type=user`, `ro.debuggable=0`, and `ro.adb.secure=1`. ADB requires authorization; root access is not promised.
- The existing maintainer property remains `UNKNOWN`. This repository is maintained by `0xra0`.
- A console `VANILLA` package label can appear for the GApps target because Infinity's separate GMS switch is disabled; actual GApps content was inspected in the filesystem.
- GSI boot compatibility changes include AVB/dm-verity fallback behavior. A stock locked/verified security state, Play Integrity, Google certification, DRM, IMS, and vendor-specific features are not guaranteed.
- No supported OTA update path is established by this release. Keep matching stock firmware and a recovery method available.

Source/configuration companion files, patch inputs, per-image public source provenance, and reference current-file copies are included in the repository. This is not the entire Android tree or a promise of a one-command, byte-identical rebuild. Upstream and third-party authors/licenses remain credited in the repository and image NOTICE material.
