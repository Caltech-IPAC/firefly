#! /bin/sh

<<<<<<< HEAD
# host variables
host_name=irsawebtest
server_dir=/hydra/server
=======
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
>>>>>>> remotes/origin/rc-planck-v2_0



# common functions
function tomcat() {
    /etc/init.d/tomcat_init $1
    ssh irsadmin@${host_name}2 /etc/init.d/tomcat_init $1
}


function deploy() {

    # default naming convention
    app=$1
    war_name=applications#$app

    # check for exceptions
    case "$1" in
      hydra)
            war_name=hydra
            ;;
      fftools)
            war_name=fftools
            ;;
      heritage)
            war_name=applications#Spitzer#SHA
            ;;
      voservices)
            war_name=applications#Spitzer#VO
            ;;
    esac

    tomcat_dir=${server_dir}/nodes/${host_name}
    version=current
    app_name=$war_name


    if [ ! -d "${server_dir}/config/${app}" ]; then
        echo -e "\nERROR!!!! App directory does not exists: ${server_dir}/config/${app}"
        return
    fi
    if [ ! -d "${server_dir}/repos/${app}/${version}" ]; then
        echo -e "\nERROR!!!! Repos directory does not exists: ${server_dir}/repos/${app}/${version}"
        return
    fi


    cd ${server_dir}/repos/${app}/${version}
    echo -e "\nDeploying   `pwd -P`"
    ls -l

    cd ${server_dir}/config/${app}
    jar xf ${server_dir}/repos/${app}/${version}/${war_name}-config.jar

    cd ${server_dir}/repos/${app}/${version}

    rm -r ${tomcat_dir}1/webapps/${war_name}*
    rm -r ${tomcat_dir}2/webapps/${war_name}*
    rm ${tomcat_dir}2/temp/ehcache/${app}/*
    rm ${tomcat_dir}1/temp/ehcache/${app}/*
    cp ${war_name}.war ${tomcat_dir}1/webapps/
    cp ${war_name}.war ${tomcat_dir}2/webapps/

}

#==============================================================================
#  MAIN
#==============================================================================


if [ "$#" -le "0" ]; then
     echo $"Usage: deploy.sh [one or more repos directory's name]"
     exit -1
fi

dostart=true
dostop=true
dodeploy=true
for i in "$@"
do
    if [ "$i" == "--deploy-only" ]; then
        dostart=false
        dostop=false
        break
    fi
    if [ "$i" == "--tomcat-start" ]; then
        dostop=false
        dodeploy=false
        break
    fi
    if [ "$i" == "--tomcat-stop" ]; then
        dostart=false
        dodeploy=false
        break
    fi
done


if [ "$dostop" == "true" ]; then
    echo -e "\nStopping Tomcat..."
    tomcat stop
    sleep 15

    echo -e "\nChecking for tomcat process on 1"
    ps -ef | grep org.apache.catalina | cut -c -100
    echo -e "\nChecking for tomcat process on 2"
    ssh irsadmin@${host_name}2 ps -ef | grep org.apache.catalina | cut -c -100
fi


if [ "$dodeploy" == "true" ]; then
    for i in "$@"
    do
        if [[ "$i" != "--"* ]]; then
            deploy "$i"
        fi
    done
fi

<<<<<<< HEAD

if [ "$dostart" == "true" ]; then
    echo -e "\nStarting Tomcat..."
    tomcat start
fi

echo -e "\nDeploy completed..."

=======
echo "Stopping Tomcat..."
stopTomcat

sleep 5
ps -ef | grep org.apache.catalina

echo "Deploying... ${server_dir}/repos/${app}/${version}"
read -p "Press [Enter] key to start ..."
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
>>>>>>> remotes/origin/rc-planck-v2_0

}
