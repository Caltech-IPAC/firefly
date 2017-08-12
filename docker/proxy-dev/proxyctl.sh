#!/bin/sh
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
        cd /hydra/cm/firefly
        gradle proxyDev:dockerImage
        docker stop proxy-dev
        docker container rm proxy-dev

        docker run -d \
                   -p 80:80 \
                   -p 443:443 \
                   -e "docker_host=`ifconfig | grep "inet " | grep -Fv 127.0.0.1 | awk '{print $2}'  | tail -1`" \
                   --name proxy-dev ipac/proxy-dev

#           -e "DOMAINS=`hostname`" \
#           -e "WEBMASTER_MAIL=loi@ipac.caltech.edu" \
#           -e "STAGING=proxy" \

        ;;
  *)
        echo $"Usage: proxyctl.sh [start|stop|shell|clean|update]"
        exit
esac

