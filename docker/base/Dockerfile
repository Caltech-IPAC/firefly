#FROM tomcat:7.0-jre8
FROM tomcat:9.0.11-jre10

#-----------------------------------------------------------------------------
# To build: docker build -t ipac/firefly --build-arg IMAGE_NAME=ipac/firefly .
# For help in running: docker run --rm  ipac/firefly --help
#-----------------------------------------------------------------------------

# Support single server deployments
# For multi server we need to look at multicast issues so ehcache can communicate


# add packages: vim, etc
# add any other standard apt packages here
RUN apt-get update && apt-get install -y \
        vim procps wget emacs24-nox \
        && rm -rf var/lib/apt/lists//*


# create catalina_base directory .. so tomcat can run as non-root
ENV CATALINA_HOME=/usr/local/tomcat
ENV CATALINA_BASE=/usr/local/tomcat-base
WORKDIR ${CATALINA_BASE}
RUN chmod g-s ${CATALINA_BASE} && \
    mkdir bin conf lib logs temp webapps work && \
    cp ${CATALINA_HOME}/conf/* ${CATALINA_BASE}/conf/ && \
    chmod +rw ${CATALINA_BASE}/conf/* && \
    chmod -R +rX ${CATALINA_HOME}


# These environment varibles are not really made to be overridden
# they can be but are mostly for setup
ENV JPDA_ADDRESS=5050
ENV CATALINA_PID=${CATALINA_BASE}/bin/catalina.pid

# work dir and config dir might be overridden if they were used in a mounted volume
# in the case make sure the directories exist
ENV SERVER_CONFIG_DIR=${CATALINA_BASE}/firefly-config
ENV FIREFLY_WORK_DIR=${CATALINA_BASE}/firefly-work
ENV FIREFLY_SHARED_WORK_DIR=''
ENV EXTERNAL_MOUNT_POINT=/external
ENV VISUALIZE_FITS_SEARCH_PATH=${EXTERNAL_MOUNT_POINT}

# container has access to the image name, used for help only
ARG IMAGE_NAME=''
ENV BUILD_TIME_NAME=${IMAGE_NAME}



# These are the file there are executed at startup, they start tomcat
COPY launchTomcat.sh \
     start-examples.txt \
     setupSharedCacheJars.sh ${CATALINA_BASE}/

# Tomcat config files, tomcat-users is for the admin username and password
# context.xml set delegate to true for we can use the classpath of tomcat
COPY tomcat-users.xml \
     context.xml  ${CATALINA_BASE}/conf/


# Make directories, make scripts executable, save old tomcat config files, remove unwanted apps
RUN chmod +x ${CATALINA_BASE}/launchTomcat.sh ${CATALINA_BASE}/setupSharedCacheJars.sh; \
    mkdir -p ${SERVER_CONFIG_DIR}; \
    mkdir -p ${FIREFLY_WORK_DIR}; \
    mkdir -p ${EXTERNAL_MOUNT_POINT}; \
    chmod 777 bin conf lib logs temp webapps work ${SERVER_CONFIG_DIR} ${FIREFLY_WORK_DIR}; \
    mv ${CATALINA_BASE}/conf/tomcat-users.xml ${CATALINA_BASE}/conf/tomcat-users.xml.in


# setenv.sh is used to defined CATALINA_OPTS and JAVA_OPTS
COPY setenv.sh ${CATALINA_BASE}/bin/

# increase max header size to avoid failing on large auth token
RUN sed -i 's/Connector port="8080"/Connector maxHttpHeaderSize="24576" port="8080"/g' ${CATALINA_BASE}/conf/server.xml

# 8080 - http
# 5050 - debug
EXPOSE 8080 5050


# ----------------------------------------------------------
# ----------------------------------------------------------
# Overide the following from the command line:
#          MIN_JVM_SIZE, MAX_JVM_SIZE, ADMIN_USER, ADMIN_PASSWORD,
#          DEBUG, jvmRoute, LOG_FILE_TO_CONSOLE, FIREFLY_OPTS,
# ----------------------------------------------------------
# ----------------------------------------------------------

# MIN_JVM_SIZE and MAX_JVM_SIZE should be used to set the min and max JVM side
# at least MAX_JVM_SIZE should almost alway be used on the command line with 
# parameter such as: -e "MAX_JVM_SIZE=4G"
ENV MIN_JVM_SIZE=1G
ENV MAX_JVM_SIZE=8G
ENV JVM_CORES=0


#User name and password to use admin
ENV ADMIN_USER=admin
ENV ADMIN_PASSWORD=replaceMe
ENV DEBUG=false
ENV MANAGER=true

# if jvmRoute is not passed the hostname (the container id) is used
# such as: -e jvmRoute="myroute1"
ENV jvmRoute=''

# file to log to console, such as -e "LOG_FILE_TO_CONSOLE=firefly.log"
ENV LOG_FILE_TO_CONSOLE=''

# FIREFLY_OPTS could be used to pass any properties, setenv.sh picks it up
ENV FIREFLY_OPTS=''

# SHARE_CACHE set to TRUE when deploying multiple apps to share the VIS_SHARED_MEM cache
ENV SHARE_CACHE=FALSE


#copy all wars, typically there should only be one
COPY *.war ${CATALINA_BASE}/webapps/

RUN groupadd -g 91 tomcat && \
    useradd -r -u 91 -g tomcat tomcat

USER tomcat:tomcat

#CMD ["bin/catalina.sh","jpda", "run"]
#CMD ["/bin/bash", "./launchTomcat.sh"]
ENTRYPOINT ["/bin/bash", "-c", "./launchTomcat.sh ${*}", "--"]
