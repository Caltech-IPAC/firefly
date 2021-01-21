#!/bin/bash
#
# Cleanup script for Firefly app

# /usr/bin/find if testing locally
find_cmd="/usr/bin/find"
echo "cleanup: started"

function doCleanup() {
    workarea="${1}"

    # find cleanup logs less than 23 hours old
    recent_cleanup=`${find_cmd} "${workarea}/cleanup_logs" -type f -mmin -$((60*23))`

    if [[ -z "${recent_cleanup}" ]] ; then

        timestamp=$(date +20%y%m%dT%H%M%S)
        log_file="${workarea}/cleanup_logs/cleanup.${timestamp}.log"

        echo "Cleaning up old work files in ${workarea}" > "${log_file}" 2>&1
        ${find_cmd} "${workarea}/stage" -type f -mtime +7 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        # master catalogs should be cleared more often than other files
        ${find_cmd} "${workarea}/perm_files" -name 'master*' -mtime +1 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        ${find_cmd} "${workarea}/perm_files" -type f -mtime +7 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        ${find_cmd} "${workarea}/temp_files" -type f -mtime +1 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        ${find_cmd} "${workarea}/visualize/fits-cache" -type f -mtime +7 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        if [ -d "${workarea}/visualize/users" ]; then
            ${find_cmd} "${workarea}/visualize/users" -type f -mtime +1 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        fi

        ${find_cmd} "${workarea}/upload" -type f -mtime +7 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1
        ${find_cmd} "${workarea}/HiPS" -type f -mtime +90 -exec /bin/rm '{}' \+ -print >> "${log_file}" 2>&1

        dirs_to_clear=("${workarea}/visualize/users" "${workarea}/temp_files")
        for d in ${dirs_to_clear[@]}; do
            if [ -d "${d}" ]; then
                # remove empty directories excluding those at the starting level
                ${find_cmd} "${d}" -mindepth 1 -depth -type d -empty -print -exec /bin/rmdir '{}' \; >> "${log_file}" 2>&1
            fi
        done

    fi
}

sleep 24h

while true; do
    # Remove temporary products for each Firefly workarea
    for workarea_dir in "$@"; do
        app_dirs=`find ${workarea_dir} -mindepth 1 -type d -prune`
        for app_dir in ${app_dirs}; do
            log_dir="${app_dir}/cleanup_logs"
            if [ ! -d "${log_dir}" ]; then
                mkdir -p "${log_dir}"
            fi
            # Remove old cleanup log files
            ${find_cmd} "${log_dir}" -type f -mtime +10 -exec /bin/rm '{}' \+

            if [ -d "${workarea_dir}" ]; then
                doCleanup "${app_dir}"
            fi
        done
    done

    sleep 24h
done