#!/bin/bash
#
# send the important log files to the console.
# pause for a few seconds, the find the log file names and do a tail -f
#
sleepTime=15s
echo "sendLogsToConsole: started: sleeping ${sleepTime}"
sleep ${sleepTime}
ALL_LOGS="${CATALINA_HOME}/logs/*[a-zA-Z].log ${CATALINA_HOME}/logs/*.out"
IFS=" " read -r -a logArray <<< "$(echo ${ALL_LOGS})"
namelist=' '
for element in "${logArray[@]}"
do
    namelist="$(basename -- ${element}) ${namelist}"
done
echo -e '\n\n>>>>>>>>> sendLogsToConsole: Tailing logs: ' "${namelist}" '\n'
tail -c +0 -f ${ALL_LOGS}


