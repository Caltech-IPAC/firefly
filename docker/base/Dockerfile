#FROM tomcat:8.0.20-jre8
#FROM tomcat:7.0-jre8
FROM tomcat:8.0-jre8

# To build: docker build -t ipac/firefly . 
# For help in running: docker run --rm  ipac/firefly --help

# support single server, single webapp deployments
# for multi server we need to look at multicast issues


# add packages: vim, etc
# add any other standard apt packages here
RUN apt-get update && apt-get install -y \
        vim \
        && rm -rf /var/lib/apt/lists/*


# These environment varibles are not really make to be overridden
# they can be but are mostly for setup
ENV JPDA_ADDRESS=5050
ENV JMX_ADDRESS=9050
ENV CATALINA_PID=${CATALINA_BASE}/bin/catalina.pid

# work dir and config dir might be overridden if they were used in a mounted volume
# in the case make sure the directories exist
ENV SERVER_CONFIG_DIR=/usr/local/tomcat/firefly-config
ENV FIREFLY_WORK_DIR=/usr/local/tomcat/firefly-work

COPY launchTomcat.sh /usr/local/tomcat
COPY setupSharedCacheJars.sh /usr/local/tomcat
RUN chmod +x /usr/local/tomcat/launchTomcat.sh /usr/local/tomcat/setupSharedCacheJars.sh; \
    mv /usr/local/tomcat/conf/context.xml /usr/local/tomcat/conf/context.xml.bak; \
    mv /usr/local/tomcat/conf/tomcat-users.xml /usr/local/tomcat/conf/tomcat-users.xml.bak; \
    mkdir -p ${SERVER_CONFIG_DIR}; \
    mkdir -p ${FIREFLY_WORK_DIR}

COPY tomcat-users.xml /usr/local/tomcat/conf
COPY context.xml /usr/local/tomcat/conf
COPY setenv.sh /usr/local/tomcat/bin

# 8080 - http
# 5050 - debug
# 9050 - jmx (jconsole)
EXPOSE 8080 5050 9050 


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
ENV MIN_JVM_SIZE=1g
ENV MAX_JVM_SIZE=8G


#User name and password to use admin
ENV ADMIN_USER=admin
ENV ADMIN_PASSWORD=replaceMe
ENV DEBUG=true

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
COPY *.war /usr/local/tomcat/webapps/

#CMD ["bin/catalina.sh","jpda", "run"]
#CMD ["/bin/bash", "./launchTomcat.sh"]
ENTRYPOINT ["/bin/bash", "-c", "./launchTomcat.sh ${*}", "--"]
