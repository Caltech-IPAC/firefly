FROM birgerk/apache-letsencrypt


#RUN apt-get update && \
#    apt-get -f stretch install libapache2-mod-auth-openidc && \
#    rm -rf /var/lib/apt/lists/*

#RUN ln -s /usr/lib/apache2/modules/mod_auth_openidc.so modules/mod_auth_openidc.so
COPY ./others/*.conf /etc/apache2/conf-enabled/

RUN   a2enmod proxy; \
      a2enmod proxy_http; \
      a2enmod proxy_wstunnel

EXPOSE 80 443

ENTRYPOINT ["httpd-foreground"]