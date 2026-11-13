# Copyright (C) 2024 Project Infinity X
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

ANDROID_VERSION := 17
INFINITYVERSION := 4.2

INFINITY_BUILD_TYPE ?= UNOFFICIAL
INFINITY_MAINTAINER ?= UNKNOWN
INFINITY_DATE_YEAR := $(shell date -u +%Y)
INFINITY_DATE_MONTH := $(shell date -u +%m)
INFINITY_DATE_DAY := $(shell date -u +%d)
INFINITY_BUILD_DATE := $(INFINITY_DATE_DAY).$(INFINITY_DATE_MONTH).$(INFINITY_DATE_YEAR)
TARGET_PRODUCT_SHORT := $(subst infinity_,,$(INFINITY_BUILD))

INFINITY_VERSION := Project_Infinity-X-$(INFINITYVERSION)-$(INFINITY_BUILD)-$(INFINITY_BUILD_DATE)
INFINITY_DISPLAY_VERSION := Project_Infinity-X-$(INFINITYVERSION)-$(INFINITY_BUILD_TYPE)
INFINITY_DISPLAY_BUILDTYPE := $(INFINITY_BUILD_TYPE)
INFINITY_FINGERPRINT := Project_Infinity/$(INFINITY_MOD_VERSION)/$(TARGET_PRODUCT_SHORT)/$(INFINITY_BUILD_DATE)

ZIP_NAME := Project_Infinity-X-$(INFINITYVERSION)-$(INFINITY_BUILD)-$(INFINITY_BUILD_DATE)-VANILLA-$(INFINITY_BUILD_TYPE)
ifeq ($(WITH_GAPPS), true)
ZIP_NAME := Project_Infinity-X-$(INFINITYVERSION)-$(INFINITY_BUILD)-$(INFINITY_BUILD_DATE)-GAPPS-$(INFINITY_BUILD_TYPE)
endif

# ro.infinity.device is NOT set here. build/soong/scripts/gen_build_prop.py
# already emits it from PRODUCT_DEVICE (config["InfinityDevice"]), the same
# way LineageOS emits ro.lineage.device. Setting it again here produced two
# non-optional assignments of one prop, which post_process_props.py rejects
# outright whenever the two values disagree -- as they do for any product
# whose PRODUCT_DEVICE differs from the lunch name minus "infinity_"
# (e.g. infinity_tdgsi_arm64 -> PRODUCT_DEVICE tdgsi_arm64_ab).
# INFINITY System Version
PRODUCT_PRODUCT_PROPERTIES += \
  ro.infinity.android.version=$(ANDROID_VERSION) \
  ro.infinity.build.version=$(INFINITY_VERSION) \
  ro.infinity.build.status=$(INFINITY_BUILD_TYPE) \
  ro.modversion=$(INFINITY_VERSION) \
  ro.infinity.build.date=$(INFINITY_BUILD_DATE) \
  ro.infinity.buildtype=$(INFINITY_BUILD_TYPE) \
  ro.infinity.fingerprint=$(INFINITY_FINGERPRINT) \
  ro.infinity.version=$(INFINITYVERSION) \
  ro.infinity.maintainer=$(INFINITY_MAINTAINER)
