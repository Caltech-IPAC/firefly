# if MAX_JVM_SIZE is not specified, use auto-sizing parameters INIT_RAM_PERCENT and MAX_RAM_PERCENT
if [ -z ${MAX_JVM_SIZE} ]; then
   JVM_SIZING="-XX:InitialRAMPercentage=${INIT_RAM_PERCENT} -XX:MaxRAMPercentage=${MAX_RAM_PERCENT}"
else
   JVM_SIZING="-Xms${MIN_JVM_SIZE} -Xmx${MAX_JVM_SIZE}"
fi

CATALINA_OPTS="\
        ${JVM_SIZING} \
        -Dserver.cores=${JVM_CORES} \
        -DjvmRoute=${JVM_ROUTE} \
        -Djava.net.preferIPv4Stack=true"

#        -Dcom.sun.management.jmxremote.port=${JMX_ADDRESS} \
#        -Dcom.sun.management.jmxremote.rmi.port=${JMX_ADDRESS} \
#        -Dcom.sun.management.jmxremote.ssl=false \
#        -Dcom.sun.management.jmxremote.authenticate=false"

JAVA_OPTS="-Dnet.sf.ehcache.enableShutdownHook=true \
           -Dserver_config_dir=${SERVER_CONFIG_DIR} \
           -Dwork.directory=${FIREFLY_WORK_DIR} \
           -Dshared.work.directory=${FIREFLY_SHARED_WORK_DIR} \
           -Dvisualize.fits.search.path=${VISUALIZE_FITS_SEARCH_PATH} \
	   ${FIREFLY_OPTS}"
