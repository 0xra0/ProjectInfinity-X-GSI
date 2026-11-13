# Infinity X TrebleDroid GSI — ARM64, A/B partition, Vanilla (no GApps)
# Target device: any Treble-compatible ARM64 A/B device (e.g. POCO C85 "dew")
# Build: lunch infinity_tdgsi_arm64 cp2a userdebug && make systemimage -j$(nproc)

TARGET_GAPPS_ARCH := arm64

# Vanilla means vanilla: Infinity's common.mk defaults WITH_GAPPS to true,
# which would pull in vendor/pixel/gms.
WITH_GAPPS := false

# Keep both GApps vendor trees out of Soong's Android.bp scan. vendor/gapps
# (MindTheGapps) and vendor/pixel/gms (partner GMS) each define overlay
# modules named GmsSettingsOverlay and GmsSettingsProviderOverlay. The
# soong_namespace at vendor/pixel/gms/common/Android.bp keeps Soong
# happy, but Soong exports bare LOCAL_MODULE names to Make, so kati fails
# with "MODULE.TARGET.ETC.GmsSettingsOverlay already defined" -- and it does
# so regardless of product inheritance, since Soong exports every enabled
# module, not just the ones in PRODUCT_PACKAGES.
PRODUCT_SOURCE_ROOT_DIRS += -vendor/gapps
PRODUCT_SOURCE_ROOT_DIRS += -vendor/pixel/gms
$(call inherit-product, build/make/target/product/aosp_arm64.mk)
$(call inherit-product, device/phh/treble/base.mk)
$(call inherit-product, device/phh/treble/infinity.mk)

PRODUCT_NAME    := infinity_tdgsi_arm64
PRODUCT_DEVICE  := tdgsi_arm64_ab
PRODUCT_BRAND   := google
PRODUCT_SYSTEM_BRAND := google
PRODUCT_MODEL   := Infinity X GSI (arm64)
PRODUCT_MANUFACTURER := ProjectInfinity-X

# Overwrite the emulator characteristic inherited from aosp_arm64.mk
PRODUCT_CHARACTERISTICS := device

PRODUCT_SYSTEM_NAME   := infinity_tdgsi_arm64
PRODUCT_SYSTEM_DEVICE := tdgsi_arm64_ab
PRODUCT_SYSTEM_BRAND  := google
