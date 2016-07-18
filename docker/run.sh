#!/bin/sh
# note: must be run from the docker directory

# need setup from inside docker thread
# cd docker
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /root/docker -v $(pwd -P)/..:/root/docker -v $(pwd -P)/m2:/root/.m2 -v $(pwd -P)/lein:/root/.lein -v $(pwd -P)/../../utils:/root/utils timmus /bin/bash
