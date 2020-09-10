
FROM ipac/firefly

USER root

RUN curl -sL https://deb.nodesource.com/setup_12.x | bash -  \
    && apt-get update \
    && apt-get install -y nodejs htmldoc software-properties-common \
    && npm install yarn -g  \
    && rm -rf /var/lib/apt/lists/*  \
# gradle version 4.10
# this version of gradle is not available via apt-get.  manual install
    && cd /usr/local \
    && wget https://services.gradle.org/distributions/gradle-4.10-bin.zip \
    && unzip gradle-4.10-bin.zip \
    && ln -sf /usr/local/gradle-4.10/bin/gradle /usr/local/bin/  \
    && rm gradle-4.10-bin.zip \
# openjdk8  -- build with 8, run with 11
    && wget -qO - https://adoptopenjdk.jfrog.io/adoptopenjdk/api/gpg/key/public | apt-key add - \
    && add-apt-repository --yes https://adoptopenjdk.jfrog.io/adoptopenjdk/deb/ \
    && apt update && apt install -y adoptopenjdk-8-hotspot \
# setup work dirs
    && mkdir -p /workarea /cm /gradle_home

ENV GRADLE_USER_HOME=/gradle_home \
    tomcat_home=${CATALINA_HOME} \
    START_MODE=start \
    GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.java.home=/usr/lib/jvm/adoptopenjdk-8-hotspot-amd64" \
    DEBUG=true

ENTRYPOINT []

CMD ["/bin/bash", "-c", "${CATALINA_HOME}/launchTomcat.sh"]
