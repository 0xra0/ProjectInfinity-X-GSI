# Excluded from patches-17

The author's crDroid A17 tree drops packages/modules/Virtualization from its
sync, and these patches add device/phh/treble/gsi_stubs to stand in for it.
Infinity's A17 manifest syncs the real Virtualization tree, so the stubs
redefine ~25 of its Soong modules and analysis fails with "already defined".
The two visibility patches exist only so the stubs can link.

Same root cause, also excluded and reverted: build 0003 (virt deps in the GSI
product), device_generic_trusty 0002, trusty_device_desktop 0001,
external_rust_android-crates-io 0001 (apex_available for com.android.virt /
microfuchsia / compos), frameworks_base 0020, Settings 0002.
frameworks_base 0020's revert conflicted with the separate npu-client removal
(dd51051f76); only the VirtualizationFrameworkInitializer line was restored.

## Partially excluded: "gsi: product trim adjustments"

personal/platform_build_target/0001 (applied) and its duplicate
personal-restless/platform_build/0002 also dropped `com.android.virt` from
base_system.mk and `com.android.virt:framework-virtualization` from
PRODUCT_APEX_BOOT_JARS. With the real Virtualization tree that fails Soong:
"[framework-virtualization] in contents must also be declared in
PRODUCT_APEX_BOOT_JARS". Those two hunks were deleted from both patch files
in place (the VNDK 28/29 symlinks and the rest are kept), and build/make
1106b10f42 restores the two lines.

## Re-applied: AppSearch 0001

AppSearch 0001 ("neuter the isolated storage service") was first excluded with
the group above, but it is also the other half of frameworks_base 0021, which
deletes android.aiseal.AiSealManager. Without it, service-appsearch fails javac
with "package android.aiseal does not exist". It is re-applied as
packages/modules/AppSearch d440a4bd (revert of the revert 328740bd). Its
framework-virtualization lib removal is harmless with the real tree present.
The patch file stays in this directory for the record.
