#!/bin/bash

NAME=${BUILD_TIME_NAME:-"ipac/firefly"}
if [ -z "${MAX_JVM_SIZE}" ]; then
  MM_NOT_USED="not used"
  MIN_JVM_SIZE=
else
  PER_NOT_USED="not used"
  INIT_RAM_PERCENT=
  MAX_RAM_PERCENT=
fi

echo -e "\n!!============================================================"
echo "!!============================================================"
echo "!!==================== For Help =============================="
echo "!!"
echo "        docker run --rm ${NAME}:latest --help"
echo "!!"
echo "!!============================================================"
echo "!!============================================================"
echo -e "!!============================================================\n\n"

echo "========== Information:  you can set properties using -e on docker run line =====  "
echo 
echo "Properties: "
echo "        Description                  Property                 Value"
echo "        -----------                  --------                 -----"
echo "        Min JVM size (*)             MIN_JVM_SIZE            ${MIN_JVM_SIZE}${MM_NOT_USED}"
echo "        Max JVM size (*)             MAX_JVM_SIZE            ${MAX_JVM_SIZE}${MM_NOT_USED}"
echo "        Initial JVM size (**)        INIT_RAM_PERCENT        ${INIT_RAM_PERCENT}${PER_NOT_USED}"
echo "        Max JVM size (**)            MAX_RAM_PERCENT         ${MAX_RAM_PERCENT}${PER_NOT_USED}"
echo "        CPU cores (0 means guess)    JVM_CORES               ${JVM_CORES}"
echo "        Admin username               ADMIN_USER              ${ADMIN_USER}"
echo "        Admin password               ADMIN_PASSWORD          ${ADMIN_PASSWORD}"
echo "        Run tomcat with debug        DEBUG                   ${DEBUG}"
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
echo "        Log directory : ${CATALINA_HOME}/logs : Directory for logs files"
echo "        Local Images  : /local/data : Root directory for images and tables that firefly can read"
echo
echo "Command line options: "
echo "        --help  : show help message, examples, stop"
echo "        --debug : start in debug mode"
echo -e "\n"


sed "s/USER/${ADMIN_USER}/" ${CATALINA_HOME}/conf/tomcat-users.xml.in | sed "s/PASSWORD/${ADMIN_PASSWORD}/" > ${CATALINA_HOME}/conf/tomcat-users.xml

#------------ if we are doing firefly.jar setup examples in the local/www directory
./setupFireflyExample.sh

if [ "$1" = "--help" ] || [ "$1" = "-help" ] || [ "$1" = "-h" ]; then
    sed "s:ipac/firefly:${NAME}:" ./start-examples.txt
    aWarFile=`ls ${CATALINA_HOME}/webapps/*.war | head -1 | awk '{print $1}'`
    onlyWar=`echo ${aWarFile} | awk -F/ '{print $NF}'`
    if [ "$onlyWar" = "firefly.war" ]; then
        cat ./customize-firefly.txt
    fi
    exit 0
fi

if [ "x$FIREFLY_SHARED_WORK_DIR" != "x" ]; then
      mkdir -p ${FIREFLY_SHARED_WORK_DIR}
fi


if [ -z ${MAX_JVM_SIZE} ]; then
   JVM_SIZING="-XX:InitialRAMPercentage=${INIT_RAM_PERCENT} -XX:MaxRAMPercentage=${MAX_RAM_PERCENT}"
else
   JVM_SIZING="-Xms${MIN_JVM_SIZE} -Xmx${MAX_JVM_SIZE}"
fi

#---------   CATALINA_OPTS must be exported for catalina.sh to pick them up
export CATALINA_OPTS="\
        ${JVM_SIZING} \
        -Dserver.cores=${JVM_CORES} \
        -Djava.net.preferIPv4Stack=true \
        -Dnet.sf.ehcache.enableShutdownHook=true \
        -Dserver_config_dir=${SERVER_CONFIG_DIR} \
        -Dwork.directory=${FIREFLY_WORK_DIR} \
        -Dshared.work.directory=${FIREFLY_SHARED_WORK_DIR} \
        -Dvisualize.fits.search.path=${VISUALIZE_FITS_SEARCH_PATH} \
	   ${FIREFLY_OPTS}"

#------- start background scripts: cleanup and log to console
${CATALINA_HOME}/cleanup.sh ${FIREFLY_WORK_DIR} ${FIREFLY_SHARED_WORK_DIR} &
${CATALINA_HOME}/sendLogsToConsole.sh &

echo "launchTomcat.sh: Starting Tomcat"
if [ "$DEBUG" = "true" ] ||[ "$DEBUG" = "t" ] ||[ "$DEBUG" = "1" ] ||  \
   [ "$DEBUG" = "TRUE" ] || [ "$DEBUG" = "True" ] || [ "$1" = "--debug" ]; then
    exec ${CATALINA_HOME}/bin/catalina.sh jpda ${START_MODE}
else
    exec ${CATALINA_HOME}/bin/catalina.sh ${START_MODE}
fi
