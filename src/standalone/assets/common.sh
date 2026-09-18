# --------------------------
# common.sh: shared variables for the Firefly standalone runtime scripts.
# Must be sourced with $INSTALL_DIR already set (every caller needs it anyway to
# locate this file). Defines: applicationDir, fireflyDir, binDir, configJsonFile,
# JQ, pidFile, portFile, redisDbDir, ffPort, redisPort.
# --------------------------

applicationDir="${INSTALL_DIR}/application/current"
fireflyDir="${HOME}/.firefly"
binDir="${INSTALL_DIR}/bin"
configJsonFile="$fireflyDir/config.json"
pidFile="$fireflyDir/pid.txt"
portFile="$fireflyDir/port.txt"
redisDbDir="${fireflyDir}/server/temp/redis"
JQ=$(which jq || echo "$binDir/jq")

if [[ -f "$portFile" && -s "$portFile" && -r "$portFile" ]]; then
    ffPort=$(cat "$portFile")
fi
if [ -f "$configJsonFile" ]; then
   redisPort=$($JQ -r ".ports.redis" "$configJsonFile" 2> /dev/null)
   if [[ -z "$ffPort" ]]; then
      ffPort=$($JQ -r ".ports.firefly" "$configJsonFile" 2> /dev/null)
   fi
fi
