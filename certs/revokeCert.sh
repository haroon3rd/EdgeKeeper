#!/bin/bash
### Script to revoke a certificate and update the CRL.  

. cert-metadata.sh

if [ ! "$1" ]; then
   echo "Provide the filename of the certificate to revoke (.pem)."
   exit 1
fi

mkdir -p "$DIR"
cd "$DIR"

touch crl_index.txt
touch crl_index.txt.attr

## if you have a custom password  for your CA key, edit this, or comment it
## out to have the openssl commands below prompt you for the password:
KEYPASS="-key $PASS"
CONFIG=../config.cfg

openssl ca -config $CONFIG -revoke "$1.pem" -keyfile ca-do-not-share.key $KEYPASS -cert ca.pem
openssl ca -config $CONFIG -gencrl -keyfile ca-do-not-share.key $KEYPASS -cert ca.pem -out ca.crl

## the command below will print the CRL in a readable form
# openssl crl -text -in *.crl
