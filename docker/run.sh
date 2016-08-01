#!/bin/sh
# note: must be run from the docker directory

# need setup from inside docker thread
# cd docker
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /home/summit/docker -v $(pwd -P)/..:/home/summit/docker -v $(pwd -P)/m2:/home/summit/.m2 -v $(pwd -P)/lein:/home/summit/.lein -v $(pwd -P)/../../utils:/home/summit/utils timmus /bin/bash
