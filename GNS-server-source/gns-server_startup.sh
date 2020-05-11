#! /bin/bash

# Created by Suman for Distressnet-NG
# first change pwd to the current directory where this script is stored.

# Restart MongoDB with the following command:
# If you face some problem with MongoDB, then
# Mongodb log is stored in cat /var/log/mongodb/mongodb.log
# Databasesare stored inside dbpath=/var/lib/mongodb

#sudo service mongodb start
#sudo service mongodb stop

OWN_ACTIVE_REPLICA="GNSApp1"
OWN_RECONFIGURATOR="reconfigurator1"

#This is done to ensure all the locally generated files are stored locally
cd "$(dirname "$0")"

# Carry out specific functions when asked to by the system
case "$1" in
  start)
    echo "Starting GNS server" &>> gns_server_terminal.out
    echo "=================Deleting old replica logs" &>> gns_server_terminal.out
    rm -r clientKeyDB paxos_logs reconfiguration_DB derby.log &>> gns_server_terminal.out
    rm /var/lib/mongodb/UMASS_GNS_DB_GNSApp1-* &>> gns_server_terminal.out

    #./gpServer.sh start all
    echo "Starting Active replica server" &>> gns_server_terminal.out
    java  \
    -cp  \
    jars/GNS-CLI.jar:jars/GNS.jar  \
    -ea  \
    -Djavax.net.ssl.keyStorePassword=qwerty  \
    -Djavax.net.ssl.trustStorePassword=qwerty  \
    -Djavax.net.ssl.keyStore=conf/keyStore.jks  \
    -Djavax.net.ssl.trustStore=conf/trustStore.jks  \
    -Djava.util.logging.config.file=conf/logging.properties  \
    -Dlog4j.configuration=conf/log4j.properties  \
    -DgigapaxosConfig=conf/gigapaxos.properties  \
    edu.umass.cs.reconfiguration.ReconfigurableNode  \
    "$OWN_ACTIVE_REPLICA"  &>> gns_server_terminal.out &

    echo "Starting Reconfigurator" &>> gns_server_terminal.out
    java  \
    -cp  \
    jars/GNS-CLI.jar:jars/GNS.jar  \
    -ea  \
    -Djavax.net.ssl.keyStorePassword=qwerty  \
    -Djavax.net.ssl.trustStorePassword=qwerty  \
    -Djavax.net.ssl.keyStore=conf/keyStore.jks  \
    -Djavax.net.ssl.trustStore=conf/trustStore.jks  \
    -Djava.util.logging.config.file=conf/logging.properties  \
    -Dlog4j.configuration=conf/log4j.properties  \
    -DgigapaxosConfig=conf/gigapaxos.properties  \
    edu.umass.cs.reconfiguration.ReconfigurableNode  \
    "$OWN_RECONFIGURATOR"  &>> gns_server_terminal.out &
    ;;
  stop)
    echo "Stopping GNS Server" &>> gns_server_terminal.out
    #./gpServer.sh stop all
    kill $(ps -ef | grep "ReconfigurableNode" | awk '{print $2}') &>> gns_server_terminal.out
    ;;
  *)
    echo "Usage: {start|stop}"
    exit 1
    ;;
esac

exit 0
