# infinity-gsi-fixups

Three corrections to what the TrebleDroid/personal patch sets do to *this*
tree (LineageOS 23.2 / Android 16). Each one is fatal at configure time, so
`gsi/setup_gsi.sh` applies this set right after the upstream sets and treats a
failure here as a hard stop.

Apply manually with:

    bash gsi/apply_patches.sh infinity-gsi-fixups

The driver skips anything already applied, so re-running is safe.

| Patch | Repo | Fixes |
|-------|------|-------|
| `0001-base-request-only-VNDK-snapshots-present-in-the-tree.patch` | `device/phh/treble` | A personal patch adds `PRODUCT_EXTRA_VNDK_VERSIONS += 30`, but LineageOS 23's manifest carries no `prebuilts/vndk/v30`. `build/make/core/board_config.mk` then hard-errors with "VNDK version 30 not found" during dumpvars, before the build even starts. Replaced with a `$(wildcard ...)`-guarded list so only snapshots actually present are requested. |
| `0001-sepolicy-merge-permissive_domains_on_user_builds-wit.patch` | `system/sepolicy` | A personal patch adds a second `permissive_domains_on_user_builds` to the `precompiled_sepolicy` module alongside upstream's existing `["backuptool"]`. Soong rejects the duplicate: `property "permissive_domains_on_user_builds" already defined`. Merged into one list. |
| `0001-Reapply-Removing-vndk-apex-v30.patch` | `packages/modules/vndk` | A personal patch reverts the removal of the `com.android.vndk.v30` APEX, but that APEX depends on `vndkcore.libraries.30.txt` and friends, which are defined by `prebuilts/vndk/v30/Android.bp` — absent here. Soong bootstrap fails on five undefined modules. Same root cause as the `device/phh/treble` fixup: v30 simply is not in this tree. |

Not covered here, because they live in files this repo owns outright rather
than in `repo`-managed projects:

- The GApps deconfliction (`WITH_GAPPS := false` plus
  `PRODUCT_SOURCE_ROOT_DIRS += -vendor/gapps -vendor/google/gms`) lives in
  `gsi/infinity_tdgsi_arm64.mk` and `gsi/infinity_tdgsi_arm64_gapps.mk`.
- Building `treble_app/TrebleApp.apk` is a step in `gsi/setup_gsi.sh`.
