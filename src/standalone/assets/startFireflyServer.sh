#!/bin/bash

# --------------------------
# Start the Firefly server
# --------------------------

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
INSTALL_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)
fireflyDir="${HOME}/.firefly"
fireflyServer="${HOME}/.firefly/server"
applicationDir="${INSTALL_DIR}/application/current"
appNew="${INSTALL_DIR}/application/new"
applicationJars="${applicationDir}/jars"
appLog="${fireflyServer}/logs/application.log"
userOpsFile="${fireflyDir}/user_ops.sh"
configJsonFile="$fireflyDir/config.json"
ADMIN_USER="admin"
ADMIN_PASSWORD="admin"
MIN_JVM_SIZE=1G
MAX_JVM_SIZE=10G
binDir="${INSTALL_DIR}/bin"
JQ=$(which jq || echo "$binDir/jq")

# todo - i think we can remove serverConfigDir
serverConfigDir="${HOME}/config"

# --------------------------
# isTrue: function to check if a variable is true or false
# --------------------------

isTrue() {
     v=$(echo "$1" | tr '[:upper:]' '[:lower:]')
     if [[ "$v" == "true" || "$v" == "t" ]]; then return 0; else return 1; fi
}


# --------------------------
# getFireflyStatusOnPort: function to call the firefly healthz endpoint to determine if the server is running
# returns:
#   - INUSE- port is in use
#   - UP- firefly server is using the port and is up
#   - FREE- the port is not being used
# --------------------------

getFireflyStatusOnPort() {
    curl --max-time 12 -sD - -o /dev/null http://localhost:$1/firefly/healthz > /tmp/fireflyStatusCheck.txt
    curlStat=$?
    if [ $curlStat -eq 28 ]; then
        echo "INUSE"
        return;
    else
        up=$(head -1 /tmp/fireflyStatusCheck.txt)
    fi

    if [[ "$up" == *200* ]]; then
      echo "UP"
    elif [[ "$up" == "" ]]; then
      echo "FREE"
    else
      echo "INUSE"
    fi
}

debugParams=
loggingLevel="INFO"
doClean="FALSE"
verbose="FALSE"
doExit="FALSE"
doHelp="FALSE"
firstInvalid="TRUE"
inBackground="TRUE"
overridePort=""
alreadyRunning="FALSE"
space="  "

# --------------------------
# evaluate command line arguments
# --------------------------

while [ $# -gt 0 ]; do
  arg="$1"
  if [[ "$arg" == "-d" || "$arg" == "--debug" || "$arg" == "-debug" ]]; then
      debugParams="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005"
  elif [ "$arg" == "--verbose" ]; then
       verbose="TRUE"
       loggingLevel="DEBUG"
  elif [ "$arg" == "--clean" ]; then
       doClean="TRUE"
  elif [ "$arg" == "--cleanAndExit" ]; then
       doClean="TRUE"
       doExit="TRUE"
  elif [[ "$arg" == "-f" || "$arg" == "--foreground" ]]; then
       inBackground="FALSE"
  elif [[ "$arg" == "--port"  ]]; then
    shift
     overridePort=$1
  elif [[ "$arg" == "--help" || "$arg" == "-h" ]]; then
      doHelp="TRUE"
      doExit="TRUE"
  else
      if isTrue $firstInvalid; then
         echo "Invalid arguments passed."
         firstInvalid="FALSE"
      fi
      echo "$space" "invalid argument:" "$arg"
      doHelp="TRUE"
  fi
  shift
done

# --------------------------
# Help: show command line options
# --------------------------

if isTrue $doHelp; then
  echo "Options:"
  echo "$space --debug, -debug, -d:  start and pause in java debug mode on port 5005"
  echo "$space --verbose:            more startup logging and set java log level to debug"
  echo "$space --clean:              clean work area before startup"
  echo "$space --cleanAndExit:       clean work only and exit"
  echo "$space -f, --foreground:     start in foreground (server starts in background by default)"
  echo "$space --port:               a port number to override the default firefly port, it can also be set in ~/.firefly/config.json"
  echo "$space --help, -h:           this message and exit"
  exit 0;
fi

# --------------------------
# clean up file system
# --------------------------

if isTrue $doClean; then
  /bin/rm -rf "${fireflyServer}/workarea"
  echo "removing: ${fireflyServer}/workarea"
  /bin/rm -rf "${fireflyServer}/temp"
  echo "removing: ${fireflyServer}/temp"
  /bin/rm -rf "${fireflyServer}/logs"
  echo "removing: ${fireflyServer}/logs"
  /bin/rm -rf "${fireflyServer}/work"
  echo "removing: ${fireflyServer}/work"
  if isTrue $doExit; then
    exit 0;
  fi
 fi

# --------------------------
# if we need to do an update then call update and exit
# --------------------------

if [[ -d "$appNew" && -f "$appNew/complete" ]]; then
  if [[ -f "$INSTALL_DIR/disableUpdate" ]]; then
     echo ">>>>>>>>> Update available but disabled"
  else
     echo ">>>>>>>>> updating..."
     exec "$appNew/../updater.sh"
  fi
fi

# --------------------------
# pull out jar files from firefly.war if necessary
# --------------------------

if [ ! -f "$applicationDir/jars/firefly.jar" ]; then
   FILES_FROM_WAR="WEB-INF/lib/firefly.jar WEB-INF/lib/json-simple-1.1.1.jar WEB-INF/config/version.tag"
   (cd $applicationDir && unzip -oj  firefly.war  ${FILES_FROM_WAR} )
fi

# --------------------------
# determine radis port
# --------------------------

redisPort=$($JQ -r ".ports.redis" "$configJsonFile")

if [[ $overridePort == "" ]]; then
   fireflyPort=$($JQ -r ".ports.firefly" "$configJsonFile")
else
   fireflyPort=$overridePort
fi


# --------------------------
# determine if firefly server port if available
# --------------------------

ffStat=$(getFireflyStatusOnPort "$fireflyPort")
if [[ $ffStat == "UP" ]]; then
    alreadyRunning="TRUE"
elif [[ $ffStat == "FREE" ]]; then
    alreadyRunning="FALSE"
elif [[ $ffStat == "INUSE" ]]; then
    echo "The port number $fireflyPort is being used by another application"
    echo "You can change the port my editing ~/.firefly/config.json or by using the --port parameter"
    exit 1;
fi

# --------------------------
# make directories
# --------------------------

[ ! -d "${applicationJars}" ] && mkdir "$applicationJars"
[ ! -d "${fireflyServer}/temp" ] &&  mkdir "${fireflyServer}/temp"
[ ! -d "${fireflyServer}/logs" ] && mkdir "${fireflyServer}/logs"
if isTrue $verbose; then
   echo  "$applicationDir/*.jar" to "$applicationJars"
fi
if ls $applicationDir/*.jar >/dev/null 2>&1; then
   /bin/mv $applicationDir/*.jar "$applicationJars"
fi


# --------------------------
# add any extra commandline options from ~/.firefly/user_ops.sh
# if should define JAVA_OPS
# --------------------------

JAVA_OPS=
if [ -f "$userOpsFile" ]; then
  source $userOpsFile
  if isTrue $verbose; then
    echo JAVA_OPS = $JAVA_OPS
  fi
fi

# --------------------------
# if on the mac, setup UI options
# --------------------------

name=$(uname)
if [[ "$name" == "Darwin" ]]; then
   splash="-splash:${applicationDir}/fireflySplash.png"
   nameParam='-Xdock:name=Firefly Server'
   runAsDesktopApplication="true"
   headless="false"
   #dockIcon="-Xdock:icon=${applicationDir}/fireflyDockIcon.png"  --- keep around if we decide to read dock
else
   splash=
   nameParam="-DdockPlaceHolder="
   runAsDesktopApplication="false"
   headless="true"
   #dockIcon=
fi


# --------------------------
# java command line options
# --------------------------


PROPS=" \
  -Dapple.awt.UIElement=true \
  -Xms${MIN_JVM_SIZE} -Xmx${MAX_JVM_SIZE} ${debugParams} \
  --add-opens java.base/java.util=ALL-UNNAMED \
  -XX:+UnlockExperimentalVMOptions \
  -XX:TrimNativeHeapInterval=30000 \
  -XX:+UseZGC \
  -Dnet.sf.ehcache.enableShutdownHook=true \
  -Dlogging.level=${loggingLevel} \
  -Djava.net.preferIPv4Stack=true \
  -Dwork.directory=${fireflyServer}/workarea \
  -DrunAsDesktopApplication=${runAsDesktopApplication} \
  -Djava.awt.headless=${headless} \
  -Dvisualize.fits.search.path=${HOME} \
  -Dredis.db.dir=${fireflyServer}/temp/redis \
  -Djava.io.tmpdir=${fireflyServer}/temp \
  -Dalerts.dir=${fireflyServer}/alerts \
  -Dserver_config_dir=${serverConfigDir} \
  -Dfirefly.port=${fireflyPort}
  -Dredis.port=${redisPort:-6379} \
  -DADMIN_USER=${ADMIN_USER} \
  -DADMIN_PASSWORD=${ADMIN_PASSWORD} \
  -DADMIN_PROTECTED= \
  -DuserHelpToLog=${inBackground} \
  ${JAVA_OPS}
  "

# --------------------------
# get the java command, install java if necessary
# --------------------------

JAVA=$("$applicationDir"/javaInstaller.sh)

# --------------------------
# setup classpath
# --------------------------

export CLASSPATH=${applicationJars}'/*'
if isTrue $verbose; then
  echo
  echo Using classpath
  echo $CLASSPATH
  echo
fi


# --------------------------
# start the firefly server using background mode (the default) or foreground
# background steps are more complex
#   - execute the java command in the background (with &)
#   - determine if firefly is already running
#   - determine the URL
#   - determine when the server is ready by monitoring ~/.firefly/ready-xxxx.txt
# --------------------------

readyFile="$fireflyDir/ready-${fireflyPort}.txt"
/bin/rm -f "$readyFile"
{
   echo "------------------------------------------------"
   echo "---------- Starting Firefly server"
   echo "---------- $(date)"
   echo "------------------------------------------------"
   echo ${JAVA} ${splash} "${nameParam}" ${PROPS} edu.caltech.ipac.app.FireflyApplication
} >> "$appLog"

if isTrue $inBackground; then
  (cd "$applicationDir" && ${JAVA} ${splash} "${nameParam}" ${PROPS} edu.caltech.ipac.app.FireflyApplication &> "${fireflyServer}/logs/backgroundStart.log" &)
  if isTrue $alreadyRunning; then
      echo "Firefly is already running on port"
  else
      echo "Firefly server starting in background (it takes a few seconds)..."
  fi

  echo
  echo "---------------------------------"
  echo "Firefly URL: http://localhost:$fireflyPort/firefly/"
  echo "---------------------------------"

  if ! isTrue $alreadyRunning; then
      echo -n "Firefly server waiting for init to complete..."
      ready=$(cat "$readyFile" 2> /dev/null)
      while ! isTrue $ready; do
         sleep .5
         ready=$(cat "$readyFile" 2> /dev/null)
      done
      echo "Ready"
      echo "to stop server: ${binDir#$PWD/}/ff stop"
  fi
else
  (cd "$applicationDir" && ${JAVA} ${splash} "${nameParam}" ${PROPS} edu.caltech.ipac.app.FireflyApplication )
fi

