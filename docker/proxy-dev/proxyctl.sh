#!/bin/bash


http_port="80"
https_port="443"
docker_host="`ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}'  | tail -1`"
firefly_port="8080"


while [[ $# -gt 1 ]]
do
key="$1"

case $key in
    -http|--http_port)
    http_port="$2"
    shift # past argument
    ;;
    -https|--https_port)
    https_port="$2"
    shift # past argument
    ;;
    -host|--docker_host)
    docker_host="$2"
    shift # past argument
    ;;
    -firefly|--firefly_port)
    firefly_port="$2"
    shift # past argument
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done


case "$1" in
  start)
        docker start proxy-dev
        ;;
  stop)
        docker stop proxy-dev
        ;;
  shell)
        docker exec -it proxy-dev /bin/bash
        ;;
  clean)
        docker rmi $(docker images --filter "dangling=true" -q --no-trunc)
        ;;
  update)
        echo ----------------------------------------
        echo http_port  = "${http_port}"
        echo https_port  = "${https_port}"
        echo docker_host  = "${docker_host}"
        echo firefly_port  = "${firefly_port}"
        echo ----------------------------------------

        DIR=$(dirname "${0}")
        cd $DIR/../../../firefly
        gradle proxyDev:dockerImage
        docker stop proxy-dev
        docker container rm proxy-dev

        docker run -d \
                   -p ${http_port}:80 \
                   -p ${https_port}:443 \
                   -e docker_host=${docker_host} \
                   -e firefly_port=${firefly_port} \
                   -e docker_host=`ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}'  | tail -1` \
                   --name proxy-dev ipac/proxy-dev

#           -e "DOMAINS=`hostname`" \
#           -e "WEBMASTER_MAIL=loi@ipac.caltech.edu" \
#           -e "STAGING=proxy" \

        ;;
  *)
        echo
        echo "Usage: proxyctl.sh [start|stop|shell|clean|update]"
        echo "  optional parameters for update only:"
        echo "    -http|--http_port <number>"
        echo "    -https|--https_port <number>"
        echo "    -host|--docker_host <x.x.x.x>  : ip address of the host running firefly"
        echo "    -firefly|--firefly_port <number>"
        echo
        exit 1
esac

