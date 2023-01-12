#!/bin/bash

cd /opt/work/${work_dir}
gradle -Penv=${env} ${project}:bAD
gradle -Penv=${env} ${project}:dev &

# extract all war files into tomcat's webapps; mod log4j to have log sent to stdout as well
cd ${CATALINA_HOME}/webapps
for n in *.war; do \
  war_dir=`basename $n .war`; \
  mkdir -p $war_dir; \
  unzip -oqd $war_dir $n; \
  sed -E -i.bak 's/##out--//' $war_dir/WEB-INF/classes/log4j2.properties; \
done

cd ${CATALINA_HOME}
${CATALINA_HOME}/launchTomcat.sh