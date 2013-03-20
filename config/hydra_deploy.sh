#! /bin/sh

# host variables
host_name=irsawebtest
server_dir=/hydra/server



# common functions
function tomcat() {
    /etc/init.d/tomcat_init $1
    ssh irsadmin@${host_name}2 /etc/init.d/tomcat_init $1
}


function deploy() {

    # default prefix
    app_prefix=applications#

    # setup which application to deploy.
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
            echo $"Usage: deploy {planck|wise|fftools|finderchart|ptf}"
            exit
    esac

    if [ ! -d "${server_dir}/config/${app}" ]; then
        echo "App directory does not exists: ${server_dir}/config/${app}"
        exit 1
    fi
    if [ ! -d "${server_dir}/repos/${app}/${version}" ]; then
        echo "Repos directory does not exists: ${server_dir}/repos/${app}/${version}"
        exit 1
    fi

    tomcat_dir=${server_dir}/nodes/${host_name}
    version=current
    app_name=${app_prefix}${app}



    cd ${server_dir}/repos/${app}/${version}
    echo "Deploying   `pwd -P`"
    ls -l
    read -p "\nPress [Enter] key to start."

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

#==============================================================================
#  MAIN
#==============================================================================
echo "Stopping Tomcat..."
tomcat stop
sleep 5

echo "\nchecking for tomcat process on 1"
ps -ef | grep org.apache.catalina | cut -c -100
echo "\nchecking for tomcat process on 2"
ssh irsadmin@${host_name}2 ps -ef | grep org.apache.catalina | cut -c -100

read -p "\nPress [Enter] key to start deploying."

for i in "$@"
do
    deploy "$i"
done

read -p "\nPress [Enter] key to start Tomcat."
echo "Starting Tomcat..."
tomcat start



