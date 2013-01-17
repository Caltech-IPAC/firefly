#! /bin/sh
version=v2_0_2
app=wise
app_prefix=applications#
app_name=${app_prefix}${app}
server_dir=/hydra/server
tomcat_dir=${server_dir}/nodes/irsawebtest

if [ ! -d "${server_dir}/config/${app}" ]; then
    echo "App directory does not exists: ${server_dir}/config/${app}"
    exit 1
fi
if [ ! -d "${server_dir}/repos/${app}/${version}" ]; then
    echo "Repos directory does not exists: ${server_dir}/repos/${app}/${version}"
    exit 1
fi

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

