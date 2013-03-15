#! /bin/sh

# setup which application to deploy.

# default prefix
app_prefix=applications#

case "$1" in
  planck)
        app=planck
        ;;
  wise)
        app=wise
        ;;
  iwise)
        app=iwise
        ;;
  fftools)
        app=iwise
        app_prefix=
        ;;
  finderchart)
        app=finderchart
        ;;
  ptf)
        app=ptf
        ;;
  *)
        echo $"Usage: hydra_deploy {planck|wise|fftools|finderchart|ptf}"
        exit
esac


# variables
host_name=irsawebtest
server_dir=/hydra/server

tomcat_dir=${server_dir}/nodes/${host_name}
version=current
app_name=${app_prefix}${app}

if [ ! -d "${server_dir}/config/${app}" ]; then
    echo "App directory does not exists: ${server_dir}/config/${app}"
    exit 1
fi
if [ ! -d "${server_dir}/repos/${app}/${version}" ]; then
    echo "Repos directory does not exists: ${server_dir}/repos/${app}/${version}"
    exit 1
fi

echo "Stopping Tomcat..."
stopTomcat

sleep 5
ps -ef | grep org.apache.catalina

echo "Deploying..."
deploy

echo "Starting Tomcat..."
startTomcat



# common functions
stopTomcat() {
    /sbin/service tomcat_init stop
    ssh irsadmin@${host_name}2 /sbin/service tomcat_init stop
}

startTomcat() {
    /sbin/service tomcat_init start
    ssh irsadmin@${host_name}2 /sbin/service tomcat_init start
}


deploy() {

    cd ${server_dir}/config/${app}
    jar xf ${server_dir}/repos/${app}/${version}/${app_name}-config.jar

    cd ${server_dir}/repos/${app}/${version}

    rm -r ${tomcat_dir}1/webapps/${app_name}*
    rm -r ${tomcat_dir}2/webapps/${app_name}*
    rm ${tomcat_dir}1/conf/Catalina/localhost/*
    rm ${tomcat_dir}2/conf/Catalina/localhost/*
    rm ${tomcat_dir}2/temp/ehcache/${app}/*
    rm ${tomcat_dir}1/temp/ehcache/${app}/*
    cp ${app_name}.war ${tomcat_dir}1/webapps/
    cp ${app_name}.war ${tomcat_dir}2/webapps/

}