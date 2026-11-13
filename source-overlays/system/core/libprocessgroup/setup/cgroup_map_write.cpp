/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "libprocessgroup"

#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <grp.h>
#include <pwd.h>
#include <sstream>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <sys/utsname.h>
#include <unistd.h>

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <processgroup/cgroup_descriptor.h>
#include <processgroup/processgroup.h>
#include <processgroup/setup.h>
#include <processgroup/util.h>

#include "../internal.h"

static constexpr const char* CGROUPS_DESC_FILE = "/etc/cgroups.json";
static constexpr const char* CGROUPS_DESC_VENDOR_FILE = "/vendor/etc/cgroups.json";

static constexpr const char* TEMPLATE_CGROUPS_DESC_API_FILE = "/etc/task_profiles/cgroups_%u.json";

static bool ChangeDirModeAndOwner(const std::string& path, mode_t mode, const std::string& uid,
                                  const std::string& gid, bool permissive_mode = false) {
    uid_t pw_uid = -1;
    gid_t gr_gid = -1;

    if (!uid.empty()) {
        passwd* uid_pwd = getpwnam(uid.c_str());
        if (!uid_pwd) {
            PLOG(ERROR) << "Unable to decode UID for '" << uid << "'";
            return false;
        }

        pw_uid = uid_pwd->pw_uid;
        gr_gid = -1;

        if (!gid.empty()) {
            group* gid_pwd = getgrnam(gid.c_str());
            if (!gid_pwd) {
                PLOG(ERROR) << "Unable to decode GID for '" << gid << "'";
                return false;
            }
            gr_gid = gid_pwd->gr_gid;
        }
    }

    auto dir = std::unique_ptr<DIR, decltype(&closedir)>(opendir(path.c_str()), closedir);

    if (dir == NULL) {
        PLOG(ERROR) << "opendir failed for " << path;
        return false;
    }

    struct dirent* dir_entry;
    while ((dir_entry = readdir(dir.get()))) {
        if (!strcmp("..", dir_entry->d_name)) {
            continue;
        }

        std::string file_path = path + "/" + dir_entry->d_name;

        if (pw_uid != -1 && lchown(file_path.c_str(), pw_uid, gr_gid) < 0) {
            PLOG(ERROR) << "lchown() failed for " << file_path;
            return false;
        }

        if (fchmodat(AT_FDCWD, file_path.c_str(), mode, AT_SYMLINK_NOFOLLOW) != 0 &&
            (errno != EROFS || !permissive_mode)) {
            PLOG(ERROR) << "fchmodat() failed for " << path;
            return false;
        }
    }

    return true;
}

static bool Mkdir(const std::string& path, mode_t mode, const std::string& uid,
                  const std::string& gid) {
    bool permissive_mode = false;

    if (mode == 0) {
        /* Allow chmod to fail */
        permissive_mode = true;
        mode = 0755;
    }

    if (mkdir(path.c_str(), mode) != 0) {
        // /acct is a special case when the directory already exists
        if (errno != EEXIST) {
            PLOG(ERROR) << "mkdir() failed for " << path;
            return false;
        } else {
            permissive_mode = true;
        }
    }

    if (uid.empty() && permissive_mode) {
        return true;
    }

    if (!ChangeDirModeAndOwner(path, mode, uid, gid, permissive_mode)) {
        PLOG(ERROR) << "change of ownership or mode failed for " << path;
        return false;
    }

    return true;
}

// To avoid issues in sdk_mac build
#if defined(__ANDROID__)

static bool IsOptionalController(const CgroupController* controller) {
    return controller->flags() & CGROUPRC_CONTROLLER_FLAG_OPTIONAL;
}

static bool MountV2CgroupController(const CgroupDescriptor& descriptor) {
    const CgroupController* controller = descriptor.controller();

    // /sys/fs/cgroup is created by cgroup2 with specific selinux permissions,
    // try to create again in case the mount point is changed
    if (!Mkdir(controller->path(), 0, "", "")) {
        LOG(ERROR) << "Failed to create directory for " << controller->name() << " cgroup";
        return false;
    }

    // The memory_recursiveprot mount option has been introduced by kernel commit
    // 8a931f801340 ("mm: memcontrol: recursive memory.low protection"; v5.7). Try first to
    // mount with that option enabled. If mounting fails because the kernel is too old,
    // retry without that mount option.
    if (mount("none", controller->path(), "cgroup2", MS_NODEV | MS_NOEXEC | MS_NOSUID,
              "memory_recursiveprot") < 0) {
        LOG(INFO) << "Mounting memcg with memory_recursiveprot failed. Retrying without.";
        if (mount("none", controller->path(), "cgroup2", MS_NODEV | MS_NOEXEC | MS_NOSUID,
                  nullptr) < 0) {
            PLOG(ERROR) << "Failed to mount cgroup v2";
            return IsOptionalController(controller);
        }
    }

    // selinux permissions change after mounting, so it's ok to change mode and owner now
    if (!ChangeDirModeAndOwner(controller->path(), descriptor.mode(), descriptor.uid(),
                               descriptor.gid())) {
        PLOG(ERROR) << "Change of ownership or mode failed for controller " << controller->name();
        return IsOptionalController(controller);
    }

    return true;
}

static bool ActivateV2CgroupController(const CgroupDescriptor& descriptor) {
    const CgroupController* controller = descriptor.controller();

    if (!Mkdir(controller->path(), descriptor.mode(), descriptor.uid(), descriptor.gid())) {
        LOG(ERROR) << "Failed to create directory for " << controller->name() << " cgroup";
        return false;
    }

    return ::ActivateControllers(controller->path(), {{controller->name(), descriptor}});
}

static bool IsCpusetV2ModeEnabled() {
    struct utsname uts;
    unsigned int major;

    if (uname(&uts) != 0 || sscanf(uts.release, "%u", &major) != 1) {
        LOG(ERROR) << "Could not parse the kernel version from uname";
        return false;
    }

    return (major >= 5);
}

static bool MountV1CgroupController(const CgroupDescriptor& descriptor) {
    const CgroupController* controller = descriptor.controller();

    // mkdir <path> [mode] [owner] [group]
    if (!Mkdir(controller->path(), descriptor.mode(), descriptor.uid(), descriptor.gid())) {
        LOG(ERROR) << "Failed to create directory for " << controller->name() << " cgroup";
        return false;
    }
    
    if (IsCpusetV2ModeEnabled()) {
        std::string options = controller->name();

        if (!strcmp(controller->name(), "cpuset")) {
            // Android depends on the noprefix option for cpuset so that cgroupfs files are not prefixed
            // with the controller name. For example /dev/cpuset/cpus instead of
            //                                       /dev/cpuset/cpuset.cpus.
            // cpuset_v2_mode is required to restore the original cpu mask after a cpu is offlined, and
            // then onlined in cgroup v1.
            options += ",noprefix,cpuset_v2_mode";
        }

        if (mount("none", controller->path(), "cgroup", MS_NODEV | MS_NOEXEC | MS_NOSUID,
                  options.c_str())) {
        // cpuset_v2_mode only exists on kernels >= 4.14; older vendor kernels
        // reject unknown v1 mount options outright. retry with the option set
        // used before 4.14, mirroring the memory_recursiveprot fallback in
        // MountV2CgroupController() above.
        if (!strcmp(controller->name(), "cpuset")) {
            std::string legacy_options = controller->name();
            legacy_options += ",noprefix";
            if (!mount("none", controller->path(), "cgroup", MS_NODEV | MS_NOEXEC | MS_NOSUID,
                       legacy_options.c_str())) {
                LOG(INFO) << "Mounted cpuset without cpuset_v2_mode (kernel < 4.14)";
                return true;
            }
            PLOG(INFO) << "cpuset fallback mount without cpuset_v2_mode failed";
        }
            if (IsOptionalController(controller)) {
                PLOG(INFO) << "Failed to mount optional controller " << controller->name();
                return true;
            }
            PLOG(ERROR) << "Failed to mount controller " << controller->name();
            return false;
        }
    } else {
        // Unfortunately historically cpuset controller was mounted using a mount command
        // different from all other controllers. This results in controller attributes not
        // to be prepended with controller name. For example this way instead of
        // /dev/cpuset/cpuset.cpus the attribute becomes /dev/cpuset/cpus which is what
        // the system currently expects.
        int res;
        if (!strcmp(controller->name(), "cpuset")) {
            // mount cpuset none /dev/cpuset nodev noexec nosuid
            res = mount("none", controller->path(), controller->name(),
                        MS_NODEV | MS_NOEXEC | MS_NOSUID, nullptr);
        } else {
            // mount cgroup none <path> nodev noexec nosuid <controller>
            res = mount("none", controller->path(), "cgroup", MS_NODEV | MS_NOEXEC | MS_NOSUID,
                        controller->name());
        }
        
        if (res != 0) {
            if (IsOptionalController(controller)) {
                PLOG(INFO) << "Failed to mount optional controller " << controller->name();
                return true;
            }
            PLOG(ERROR) << "Failed to mount controller " << controller->name();
            return false;
        }
    }
    return true;
}

static bool SetupCgroup(const CgroupDescriptor& descriptor) {
    const CgroupController* controller = descriptor.controller();

    if (controller->version() == 2) {
        if (controller->name() == CGROUPV2_HIERARCHY_NAME) {
            return MountV2CgroupController(descriptor);
        } else {
            return ActivateV2CgroupController(descriptor);
        }
    } else {
        return MountV1CgroupController(descriptor);
    }
}

#else

// Stubs for non-Android targets.
static bool SetupCgroup(const CgroupDescriptor&) {
    return false;
}

#endif

CgroupDescriptor::CgroupDescriptor(uint32_t version, const std::string& name,
                                   const std::string& path, mode_t mode, const std::string& uid,
                                   const std::string& gid, uint32_t flags,
                                   uint32_t max_activation_depth)
    : controller_(version, flags, name, path, max_activation_depth),
      mode_(mode),
      uid_(uid),
      gid_(gid) {}

void CgroupDescriptor::set_mounted(bool mounted) {
    uint32_t flags = controller_.flags();
    if (mounted) {
        flags |= CGROUPRC_CONTROLLER_FLAG_MOUNTED;
    } else {
        flags &= ~CGROUPRC_CONTROLLER_FLAG_MOUNTED;
    }
    controller_.set_flags(flags);
}

static bool CreateV2SubHierarchy(const std::string& path, const CgroupDescriptorMap& descriptors) {
    const auto cgv2_iter = descriptors.find(CGROUPV2_HIERARCHY_NAME);
    if (cgv2_iter == descriptors.end()) return false;
    const CgroupDescriptor cgv2_descriptor = cgv2_iter->second;

    if (!Mkdir(path, cgv2_descriptor.mode(), cgv2_descriptor.uid(), cgv2_descriptor.gid())) {
        PLOG(ERROR) << "Failed to create directory for " << path;
        return false;
    }

    // Activate all v2 controllers in path so they can be activated in
    // children as they are created.
    return ::ActivateControllers(path, descriptors);
}

bool CgroupSetup() {
    CgroupDescriptorMap descriptors;

    if (getpid() != 1) {
        LOG(ERROR) << "Cgroup setup can be done only by init process";
        return false;
    }

    // load cgroups.json file
    if (!ReadDescriptors(&descriptors)) {
        LOG(ERROR) << "Failed to load cgroup description file";
        return false;
    }

    // setup cgroups
    for (auto& [name, descriptor] : descriptors) {
        if (descriptor.controller()->flags() & CGROUPRC_CONTROLLER_FLAG_MOUNTED) {
            LOG(WARNING) << "Attempt to call CgroupSetup() more than once";
            return true;
        }

        // lmkd requires the memory controller in the v1 hierarchy when using its
        // vmpressure-based kill strategy. This happens in two cases:
        //
        //   1. The kernel lacks CONFIG_PSI: lmkd cannot open /proc/pressure/memory
        //      and its PSI path fails entirely.
        //
        //   2. The vendor has set ro.lmk.use_minfree_levels=true or
        //      ro.lmk.use_new_strategy=false: lmkd gates itself to the old kill
        //      strategy, which hard-requires v1 memcg regardless of PSI support.
        //
        // CgroupSetup() runs as PID 1 before PropertyLoadBootDefaults() has
        // loaded vendor properties.  GetBoolProperty() returns false for any
        // vendor-side property at this point, so we scan the same property files
        // that PropertyLoadBootDefaults() would load for vendor/ODM/product
        // partitions.  Later files override earlier ones in that load order, so
        // we scan all of them and let the last match win.
        //
        // In either case, if the memory controller is currently declared as v2 we
        // fall back to mounting it as a v1 cgroup at /dev/memcg. Devices where
        // PSI is available and the vendor has not forced use_minfree_levels are
        // unaffected: the condition below evaluates false and the v2 path is taken.
        if (name == "memory" && descriptor.controller()->version() == 2) {
            const bool psi_available = access("/proc/pressure/memory", F_OK) == 0;

            // scan property files from all partitions that PropertyLoadBootDefaults
            // would read, in the same order.  the property service has not loaded
            // these yet, so we must read the files directly.  parse line-by-line
            // to skip comments (lines starting with #) which are common in
            // build.prop files generated by post_process_props.py.
            bool minfree_levels = false;
            static const char* prop_files[] = {
                    "/system/build.prop",
                    "/system_ext/etc/build.prop",
                    "/system_ext/build.prop",
                    "/vendor/default.prop",
                    "/vendor/build.prop",
                    "/vendor_dlkm/etc/build.prop",
                    "/odm_dlkm/etc/build.prop",
                    "/odm/etc/build.prop",
                    "/odm/build.prop",
                    "/product/etc/build.prop",
                    "/product/build.prop",
            };
            for (const char* path : prop_files) {
                std::string contents;
                if (!android::base::ReadFileToString(path, &contents)) continue;
                // parse line-by-line; last non-commented value wins per file,
                // and later files override earlier files.
                std::istringstream stream(contents);
                std::string line;
                while (std::getline(stream, line)) {
                    // strip trailing \r from files with windows line endings
                    if (!line.empty() && line.back() == '\r') line.pop_back();
                    if (line.empty() || line[0] == '#') continue;
                    if (line == "ro.lmk.use_minfree_levels=true" ||
                        line == "ro.lmk.use_new_strategy=false") {
                        minfree_levels = true;
                    } else if (line == "ro.lmk.use_minfree_levels=false" ||
                               line == "ro.lmk.use_new_strategy=true") {
                        minfree_levels = false;
                    }
                }
            }

            if (!psi_available || minfree_levels) {
                LOG(WARNING) << "Mounting memory cgroup as v1 at /dev/memcg: "
                             << (!psi_available ? "kernel lacks PSI support"
                                                : "vendor forces old lmkd kill strategy "
                                                  "(use_minfree_levels=true or "
                                                  "use_new_strategy=false)");
                descriptor = CgroupDescriptor(
                        1, name, "/dev/memcg", descriptor.mode(), descriptor.uid(),
                        descriptor.gid(), descriptor.controller()->flags() &
                        ~CGROUPRC_CONTROLLER_FLAG_NEEDS_ACTIVATION, UINT32_MAX);
            }
        }

        if (!SetupCgroup(descriptor)) {
            // issue a warning and proceed with the next cgroup
            // issue a warning and proceed with the next cgroup
            LOG(WARNING) << "Failed to setup " << name << " cgroup";
        }
    }

    const auto it = descriptors.find(CGROUPV2_HIERARCHY_NAME);
    const std::string cgroup_v2_root = (it == descriptors.end())
                                               ? CGROUP_V2_ROOT_DEFAULT
                                               : it->second.controller()->path();

    LOG(INFO) << "Using system/app isolation under: " << cgroup_v2_root;
    if (!CreateV2SubHierarchy(cgroup_v2_root + "/apps", descriptors) ||
        !CreateV2SubHierarchy(cgroup_v2_root + "/system", descriptors)) {
        return false;
    }

    return true;
}
