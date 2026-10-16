#!/bin/bash
#
# Cleanup script for the Firefly app.
#
# Usage: cleanup.sh [--once] [--dry-run] [--help]
#   (default)   run forever: every CLEANUP_INTERVAL sweep the targeted work areas, then sleep.
#               Sweeps append to /firefly/logs/cleanup/<app>/cleanup.<YYYYMMDD>.log, one file
#               per day, keeping 7 days.
#   --once      make one sweep, log it to stdout and exit; for an external scheduler such as
#               a Kubernetes CronJob.  Exits 1 when the sweep cannot run.
#   --dry-run   log what would be removed, remove nothing.
#
# An app has two work areas, each a dir named after the app:
#     workarea          /firefly/workarea/<app>           this container's own files
#     shared workarea   /firefly/shared-workarea/<app>    files shared by all replicas
# where <app> is the simple name of the application; see CLEANUP_APP_NAME.
# Each targeted work area gets two passes per sweep, workarea first:
#
#   age pass     Each entry has a maximum age; files not *modified* within it are removed.
#   size pass    Only when the work area's MAX is set.  When usage is at or above it, ages
#                are ignored and the oldest files are removed until usage is down to
#                90% of MAX.
#
# ------------------------------------------------------------------------------------
# Environment variables (all optional)
#
#   Name                   Default  Description
#   ---------------------  -------  ---------------------------------------------------
#   CLEANUP_INTERVAL       1h       how often a sweep runs; ignored with --once
#   CLEANUP_TARGETS        all      work areas to sweep: all, workarea, shared or none.
#                                   Use workarea in app containers when one other
#                                   process (e.g. a CronJob) sweeps the shared workarea.
#   CLEANUP_APP_NAME       auto     the app, i.e. the work area dir name.  Unset uses the
#                                   first dir under /firefly/workarea; required when
#                                   CLEANUP_TARGETS=shared.
#
#   age pass -- maximum age per group of entries
#   CLEANUP_AGE_WORKAREA   1h       workarea: temp_files, perm_files, visualize/*
#   CLEANUP_AGE_HIPS       90d      workarea: HiPS
#   CLEANUP_AGE_SHARED     7d       shared workarea: stage, upload
#
#   size pass -- a usage limit per work area; unset or empty means no size pass
#   CLEANUP_WORKAREA_MAX   unset    workarea
#   CLEANUP_SHARED_MAX     unset    shared workarea
#
# Ages are <n>m, <n>h or <n>d (minutes, hours, days).
# A MAX is one of:
#   <n>%     1-99, percent of the work area's filesystem in use, as df's Use% reports it.
#            Counts everything on that filesystem; best when the volume is dedicated.
#   <size>   <n>K, <n>M, <n>G or <n>T, binary (1G = 1024M); Kubernetes' Ki/Mi/Gi/Ti also
#            work.  Measured with du of the work area. Use <n>% to keep the volume itself from filling.
# An invalid age falls back to its default, and an invalid MAX to no size pass, with a warning.
#
# Notes:
#   - Age is modification time.
#   - Only the entries listed below are ever touched; anything else in a work area, and
#     anything outside the app's work areas, is left alone, in both passes.
#   - Files may live up to CLEANUP_INTERVAL longer than their configured age, since they
#     only expire when a sweep runs.
#
set -u

readonly FIND=/usr/bin/find
readonly RM=/bin/rm
readonly LOG_ROOT=/firefly/logs/cleanup   # outside the dirs being cleaned
readonly LOG_KEEP=7                       # days of logs kept per app
readonly CLEAN_TO_PCT=90                  # the size pass cleans down to this % of MAX

warn() {      # <message...>          -- reports a problem and carries on
    echo "Cleanup: WARNING: $*" >&2
}

error() {     # <message...>          -- reports a misconfiguration; the caller skips the sweep
    echo "Cleanup: ERROR: $*" >&2
}

require() {   # <what is missing>     -- reports it and stops the script
    echo "cleanup.sh: requires $1" >&2
    exit 1
}

# ------------------------------------------------------------------------------------
# Command line
# ------------------------------------------------------------------------------------
ONCE=false
DRY_RUN=false
for arg in "$@"; do
    case "${arg}" in
        --once)    ONCE=true ;;
        --dry-run) DRY_RUN=true ;;
        -h|--help) awk 'NR > 1 && !/^#/ {exit} NR > 1 {sub(/^# ?/, ""); print}' "$0"; exit 0 ;;
        *)         echo "cleanup.sh: unknown option '${arg}'; see --help" >&2; exit 2 ;;
    esac
done
readonly ONCE DRY_RUN

# ------------------------------------------------------------------------------------
# Requirements: bash 4+ (readarray, ${var,,}) and a GNU userland (find -printf, sort -z,
# date -d).
# ------------------------------------------------------------------------------------
((BASH_VERSINFO[0] >= 4))                  || require "bash 4+, found ${BASH_VERSION}"
${FIND} /dev/null -maxdepth 0 -printf '' 2>/dev/null || require "GNU findutils (find -printf)"
printf '' | sort -z >/dev/null 2>&1        || require "GNU coreutils (sort -z)"
date -d @0 >/dev/null 2>&1                 || require "GNU coreutils (date -d)"

# ------------------------------------------------------------------------------------
# Work area definitions.  Entries are "<path relative to the work area>|<env var>|<default>"
# ------------------------------------------------------------------------------------
readonly WORKAREA_DIR=/firefly/workarea
readonly WORKAREA_ENTRIES=(
    "temp_files|CLEANUP_AGE_WORKAREA|1h"
    "perm_files|CLEANUP_AGE_WORKAREA|1h"
    "visualize/fits-cache|CLEANUP_AGE_WORKAREA|1h"
    "visualize/users|CLEANUP_AGE_WORKAREA|1h"
    "HiPS|CLEANUP_AGE_HIPS|90d"
)

readonly SHARED_WORKAREA_DIR=/firefly/shared-workarea
readonly SHARED_WORKAREA_ENTRIES=(
    "stage|CLEANUP_AGE_SHARED|7d"
    "upload|CLEANUP_AGE_SHARED|7d"
)

# ------------------------------------------------------------------------------------
# Settings: read an env var, validate it, fall back to the default
# ------------------------------------------------------------------------------------

# "2d" -> 2880.  Prints nothing and fails if the value is not <n>m, <n>h or <n>d.
to_minutes() {
    local num="${1%[mhd]}" unit="${1: -1}"
    [[ "${num}" =~ ^[0-9]+$ ]] || return 1
    case "${unit}" in
        m) echo "${num}" ;;
        h) echo $((num * 60)) ;;
        d) echo $((num * 1440)) ;;
        *) return 1 ;;
    esac
}

declare -A AGE_MINUTES=()   # age env var -> max age in minutes
declare -A AGE_SHOWN=()     # age env var -> the value in effect, as written

# age_settings <entry-spec>... -- resolves each distinct age env var once
age_settings() {
    local spec entry name default value minutes
    for spec in "$@"; do
        IFS='|' read -r entry name default <<< "${spec}"
        [[ -n "${AGE_MINUTES[${name}]:-}" ]] && continue   # several entries share one
        value="${!name:-${default}}"
        if ! minutes=$(to_minutes "${value}"); then
            warn "${name}='${value}' is not a valid age (use <n>m, <n>h or <n>d); using ${default}"
            value="${default}"
            minutes=$(to_minutes "${default}")
        fi
        AGE_MINUTES[${name}]="${minutes}"
        AGE_SHOWN[${name}]="${value}"
    done
}

# describe_ages <entry-spec>... -- prints each distinct age setting, in table order
describe_ages() {
    local spec entry name seen=" "
    for spec in "$@"; do
        IFS='|' read -r entry name _ <<< "${spec}"
        [[ "${seen}" == *" ${name} "* ]] && continue
        seen+="${name} "
        printf 'Cleanup:   %s=%s\n' "${name}" "${AGE_SHOWN[${name}]}"
    done
}

# "5Gi" -> 5242880.  Prints nothing and fails if the value is not a positive <n>K|M|G|T.
to_kb() {
    local num
    [[ "${1^^}" =~ ^([0-9]+)([KMGT])I?B?$ ]] || return 1
    num=$((10#${BASH_REMATCH[1]}))
    ((num > 0)) || return 1
    case "${BASH_REMATCH[2]}" in
        K) echo "${num}" ;;
        M) echo $((num * 1024)) ;;
        G) echo $((num * 1024 ** 2)) ;;
        T) echo $((num * 1024 ** 3)) ;;
    esac
}

# "85%" -> "pct 85", "5Gi" -> "kb 5242880".  Fails if the value is neither.
to_max() {
    local pct kb
    if [[ "$1" =~ ^([0-9]+)%$ ]]; then
        pct=$((10#${BASH_REMATCH[1]}))
        ((pct >= 1 && pct <= 99)) || return 1
        echo "pct ${pct}"
    else
        kb=$(to_kb "$1") || return 1
        echo "kb ${kb}"
    fi
}

# max_setting <envVar> -- prints the parsed max, or "off" when unset, empty or invalid
max_setting() {
    local name="$1" value="${!1:-}" max
    [[ -n "${value}" ]] || { echo off; return 0; }
    if ! max=$(to_max "${value}"); then
        warn "${name}='${value}' is not valid (use <n>% from 1 to 99, or a size such as 5Gi);" \
             "no size pass"
        max=off
    fi
    echo "${max}"
}

# "pct 85" -> "85%", "kb 5242880" -> "5242880 KB", "off" -> "off"
describe_max() {
    local mode value
    read -r mode value <<< "$1"
    case "${mode}" in
        pct) echo "${value}%" ;;
        kb)  echo "${value} KB" ;;
        *)   echo "off" ;;
    esac
}

# targets_setting -- prints "<target> <workarea> <shared>", the last two true or false
targets_setting() {
    local value="${CLEANUP_TARGETS:-all}"
    case "${value,,}" in
        all)      echo "all true true" ;;
        workarea) echo "workarea true false" ;;
        shared)   echo "shared false true" ;;
        none)     echo "none false false" ;;
        *)
            warn "CLEANUP_TARGETS='${value}' must be all, workarea, shared or none; using all"
            echo "all true true" ;;
    esac
}

# ------------------------------------------------------------------------------------
# Path helpers
# ------------------------------------------------------------------------------------

# CLEANUP_APP_NAME: Unset, it falls back to the first dir under the workarea (sorted, so the choice is
# stable across sweeps).  That is right for the usual one-app container; set
# CLEANUP_APP_NAME when there are multiple apps under the workarea and you want to sweep a specific one.
# Set, it must name a dir under the workarea when the workarea is a target, or the sweep is
# skipped with an error: a wrong name would otherwise clean nothing, quietly.  With only the
# shared-workarea targeted a missing dir is not an error, since the app creates it on first use.
# The shared-workarea is not searched: it may hold other data (e.g. redis), so a guess could be wrong.
# Returns 1 on an error, 2 when there is nothing to clean yet.
resolve_app_name() {
    local name="${CLEANUP_APP_NAME:-}" dirs=()
    if [[ -n "${name}" ]]; then
        if [[ "${DO_WORKAREA}" == true && ! -d "${WORKAREA_DIR}/${name}" ]]; then
            error "CLEANUP_APP_NAME='${name}': ${WORKAREA_DIR}/${name} is not a dir;" \
                  "skipping this sweep"
            return 1
        fi
        if [[ "${DO_WORKAREA}" != true && ! -d "${SHARED_WORKAREA_DIR}/${name}" ]]; then
            echo "Cleanup: ${SHARED_WORKAREA_DIR}/${name} does not exist yet; nothing to clean" >&2
            return 2
        fi
        echo "${name}"
        return 0
    fi
    if [[ "${DO_WORKAREA}" != true ]]; then
        warn "CLEANUP_APP_NAME must be set when the workarea is not a target; skipping this sweep"
        return 1
    fi
    readarray -t dirs < <(${FIND} "${WORKAREA_DIR}" -mindepth 1 -maxdepth 1 -type d \
                          ! -name cleanup_logs -printf '%f\n' 2>/dev/null | sort)
    ((${#dirs[@]})) || {
        warn "no application dir under ${WORKAREA_DIR} yet; skipping this sweep"
        return 1
    }
    ((${#dirs[@]} > 1)) && warn "${#dirs[@]} application dirs under ${WORKAREA_DIR}" \
            "(${dirs[*]}); using '${dirs[0]}' -- set CLEANUP_APP_NAME to pick another"
    echo "${dirs[0]}"
}

# Remove empty dirs below an entry, but never the entry itself.
prune_empty_dirs() {
    local path
    for path in "$@"; do
        ${FIND} "${path}" -mindepth 1 -depth -type d -empty -delete
    done
}

# ------------------------------------------------------------------------------------
# Pass 1: age -- each entry expires on its own -mmin
# ------------------------------------------------------------------------------------

# clean_by_age <appDir> <entry-spec>...
clean_by_age() {
    local app_dir="$1"; shift
    local spec entry env_var minutes path

    echo "Age pass: removing files not modified within each entry's max age"
    for spec in "$@"; do
        IFS='|' read -r entry env_var _ <<< "${spec}"
        path="${app_dir}/${entry}"
        [[ -d "${path}" ]] || continue
        minutes="${AGE_MINUTES[${env_var}]}"

        echo
        echo "--- ${entry}: older than ${minutes} minutes (${env_var}=${AGE_SHOWN[${env_var}]})"
        if [[ "${DRY_RUN}" == true ]]; then
            ${FIND} "${path}" -type f -mmin "+${minutes}" -print
        else
            ${FIND} "${path}" -type f -mmin "+${minutes}" -print -delete
            prune_empty_dirs "${path}"
        fi
    done
}

# ------------------------------------------------------------------------------------
# Pass 2: size -- ignore ages, remove the oldest files until usage is under the max
# ------------------------------------------------------------------------------------

# clean_by_size <appDir> <max> <path>...
# A "pct" max is df of appDir's filesystem; a "kb" max is du of appDir.
clean_by_size() {
    local app_dir="$1" max="$2"; shift 2
    local paths=("$@")
    local mode value used_kb avail_kb max_kb target_kb need_kb freed_kb=0 removed=0
    local mtime kb path

    read -r mode value <<< "${max}"
    echo
    if [[ "${mode}" == pct ]]; then
        # Use% as df reports it: used / (used + available), so reserved blocks count as full
        read -r used_kb avail_kb < <(df -Pk "${app_dir}" 2>/dev/null | awk 'NR==2 {print $3, $4}')
        if ! [[ "${used_kb:-}" =~ ^[0-9]+$ && "${avail_kb:-}" =~ ^[0-9]+$ ]] ||
           ((used_kb + avail_kb == 0)); then
            warn "cannot read disk usage for ${app_dir}; skipping the size pass"
            return
        fi
        max_kb=$(((used_kb + avail_kb) * value / 100))
        target_kb=$((max_kb * CLEAN_TO_PCT / 100))
        echo "Size pass (df of ${app_dir}): $((used_kb * 100 / (used_kb + avail_kb)))% used," \
             "max ${value}%, cleans down to $((value * CLEAN_TO_PCT / 100))%"
    else
        # this app's own dir; other apps and data on the volume are not this app's to cap
        used_kb=$(du -skx "${app_dir}" 2>/dev/null | awk '{print $1}')
        if ! [[ "${used_kb:-}" =~ ^[0-9]+$ ]]; then
            warn "cannot read disk usage for ${app_dir}; skipping the size pass"
            return
        fi
        max_kb="${value}"
        target_kb=$((max_kb * CLEAN_TO_PCT / 100))
        echo "Size pass (du of ${app_dir}): ${used_kb} KB used, max ${max_kb} KB," \
             "cleans down to ${target_kb} KB"
    fi
    if ((used_kb < max_kb)); then
        echo "Nothing to do -- below the max"
        return
    fi

    need_kb=$((used_kb - target_kb))
    echo "Need to free ${need_kb} KB; removing oldest files first"
    echo

    ((${#paths[@]})) || { warn "no entries exist under ${app_dir}; nothing can be freed"; return; }

    # mtime <tab> size-in-KB <tab> path, NUL terminated, oldest first
    while IFS=$'\t' read -r -d '' mtime kb path; do
        ((freed_kb >= need_kb)) && break
        echo "$(date -d "@${mtime%.*}" '+%Y-%m-%d %H:%M') ${kb}K ${path}"
        [[ "${DRY_RUN}" == true ]] || ${RM} -f -- "${path}" || continue
        freed_kb=$((freed_kb + kb))
        removed=$((removed + 1))
    done < <(${FIND} "${paths[@]}" -type f -printf '%T@\t%k\t%p\0' | sort -z -t$'\t' -k1,1n)

    echo
    echo "Removed ${removed} files, freed ${freed_kb} KB of the ${need_kb} KB needed"
    ((freed_kb < need_kb)) && warn "${app_dir} is still above its target: nothing left to remove"
    [[ "${DRY_RUN}" == true ]] || prune_empty_dirs "${paths[@]}"
    return 0
}

# ------------------------------------------------------------------------------------
# Sweep
# ------------------------------------------------------------------------------------

# sweep_dir <label> <baseDir> <app> <max> <entry-spec>...
sweep_dir() {
    local label="$1" base="$2" app="$3" max="$4"; shift 4
    local app_dir="${base}/${app}" paths=() spec entry

    echo
    echo "----- ${label}: ${app_dir}"
    [[ -d "${app_dir}" ]] || { echo "does not exist, skipping"; return; }
    clean_by_age "${app_dir}" "$@"

    [[ "${max}" == off ]] && return
    for spec in "$@"; do
        IFS='|' read -r entry _ _ <<< "${spec}"
        [[ -d "${app_dir}/${entry}" ]] && paths+=("${app_dir}/${entry}")
    done
    clean_by_size "${app_dir}" "${max}" "${paths[@]}"
}

# Does the work of one sweep, writing to stdout.  The workarea goes first so its
# short-lived files are reclaimed before the shared ones when both share a filesystem.
sweep_body() {
    local app="$1"
    echo
    echo "===== cleanup start -- app '${app}' -- $(date) ====="
    [[ "${DRY_RUN}" == true ]] && echo "DRY RUN -- nothing will be removed"

    if [[ "${DO_WORKAREA}" == true ]]; then
        sweep_dir "workarea" "${WORKAREA_DIR}" "${app}" "${WORKAREA_MAX}" "${WORKAREA_ENTRIES[@]}"
    fi
    if [[ "${DO_SHARED}" == true ]]; then
        sweep_dir "shared-workarea" "${SHARED_WORKAREA_DIR}" "${app}" "${SHARED_MAX}" \
                  "${SHARED_WORKAREA_ENTRIES[@]}"
    fi

    echo
    echo "===== cleanup done -- $(date) ====="
}

# One sweep, appended to today's log, or to stdout if that fails.
sweep_to_log() {
    local app="$1" log_dir="${LOG_ROOT}/${app}" log_file

    log_file="${log_dir}/cleanup.$(date +%Y%m%d).log"
    if ! mkdir -p "${log_dir}" 2>/dev/null || ! : >> "${log_file}" 2>/dev/null; then
        warn "cannot write under ${log_dir}; logging this sweep to stdout"
        echo "Cleanup: sweeping app '${app}'"
        sweep_body "${app}" 2>&1
        return
    fi

    echo "Cleanup: sweeping app '${app}', log: ${log_file}"
    sweep_body "${app}" >> "${log_file}" 2>&1

    # keep the most recent days for this app
    ls -1t "${log_dir}"/cleanup.*.log 2>/dev/null | tail -n "+$((LOG_KEEP + 1))" | \
        while read -r old; do ${RM} -f -- "${old}"; done
}

# ------------------------------------------------------------------------------------
# Resolve settings once, report them, then sweep
# ------------------------------------------------------------------------------------

# Resolve CLEANUP_INTERVAL: how often a sweep runs.
readonly DEFAULT_INTERVAL=1h
INTERVAL="${CLEANUP_INTERVAL:-${DEFAULT_INTERVAL}}"
if ! [[ "${INTERVAL}" =~ ^[0-9]+[mhd]$ ]]; then
    warn "CLEANUP_INTERVAL='${INTERVAL}' is not valid (use <n>m, <n>h or <n>d);" \
         "using ${DEFAULT_INTERVAL}"
    INTERVAL="${DEFAULT_INTERVAL}"
fi
readonly INTERVAL

read -r TARGETS DO_WORKAREA DO_SHARED <<< "$(targets_setting)"
readonly TARGETS DO_WORKAREA DO_SHARED
if [[ "${DO_WORKAREA}" == true ]]; then
    age_settings "${WORKAREA_ENTRIES[@]}"
    readonly WORKAREA_MAX=$(max_setting CLEANUP_WORKAREA_MAX)
fi
if [[ "${DO_SHARED}" == true ]]; then
    age_settings "${SHARED_WORKAREA_ENTRIES[@]}"
    readonly SHARED_MAX=$(max_setting CLEANUP_SHARED_MAX)
fi

if [[ "${ONCE}" == true ]]; then mode="once"; else mode="every ${INTERVAL}"; fi
[[ "${DRY_RUN}" == true ]] && mode+=", dry run"
echo "Cleanup: started with:"
printf 'Cleanup:   %s\n' \
    "mode=${mode}" \
    "CLEANUP_TARGETS=${TARGETS}" \
    "CLEANUP_APP_NAME=${CLEANUP_APP_NAME:-auto}"
if [[ "${DO_WORKAREA}" == true ]]; then
    describe_ages "${WORKAREA_ENTRIES[@]}"
    printf 'Cleanup:   %s\n' "CLEANUP_WORKAREA_MAX=$(describe_max "${WORKAREA_MAX}")"
fi
if [[ "${DO_SHARED}" == true ]]; then
    describe_ages "${SHARED_WORKAREA_ENTRIES[@]}"
    printf 'Cleanup:   %s\n' "CLEANUP_SHARED_MAX=$(describe_max "${SHARED_MAX}")"
fi

if [[ "${DO_WORKAREA}" != true && "${DO_SHARED}" != true ]]; then
    echo "Cleanup: no targets; nothing to do"
    exit 0
fi

if [[ "${ONCE}" == true ]]; then
    app=$(resolve_app_name)
    case $? in
        0) sweep_body "${app}" ;;
        2) ;;                       # nothing to clean yet
        *) exit 1 ;;
    esac
    exit 0
fi

sleep 30    # give the app time to create its work area before the first sweep

while true; do
    # re-resolve every sweep: the app dir may not exist yet at container start
    if app=$(resolve_app_name); then sweep_to_log "${app}"; fi
    sleep "${INTERVAL}"
done
