FROM birgerk/apache-letsencrypt

RUN apt-get update && \
    apt-get -f --assume-yes install libapache2-mod-auth-openidc && \
    apt-get install wget && \
    apt-get install telnet && \
    rm -rf /var/lib/apt/lists/*

COPY ./others/*.conf /etc/apache2/conf-enabled/

RUN   a2enmod proxy; \
      a2enmod proxy_http; \
      a2enmod proxy_wstunnel; \
      a2enmod auth_openidc

RUN   mkdir /etc/apache2/certs; \
      openssl req \
            -new \
            -newkey rsa:4096 \
            -days 365 \
            -nodes \
            -x509 \
            -subj "/C=US/ST=CA/L=dev/O=dev/CN=localhost" \
            -keyout /etc/apache2/certs/localhost.key \
            -out /etc/apache2/certs/localhost.cert


EXPOSE 80 443

# Default environment varibles.
ENV docker_host=localhost
ENV firefly_port=8080
