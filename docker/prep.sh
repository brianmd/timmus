#!/bin/sh
# note: must be run from the docker directory

git clone git@bitbucket.org:summitelectricsupply/jars.git /home/summit/docker
cd /home/summit/jars && cp -r linux-64bit .

cd jars/instantclient_11_2
mvn install:install-file -X -DgroupId=local -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar -Dfile=ojdbc6.jar -DgeneratePom=true


