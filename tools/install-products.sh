#!/usr/bin/env bash
# Install the Infinity GSI products only. Does not apply patches or run a build.
set -euo pipefail
source_repo="$(cd "$(dirname "$0")/.." && pwd)"
android_tree="${1:?Usage: install-products.sh /path/to/android-source}"
[ -d "$android_tree/device/phh/treble" ] || { echo "Sync device/phh/treble first" >&2; exit 1; }
for product in infinity.mk infinity_tdgsi_arm64.mk infinity_tdgsi_arm64_gapps.mk; do
    cp "$source_repo/gsi/$product" "$android_tree/device/phh/treble/$product"
done
products="$android_tree/device/phh/treble/AndroidProducts.mk"
if [ ! -f "$products" ]; then
    cp "$source_repo/gsi/AndroidProducts.mk" "$products"
else
    for product in infinity_tdgsi_arm64.mk infinity_tdgsi_arm64_gapps.mk; do
        if ! grep -Fq -- "/$product" "$products"; then
            printf '\nPRODUCT_MAKEFILES += $(LOCAL_DIR)/%s\n' "$product" >> "$products"
        fi
    done
fi
printf 'Products installed. Review docs/BUILDING.md before building.\n'
