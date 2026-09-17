#!/bin/bash

# --------------------------
# stop the Firefly server
# --------------------------

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
source "$SCRIPT_DIR/common.sh"

# --------------------------
# stopOrphans: kill any firefly/redis-server process bound to the configured ports that
# isn't tracked by pid.txt. Normally the tracked Firefly process (and its embedded Redis
# child, via a JVM shutdown hook when Firefly receives a plain SIGTERM) are already
# stopped by the time this runs, so this is a no-op in the common case. It only matters
# if a previous run was killed with -9, crashed, or its pid file was lost/deleted.
# `pgrep -f` is available by default on both macOS and the documented Linux distros
# (it ships in the base procps/procps-ng package), never matches itself, and returns
# PIDs directly.
# --------------------------

stopOrphans() {
  if [ ! -f "$configJsonFile" ]; then
    return
  fi
  if [[ -n "$ffPort" ]]; then
    fireflyPids=$(pgrep -f "\-Dfirefly\.port=${ffPort}([^0-9]|\$).*FireflyApplication")
    for pid in $fireflyPids; do
      echo "Stopping orphaned firefly process at process id $pid"
      kill -9 "$pid" 2> /dev/null
    done
  fi
  if [[ -z "$redisPort" || "$redisPort" == "null" ]]; then
    return
  fi
  redisPids=$(pgrep -f "redis-server.*:${redisPort}([^0-9]|\$)")
  for pid in $redisPids; do
    echo "Stopping orphaned redis-server process at process id $pid"
    kill -9 "$pid" 2> /dev/null
  done
  /bin/rm -f "$fireflyDir/port.txt" "$fireflyDir/pid.txt"
}


if [[ "$1" == "help" || "$1" == "-h" || "$1" == "--help" ]]; then
    echo "stop the firefly server"
else
   if [[ -f "$pidFile" && -s "$pidFile" && -r "$pidFile" ]]; then
        pid=$(cat "$pidFile");
        kill "$pid"
        echo "Stopping firefly at process id $pid..."
        echo "Waiting for Firefly to stop..."
        sleep .25
        for i in {1..3}; do
          if ! kill -0 "$pid" 2> /dev/null; then
            break
          else
            sleep 1
          fi
        done
        if kill -0 "$pid" 2> /dev/null; then
          echo "Firefly did not stop after 3 seconds, forcing it to stop at process id $pid..."
          kill -9 "$pid"
        fi
        echo "Firefly stopped."
   else
        echo "Firefly is not running or pid file was lost"
   fi
   /bin/rm -f "$fireflyDir/ready-$ffPort.txt"
   stopOrphans
fi
