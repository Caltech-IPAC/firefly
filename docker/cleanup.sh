#!/bin/bash
#
# Cleanup script for the Firefly app.
#
# Runs forever: every CLEANUP_INTERVAL it sweeps both base work areas, then sleeps.
# Sweeps append to /firefly/logs/cleanup/<app>/cleanup.<YYYYMMDD>.log, one file per day,
# rolling at midnight and keeping CLEANUP_LOG_KEEP days.  It sits outside the work areas
# so cleanup never competes with its own record.
#
# Each base dir holds one sub-directory per application, e.g.
#     /firefly/workarea/<app>/temp_files
# where <app> is the servlet display-name.  This script sweeps exactly one app, so a
# container running several never has one app's cleanup touch another's files.  See
# CLEANUP_APP_NAME below.  The app dir is cleaned in one of two modes:
#
#   age mode (default)  Each entry below a base dir has its own maximum age; files not
#                       *modified* within that age are removed.
#   pct-full mode       Enabled by setting the base dir's *_PCT_FULL to 1-99.  Per-entry
#                       ages are ignored; nothing is removed until the filesystem is at
#                       least PCT_FULL percent full, then the oldest files are removed
#                       until usage drops to (PCT_FULL - PCT_DELTA).
#
# ------------------------------------------------------------------------------------
# Environment variables (all optional)
#
#   Name                              Default  Description
#   --------------------------------  -------  ----------------------------------------
#   CLEANUP_APP_NAME                  auto     app dir to sweep under both base dirs;
#                                              unset uses the first dir under the
#                                              workarea; set it when >1 app is served
#   CLEANUP_INTERVAL                  1h       how often a sweep runs
#   CLEANUP_LOG_KEEP                  7        days of logs kept (in /firefly/logs/cleanup)
#   CLEANUP_DRY_RUN                   false    log what would be removed, remove nothing
#
#   age mode -- maximum age per directory
#   CLEANUP_AGE_TEMP_FILES            1h       everything except; Hips, stage, upload
#                                              (defaults to CLEANUP_INTERVAL)
#   CLEANUP_AGE_HIPS                  90d      HiPS
#   CLEANUP_AGE_STAGE                 7d       stage
#   CLEANUP_AGE_UPLOAD                7d       upload
#
#   pct-full mode -- applies to both base dirs, each against its own filesystem
#   CLEANUP_PCT_FULL                  0        1-99 switches to pct-full mode; 0 = off
#   CLEANUP_PCT_DELTA                 20       percent below PCT_FULL to clean down to
#
# Ages are <n>m, <n>h or <n>d (minutes, hours, days).  An invalid value falls back to
# the default with a warning.
#
# Notes:
#   - Age is modification time.
#   - Only the entries listed below are ever touched, and only under this app's dir;
#     anything else under a base dir is left alone, in both modes.
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

# CLEANUP_APP_NAME: Unset, it falls back to the first dir under the workarea (sorted, so the choice is
# stable across sweeps).  That is right for the usual one-app container; set
# CLEANUP_APP_NAME when a container serves several applications.
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
# Mode: age -- each entry expires on its own -mmin
# ------------------------------------------------------------------------------------

clean_by_age() {
    local base="$1" app="$2"; shift 2
    local spec entry env_var default minutes paths

    echo "Age mode: removing files not modified within each entry's max age"
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
# Mode: pct-full -- ignore ages, remove the oldest files until there is room
# ------------------------------------------------------------------------------------

clean_by_pct_full() {
    local base="$1" app="$2" pct_full="$3" pct_delta="$4"; shift 4
    local spec entry paths=() entry_paths_out
    local total_kb used_kb used_pct target_pct need_kb freed_kb=0 removed=0
    local mtime kb path

    read -r total_kb used_kb < <(df -Pk "${base}" | awk 'NR==2 {print $2, $3}')
    if ! [[ "${total_kb:-0}" =~ ^[1-9][0-9]*$ ]]; then
        warn "cannot read disk usage for ${base}; skipping"
        return
    fi
    used_pct=$((used_kb * 100 / total_kb))
    target_pct=$((pct_full - pct_delta))
    ((target_pct < 0)) && target_pct=0      # delta larger than pct_full means "empty it"

    echo "Pct-full mode: ${used_pct}% used, cleans above ${pct_full}%, target ${target_pct}%"
    if ((used_pct < pct_full)); then
        echo "Nothing to do -- below ${pct_full}%"
        return
    fi

    need_kb=$((used_kb - total_kb * target_pct / 100))
    echo "Need to free ${need_kb} KB; removing oldest files first"
    echo

    for spec in "$@"; do
        IFS='|' read -r entry _ _ <<< "${spec}"
        readarray -t entry_paths_out < <(entry_paths "${base}" "${app}" "${entry}")
        ((${#entry_paths_out[@]})) && paths+=("${entry_paths_out[@]}")
    done
    ((${#paths[@]})) || { warn "no entries exist under ${base}/${app}; nothing can be freed"; return; }

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
    ((freed_kb < need_kb)) && warn "${base} is still above ${target_pct}%: nothing left to remove"
    [[ "${DRY_RUN}" == true ]] || prune_empty_dirs "${paths[@]}"
    return 0
}

# ------------------------------------------------------------------------------------
# Per base dir driver
# ------------------------------------------------------------------------------------

# clean_base <label> <baseDir> <app> <pctFull> <pctDelta> <entry>...
# Writes to stdout; sweep() redirects one log file around both calls.
clean_base() {
    local label="$1" base="$2" app="$3" pct_full="$4" pct_delta="$5"; shift 5
    local app_dir="${base}/${app}"

    echo
    echo "----- ${label}: ${app_dir}"
    [[ -d "${app_dir}" ]] || { echo "does not exist, skipping"; return; }
    if ((pct_full > 0)); then
        clean_by_pct_full "${base}" "${app}" "${pct_full}" "${pct_delta}" "$@"
    else
        clean_by_age "${base}" "${app}" "$@"
    fi
}

# The work of one sweep, written to stdout for sweep() to place
sweep_body() {
    local app="$1"
    echo
    echo "===== cleanup start -- app '${app}' -- $(date) ====="
    [[ "${DRY_RUN}" == true ]] && echo "DRY RUN -- nothing will be removed"
    clean_base "workarea" "${WORKAREA_DIR}" "${app}" \
               "${PCT_FULL}" "${PCT_DELTA}" "${WORKAREA_ENTRIES[@]}"
    clean_base "shared-workarea" "${SHARED_WORKAREA_DIR}" "${app}" \
               "${PCT_FULL}" "${PCT_DELTA}" "${SHARED_WORKAREA_ENTRIES[@]}"
    echo
    echo "===== cleanup done -- $(date) ====="
}

# One pass over both base dirs, appended to today's log.  The name carries the date,
# so the first sweep after midnight rolls onto a new file by itself.  If the log cannot
# be written, clean anyway and fall back to stdout -- a full disk is the worse failure.
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

readonly PCT_FULL=$(int_setting CLEANUP_PCT_FULL 0 0 99)
readonly PCT_DELTA=$(int_setting CLEANUP_PCT_DELTA 20 1 99)

describe_mode() {   # <pctFull> <pctDelta>
    local target=$(($1 - $2))
    ((target < 0)) && target=0
    if (($1 > 0)); then echo "pct-full (clean above $1%, down to ${target}%)"
    else echo "age (per-entry max age)"; fi
}

echo "Cleanup: started; interval ${INTERVAL}, keeping ${LOG_KEEP} days of logs, dry run ${DRY_RUN}"
echo "Cleanup:   app -> ${CLEANUP_APP_NAME:-<first dir under ${WORKAREA_DIR}>}"
echo "Cleanup:   mode -> $(describe_mode "${PCT_FULL}" "${PCT_DELTA}")"

while true; do
    # re-resolve every sweep: the app dir may not exist yet at container start
    if app=$(resolve_app_name); then sweep "${app}"; fi
    sleep "${INTERVAL}"
done
