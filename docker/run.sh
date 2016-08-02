#!/bin/sh
# note: must be run from the docker directory
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /home/summit/docker --add-host mysql.dev:10.10.0.110 --add-host mysql.prod:10.5.0.138 -v $(pwd -P)/..:/home/summit/docker -v $(pwd -P)/m2:/home/summit/.m2 -v $(pwd -P)/lein:/home/summit/.lein -v $(pwd -P)/../../utils:/home/summit/utils timmus /bin/bash
