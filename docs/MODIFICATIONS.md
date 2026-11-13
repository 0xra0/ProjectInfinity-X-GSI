# Modification scope and provenance

This release combines upstream Infinity 4.2 / Android 17 with a locally adapted TrebleDroid GSI configuration. It is maintained as an independent build by `0xra0`.

## Configuration and packaging

- `infinity_tdgsi_arm64` and `infinity_tdgsi_arm64_gapps` inherit AOSP ARM64 and TrebleDroid base product configurations, then Infinity's phone configuration.
- Both use `PRODUCT_DEVICE := tdgsi_arm64_ab` and put product/system_ext contents into a single system filesystem image.
- Both exclude `vendor/pixel/gms` from Blueprint scanning. Vanilla also excludes `vendor/gapps`; GApps inherits `vendor/gapps/arm64/arm64-vendor.mk` directly.
- `WITH_GAPPS := false` deliberately prevents Infinity's separate Pixel GMS inheritance. It does not remove the directly inherited MindTheGapps package set.
- The MindTheGapps branch is `cinnamonbun`. Some TrebleDroid dependencies remain on their selected Android 16 branches while the platform is Android 17.
- Available VNDK snapshots are selected rather than requiring an absent v30 repository.

## Display and vendor compatibility

The patch inputs and actual source inspection include:

- MediaTek GED KPI integration and vendor EGL fallback work.
- SurfaceFlinger display power-on sequencing for vendor HWC implementations that leave a display powered on from the bootloader.
- BLASTBufferQueue initialization that moves `setMaxDequeuedBufferCount()` after the initialization mutex is released, avoiding a recursive lock path.
- HWC backpressure disabled by default with the port's explicit property control, and GPU work fallback for kernels without the expected tracepoint.
- Framework changes for RRO matching, brightness mapping, status-bar/corner geometry overrides, primary-camera selection, SSAID migration, and fingerprint cleanup behavior.
- TrebleApp and hardware overlays as GSI compatibility components.

These are source-level changes, not claims that every affected feature or device works. In particular, the separate historical LocalDisplayAdapter `STATE_ON` idea is **not** claimed as applied: this checkout still initializes its display states to `STATE_UNKNOWN`. See the triage for distinctions between applied, superseded, excluded, and inapplicable patches.

## Policy, boot, and build integration

- ADB authentication follows `ro.adb.secure`, with a secure default. The supplied images have `ro.adb.secure=1`.
- GSI policy adaptation includes permissive-domain list/build handling; this does not establish a measured runtime SELinux status on a device.
- First-stage mount compatibility changes can tolerate selected AVB/dm-verity setup failures. This is a real verified-boot tradeoff, not a stock security-equivalent configuration.
- The real `packages/modules/Virtualization` implementation is retained. Replacement `gsi_stubs` and related conflicting removal patches were excluded or reverted. AppSearch's separate isolated-storage adaptation was reapplied as recorded in the triage.
- APN conflicts, Android 17 API surface changes, missing VNDK inputs, and duplicate `ro.infinity.device` emission were corrected for this source tree.
- Host-side GNU diff selection and recovery of missing reference Git objects resolved build-host issues. They are not extra phone features and did not require disabling API checks.

## Patch archive status

The starting A17 input collection came from [Doze-off/patches, branch patches-17](https://github.com/Doze-off/patches/tree/patches-17), recorded locally at `5bad1bc`. The first apply pass reported 371 applied, 7 already present, 54 failed, and one absent repository; subsequent hand-ports and reversions changed that state.

Consequently, directory membership is not an applied-patch manifest. `gsi/patches-17-excluded` also retains the AppSearch input for historical context even though its needed adaptation was later reapplied. Use [patch-triage.md](patch-triage.md), image-specific source metadata, and the reference file snapshot together.

The supplied `source-overlays` are current checkout file copies for audit and porting, not a replacement for a full Android checkout and not an automatic patch installer. They may include unchanged files touched by a failed or superseded input patch. Their inventory distinguishes present files from absent paths. They describe the checkout at publication, not a promise that both differently dated images can be rebuilt byte-for-byte from this companion repository alone.

## Attribution

The underlying ROM and most feature changes originate with ProjectInfinity-X, AOSP, LineageOS, TrebleDroid, Doze-off, MisterZtr, phhusson, and the authors named in patch headers. Local integration work should not be read as original authorship of those changes.
