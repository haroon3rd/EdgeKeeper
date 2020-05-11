#! /bin/sh

# first change pwd to the current directory where this script is stored.
cd "$(dirname "$0")"

JARFILENAME="EdgeKeeper-Linux.jar"

# Carry out specific functions when asked to by the system
case "$1" in
  start)
    echo "Starting EdgeKeeper"
    java -jar $JARFILENAME  > ek_terminal.out 2>ek_terminal.out &
    ;;
  stop)
    echo "Stopping GNS Service"
    kill $(ps -ef | grep $JARFILENAME | awk '{print $2}') > ek_terminal.out 2>ek_terminal.out &
    ;;
  *)
    echo "Usage: gns-service {start|stop}"
    exit 1
    ;;
esac

exit 0


