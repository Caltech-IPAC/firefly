#!/bin/bash

DEF_JVM_ROUTE=`hostname`
JVM_ROUTE=${jvmRoute:-$DEF_JVM_ROUTE}

echo "========== Information:  you can set properties using -e on docker run line =====  "
echo 
echo "Properties: "
echo "          Description                  Property             Value"
echo "          -----------                  --------             -----"
echo "          Min JVM size                 MIN_JVM_SIZE         ${MIN_JVM_SIZE}"
echo "          Min JVM size                 MAX_JVM_SIZE         ${MAX_JVM_SIZE}"
echo "          Multi node sticky routing    jvmRoute             ${JVM_ROUTE}"
echo "          Admin username               ADMIN_USER           ${ADMIN_USER}"
echo "          Admin password               ADMIN_PASSWORD       ${ADMIN_PASSWORD}"
echo "          Run tomcat with debug        DEBUG                ${DEBUG}"
echo "          Run tomcat with debug        LOG_FILE_TO_CONSOLE  ${LOG_FILE_TO_CONSOLE}"
echo "          Multi web app shared cache   SHARE_CACHE          ${SHARE_CACHE}"
echo "          Extra firefly properties     FIREFLY_OPTS         ${FIREFLY_OPTS}"
echo
echo "Ports: "
echo "        8080 - http"
echo "        5050 - debug"
echo "        9050 - jmx (jconsole)"
echo
echo "Command line options: "
echo "        --help  : show help message and stop"
echo "        --debug : start in debug mode"


userFile=/usr/local/tomcat/conf/tomcat-users.xml
sed "s/USER/${ADMIN_USER}/" ${userFile} | sed "s/PASSWORD/${ADMIN_PASSWORD}/" > u.tmp
mv u.tmp ${userFile}

if [ "x$LOG_FILE_TO_CONSOLE" != "x" ]; then
     logFile=/usr/local/tomcat/logs/${LOG_FILE_TO_CONSOLE}
     touch $logFile
     tail -f $logFile &
fi

if [ "$1" = "--help" ] || [ "$1" = "-help" ] || [ "$1" = "-h" ]; then
    echo
	echo "examples:"
	echo '        docker run -p 8055:8080 -p 5050:5050 -p 9050:9050 -e "MAX_JVM_SIZE=4G" -e "ADMIN_PASSWORD=sam" -e "LOG_FILE_TO_CONSOLE=firefly.log"  --rm --name firefly ipac/firefly'
    echo
	echo "or:"
	echo '        docker run -p 8055:8080 -p 5050:5050 -p 9050:9050 -e "MAX_JVM_SIZE=8G" -e "ADMIN_PASSWORD=sam" -e "LOG_FILE_TO_CONSOLE=firefly.log"  --rm --name aServer ipac/firefly'
    echo
	echo "or (production like):"
	echo '        docker run -p 8055:8080 -p 9050:9050 -e "MAX_JVM_SIZE=30G" -e "ADMIN_PASSWORD=sam" -e SHARE_CACHE="TRUE" -e DEBUG="FALSE" -e "jvmRoute=MyHostName" --name productionServer ipac/firefly'
    exit 0
fi
echo "========================================================================="


if [ "$SHARE_CACHE" = "true" ] ||[ "$SHARE_CACHE" = "t" ] ||[ "$SHARE_CACHE" = "1" ] ||  \
   [ "$SHARE_CACHE" = "TRUE" ] || [ "$SHARE_CACHE" = "True" ] ; then
   ./setupSharedCacheJars.sh
fi


if [ "$DEBUG" = "true" ] ||[ "$DEBUG" = "t" ] ||[ "$DEBUG" = "1" ] ||  \
   [ "$DEBUG" = "TRUE" ] || [ "$DEBUG" = "True" ] || [ "$1" = "--debug" ]; then
     exec /usr/local/tomcat/bin/catalina.sh jpda run
else
     exec /usr/local/tomcat/bin/catalina.sh run
fi
