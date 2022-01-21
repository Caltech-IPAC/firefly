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

echo "========== Information:  you can set environment variable using -e on docker run line =====  "
echo 
echo "Environment Variables:"
echo "        Description                      Name                       Value"
echo "        -----------                      --------                   -----"
echo "        Admin username                   ADMIN_USER                 ${ADMIN_USER}"
echo "        Admin password                   ADMIN_PASSWORD             ${ADMIN_PASSWORD}"
echo "        Additional data path             VISUALIZE_FITS_SEARCH_PATH ${VISUALIZE_FITS_SEARCH_PATH}"
echo "        Clean internal(eg- 720m, 5h, 3d) CLEANUP_INTERVAL           ${CLEANUP_INTERVAL}"
echo
echo "Advanced environment variables:"
echo "        Min JVM size (*)                 MIN_JVM_SIZE               ${MIN_JVM_SIZE}${MM_NOT_USED}"
echo "        Max JVM size (*)                 MAX_JVM_SIZE               ${MAX_JVM_SIZE}${MM_NOT_USED}"
echo "        Initial JVM size (**)            INIT_RAM_PERCENT           ${INIT_RAM_PERCENT}${PER_NOT_USED}"
echo "        Max JVM size (**)                MAX_RAM_PERCENT            ${MAX_RAM_PERCENT}${PER_NOT_USED}"
echo "        CPU cores (0 means guess)        JVM_CORES                  ${JVM_CORES}"
echo "        Run tomcat with debug            DEBUG                      ${DEBUG}"
echo "        Extra firefly properties         FIREFLY_OPTS               ${FIREFLY_OPTS}"
echo "  (*)  If MAX_JVM_SIZE is blank, autosizing properties INIT_RAM_PERCENT and MAX_RAM_PERCENT are used instead"
echo "  (**) Autosizing properties INIT_RAM_PERCENT and MAX_RAM_PERCENT are not used if MAX_JVM_SIZE is set"
echo
echo "Ports: "
echo "        8080 - http"
echo "        5050 - debug"
echo
echo "Volume Mount Points: "
echo "    /firefly/logs             : logs directory"
echo "    /firefly/workarea         : work area for temporary files"
echo "    /firefly/shared-workarea  : work area for files that are shared between multiple instances of the application"
echo "    /external                 : default external data directory visible to Firefly"
echo
echo "  Less used:"
echo "    /firefly/config           : used to override application properties"
echo "    /firefly/logs/statistics  : directory for statistics logs"
echo "    /firefly/alerts           : alerts monitor will watch this directory for application alerts"
echo
echo "Command line options: "
echo "        --help  : show help message, examples, stop"
echo "        --debug : start in debug mode"
echo -e "\n"


#sed "s/USER/${ADMIN_USER}/" ${CATALINA_HOME}/conf/tomcat-users.xml.in | sed "s/PASSWORD/${ADMIN_PASSWORD}/" > ${CATALINA_HOME}/conf/tomcat-users.xml

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

if [ -z ${MAX_JVM_SIZE} ]; then
   JVM_SIZING="-XX:InitialRAMPercentage=${INIT_RAM_PERCENT} -XX:MaxRAMPercentage=${MAX_RAM_PERCENT}"
else
   JVM_SIZING="-Xms${MIN_JVM_SIZE} -Xmx${MAX_JVM_SIZE}"
fi

if [ -z ${VISUALIZE_FITS_SEARCH_PATH} ]; then
   VIS_PATH="/external"
else
   VIS_PATH="/external:${VISUALIZE_FITS_SEARCH_PATH}"
fi

#---------   CATALINA_OPTS must be exported for catalina.sh to pick them up
export CATALINA_OPTS="\
  ${JVM_SIZING} \
  -DADMIN_USER=${ADMIN_USER} \
  -DADMIN_PASSWORD=${ADMIN_PASSWORD} \
  -Dhost.name=${HOSTNAME} \
  -Dserver.cores=${JVM_CORES} \
  -Djava.net.preferIPv4Stack=true \
  -Dwork.directory=/firefly/workarea \
  -Dshared.work.directory=/firefly/shared-workarea \
  -Dserver_config_dir=/firefly/config \
  -Dstats.log.dir=/firefly/logs/statistics \
  -Dalerts.dir=/firefly/alerts \
  -Dvisualize.fits.search.path=${VIS_PATH} \
	${FIREFLY_OPTS}"


#------- start background scripts: cleanup and log to console
${CATALINA_HOME}/cleanup.sh /firefly/workarea /firefly/shared-workarea &
${CATALINA_HOME}/sendLogsToConsole.sh &

echo "launchTomcat.sh: Starting Tomcat"
if [ "$DEBUG" = "true" ] ||[ "$DEBUG" = "t" ] ||[ "$DEBUG" = "1" ] ||  \
   [ "$DEBUG" = "TRUE" ] || [ "$DEBUG" = "True" ] || [ "$1" = "--debug" ]; then
    exec ${CATALINA_HOME}/bin/catalina.sh jpda ${START_MODE}
else
    exec ${CATALINA_HOME}/bin/catalina.sh ${START_MODE}
fi
