#!/usr/bin/env bash
# apply_patches.sh — resilient driver for the LineageOS_gsi patch sets.
#
# Replaces LineageOS_gsi/patches/apply-patches.sh, which has two flaws that
# bite this tree:
#
#   1. `set -e` + an unguarded `pushd` means a patch directory whose repo is
#      not in this tree (vendor/magisk, hardware/oplus, vendor/lineage) kills
#      the entire run — every later set is then silently never applied.
#   2. No already-applied check, so it cannot be re-run after a partial sync.
#
# This driver skips absent repos, skips already-applied patches, keeps going
# after a failure, and prints a summary. It lives in gsi/ because that
# directory is not managed by `repo` and so survives `repo sync`.
#
# Usage: bash gsi/apply_patches.sh [--dry-run] [set ...]
#        sets default to: trebledroid trebledroid-staging personal
#
# A set name resolves to LineageOS_gsi/patches/<set>, gsi/patches/<set>, or a
# literal directory path, in that order.

set -uo pipefail

TREE="$(cd "$(dirname "$0")/.." && pwd)"
cd "$TREE"

DRY_RUN=0
if [ "${1:-}" = "--dry-run" ]; then DRY_RUN=1; shift; fi
SETS=("$@")
[ ${#SETS[@]} -eq 0 ] && SETS=(trebledroid trebledroid-staging personal)

RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'

n_applied=0; n_skipped=0; n_failed=0; n_norepo=0
FAILED_LIST=()
NOREPO_LIST=()

# Map a patch directory name to its path in the tree, matching the upstream
# driver's conventions (underscores become slashes, "platform/" is stripped).
map_tree() {
    local tree
    tree="$(echo "$1" | tr _ / | sed -e 's;platform/;;g')"
    case "$tree" in
        build)                   tree=build/make ;;
        testing)                 tree=platform_testing ;;
        vendor/hardware/overlay) tree=vendor/hardware_overlay ;;
        treble/app)              tree=treble_app ;;
        vendor/partner/gms)      tree=vendor/partner_gms ;;
        # Doze-off patches-17 directory names that do not map 1:1.
        build/target)            tree=build/make ;;
        system/fs/fs/mgr)        tree=system/fs/fs_mgr ;;
        prebuilts/module/sdk/Profiling) tree=prebuilts/module_sdk/Profiling ;;
        test/vts-testcase/hal/treble/vintf) tree=test/vts-testcase/hal ;;
    esac
    echo "$tree"
}

subject_of() {
    sed -n 's/^Subject: \(\[[^]]*\] \)\?//p' "$1" | head -1
}

apply_one() {
    local repo="$1" patch="$2" name
    name="$(basename "$patch")"
    # git -C changes directory before reading the patch, so the path must be
    # absolute or it stops resolving.
    case "$patch" in /*) ;; *) patch="$TREE/$patch" ;; esac

    # Already applied? A clean reverse-apply is not proof on its own: for a
    # pure deletion whose surrounding context repeats just after the deleted
    # block, git finds the context at an offset and the reverse check passes
    # on an unpatched tree. When the patch also applies forward, the commit
    # subject in the log decides.
    if git -C "$repo" apply --reverse --check "$patch" >/dev/null 2>&1; then
        if ! git -C "$repo" apply --check "$patch" >/dev/null 2>&1 ||
           git -C "$repo" log -n 400 --format=%s | grep -qxF -- "$(subject_of "$patch")"; then
            n_skipped=$((n_skipped + 1))
            return 0
        fi
    fi

    if [ "$DRY_RUN" = "1" ]; then
        if git -C "$repo" apply --check "$patch" >/dev/null 2>&1; then
            echo -e "  ${GREEN}would apply${NC}  $name"
            n_applied=$((n_applied + 1))
        else
            echo -e "  ${RED}would FAIL${NC}   $name"
            n_failed=$((n_failed + 1)); FAILED_LIST+=("$repo: $name")
        fi
        return 0
    fi

    if git -C "$repo" apply --check "$patch" >/dev/null 2>&1; then
        if git -C "$repo" am "$patch" >/dev/null 2>&1; then
            n_applied=$((n_applied + 1)); return 0
        fi
        git -C "$repo" am --abort >/dev/null 2>&1
    fi

    # Fall back to fuzzy application, then commit by hand. GNU patch writes
    # .orig backups whenever it applies with fuzz, so backups are turned off.
    # Only the patch's own paths are staged: a bare `git add -A` also commits
    # untracked files such as the infinity*.mk products in device/phh/treble.
    if patch -d "$repo" -f -p1 --no-backup-if-mismatch --dry-run --silent < "$patch" >/dev/null 2>&1; then
        patch -d "$repo" -f -p1 --no-backup-if-mismatch --silent < "$patch" >/dev/null 2>&1
        sed -n 's;^diff --git a/[^ ]* b/;;p' "$patch" | sort -u |
            xargs -r git -C "$repo" add -A -- >/dev/null 2>&1
        git -C "$repo" commit -q -m "$(subject_of "$patch")" >/dev/null 2>&1
        echo -e "  ${YELLOW}fuzzy${NC}        $name"
        n_applied=$((n_applied + 1)); return 0
    fi

    echo -e "  ${RED}FAILED${NC}       $name"
    n_failed=$((n_failed + 1)); FAILED_LIST+=("$repo: $name")
    return 0
}

for set_name in "${SETS[@]}"; do
    # A set is looked up under the upstream patch tree first, then under our
    # own gsi/patches (which survives repo sync), then taken as a literal path.
    set_dir=""
    for cand in "LineageOS_gsi/patches/$set_name" "gsi/patches/$set_name" "$set_name"; do
        [ -d "$cand" ] && { set_dir="$cand"; break; }
    done
    [ -n "$set_dir" ] || { echo -e "${YELLOW}[patches]${NC} no such set: $set_name"; continue; }

    echo -e "\n${BLUE}### ${set_name} ###${NC}"

    for dir in "$set_dir"/*/; do
        [ -d "$dir" ] || continue
        repo="$(map_tree "$(basename "$dir")")"

        if [ ! -d "$repo" ]; then
            echo -e "${YELLOW}[skip]${NC} $repo not in this tree"
            n_norepo=$((n_norepo + 1)); NOREPO_LIST+=("$repo ($set_name)")
            continue
        fi

        echo "[$repo]"
        for patch in "$dir"*.patch; do
            [ -f "$patch" ] || continue
            apply_one "$repo" "$patch"
        done
    done
done

echo
echo "======================================================"
echo -e "applied:        ${GREEN}${n_applied}${NC}"
echo -e "already there:  ${n_skipped}"
echo -e "failed:         ${RED}${n_failed}${NC}"
echo -e "absent repos:   ${YELLOW}${n_norepo}${NC}"
echo "======================================================"

if [ ${#NOREPO_LIST[@]} -gt 0 ]; then
    echo; echo "Repos not in this tree (their patches were skipped):"
    printf '  %s\n' "${NOREPO_LIST[@]}"
fi

if [ ${#FAILED_LIST[@]} -gt 0 ]; then
    echo; echo "Failed patches:"
    printf '  %s\n' "${FAILED_LIST[@]}"
    exit 1
fi
