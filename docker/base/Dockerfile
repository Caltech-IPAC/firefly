FROM tomcat:9.0-jdk11-openjdk

#-----------------------------------------------------------------------------
# To build: docker build -t ipac/firefly --build-arg IMAGE_NAME=ipac/firefly .
# For help in running: docker run --rm  ipac/firefly --help
#-----------------------------------------------------------------------------

# Support single server deployments

# - add packages: vim, wget, etc
# - add any other standard apt packages here
# - this is a big part of the layer so do it early
# - emacs removed because it is so big: to readd emacs: emacs-nox
RUN apt-get update && apt-get install -y \
        vim procps wget \
        && rm -rf /var/lib/apt/lists/*;

# First set of environment variable
#        - create catalina_base directory .. so tomcat can run as non-root
#        - These environment varibles are not really made to be overridden
#           they can be but are mostly for setup
#        - work dir and config dir might be overridden if they were used in a mounted volume
#           in the case make sure the directories exist
#        - BUILD_TIME_NAME - container has access to the image name, used for help only
# Second set of environment variable
# These are the users replaceable ones, basicly runtime arguments
#          MIN_JVM_SIZE and MAX_JVM_SIZE can be used to set the min and max JVM side
#          - If MAX_JVM_SIZE is not set, the memory is autosized to the memory available to the container.
#          - Set the available memory on the command line with --memory="4g"
#          - You can change MAX_RAM_PERCENT on the command line with -e "MAX_RAM_PERCENT=80"
#          - also- User name and password to use admin
#          - FIREFLY_OPTS could be used to pass any properties
ENV CATALINA_HOME=/usr/local/tomcat \
    EXTERNAL_MOUNT_POINT=/external \
    MAX_JVM_SIZE=\
    MIN_JVM_SIZE=1G \
    INIT_RAM_PERCENT=10\
    MAX_RAM_PERCENT=93\
    JVM_CORES=0\
    ADMIN_USER=admin\
    ADMIN_PASSWORD=replaceMe \
    DEBUG=false \
    FIREFLY_OPTS=''

# ----------------------------------------------------------
# ----------------------------------------------------------
# Overide the following from the command line:
#          MIN_JVM_SIZE, MAX_JVM_SIZE,
#          INIT_RAM_PERCENT, MAX_RAM_PERCENT,
#          ADMIN_USER, ADMIN_PASSWORD,
#          DEBUG, FIREFLY_OPTS
# ----------------------------------------------------------
# ----------------------------------------------------------

ARG IMAGE_NAME=''

ENV JPDA_ADDRESS=5050 \
    CATALINA_PID=${CATALINA_HOME}/bin/catalina.pid \
    SERVER_CONFIG_DIR=${CATALINA_HOME}/firefly-config \
    FIREFLY_WORK_DIR=${CATALINA_HOME}/firefly-work \
    FIREFLY_SHARED_WORK_DIR='' \
    VISUALIZE_FITS_SEARCH_PATH=${EXTERNAL_MOUNT_POINT} \
    BUILD_TIME_NAME=${IMAGE_NAME}

WORKDIR ${CATALINA_HOME}

# set up directory protections, copy stuff around, add tomcat user and group
RUN mkdir -p bin conf conf/Catalina/localhost lib logs temp webapps work /local/www && \
    chmod +rw ${CATALINA_HOME}/conf/* && \
    chmod 777 /local /local/www conf && \
    chmod -R +rX ${CATALINA_HOME} &&\
    groupadd -g 91 tomcat && \
    useradd -r -u 91 -g tomcat tomcat

# These are the file that are executed at startup: start tomcat, logging, help, etc
COPY *.sh *.txt ${CATALINA_HOME}/

# Tomcat config files, tomcat-users is for the admin username and password
# context.xml set delegate to true for we can use the classpath of tomcat
COPY tomcat-users.xml context.xml  ${CATALINA_HOME}/conf/
COPY local.xml ${CATALINA_HOME}/conf/Catalina/localhost

# Make directories, make scripts executable, save old tomcat config files, remove unwanted apps
# increase max header size to avoid failing on large auth token
RUN chmod +x ${CATALINA_HOME}/*.sh; \
    mkdir -p ${SERVER_CONFIG_DIR} ${FIREFLY_WORK_DIR} ${EXTERNAL_MOUNT_POINT}; \
    chmod 777 bin conf lib logs temp webapps work ${CATALINA_HOME} ${SERVER_CONFIG_DIR} ${FIREFLY_WORK_DIR}; \
    mv ${CATALINA_HOME}/conf/tomcat-users.xml ${CATALINA_HOME}/conf/tomcat-users.xml.in; \
    sed -i 's/Connector port="8080"/Connector maxHttpHeaderSize="24576" port="8080"/g' ${CATALINA_HOME}/conf/server.xml

# 8080 - http,  5050 - debug
EXPOSE 8080 5050

USER tomcat:tomcat

#copy all wars, typically there should only be one
COPY *.war ${CATALINA_HOME}/webapps/

#CMD ["/bin/bash", "./launchTomcat.sh"]
ENTRYPOINT ["/bin/bash", "-c", "./launchTomcat.sh ${*}", "--"]
