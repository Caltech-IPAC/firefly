#!/bin/bash

sed "s/@DOMAINS@/$DOMAINS/g; s/@WEBMASTER_MAIL@/$WEBMASTER_MAIL/g" -i /etc/apache2/sites-enabled/default.conf

# init only if lets-encrypt is running for the first time and if DOMAINS was set
if [ ! -f /letsencrypt_ran ] && [ ! -z "$DOMAINS" ]; then
  /run_letsencrypt.sh --domains $DOMAINS
  touch /letsencrypt_ran
fi