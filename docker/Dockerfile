ARG BUILDPLATFORM=linux/amd64

# ==============================
# Stage 1: Base dependencies
# ==============================
FROM --platform=${BUILDPLATFORM} eclipse-temurin:21-jdk-jammy AS base

RUN echo "============> Build platform: $(uname -m)"

RUN apt-get update && apt-get install -y \
      curl git unzip wget ca-certificates gnupg htmldoc \
    && curl -fsSL https://deb.nodesource.com/setup_18.x | bash - \
    && apt-get install -y nodejs \
    && npm install -g yarn \
# gradle version 8.10  Not available via apt-get
    && cd /usr/local \
    && wget -q https://services.gradle.org/distributions/gradle-8.10-bin.zip \
    && unzip -q gradle-8.10-bin.zip \
    && ln -sf /usr/local/gradle-8.10/bin/gradle /usr/local/bin/gradle \
    && rm gradle-8.10-bin.zip \
# cleanup
    && rm -rf /var/lib/apt/lists/*;

WORKDIR /opt/work


# =====================================
# Stage 2: Node modules and maven cache
# =====================================
FROM --platform=${BUILDPLATFORM} base AS deps

RUN echo "============> Build platform: $(uname -m)"

WORKDIR /opt/work/lib
COPY firefly/package.json firefly/yarn.lock ./
# 900000= 15 minutes because QEMU(arm64 emulator) is super slow.
RUN yarn install --ignore-platform --frozen-lockfile

# Copy only minimal build descriptors (no source) so we can resolve base
WORKDIR /opt/work/firefly
COPY firefly/settings.gradle firefly/gradle.properties firefly/build.gradle ./
COPY firefly/buildScript/ ./buildScript
COPY firefly/src/firefly/build.gradle src/firefly/build.gradle

# Warm up local Gradle/Maven caches (no build yet)
# `gradle dependencies` downloads all declared jars
RUN gradle --no-daemon --quiet --refresh-dependencies firefly:dependencies || true


# =====================================
# Stage 3: Builder (Gradle, amd64 only)
# =====================================
FROM --platform=${BUILDPLATFORM} base AS builder

ARG build_dir=firefly
ARG target=firefly:warAll
ARG env=local
ARG BranchOverride=''
ARG checkoutFirefly

RUN echo "============> Build platform: $(uname -m)"

WORKDIR /opt/work
COPY . .
COPY --from=deps /opt/work/lib/node_modules ./firefly/node_modules
COPY --from=deps /root/.gradle /root/.gradle


WORKDIR /opt/work/${build_dir}
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false"
RUN if [ -z "$checkoutFirefly" ] ; then echo skip checkout ; else gradle firefly:checkoutFirefly ; fi
RUN gradle -Penv=${env} -PBranchOverride=${BranchOverride} ${target}


# ==============================
# Dev environment
# ==============================
FROM base AS dev_env

RUN apt-get update && apt-get install -y vim procps python3 \
# cleanup
    && rm -rf /var/lib/apt/lists/*

COPY --from=tomcat:11.0.11-jre21-temurin-jammy /usr/local/tomcat /usr/local/tomcat
COPY --from=deps /opt/work/lib/node_modules ./firefly/node_modules
COPY --from=deps /root/.gradle /root/.gradle

ENV CATALINA_HOME=/usr/local/tomcat \
    USE_ADMIN_AUTH=false \
    DEBUG=true \
    work_dir=firefly \
    project=firefly \
    env=local

WORKDIR ${CATALINA_HOME}

# Tomcat scripts, config files, etc
COPY firefly/docker/*.sh firefly/docker/*.py ./
COPY firefly/docker/tomcat-users.xml conf/

# set up directory protections, copy stuff around, add tomcat user and group
RUN mkdir -p /firefly/workarea /firefly/logs/statistics /firefly/alerts \
    && chmod a+x *.sh

CMD ./devMode.sh

# ==============================
# Stage 4: Runtime image
# ==============================
# Description:
# A tomcat application server running as UID 91(tomcat) by default.
# It's designed to support running as a different user via -u or --user param.
#
#-----------------------------------------------------------------------------
# To build: docker build -t ipac/firefly
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


FROM tomcat:11.0.11-jre21-temurin-jammy

ARG build_dir=firefly
ARG user=tomcat
ARG group=tomcat
ARG uid=91
ARG gid=91
ARG TARGETARCH
ARG TARGETOS
ARG DUCKDB_VERSION=v1.1.3

# These are the users replaceable environment variables, basically runtime arguments
#          - Set the available memory on the command line with --memory="4g"
#          - You can change MAX_RAM_PERCENT on the command line with -e "MAX_RAM_PERCENT=80"
#          - also- User name and password to use admin
#          - PROPS could be used to pass any properties

ENV JVM_CORES=0\
    DEBUG=false \
    CLEANUP_INTERVAL=12h \
    PROPS='' \
    PYTHONUNBUFFERED=1

# ----------------------------------------------------------
# ----------------------------------------------------------
# Overide the following from the command line:
#          INIT_RAM_PERCENT, MAX_RAM_PERCENT,
#          ADMIN_USER, ADMIN_PASSWORD,
#          DEBUG, PROPS
# ----------------------------------------------------------
# ----------------------------------------------------------

# - add packages: vim, wget, etc
# - add any other standard apt packages here
# - this is a big part of the layer so do it early
# - emacs removed because it is so big: to readd emacs: emacs-nox
RUN apt-get update && apt-get install -y \
        vim procps wget unzip python3 \
        && rm -rf /var/lib/apt/lists/*;

WORKDIR ${CATALINA_HOME}

# set up directory protections, copy stuff around, add tomcat user and group
RUN mkdir -p conf/Catalina/localhost /local/www /firefly/config /firefly/workarea \
           /firefly/shared-workarea /firefly/logs/statistics /firefly/alerts \
    && groupadd -g ${gid} ${group} \
    && useradd -u ${uid} -g ${group} -s /bin/sh ${user} \
    && rm -r logs && ln -s /firefly/logs logs

# These are the file that are executed at startup: start tomcat, logging, help, etc
COPY firefly/docker/*.sh firefly/docker/*.txt firefly/docker/*.py ${CATALINA_HOME}/

# Tomcat config files, tomcat-users is for the admin username and password
# context.xml set delegate to true for we can use the classpath of tomcat
COPY firefly/docker/tomcat-users.xml conf/
COPY firefly/docker/local.xml conf/Catalina/localhost

#copy all wars, typically there should only be one
COPY --from=builder /opt/work/${build_dir}/build/dist/*.war ${CATALINA_HOME}/webapps-ref/

# Turn on GZIP compression by adding these attributes to the Tomcat connector:
# - compression="on" → turn on response compression
# - useSendfile="false" →  avoids bypassing compression for static files
# - compressibleMimeType → list of text-based formats to compress (HTML, CSS, JS, JSON, VOTable, XML, CSV, SVG, Fonts)
RUN sed -i 's/<Connector port="8080" protocol="HTTP\/1.1"/<Connector port="8080" protocol="HTTP\/1.1" compression="on" useSendfile="false" compressibleMimeType="text\/html,text\/plain,text\/css,text\/javascript,application\/javascript,application\/json,application\/xml,text\/xml,application\/x-votable+xml,application\/x-yaml,application\/ld+json,image\/svg+xml,text\/csv,application\/xhtml+xml,application\/rss+xml,application\/atom+xml,application\/x-font-ttf,font\/otf,font\/woff,font\/woff2"/' \
    $CATALINA_HOME/conf/server.xml

# preinstall DuckDB extensions; remember to update version when DuckDB is updated
ENV DUCKDB_TARGET=${DUCKDB_VERSION}/${TARGETOS}_${TARGETARCH}
RUN mkdir -p temp/$DUCKDB_TARGET \
    && wget -P temp/$DUCKDB_TARGET http://community-extensions.duckdb.org/$DUCKDB_TARGET/magic.duckdb_extension.gz \
    && gunzip -f temp/$DUCKDB_TARGET/magic.duckdb_extension.gz

# Add permission to files and directories needed for runtime
# increase max header size to avoid failing on large auth token
RUN chmod a+x *.sh \
  && chmod -R a+w *.txt temp work /local/www /firefly ${CATALINA_HOME}/webapps \
  && sed -i 's/Connector port="8080"/Connector maxHttpHeaderSize="24576" port="8080"/g' ${CATALINA_HOME}/conf/server.xml

# 8080 - http,  5050 - debug
EXPOSE 8080 5050

USER tomcat

ENTRYPOINT ["python3", "./entrypoint.py"]
