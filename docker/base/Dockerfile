FROM tomcat:9.0-jdk11-openjdk

#-----------------------------------------------------------------------------
# To build: docker build -t ipac/firefly --build-arg IMAGE_NAME=ipac/firefly .
# For help in running: docker run --rm  ipac/firefly --help
#-----------------------------------------------------------------------------

# Support single server deployments
# For multi server we need to look at multicast issues so ehcache can communicate

# - add packages: vim, etc
# - add any other standard apt packages here
# - this is a big part of the layer so do it early
RUN apt-get update && apt-get install -y \
        vim procps wget \
        && rm -rf /var/lib/apt/lists/*;


# First set of environment variable
#        - create catalina_base directory .. so tomcat can run as non-root
#        - These environment varibles are not really made to be overridden
#           they can be but are mostly for setup
#         - work dir and config dir might be overridden if they were used in a mounted volume
#           in the case make sure the directories exist
#         - BUILD_TIME_NAME - container has access to the image name, used for help only
# Second set of environment variable
# These are the users replaceable ones, basicly runtime arguments
#          MIN_JVM_SIZE and MAX_JVM_SIZE can be used to set the min and max JVM side
#          - If MAX_JVM_SIZE is not set, the memory is autosized to the memory available to the container.
#          - Set the available memory on the command line with --memory="4g"
#          - You can change MAX_RAM_PERCENT on the command line with -e "MAX_RAM_PERCENT=80"
#          - also- User name and password to use admin
#          - if jvmRoute is not passed the hostname (the container id) is used such as: -e jvmRoute="myroute1"
#          - LOG_FILE_TO_CONSOLE file to log to console, such as -e "LOG_FILE_TO_CONSOLE=firefly.log"
#          - FIREFLY_OPTS could be used to pass any properties, setenv.sh picks it up
#          - SHARE_CACHE set to TRUE when deploying multiple apps to share the VIS_SHARED_MEM cache

ENV CATALINA_HOME=/usr/local/tomcat \
    CATALINA_BASE=/usr/local/tomcat-base \
    EXTERNAL_MOUNT_POINT=/external \
\
    MAX_JVM_SIZE=\
    MIN_JVM_SIZE=1G\
    INIT_RAM_PERCENT=10\
    MAX_RAM_PERCENT=90\
    JVM_CORES=0\
    ADMIN_USER=admin\
    ADMIN_PASSWORD=replaceMe \
    DEBUG=false \
    MANAGER=true \
    jvmRoute='' \
    LOG_FILE_TO_CONSOLE='' \
    FIREFLY_OPTS='' \
    SHARE_CACHE=FALSE

# ----------------------------------------------------------
# ----------------------------------------------------------
# Overide the following from the command line:
#          MIN_JVM_SIZE, MAX_JVM_SIZE,
#          INIT_RAM_PERCENT, MAX_RAM_PERCENT,
#          ADMIN_USER, ADMIN_PASSWORD,
#          DEBUG, jvmRoute, LOG_FILE_TO_CONSOLE, FIREFLY_OPTS,
# ----------------------------------------------------------
# ----------------------------------------------------------

ARG IMAGE_NAME=''

ENV JPDA_ADDRESS=5050 \
    CATALINA_PID=${CATALINA_BASE}/bin/catalina.pid \
    SERVER_CONFIG_DIR=${CATALINA_BASE}/firefly-config \
    FIREFLY_WORK_DIR=${CATALINA_BASE}/firefly-work \
    FIREFLY_SHARED_WORK_DIR='' \
    VISUALIZE_FITS_SEARCH_PATH=${EXTERNAL_MOUNT_POINT} \
    BUILD_TIME_NAME=${IMAGE_NAME}

WORKDIR ${CATALINA_BASE}

# - set up directory protections
# - copy stuff around
# - add tomecase user
RUN chmod g-s ${CATALINA_BASE} && \
    mkdir -p bin conf conf/Catalina/localhost lib logs temp webapps work /local/www && \
    cp ${CATALINA_HOME}/conf/* ${CATALINA_BASE}/conf/ && \
    chmod +rw ${CATALINA_BASE}/conf/* && \
    chmod 777 /local   && \
    chmod 777 /local/www   && \
    chmod -R 777  conf && \
    chmod -R +rX ${CATALINA_HOME} &&\
    groupadd -g 91 tomcat && \
    useradd -r -u 91 -g tomcat tomcat




# These are the file there are executed at startup, they start tomcat
COPY launchTomcat.sh \
     cleanup.sh \
     start-examples.txt \
     customize-firefly.txt \
     setupSharedCacheJars.sh \
     setupFireflyExample.sh ${CATALINA_BASE}/

# Tomcat config files, tomcat-users is for the admin username and password
# context.xml set delegate to true for we can use the classpath of tomcat
COPY tomcat-users.xml \
     context.xml  ${CATALINA_BASE}/conf/

COPY local.xml ${CATALINA_BASE}/conf/Catalina/localhost

# setenv.sh is used to defined CATALINA_OPTS and JAVA_OPTS
COPY setenv.sh ${CATALINA_BASE}/bin/


# Make directories, make scripts executable, save old tomcat config files, remove unwanted apps
# increase max header size to avoid failing on large auth token
RUN chmod +x ${CATALINA_BASE}/launchTomcat.sh ${CATALINA_BASE}/cleanup.sh  \
            ${CATALINA_BASE}/setupSharedCacheJars.sh ${CATALINA_BASE}/setupFireflyExample.sh; \
    chmod 777 ${CATALINA_BASE}; \
    mkdir -p ${SERVER_CONFIG_DIR}; \
    mkdir -p ${FIREFLY_WORK_DIR}; \
    mkdir -p ${EXTERNAL_MOUNT_POINT}; \
    chmod 777 bin conf lib logs temp webapps work ${SERVER_CONFIG_DIR} ${FIREFLY_WORK_DIR}; \
    mv ${CATALINA_BASE}/conf/tomcat-users.xml ${CATALINA_BASE}/conf/tomcat-users.xml.in; \
    sed -i 's/Connector port="8080"/Connector maxHttpHeaderSize="24576" port="8080"/g' ${CATALINA_BASE}/conf/server.xml



# 8080 - http
# 5050 - debug
EXPOSE 8080 5050

USER tomcat:tomcat


#copy all wars, typically there should only be one
COPY *.war ${CATALINA_BASE}/webapps/


#CMD ["bin/catalina.sh","jpda", "run"]
#CMD ["/bin/bash", "./launchTomcat.sh"]
ENTRYPOINT ["/bin/bash", "-c", "./launchTomcat.sh ${*}", "--"]
