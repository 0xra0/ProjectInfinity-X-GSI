# Infinity X TrebleDroid GSI — ARM64, A/B partition, with MindTheGapps (Android 17 / cinnamonbun)
# Build: lunch infinity_tdgsi_arm64_gapps cp2a userdebug && make systemimage -j$(nproc)
# Requires: vendor/gapps synced via .repo/local_manifests/infinity_gsi.xml

TARGET_GAPPS_ARCH := arm64

# This variant ships MindTheGapps (vendor/gapps/arm64/arm64-vendor.mk),
# not Infinity's partner GMS, so keep vendor/pixel/gms out of Soong's scan:
# both trees define GmsSettingsOverlay and GmsSettingsProviderOverlay, and the
# duplicate names collide in kati even though a soong_namespace hides the clash
# from Soong. WITH_GAPPS off for the same reason -- it is what pulls gms in.
WITH_GAPPS := false
PRODUCT_SOURCE_ROOT_DIRS += -vendor/pixel/gms
$(call inherit-product, build/make/target/product/aosp_arm64.mk)
$(call inherit-product, device/phh/treble/base.mk)
# MindTheGapps entry point. Not device/phh/treble/gapps.mk: that expects a
# Pixel Experience vendor/gapps/config.mk and pulls partner-GMS packages.
$(call inherit-product, vendor/gapps/arm64/arm64-vendor.mk)
$(call inherit-product, device/phh/treble/infinity.mk)

PRODUCT_NAME    := infinity_tdgsi_arm64_gapps
PRODUCT_DEVICE  := tdgsi_arm64_ab
PRODUCT_BRAND   := google
PRODUCT_SYSTEM_BRAND := google
PRODUCT_MODEL   := Infinity X GSI (arm64, GApps)
PRODUCT_MANUFACTURER := ProjectInfinity-X

PRODUCT_CHARACTERISTICS := device

PRODUCT_SYSTEM_NAME   := infinity_tdgsi_arm64_gapps
PRODUCT_SYSTEM_DEVICE := tdgsi_arm64_ab
PRODUCT_SYSTEM_BRAND  := google

# version.mk derived ZIP_NAME from WITH_GAPPS, which is off above; relabel it.
ZIP_NAME := $(subst -VANILLA-,-GAPPS-,$(ZIP_NAME))
