#!/bin/bash
#
# pause for a few seconds, then add console to the 'brief' appender in log4j.properties so
# logs are sent to standard out as well.
#
sleepTime=15s

if ! ls ${CATALINA_HOME}/webapps/*/WEB-INF/config/log4j.properties.bak; then
  echo "sendLogsToConsole: started: sleeping ${sleepTime}"
  sleep ${sleepTime}
  echo 'changing log4j config file to also send output to console'
  sed -E -i.bak 's/logger\.(brief\.)?edu(.*)$/logger\.\1edu\2, console/' ${CATALINA_HOME}/webapps/*/WEB-INF/config/log4j.properties
fi
