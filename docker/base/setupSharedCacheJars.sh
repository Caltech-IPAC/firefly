#!/bin/bash

tcDir=/usr/local/tomcat
webLib=WEB-INF/lib
mkdir ${tcDir}/tmpUnzip
cd ${tcDir}/tmpUnzip

aWarFile=`ls ${tcDir}/webapps/*.war | head -1 | awk '{print $1}'`
echo "aWarFile=${aWarFile}"
if [ -f ${aWarFile} ]; then
      unzip -n -j ${aWarFile}  ${webLib}/ehcache-2.7.4.jar \
                               ${webLib}/ehcache-web-2.0.4.jar \
                               ${webLib}/slf4j-api-1.6.6.jar
      mv ehcache-2.7.4.jar ehcache-web-2.0.4.jar slf4j-api-1.6.6.jar ${tcDir}/lib
      echo "successfully extracted ehcache-2.7.4.jar ehcache-web-2.0.4.jar slf4j-api-1.6.6.jar from ${aWarFile}"
      echo "moved jars to ${tcDir}/lib"
else
      echo "Could not find a war file to extract cache jars"
fi
cd ${tcDir}
rmdir ${tcDir}/tmpUnzip



