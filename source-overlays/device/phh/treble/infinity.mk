# GSI always has insecure ADB; avoids conflict with TrebleDroid base.mk (ro.adb.secure=0)
WITH_ADB_INSECURE := true

# Inherit ProjectInfinity-X vendor configuration.
# Placed at device/phh/treble/infinity.mk by setup_gsi.sh.
$(call inherit-product, vendor/infinity/config/common_full_phone.mk)
-include vendor/infinity/build/core/config.mk
-include vendor/infinity/build/core/apicheck.mk
