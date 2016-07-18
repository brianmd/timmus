#!/bin/sh
# note: must be run from the docker directory

# need setup from inside docker thread
# cd docker
mkdir -p m2
mkdir -p lein
docker run -ti --rm -w /root/docker -p 127.0.0.1:7000:7000 -p 127.0.0.1:3007:3007 -v $(pwd -P)/..:/root/docker -v $(pwd -P)/m2:/root/.m2 -v $(pwd -P)/lein:/root/.lein -v $(pwd -P)/../../utils:/root/utils timmus lein repl
