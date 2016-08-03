#!/bin/sh
# Get host's UID so can set container's UID/GID to the same.
WHOAMI=`whoami`
export USERID=`grep $WHOAMI /etc/passwd | cut -d: -f 3`
docker build --build-arg USERID=$USERID -t timmus .
