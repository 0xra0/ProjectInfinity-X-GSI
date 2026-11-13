# Building / porting the GSI source

This is a release/configuration companion, not the hundreds-of-repositories Android tree. Both published images have completed builds, but a clean-tree automated reproduction has not been tested. Some hand-ports are recorded in the triage and reference source copies; applying the original patch collection blindly does not reproduce the final state.

## 1. Sync Android 17

Use a suitable Linux Android build host, the required AOSP packages, Git LFS, substantial disk space, and the branch's supported JDK/toolchain. Build parallelism must fit available RAM; the build logs showed memory pressure with unrestricted `nproc` parallelism.

```sh
mkdir infinity-android17
cd infinity-android17
repo init --no-repo-verify --git-lfs \
  -u https://github.com/ProjectInfinity-X/manifest \
  -b 17 -g default,-mips,-darwin,-notdefault
mkdir -p .repo/local_manifests
cp /path/to/ProjectInfinity-X-GSI/manifests/infinity_gsi.xml .repo/local_manifests/
repo sync -c --no-clone-bundle --no-tags --optimized-fetch -j4
```

This intentionally does not use another checkout as a Git reference. If you choose `--reference` yourself, keep that source object store available until you have properly made the new checkout independent.

`metadata/*-public-source-manifest.xml` records the revisions observed inside the released images. It can include locally created commits that upstream does not serve, so it is provenance metadata, not a directly syncable replacement for the upstream manifest. Private/unused Pixel GMS entries are omitted if present.

## 2. Review and port the patch inputs

Copy the companion `gsi` directory into the Android tree if desired. The original patch driver can preview the input sets:

```sh
bash gsi/apply_patches.sh --dry-run \
  gsi/patches-17/trebledroid \
  gsi/patches-17/rom \
  gsi/patches-17/personal-restless \
  gsi/patches-17/personal \
  gsi/patches-17/release-builds \
  infinity-gsi-fixups
```

Dry-run output is a review aid, not proof that the sequence will apply without conflicts. On a **fresh** tree, reviewed sets can be applied with the same command without `--dry-run`, followed by the hand-ports and exclusions in [patch-triage.md](patch-triage.md). This is a maintainer porting step, not a novice installation step.

The original collection targets another ROM and some patch assumptions do not hold for Infinity. Preserve the real virtualization tree, review VNDK selection and APN changes, and retain the final security/policy decisions described in the triage. Do not repeatedly run the full fuzzy patch driver over an already patched tree; later patches can change earlier patch context.

`source-overlays/` contains selected current files touched by patch inputs and explicit integration fixes, with hashes and absent-path records. Use it for comparison/hand-porting. Do not copy it blindly over a different upstream revision; it is not a tested automatic installer or a complete corresponding-source reconstruction.

## 3. Install product definitions

From this companion repository:

```sh
bash tools/install-products.sh /path/to/infinity-android17
```

This helper copies the three Infinity makefiles and adds missing product registrations without applying patches or running a build. TrebleApp's APK must also be prepared according to its own project instructions; the Android build consumes it as a prebuilt. Keep the required Gradle/JDK/SDK prerequisites separate from the platform build.

## 4. Build one variant

Inside the Android checkout:

```sh
source build/envsetup.sh
lunch infinity_tdgsi_arm64 cp2a userdebug
[ "$TARGET_PRODUCT" = infinity_tdgsi_arm64 ] || exit 1
m systemimage -j8
cp out/target/product/tdgsi_arm64_ab/system.img system-vanilla.img
```

Then, if needed, select GApps in the same sourced shell:

```sh
lunch infinity_tdgsi_arm64_gapps cp2a userdebug
[ "$TARGET_PRODUCT" = infinity_tdgsi_arm64_gapps ] || exit 1
m systemimage -j8
cp out/target/product/tdgsi_arm64_ab/system.img system-gapps.img
```

Choose the job count for your host. Use the three-argument lunch form. Both variants use the same output directory, so save the vanilla image before building GApps. Do not set `WITH_GAPPS=true` for these products: that selects Infinity's separate Pixel GMS stack and conflicts with the direct MindTheGapps integration.

`INFINITY_MAINTAINER` can be set before lunch for a newly built image. It does not retroactively change the already published binaries, whose maintainer property remains `UNKNOWN`.

## 5. Package and check

```sh
xz -6 -T4 -k system-vanilla.img
xz -6 -T4 -k system-gapps.img
xz -t system-vanilla.img.xz system-gapps.img.xz
sha256sum system-vanilla.img.xz system-gapps.img.xz > SHA256SUMS
```

Keep build success distinct from hardware validation. Test boot, encryption, display, cellular/IMS, audio, cameras, sensors, fingerprint, suspend, and charging on each supported device before describing it as tested.

## Recorded host issues

- An incompatible Subversion `diff` helper at `/usr/bin/diff` rejected Soong's `-u -F` flags; use GNU diff. Do not bypass API validation to solve a host-tool problem.
- Removing/moving the tree used for Git alternates left missing objects in this checkout. The release build resumed after pinned-object recovery; some recovered repositories now have shallow history. That recovery is a host repair, not a source patch to replay.
- A console `VANILLA` filename label does not determine actual package content. Inspect the filesystem and product inheritance.
