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
echo "        Description                  Property                 Value"
echo "        -----------                  --------                 -----"
echo "        Min JVM size (*)             MIN_JVM_SIZE            ${MIN_JVM_SIZE}"
echo "        Max JVM size (*)             MAX_JVM_SIZE            ${MAX_JVM_SIZE}"
echo "        Initial JVM size (**)        INIT_RAM_PERCENT        ${INIT_RAM_PERCENT}"
echo "        Max JVM size (**)            MAX_RAM_PERCENT         ${MAX_RAM_PERCENT}"
echo "        CPU cores (0 means guess)    JVM_CORES               ${JVM_CORES}"
echo "        Multi node sticky routing    jvmRoute                ${JVM_ROUTE}"
echo "        Admin username               ADMIN_USER              ${ADMIN_USER}"
echo "        Admin password               ADMIN_PASSWORD          ${ADMIN_PASSWORD}"
echo "        Run tomcat with debug        DEBUG                   ${DEBUG}"
echo "        An addition log file         LOG_FILE_TO_CONSOLE     ${LOG_FILE_TO_CONSOLE}"
echo "        Tomcat Manager available     MANAGER                 ${MANAGER}"
echo "        Multi web app shared cache   SHARE_CACHE             ${SHARE_CACHE}"
echo "        Extra firefly properties     FIREFLY_OPTS            ${FIREFLY_OPTS}"
echo "        Shared work Area             FIREFLY_SHARED_WORK_DIR ${FIREFLY_SHARED_WORK_DIR}"
echo "(*)  If MAX_JVM_SIZE is blank, autosizing properties INIT_RAM_PERCENT and MAX_RAM_PERCENT are used instead"
echo "(**) Autosizing properties INIT_RAM_PERCENT and MAX_RAM_PERCENT are not used if MAX_JVM_SIZE is set"
echo
echo "Ports: "
echo "        8080 - http"
echo "        5050 - debug"
echo
echo "Volume Mount Points: "
echo "        Log directory : ${CATALINA_BASE}/logs : Directory for logs files"
echo "        Local Images  : /local/data : Root directory for images and tables that firefly can read"
echo
echo "Command line options: "
echo "        --help  : show help message, examples, stop"
echo "        --debug : start in debug mode"


sed "s/USER/${ADMIN_USER}/" ${CATALINA_BASE}/conf/tomcat-users.xml.in | sed "s/PASSWORD/${ADMIN_PASSWORD}/" > ${CATALINA_BASE}/conf/tomcat-users.xml

if [ "x$LOG_FILE_TO_CONSOLE" != "x" ]; then
     logFile=${CATALINA_BASE}/logs/${LOG_FILE_TO_CONSOLE}
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

if [ "$MANAGER" = "true" ] || [ "$MANAGER" = "t" ] || [ "$MANAGER" = "1" ] ||  \
   [ "$MANAGER" = "TRUE" ] || [ "$MANAGER" = "True" ] ; then
   cp -r ${CATALINA_HOME}/webapps/manager ${CATALINA_BASE}/webapps/
   mkdir -p ${CATALINA_BASE}/conf/Catalina/localhost
   sed "s/<Context>/<Context privileged='true'>/" ${CATALINA_BASE}/conf/context.xml > ${CATALINA_BASE}/conf/Catalina/localhost/manager.xml

fi

if [ "x$FIREFLY_SHARED_WORK_DIR" != "x" ]; then
      mkdir -p ${FIREFLY_SHARED_WORK_DIR}
fi

# run cleanup script in the background
${CATALINA_BASE}/cleanup.sh ${FIREFLY_WORK_DIR} ${FIREFLY_SHARED_WORK_DIR} &

if [ "$DEBUG" = "true" ] ||[ "$DEBUG" = "t" ] ||[ "$DEBUG" = "1" ] ||  \
   [ "$DEBUG" = "TRUE" ] || [ "$DEBUG" = "True" ] || [ "$1" = "--debug" ]; then
     exec ${CATALINA_HOME}/bin/catalina.sh jpda run
else
     exec ${CATALINA_HOME}/bin/catalina.sh run
fi
