#!/bin/bash

# This script opens 4 terminal windows.

INTERFACE="enp0s31f6"

while true; do
  echo "bringing interface up"
  sudo ifconfig $INTERFACE up
  sleep 60s

  echo "bringing interface down"
  sudo ifconfig $INTERFACE down
  sleep 40s
done
