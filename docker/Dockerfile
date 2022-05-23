ARG build_dir=firefly
ARG target=firefly:warAll
ARG env=local
ARG BranchOverride=''

FROM openjdk:17-jdk-slim-buster AS deps

RUN apt-get update && apt-get install -y curl git htmldoc unzip wget

RUN apt-get update \
# use node v12.x. may not be available via apt-get
    && curl -sL https://deb.nodesource.com/setup_12.x | bash -  \
    && apt-get install -y nodejs \
    && npm install yarn -g  \
# gradle version 17.4  Not available via apt-get
    && cd /usr/local \
    && wget  -q https://services.gradle.org/distributions/gradle-7.4-bin.zip \
    && unzip -q gradle-7.4-bin.zip \
    && ln -sf /usr/local/gradle-7.4/bin/gradle /usr/local/bin/  \
    && rm gradle-7.4-bin.zip \
# cleanup
    && rm -rf /var/lib/apt/lists/*;

WORKDIR "/opt/work"

FROM deps AS node_module

WORKDIR "/opt/work/lib"
COPY firefly/package.json firefly/yarn.lock ./
RUN yarn install --ignore-platform --frozen-lockfile


FROM node_module AS builder

ARG build_dir
ARG target
ARG env
ARG BranchOverride

WORKDIR /opt/work
COPY . .
COPY --from=node_module /opt/work/lib/node_modules ./firefly/node_modules

WORKDIR /opt/work/${build_dir}
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false"
RUN gradle -Penv=${env} -PBranchOverride=${BranchOverride} ${target}


# Beginning of runtime image script

# Description:
# A tomcat application server running as UID 91(tomcat) by default.
# It's designed to support running as a different user via -u or --user param.
#
#-----------------------------------------------------------------------------
# To build: docker build -t ipac/firefly --build-arg IMAGE_NAME=ipac/firefly .
# For help in running: docker run --rm  ipac/firefly --help
#-----------------------------------------------------------------------------
#
# Below are predefined directories Firefly uses during runtime.
# Mount these directories to an external volume or to the host filesystem if you would like it
# to persists beyond the container's lifecycle.
#
# Firefly mountPaths:
# /firefly/config           : used to override application properties
# /firefly/workarea         : work area for temporary files
# /firefly/shared-workarea  : work area for files that are shared between multiple instances of the application
# /firefly/logs             : logs directory
# /firefly/logs/statistics  : directory for statistics logs
# /firefly/alerts           : alerts monitor will watch this directory for application alerts
# /external                 : default external data directory visible to Firefly


FROM tomcat:9.0-jdk17-openjdk-buster

ARG build_dir
ARG user=tomcat
ARG group=tomcat
ARG uid=91
ARG gid=91


# - add packages: vim, wget, etc
# - add any other standard apt packages here
# - this is a big part of the layer so do it early
# - emacs removed because it is so big: to readd emacs: emacs-nox
RUN apt-get update && apt-get install -y \
        vim procps wget unzip \
        && rm -rf /var/lib/apt/lists/*;

# These are the users replaceable environment variables, basically runtime arguments
#          - Set the available memory on the command line with --memory="4g"
#          - You can change MAX_RAM_PERCENT on the command line with -e "MAX_RAM_PERCENT=80"
#          - also- User name and password to use admin
#          - PROPS could be used to pass any properties

ENV INIT_RAM_PERCENT=10\
    MAX_RAM_PERCENT=100\
    JVM_CORES=0\
    ADMIN_USER=admin\
    ADMIN_PASSWORD='' \
    DEBUG=false \
    CLEANUP_INTERVAL=12h \
    PROPS=''

# ----------------------------------------------------------
# ----------------------------------------------------------
# Overide the following from the command line:
#          INIT_RAM_PERCENT, MAX_RAM_PERCENT,
#          ADMIN_USER, ADMIN_PASSWORD,
#          DEBUG, PROPS
# ----------------------------------------------------------
# ----------------------------------------------------------

ARG IMAGE_NAME=''

ENV JPDA_ADDRESS=*:5050 \
    VISUALIZE_FITS_SEARCH_PATH='' \
    BUILD_TIME_NAME=${IMAGE_NAME} \
    START_MODE=run

WORKDIR ${CATALINA_HOME}

# set up directory protections, copy stuff around, add tomcat user and group
RUN mkdir -p conf/Catalina/localhost /local/www /firefly/config /firefly/workarea /firefly/shared-workarea /firefly/logs/statistics /firefly/alerts \
  && groupadd -g ${gid} ${group} && useradd -u ${uid} -g ${group} -s /bin/sh ${user} \
  && rm -r logs && ln -s /firefly/logs logs

# These are the file that are executed at startup: start tomcat, logging, help, etc
COPY firefly/docker/*.sh firefly/docker/*.txt ${CATALINA_HOME}/

# Tomcat config files, tomcat-users is for the admin username and password
# context.xml set delegate to true for we can use the classpath of tomcat
COPY firefly/docker/tomcat-users.xml conf/
COPY firefly/docker/local.xml conf/Catalina/localhost

#copy all wars, typically there should only be one
COPY --from=builder /opt/work/${build_dir}/build/dist/*.war ${CATALINA_HOME}/webapps/

# extract all war files into tomcat's webapps; mod log4j to have log sent to stdout as well
WORKDIR ${CATALINA_HOME}/webapps
RUN for n in *.war; do \
    war_dir=`basename $n .war`; \
    mkdir -p $war_dir; \
    unzip -oqd $war_dir $n; \
    sed -E -i.bak 's/##out--//' $war_dir/WEB-INF/classes/log4j2.properties; \
    done

# Add permission to files and directories needed for runtime
# increase max header size to avoid failing on large auth token
WORKDIR ${CATALINA_HOME}
RUN chmod a+x *.sh \
  && chmod -R a+w *.txt temp work /local/www /firefly \
  && sed -i 's/Connector port="8080"/Connector maxHttpHeaderSize="24576" port="8080"/g' ${CATALINA_HOME}/conf/server.xml

# 8080 - http,  5050 - debug
EXPOSE 8080 5050

USER tomcat

#CMD ["/bin/bash", "./launchTomcat.sh"]
ENTRYPOINT ["/bin/bash", "-c", "./launchTomcat.sh ${*}", "--"]
