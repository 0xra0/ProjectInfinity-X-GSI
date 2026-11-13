# Android 17 patch apply — triage (2026-11-11)

Source: Doze-off/patches `patches-17` @ 5bad1bc, built by its author against **crDroid A17**
(not Infinity), so `git am -3` cannot help: the preimage blobs are not in this tree.
Full log: `apply-a17.log`. Command:

    bash gsi/apply_patches.sh gsi/patches-17/{trebledroid,rom,personal-restless,personal,release-builds} infinity-gsi-fixups

Result: **371 applied, 7 already present, 54 failed, 1 absent repo (vendor/magisk)**.
Failed patches left no partial changes (the fuzzy path is gated on a clean dry run).

Fixed during the run: `apply_patches.sh` committed GNU patch `.orig` backups with the
fuzzy patches (9 files). Removed by a cleanup commit in device/phh/treble, frameworks/av,
frameworks/base, frameworks/native, treble_app; the script now passes `--no-backup-if-mismatch`.

## Fuzzy (applied with offset/fuzz — review the diff)

| Repo | Patch | Why it matters |
|---|---|---|
| frameworks/native | trebledroid 0009 MTK GED KPI | MTK GPU frame rate — dew-critical |
| frameworks/av | personal-restless 0004 MediaProfiles fallback | was your own A16 commit |
| frameworks/base | trebledroid 0019 pick-up sensor auto-detect | was your own A16 commit |
| frameworks/base | trebledroid 0004, 0011, 0039 | sdcard visible flag, HOME wake, linear backlight |
| frameworks/base | personal-restless 0020, 0042 | virtualization refs, HIDL lights fallback |
| device/phh/treble | personal-restless 0045 | attention screen-on |
| treble_app | personal-restless 0015 | versionCode at build time |

## Failed — by category

### A. Build-critical (the GSI will not build or boot without them)
| Repo | Patch | Finding |
|---|---|---|
| device/phh/treble | infinity-gsi-fixups 0001 VNDK | **Still needed**: A17 has no `prebuilts/vndk/v30`, and base.mk now says `+= 28 29 30` → hard error. Re-port to the new line. |
| system/sepolicy | infinity-gsi-fixups 0001 permissive_domains | 1 of 2 hunks; Android.bp moved |
| system/sepolicy | release-builds 0001 userdebug sepolicy | `permissive su;` moved to su.te:145 (outside `userdebug_or_eng`) |
| build/make | personal-restless 0002 gsi product trim | base_system.mk / default_art_config.mk rewritten in A17 |
| build/make | release-builds 0001 adb auth | **Already achieved**: A17 gsi_system_ext.prop has no `ro.adb.secure=0` |
| packages/modules/adb | release-builds 0001 ro.adb.secure | **Policy conflict** with your A16 MTK fix (see below) |
| device/generic/common | rom 0001 sample APN | Lineage already guards it with `ifeq ($(LINEAGE_BUILD),)` — verify LINEAGE_BUILD is set |

### B. Relevant to dew / MediaTek / generic GSI boot
- frameworks/native: SF-HWC backpressure (tdroid 0013 = 0017 duplicate), restore mPropagateBackpressure (pr 0002 = 0017 duplicate), stall GpuWork without gpu_work_period (pr 0003 = 0018 duplicate)
- system/core: tdroid 0003 console service, tdroid 0012 ignore dm-verity/AVB failures, rom 0001 auto_reboot on older vendor kernels
- frameworks/base: tdroid 0006 persistent default override prop, 0014 RRO fnmatch (vendor/hardware_overlay matching), 0015 console notification, 0022 fingerprint cleanups, 0025 screen recorder AVC, 0036 brightness curve, 0047 dynamic-sensor parse crash; pr 0005 status bar/corner props, 0009 SSAID NPE, 0010 low-end animation override, 0024 linear brightness curve, 0026/0029 keyguard
- frameworks/av pr 0001 CameraParameters NULL segfault
- frameworks/opt/telephony tdroid 0005 binder revert
- packages/apps/Settings pr 0005 View Logs, pr 0012 fingerprint PIN crash
- packages/modules/Bluetooth pr 0001 legacy controller tolerance

### C. Other vendors only (not needed for POCO C85)
frameworks/base tdroid 0035/0042 FOD (Asus/Samsung/Xiaomi under-display — C85 has side FP), 0040 Samsung brightness, 0041 Samsung sysinput; device/phh/treble pr 0052 oplus FP; treble_app pr 0001 Unihertz.

### D. Old (4.x) kernels only (dew launched on A15 → GKI ≥ 5.10)
packages/modules/Connectivity tdroid 0002/0006 BPF-less kernels (29/39 hunks fail — A17 BPF rewrite), system/vold tdroid 0006 session keyring, frameworks/base pr 0011 fs-verity skip.

### E. Optional features
frameworks/base tdroid 0046/0051 microG spoofing; tethering VPN upstream (Connectivity pr 0005 + base pr 0030); treble_app pr 0009/0014/0016/0017/0018 (0014 needs 8 files our treble_app master lacks); device/phh/treble pr 0042 partner wallpapers (4 missing files).

## Resolution of groups A + B (2026-11-11, user chose A + B; adb follows ro.adb.secure)

Hand-ported commits carry the original author/message plus a "[Hand-ported to Infinity A17 …]" trailer.

| Result | Patches |
|---|---|
| **Ported** | device/phh/treble fixup 0001 VNDK (`foreach` over present snapshots; v30 absent); system/sepolicy fixup 0001 (merged both permissive lists incl. su); system/sepolicy release-builds 0001 (policy.go + drop `permissive su;`); packages/modules/adb release-builds 0001 (auth = `ro.adb.secure`, default true); device/generic/common rom 0001 (APN copy removed — LINEAGE_BUILD is empty for infinity_* targets); frameworks/native tdroid 0017 + pr 0017 (HWC backpressure off unless `persist.sys.phh.enable_sf_hwc_backpressure=1`; dead debug.sf.disable_backpressure read removed); frameworks/native pr 0018 GpuWork stall (+ fixed the patch's empty auto-latch `if`, which made `Disabled` unreachable); system/core tdroid 0012 dm-verity/AVB (no_vbmeta handling moved to first_stage_mount_android.cpp — the patch put it in the microdroid file; null-checked retry); frameworks/base tdroid 0006, 0014 (single A17 implementation in FrameworkParsingPackageUtils), 0022, 0036 (kept `public` for -Xexplicit-api=strict); frameworks/base pr 0005 (pure overrides; Infinity's extra paddings kept), pr 0009, pr 0024 |
| **Already in Infinity** | build/make release-builds 0001 (no `ro.adb.secure=0`); build/make pr 0002 trim (all but DeskClock — kept, Infinity has no other clock); system/core tdroid 0003 (`ro.console.enable` gate); frameworks/base tdroid 0015, 0025, pr 0010 (Infinity's own ActivityAnimations); frameworks/av pr 0001; telephony tdroid 0005; Bluetooth pr 0001 |
| **Superseded** | frameworks/native tdroid 0013, pr 0002, pr 0003 (older revisions of 0017/0017/0018) |
| **Not applicable** (crDroid-only code absent from Infinity) | system/core rom 0001 auto_reboot; frameworks/base pr 0026 + Settings pr 0012 (biometric second factor), pr 0029 (lock-time GC); Settings pr 0005 (View Logs) |
| **Moved to C** | frameworks/base tdroid 0047 (wraps 0035's dynamicUdfpsProps) |

Security trade-off accepted with group B: tdroid 0012 makes dm-verity setup failure non-fatal and retries first-stage mount without AVB. Matches every TrebleDroid GSI; dew runs with verification disabled anyway.

## Excluded after the first A17 build check

personal-restless device/phh/treble 0034 (`gsi_stubs`) plus its two visibility grants
(frameworks/native 0007, openssl 0001) were reverted and moved to `gsi/patches-17-excluded/`.
The author's crDroid tree drops packages/modules/Virtualization from the sync; Infinity's
manifest keeps it, so the stubs redefined ~25 of its Soong modules ("already defined").
Same root cause, also reverted and excluded: build 0003, device_generic_trusty 0002,
trusty_device_desktop 0001, external_rust_android-crates-io 0001, frameworks_base 0020,
Settings 0002 (9 patches; AppSearch 0001 was re-applied after the second build, see gsi/patches-17-excluded/README.md).
Also partly excluded: the virt hunks of "gsi: product trim adjustments" (personal 0001 /
personal-restless 0002) — removed from both patch files, restored by build/make 1106b10f42.

## Falsely skipped patches (found 2026-11-12)

apply_patches.sh counted a patch as "already there" whenever `git apply --reverse
--check` passed. For a pure deletion whose context repeats just after the deleted
block, that check passes on an unpatched tree, so two patches never ran:

- prebuilts/sdk: personal-restless 0001 "drop the aiseal package from the api 37
  latest api text" — caused metalava `Removed package android.aiseal [RemovedPackage]`.
- packages/modules/vndk: infinity-gsi-fixups 0001 "Reapply Removing vndk apex v30".

Both applied by hand. The script now also requires the forward check to fail, or
the subject to be in `git log`, before it skips.

Do not re-run the full script over an already-patched tree: a patch whose context
a later patch changed fails its reverse check, and `patch -f` then applies it a
second time with fuzz (a re-run on 2026-11-12 made 20 such commits; they were
reset). Use `--dry-run` first and apply single patches with `git am`.

### Collateral from that reset (found and fixed 2026-11-12)

The re-run's first fuzzy commit in device/phh/treble (`d0f1545`, made with
`git add -A`) swept in the untracked product files `infinity.mk`,
`infinity_tdgsi_arm64.mk` and `infinity_tdgsi_arm64_gapps.mk`, so the reset
deleted them. Lunch then failed (`infinity_tdgsi_arm64.mk does not exist`),
still exited 0, and `m` built the default `aosp_arm64`. Without the product's
`PRODUCT_SOURCE_ROOT_DIRS` exclusions, Soong saw the duplicate modules in
vendor/gapps vs vendor/pixel/gms and in hardware/lineage/compat vs
prebuilts/misc/protobuf_vendorcompat. The files were restored from `d0f1545`
(identical to `gsi/*.mk`). No other reset commit picked up files outside its patch.
If the product files go missing again: `cp gsi/infinity*.mk device/phh/treble/`.


## Missing reference Git objects (2026-11-13)

The GApps build stopped while exporting `product/etc/build-manifest.xml`:
`repo manifest -r` could not resolve QcRilAm's HEAD. The A17 checkout had
borrowed Git objects from `/mnt/sata/ProjectInfinity-X`, which was no longer
available. The same issue affected other repositories.

Restored the pinned snapshots for 106 projects without checking out or resetting
source files. Shallow fetches recovered the current snapshots; older history
beyond the shallow boundaries was not recreated. Gallery2's 2,231 blobs were
reconstructed from existing working files only after matching their index hashes.
Broken alternate paths and stale commit-graph caches were backed up beside their
original files as `*.missing-reference-backup`. Temporary HTTP settings used for
network retries were restored.

Validation: all 106 repaired projects passed Git connectivity checks, and the
original pinned manifest export plus `grep -Ev "proprietary_"` succeeded. Reports:
`out/git-reference-connectivity-check.json` and
`out/build-manifest-repair-verified.xml`. No ROM build was run during this repair.
