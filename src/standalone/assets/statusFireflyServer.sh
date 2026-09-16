#!/bin/bash

# --------------------------
# show status from Firefly server
# --------------------------

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
fireflyDir="${HOME}/.firefly"
applicationDir="${INSTALL_DIR}/application/current"
configJsonFile="$fireflyDir/config.json"
binDir="${INSTALL_DIR}/bin"
portFile="$fireflyDir/port.txt"
JQ=$(which jq || echo "$binDir/jq")

if [[ -f "$portFile" && -s "$portFile" && -r "$portFile" ]]; then
    ffPort=$(cat "$portFile");
fi
if [ -f "$configJsonFile" ]; then
   redisPort=$($JQ -r ".ports.redis" "$configJsonFile" 2> /dev/null)
  if [[ -z "$ffPort" ]]; then
     ffPort=$($JQ -r ".ports.firefly" "$configJsonFile" 2> /dev/null)
  fi
fi

if [[ "$1" == "help" || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "show if the firefly server is running and healthy"
else
    if [[ -z "$ffPort" ]]; then
         echo "unknown server port, could not find status"
         exit 0
    fi
    up=$(curl -sD - -o /dev/null http://localhost:"$ffPort"/firefly/healthz | head -1)
    if [[ "$up" == *200* ]]; then
        JAVA=$("$applicationDir"/javaInstaller.sh)
        if [[ -n "$JAVA" ]]; then
          jVersion=$("$JAVA" --version | head -n 1)
        else
          jVersion="unknown"
        fi
        if [[ -f "$fireflyDir/pid.txt" && -r "$fireflyDir/pid.txt" ]]; then
          pid=$(cat "$fireflyDir/pid.txt")
        else
          pid="unknown"
        fi
        if [[ -f "$fireflyDir/version.txt" && -r "$fireflyDir/version.txt" ]]; then
          fireflyVersionStr=$(cat "$fireflyDir/version.txt")
        else
          fireflyVersionStr="unknown"
        fi
        echo "---"
        echo "Firefly server:  running and healthy (pid $pid)"
        echo "Firefly Version: $fireflyVersionStr"
        echo "Java Version:    $jVersion"
        echo "Ports:           firefly $ffPort, redis $redisPort, debug (if started with -d) 5005"
        echo "Work dir:        $fireflyDir/server/workarea"
        echo "Log dir:         $fireflyDir/server/logs"
    elif [[ "$up" == "" ]]; then
        echo "The Firefly server is not running"
    else
        echo "The Firefly server is return an error: $up"
    fi
fi
