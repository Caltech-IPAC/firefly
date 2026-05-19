#!/bin/bash


cleanupMinutes=1440
logfileMaxAge=7200
rm_cmd=/bin/rm
rm_cmd="echo will remove:"
workarea_dir="${HOME}/.firefly/server/workarea/firefly"
shared_workarea="${HOME}/.firefly/server/shared-workarea"
find_cmd="/usr/bin/find"

doCleanup() {
    workarea="${1}"
    log_dir="${workarea}/cleanup_logs"
    mkdir -p "${log_dir}"

    # cleanup old log files
    ${find_cmd} "${log_dir}" -type f -mmin +${logfileMaxAge} -exec $rm_cmd '{}' \+

    # cleanup old work files
    timestamp=$(date +20%y%m%dT%H%M%S)
    log_file="${log_dir}/cleanup.${timestamp}.log"
    clean_dirs=("${workarea}/temp_files" "${workarea}/visualize/fits-cache" "${workarea}/visualize/users")
    dirs_to_clear=("${workarea}/visualize/users" "${workarea}/temp_files")
    echo "Cleanup: " $workarea
    echo 'Cleanup: log file: ' "${log_file}"
    {
        echo "Cleaning up work files older that ${cleanupMinutes} minutes, dir: ${workarea}"
        [[ -d "${workarea}/HiPS" ]] && ${find_cmd} "${workarea}/HiPS" -type f -mtime +90 -exec $rm_cmd '{}' \+ -print
        [[ -d "${workarea}/stage" ]] && ${find_cmd} "${workarea}/stage" -type f -mtime +7 -exec $rm_cmd '{}' \+ -print
        [[ -d "${workarea}/upload" ]] && ${find_cmd} "${workarea}/upload" -type f -mtime +7 -exec $rm_cmd '{}' \+ -print
        [[ -d "${workarea}/perm_files" ]] && ${find_cmd} "${workarea}/perm_files" -type f -atime +1 -exec $rm_cmd '{}' \+ -print
        for dir in "${clean_dirs[@]}"; do
           if [ -d "${dir}" ]; then
              ${find_cmd} "${dir}" -type f -amin +${cleanupMinutes} -exec $rm_cmd '{}' \+ -print
           fi
        done
        for dir in "${dirs_to_clear[@]}"; do    # remove empty directories excluding those at the starting level
           [[ -d "${dir}" ]] && ${find_cmd} "${dir}" -mindepth 1 -depth -type d -empty -print -exec $rm_cmd '{}' \;
        done
    } > "${log_file}" 2>&1
}


# Remove temporary products for each Firefly workarea
# Find app directories (should be only one, but loop for safety)
doCleanup "${workarea_dir}"

