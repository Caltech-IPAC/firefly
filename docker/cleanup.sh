#!/bin/bash
#
# Cleanup script for Firefly app
# CLEANUP_INTERVAL = nUnit   # where n is a positive number and Unit is one of 'd, h, or m'
# Note:  files may stay around for up to 2x the value of CLEANUP_INTERVAL because it sleeps
#        for the same amount of time before checking for stale files.
DEFAULT_CLEANUP_INTERVAL=2d
find_cmd="/usr/bin/find"
allDigitsRE='^[0-9]+$'

if [ -z "${CLEANUP_INTERVAL}" ]; then
   CLEANUP_INTERVAL=${DEFAULT_CLEANUP_INTERVAL}
fi

function getMultiplier() { #return the minute multiplier the unit (h or m or d), 24h would return 60
    instr="${1}"
    unit=$(echo "${instr}" | cut -c"${#instr}"-"${#instr}")
    if [  "${unit}" == 'm' ]; then echo 1;
    elif [ "${unit}" == 'd'  ]; then echo 1440
    elif [ "${unit}" == 'h'  ]; then echo 60
    else echo 0
    fi
}

function getInterval() { # get the interval without the unit i.e. "24h" would return a 24
    instr="${1}"
    echo "${instr}" | cut -c-$((${#instr}-1))
}

interval=$(getInterval "${CLEANUP_INTERVAL}")
mult=$(getMultiplier "${CLEANUP_INTERVAL}")
if  ! [[ "${interval}" =~ $allDigitsRE ]] || [ "$mult" -eq 0 ]; then  # check for an invalid CLEANUP_INTERVAL
   echo "CLEANUP_INTERVAL of ${CLEANUP_INTERVAL} is invalid, must be number and m (minutes), h (hours) or d (days)"
   echo '     Examples: 4h or 720m or 1d or 5d'
   echo "     Setting CLEANUP_INTERVAL to 2 days (2d)"
   CLEANUP_INTERVAL=${DEFAULT_CLEANUP_INTERVAL}
   interval=$(getInterval "${DEFAULT_CLEANUP_INTERVAL}")
   mult=$(getMultiplier "${DEFAULT_CLEANUP_INTERVAL}")
fi

cleanupMinutes=$((interval*mult))
logfileMaxAge=$((cleanupMinutes*10+5))

function doCleanup() {
    workarea="${1}"
    log_dir="${workarea}/cleanup_logs"
    mkdir -p "${log_dir}"

    # cleanup old log files
    ${find_cmd} "${log_dir}" -type f -mmin +${logfileMaxAge} -exec /bin/rm '{}' \+

    # cleanup old work files
    timestamp=$(date +20%y%m%dT%H%M%S)
    log_file="${log_dir}/cleanup.${timestamp}.log"
    clean_dirs=("${workarea}/temp_files" "${workarea}/visualize/fits-cache" "${workarea}/visualize/users")
    dirs_to_clear=("${workarea}/visualize/users" "${workarea}/temp_files")
    echo 'Cleanup, log_file: ' "${log_file}"
    {
        echo "Cleaning up work files older that ${cleanupMinutes} minutes, dir: ${workarea}"
        [[ -d "${workarea}/HiPS" ]] && ${find_cmd} "${workarea}/HiPS" -type f -mtime +90 -exec /bin/rm '{}' \+ -print
        [[ -d "${workarea}/stage" ]] && ${find_cmd} "${workarea}/stage" -type f -mtime +7 -exec /bin/rm '{}' \+ -print
        [[ -d "${workarea}/upload" ]] && ${find_cmd} "${workarea}/upload" -type f -mtime +7 -exec /bin/rm '{}' \+ -print
        [[ -d "${workarea}/perm_files" ]] && ${find_cmd} "${workarea}/perm_files" -type f -atime +1 -exec /bin/rm '{}' \+ -print
        for dir in "${clean_dirs[@]}"; do
           if [ -d "${dir}" ]; then
              ${find_cmd} "${dir}" -type f -amin +${cleanupMinutes} -exec /bin/rm '{}' \+ -print
           fi
        done
        for dir in "${dirs_to_clear[@]}"; do    # remove empty directories excluding those at the starting level
           [[ -d "${dir}" ]] && ${find_cmd} "${dir}" -mindepth 1 -depth -type d -empty -print -exec /bin/rmdir '{}' \;
        done
     } > "${log_file}" 2>&1
}

sleepTime="$((cleanupMinutes+1))m"
echo "Cleanup: started, interval time:" ${CLEANUP_INTERVAL}, "(${cleanupMinutes} minutes), sleepTime: ${sleepTime}"

# Predefined directories
workarea_dir="/firefly/workarea"
shared_workarea="/firefly/shared-workarea"

while true; do
    # Remove temporary products for each Firefly workarea
    sleep ${sleepTime}

    # Find app directories (should be only one, but loop for safety)
    app_workdirs=$(${find_cmd} "${workarea_dir}" -mindepth 1 -type d -prune)

    for work_dir in ${app_workdirs}; do
        doCleanup "${work_dir}"

        # Handle matching shared-workarea directory, if it exists
        app_name="${work_dir#${workarea_dir}/}"
        shared_work_dir="${shared_workarea}/${app_name}"
        if [ -d "${shared_work_dir}" ]; then
            doCleanup "${shared_work_dir}"
        fi

    done
done