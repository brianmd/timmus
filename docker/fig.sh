#!/bin/sh
# note: must be run from the docker directory
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /root/docker -p 127.0.0.1:7002:7002 -p 127.0.0.1:3449:3449 -v $(pwd -P)/..:/root/docker -v $(pwd -P)/m2:/root/.m2 -v $(pwd -P)/lein:/root/.lein -v $(pwd -P)/../../utils:/root/utils timmus lein figwheel
