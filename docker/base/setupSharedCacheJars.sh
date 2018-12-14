#!/bin/bash

tmpDir=${CATALINA_BASE}/temp
webLib=WEB-INF/lib
mkdir ${tmpDir}/tmpUnzip
cd ${tmpDir}/tmpUnzip

aWarFile=`ls ${CATALINA_BASE}/webapps/*.war | head -1 | awk '{print $1}'`
echo "aWarFile=${aWarFile}"
if [ -f ${aWarFile} ]; then
      unzip -n -j ${aWarFile}  ${webLib}/ehcache-2.7.4.jar \
                               ${webLib}/ehcache-web-2.0.4.jar \
                               ${webLib}/slf4j-api-1.6.6.jar
      mv ehcache-2.7.4.jar ehcache-web-2.0.4.jar slf4j-api-1.6.6.jar ${CATALINA_BASE}/lib
      echo "successfully extracted ehcache-2.7.4.jar ehcache-web-2.0.4.jar slf4j-api-1.6.6.jar from ${aWarFile}"
      echo "moved jars to ${CATALINA_BASE}/lib"
else
      echo "Could not find a war file to extract cache jars"
fi
cd ${CATALINA_BASE}
rmdir ${tmpDir}/tmpUnzip



