# there are many different options to start tomcat
# this is a sample setenv.sh file to get you started.

# directory containing runtime configurable properties.
# this is very important.  heritage will not run if this is missing.
SERVER_CONFIG_DIR=%server.config.dir%
MIN_JVM_SIZE=%min.jvm.size%
MAX_JVM_SIZE=%max.jvm.size%

ADDTL_JVM_OPTS=%addtl.jvm.opts%

# use these parameters when running in debug
# ie.  catalina.sh jpda start, instead of catalina.sh start
JPDA_ADDRESS=1239
JPDA_SUSPEND=n

JAVA_OPTS="-Xms${MIN_JVM_SIZE}m -Xmx${MAX_JVM_SIZE}m ${ADDTL_JVM_OPTS} -XX:PermSize=256m -Dserver_config_dir=$SERVER_CONFIG_DIR"
