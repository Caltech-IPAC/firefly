#!/bin/bash

cd /opt/work/${work_dir}
gradle -Penv=${env} ${project}:bAD
gradle -Penv=${env} ${project}:dev &

cd ${CATALINA_HOME}
/opt/tomcat/launchTomcat.sh