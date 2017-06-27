#!/bin/bash

DEF_JVM_ROUTE=`hostname`
JVM_ROUTE=${jvmRoute:-$DEF_JVM_ROUTE}


if [ "x$BUILD_TIME_NAME" != "x" ]; then
   NAME=${BUILD_TIME_NAME}
else
   NAME="ipac/firefly"
fi


echo
echo
echo "============================================================"
echo "============================================================"
echo "==================== For Help =============================="
echo
echo "docker run --rm ${NAME} --help"
echo
echo "============================================================"
echo "============================================================"
echo "============================================================"
echo
echo
echo
echo

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
echo "Volume Mount Points: "
echo "        Log directory : /usr/local/tomcat/logs : Directory for logs files"
echo "        Local Images  : /local/data : Root directory for images and tables that firefly can read"
echo
echo "Command line options: "
echo "        --help  : show help message, examples, stop"
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
    sed "s:ipac/firefly:${NAME}:" ./start-examples.txt
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
