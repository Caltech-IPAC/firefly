#!/bin/bash
#
# if this is a firefly.war docker container do some extra setup
# - put the 3 main html files in /local/www
# - modify the script line of the html files so that they will load
# to test: http://localhost:<port>/local/
#
aWarFile=`ls ${CATALINA_HOME}/webapps-ref/*.war | head -1 | awk '{print $1}'`
onlyWar=`echo ${aWarFile} | awk -F/ '{print $NF}'`
dirEmpty=`find /local/www -maxdepth 0 -empty -exec echo true \;`
if [ "$onlyWar" = "firefly.war" ] && [ "$dirEmpty" = "true" ]; then
    echo "Alt Entry Point: beginning set up example"
    mkdir /tmp/tmp-expand
    cd /tmp/tmp-expand
    unzip -d . ${aWarFile} firefly.html slate.html firefly-dev.html
    mv slate.html slate-old.html
    mv firefly-dev.html firefly-dev-old.html
    cat firefly.html | sed -e s,firefly_loader.js,/firefly/firefly_loader.js, > index.html
    cat slate-old.html | sed -e s,firefly_loader.js,/firefly/firefly_loader.js, > slate.html
    cat firefly-dev-old.html | sed -e s,firefly_loader.js,/firefly/firefly_loader.js, > firefly-dev.html
    cp {index,slate,firefly-dev}.html /local/www
    cd ..
    rm -r /tmp/tmp-expand
    echo "Alt Entry Point: setting up example: index.html, slate.html, firefly-dev.html in /local/www"
fi
