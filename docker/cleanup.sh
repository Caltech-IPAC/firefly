#!/bin/bash
#
# Cleanup script for the Firefly app.
#
# Runs forever: every CLEANUP_INTERVAL it sweeps both base work areas, then sleeps.
# Sweeps append to /firefly/logs/cleanup/<app>/cleanup.<YYYYMMDD>.log, one file per day,
# rolling at midnight and keeping CLEANUP_LOG_KEEP days.
#
# Each base dir holds one sub-directory per application, e.g.
#     /firefly/workarea/<app>/temp_files
# where <app> is the simple name of the application.
# See # CLEANUP_APP_NAME below for details.  Each sweep makes up to two passes over the app dir:
#
#   age pass (always)   Each entry below a base dir has its own maximum age; files not
#                       *modified* within that age are removed.
#   pct-full pass       Runs after the age pass when CLEANUP_PCT_FULL is 1-99.  Max ages
#                       are ignored: while the filesystem is still at least PCT_FULL
#                       percent full, the oldest files are removed until usage drops to
#                       (PCT_FULL - PCT_RECLAIM).
#
# ------------------------------------------------------------------------------------
# Environment variables (all optional)
#
#   Name                              Default  Description
#   --------------------------------  -------  ----------------------------------------
#   CLEANUP_APP_NAME                  auto     app dir to sweep under both base dirs;
#                                              unset uses the first dir under the
#                                              workarea; set it when necessary.
#   CLEANUP_INTERVAL                  1h       how often a sweep runs
#   CLEANUP_LOG_KEEP                  7        days of logs kept (in /firefly/logs/cleanup)
#   CLEANUP_DRY_RUN                   false    log what would be removed, remove nothing
#
#   age pass -- maximum age per directory
#   CLEANUP_AGE_TEMP_FILES            1h       everything except; Hips, stage, upload
#                                              (defaults to CLEANUP_INTERVAL)
#   CLEANUP_AGE_HIPS                  90d      HiPS
#   CLEANUP_AGE_STAGE                 7d       stage
#   CLEANUP_AGE_UPLOAD                7d       upload
#
#   pct-full pass -- runs after the age pass, over both base dirs
#   CLEANUP_PCT_FULL                  85       1-99 enables the pass; 0 = off
#   CLEANUP_PCT_RECLAIM               5        percent of the filesystem to free
#   CLEANUP_SHARED_PCT_FULL           =above   shared-workarea only; it often has its own volume.
#   CLEANUP_SHARED_PCT_RECLAIM        =above
#
# Ages are <n>m, <n>h or <n>d (minutes, hours, days).  An invalid value falls back to
# the default with a warning.
#
# Notes:
#   - Age is modification time.
#   - Only the entries listed below are ever touched, and only under this app's dir;
#     anything else under a base dir is left alone, in both passes.
#   - pct-full reads df, which measures the whole filesystem.  When several apps share a
#     volume each one triggers on total usage but can only free its own files.
#   - Files may live up to CLEANUP_INTERVAL longer than their configured age, since they
#     only expire when a sweep runs.
#
set -u

readonly FIND=/usr/bin/find
readonly RM=/bin/rm
readonly LOG_ROOT=/firefly/logs/cleanup   # outside the dirs being cleaned

# ------------------------------------------------------------------------------------
# Requirements: bash 4+ (readarray, ${var,,}) and a GNU userland (find -printf, sort -z,
# date -d).
# ------------------------------------------------------------------------------------

warn() {      # <message...>          -- reports a problem and carries on
    echo "Cleanup: WARNING: $*" >&2
}

require() {   # <what is missing>     -- reports it and stops the script
    echo "cleanup.sh: requires $1" >&2
    exit 1
}
((BASH_VERSINFO[0] >= 4))                  || require "bash 4+, found ${BASH_VERSION}"
${FIND} /dev/null -maxdepth 0 -printf '' 2>/dev/null || require "GNU findutils (find -printf)"
printf '' | sort -z >/dev/null 2>&1        || require "GNU coreutils (sort -z)"
date -d @0 >/dev/null 2>&1                 || require "GNU coreutils (date -d)"

# Resolve CLEANUP_INTERVAL: how often a sweep runs, and also used as the default age for temp files.
readonly DEFAULT_INTERVAL=1h
INTERVAL="${CLEANUP_INTERVAL:-${DEFAULT_INTERVAL}}"
if ! [[ "${INTERVAL}" =~ ^[0-9]+[mhd]$ ]]; then
    warn "CLEANUP_INTERVAL='${INTERVAL}' is not valid (use <n>m, <n>h or <n>d);" \
         "using ${DEFAULT_INTERVAL}"
    INTERVAL="${DEFAULT_INTERVAL}"
fi
readonly INTERVAL

# ------------------------------------------------------------------------------------
# Base dir definitions.  Entries are "<path relative to an app dir>|<env var>|<default>"
# ------------------------------------------------------------------------------------
readonly WORKAREA_DIR=/firefly/workarea
readonly WORKAREA_ENTRIES=(
    "temp_files|CLEANUP_AGE_TEMP_FILES|${INTERVAL}"
    "perm_files|CLEANUP_AGE_TEMP_FILES|${INTERVAL}"
    "visualize/fits-cache|CLEANUP_AGE_TEMP_FILES|${INTERVAL}"
    "visualize/users|CLEANUP_AGE_TEMP_FILES|${INTERVAL}"
    "HiPS|CLEANUP_AGE_HIPS|90d"
)

readonly SHARED_WORKAREA_DIR=/firefly/shared-workarea
readonly SHARED_WORKAREA_ENTRIES=(
    "stage|CLEANUP_AGE_STAGE|7d"
    "upload|CLEANUP_AGE_UPLOAD|7d"
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

# minutes_setting <envVar> <default>
minutes_setting() {
    local name="$1" default="$2" value="${!1:-$2}" minutes
    if ! minutes=$(to_minutes "${value}"); then
        warn "${name}='${value}' is not a valid age (use <n>m, <n>h or <n>d); using ${default}"
        minutes=$(to_minutes "${default}")
    fi
    echo "${minutes}"
}

# int_setting <envVar> <default> <min> <max>
int_setting() {
    local name="$1" default="$2" min="$3" max="$4" value="${!1:-$2}"
    if ! [[ "${value}" =~ ^[0-9]+$ ]] || ((value < min || value > max)); then
        warn "${name}='${value}' must be an integer from ${min} to ${max}; using ${default}"
        value="${default}"
    fi
    echo "${value}"
}

# bool_setting <envVar> <default>
bool_setting() {
    local value="${!1:-$2}"
    [[ "${value,,}" == true || "${value,,}" == yes || "${value}" == 1 ]] && echo true || echo false
}

# ------------------------------------------------------------------------------------
# Path helpers
# ------------------------------------------------------------------------------------

# entry_paths <baseDir> <app> <relativePath> -- prints <base>/<app>/<entry> if it exists
entry_paths() {
    local path="${1}/${2}/${3}"
    [[ -d "${path}" ]] && echo "${path}"
    return 0
}

# entry_paths_of <baseDir> <app> <entry-spec>... -- every existing path in an entry table
entry_paths_of() {
    local base="$1" app="$2"; shift 2
    local spec entry
    for spec in "$@"; do
        IFS='|' read -r entry _ _ <<< "${spec}"
        entry_paths "${base}" "${app}" "${entry}"
    done
    return 0
}

# same_fs <dirA> <dirB> -- true when both sit on one filesystem, so df reports one pool
same_fs() {
    local a b
    a=$(stat -c %d "$1" 2>/dev/null) && b=$(stat -c %d "$2" 2>/dev/null) || return 1
    [[ "${a}" == "${b}" ]]
}

# CLEANUP_APP_NAME: Unset, it falls back to the first dir under the workarea (sorted, so the choice is
# stable across sweeps).  That is right for the usual one-app container; set
# CLEANUP_APP_NAME when there are multiple apps under the workarea and you want to sweep a specific one.
resolve_app_name() {
    local dirs=()
    if [[ -n "${CLEANUP_APP_NAME:-}" ]]; then echo "${CLEANUP_APP_NAME}"; return 0; fi
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

clean_by_age() {
    local label="$1" base="$2" app="$3"; shift 3
    local app_dir="${base}/${app}" spec entry env_var default minutes paths

    echo
    echo "----- ${label}: ${app_dir}"
    [[ -d "${app_dir}" ]] || { echo "does not exist, skipping"; return; }
    echo "Age pass: removing files not modified within each entry's max age"
    for spec in "$@"; do
        IFS='|' read -r entry env_var default <<< "${spec}"
        minutes=$(minutes_setting "${env_var}" "${default}")

        readarray -t paths < <(entry_paths "${base}" "${app}" "${entry}")
        ((${#paths[@]})) || continue

        echo
        echo "--- ${entry}: older than ${minutes} minutes (${env_var}=${!env_var:-${default}})"
        if [[ "${DRY_RUN}" == true ]]; then
            ${FIND} "${paths[@]}" -type f -mmin "+${minutes}" -print
        else
            ${FIND} "${paths[@]}" -type f -mmin "+${minutes}" -print -delete
            prune_empty_dirs "${paths[@]}"
        fi
    done
}

# ------------------------------------------------------------------------------------
# Pass 2: pct-full -- ignore ages, remove the oldest files until there is room
# ------------------------------------------------------------------------------------

# clean_by_pct_full <label> <dfDir> <pctFull> <pctReclaim> <path>...
# The paths may span both base dirs: when they share a filesystem they share one pool
# of free space, so they must be ranked together.
clean_by_pct_full() {
    local label="$1" df_dir="$2" pct_full="$3" pct_reclaim="$4"; shift 4
    local paths=("$@")
    local total_kb used_kb used_pct target_pct need_kb freed_kb=0 removed=0
    local mtime kb path

    echo
    echo "----- ${label}"
    read -r total_kb used_kb < <(df -Pk "${df_dir}" | awk 'NR==2 {print $2, $3}')
    if ! [[ "${total_kb:-0}" =~ ^[1-9][0-9]*$ ]]; then
        warn "cannot read disk usage for ${df_dir}; skipping"
        return
    fi
    used_pct=$((used_kb * 100 / total_kb))
    target_pct=$((pct_full - pct_reclaim))
    ((target_pct < 0)) && target_pct=0      # delta larger than pct_full means "empty it"

    echo "Pct-full pass: ${used_pct}% used, cleans above ${pct_full}%, target ${target_pct}%"
    if ((used_pct < pct_full)); then
        echo "Nothing to do -- below ${pct_full}%"
        return
    fi

    need_kb=$((used_kb - total_kb * target_pct / 100))
    echo "Need to free ${need_kb} KB; removing oldest files first"
    echo

    ((${#paths[@]})) || { warn "no entries exist for ${label}; nothing can be freed"; return; }

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
    ((freed_kb < need_kb)) && warn "${label} is still above ${target_pct}%: nothing left to remove"
    [[ "${DRY_RUN}" == true ]] || prune_empty_dirs "${paths[@]}"
    return 0
}

# ------------------------------------------------------------------------------------
# Per base dir driver
# ------------------------------------------------------------------------------------

# Does the work of one sweep, writing to stdout.
sweep_body() {
    local app="$1" work_paths=() shared_paths=() pf pd
    echo
    echo "===== cleanup start -- app '${app}' -- $(date) ====="
    [[ "${DRY_RUN}" == true ]] && echo "DRY RUN -- nothing will be removed"

    # First pass: expire by age.  Always runs.
    clean_by_age "workarea" "${WORKAREA_DIR}" "${app}" "${WORKAREA_ENTRIES[@]}"
    clean_by_age "shared-workarea" "${SHARED_WORKAREA_DIR}" "${app}" "${SHARED_WORKAREA_ENTRIES[@]}"

    # Second pass: if still too full, reclaim least-recently-modified first, regardless of the max ages.
    if ((PCT_FULL > 0 || SHARED_PCT_FULL > 0)); then
        readarray -t work_paths   < <(entry_paths_of "${WORKAREA_DIR}" "${app}" "${WORKAREA_ENTRIES[@]}")
        readarray -t shared_paths < <(entry_paths_of "${SHARED_WORKAREA_DIR}" "${app}" "${SHARED_WORKAREA_ENTRIES[@]}")
        if same_fs "${WORKAREA_DIR}" "${SHARED_WORKAREA_DIR}"; then
            # Same filesystem: sweep both base dirs as one pool, at the lower threshold.
            pf="${PCT_FULL}"; pd="${PCT_RECLAIM}"
            if ((SHARED_PCT_FULL > 0)) && ((PCT_FULL == 0 || SHARED_PCT_FULL < PCT_FULL)); then
                pf="${SHARED_PCT_FULL}"; pd="${SHARED_PCT_RECLAIM}"
            fi
            if ((PCT_FULL != SHARED_PCT_FULL || PCT_RECLAIM != SHARED_PCT_RECLAIM)); then
                echo "note: both base dirs are on one filesystem; using the stricter" \
                     "${pf}%/${pd}% and ignoring the other setting"
            fi
            clean_by_pct_full "both work areas (one filesystem)" "${WORKAREA_DIR}" "${pf}" "${pd}" \
                              "${work_paths[@]}" "${shared_paths[@]}"
        else
            if ((PCT_FULL > 0)); then
                clean_by_pct_full "workarea: ${WORKAREA_DIR}/${app}" "${WORKAREA_DIR}" \
                                  "${PCT_FULL}" "${PCT_RECLAIM}" "${work_paths[@]}"
            fi
            if ((SHARED_PCT_FULL > 0)); then
                clean_by_pct_full "shared-workarea: ${SHARED_WORKAREA_DIR}/${app}" "${SHARED_WORKAREA_DIR}" \
                                  "${SHARED_PCT_FULL}" "${SHARED_PCT_RECLAIM}" "${shared_paths[@]}"
            fi
        fi
    fi

    echo
    echo "===== cleanup done -- $(date) ====="
}

# One pass over both base dirs, appended to today's log, or to stdout if that fails.
sweep() {
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
# Resolve settings once, report them, then loop
# ------------------------------------------------------------------------------------

readonly LOG_KEEP=$(int_setting CLEANUP_LOG_KEEP 7 1 1000)
readonly DRY_RUN=$(bool_setting CLEANUP_DRY_RUN false)

readonly PCT_FULL=$(int_setting CLEANUP_PCT_FULL 85 0 99)
readonly PCT_RECLAIM=$(int_setting CLEANUP_PCT_RECLAIM 5 1 99)
# shared-workarea may sit on its own volume, where df measures only this app's quota
readonly SHARED_PCT_FULL=$(int_setting CLEANUP_SHARED_PCT_FULL "${PCT_FULL}" 0 99)
readonly SHARED_PCT_RECLAIM=$(int_setting CLEANUP_SHARED_PCT_RECLAIM "${PCT_RECLAIM}" 1 99)



# Prints each distinct resolved age setting from the entry tables.
describe_ages() {
    local spec entry env_var default seen=""
    for spec in "${WORKAREA_ENTRIES[@]}" "${SHARED_WORKAREA_ENTRIES[@]}"; do
        IFS='|' read -r entry env_var default <<< "${spec}"
        [[ " ${seen} " == *" ${env_var} "* ]] && continue   # several entries share one
        seen+=" ${env_var}"
        printf 'Cleanup:   %s=%s\n' "${env_var}" "${!env_var:-${default}}"
    done
}

echo "Cleanup: started with:"
printf 'Cleanup:   %s\n' \
    "CLEANUP_APP_NAME=${CLEANUP_APP_NAME:-}" \
    "CLEANUP_INTERVAL=${INTERVAL}" \
    "CLEANUP_LOG_KEEP=${LOG_KEEP}" \
    "CLEANUP_DRY_RUN=${DRY_RUN}"
describe_ages
printf 'Cleanup:   %s\n' \
    "CLEANUP_PCT_FULL=${PCT_FULL}" \
    "CLEANUP_PCT_RECLAIM=${PCT_RECLAIM}" \
    "CLEANUP_SHARED_PCT_FULL=${SHARED_PCT_FULL}" \
    "CLEANUP_SHARED_PCT_RECLAIM=${SHARED_PCT_RECLAIM}"

sleep 30    # give the app time to create its work area before the first sweep

while true; do
    # re-resolve every sweep: the app dir may not exist yet at container start
    if app=$(resolve_app_name); then sweep "${app}"; fi
    sleep "${INTERVAL}"
done
