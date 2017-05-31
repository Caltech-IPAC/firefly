
CATALINA_OPTS="\
        -Xms${MIN_JVM_SIZE} \
        -Xmx${MAX_JVM_SIZE} \
        -DjvmRoute=${JVM_ROUTE}
        -Djava.rmi.server.hostname= \
        -Dcom.sun.management.jmxremote.port=${JMX_ADDRESS} \
        -Dcom.sun.management.jmxremote.rmi.port=${JMX_ADDRESS} \
        -Dcom.sun.management.jmxremote.ssl=false \
        -Dcom.sun.management.jmxremote.authenticate=false"

JAVA_OPTS="-Dnet.sf.ehcache.enableShutdownHook=true \
           -Dserver_config_dir=${SERVER_CONFIG_DIR} \
           -Dwork.directory=${FIREFLY_WORK_DIR} \
	   ${FIREFLY_OPTS}"
