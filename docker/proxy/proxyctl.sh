#!/bin/sh
case "$1" in
  start)
        docker start proxy
        ;;
  stop)
        docker stop proxy
        ;;
  shell)
        docker exec -it proxy /bin/bash
        ;;
  update)
        docker stop proxy
        docker container rm proxy

        docker pull ipac/proxy
        docker run -d \
                   -p 80:80 \
                   -p 443:443 \
                   -e "DOMAINS=`hostname`" \
                   -e "WEBMASTER_MAIL=loi@ipac.caltech.edu" \
                   --network=local_nw \
                   --restart=unless-stopped \
                   --name proxy ipac/proxy

#               -e "DOMAINS=`hostname`" \
#               -e "STAGING=proxy" \
        ;;
  *)
        echo $"Usage: proxyctl.sh [start|stop|shell|update]"
        exit
esac

