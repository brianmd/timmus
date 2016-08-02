#!/bin/sh
# note: must be run from the docker directory
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /home/summit/docker --add-host mysql.dev:10.10.0.110 --add-host mysql.prod:10.5.0.138 -p 127.0.0.1:7002:7002 -p 0.0.0.0:3449:3449 -v $(pwd -P)/..:/home/summit/docker -v $(pwd -P)/m2:/home/summit/.m2 -v $(pwd -P)/lein:/home/summit/.lein -v $(pwd -P)/../../utils:/home/summit/utils timmus lein figwheel
