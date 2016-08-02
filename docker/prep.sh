#!/bin/sh
# note: must be run from the docker directory

git clone git@bitbucket.org:summitelectricsupply/jars.git /home/summit/docker
cd /home/summit/jars && cp -r linux-64bit .

