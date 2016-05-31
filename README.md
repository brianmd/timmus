# timmus

This app was created with:
   lein new luminus timmus +cljs +swagger +mysql +auth +war

To install this project:

    $ git clone git@github.com:brianmd/timmus.git
    $ cd timmus
    $ git clone git@bitbucket.org:summitelectricsupply/jars.git

  also will need [dev|qas|prd].jcoDestination and a profiles.clj

You will need to download the oracle jdbc driver as well. Change the directory as appropriate in the following command:

    $ mvn install:install-file -X -DgroupId=local -DartifactId=ojdbc6 -Dversion=11.2.0.4 -Dpackaging=jar -Dfile=/Users/bmd/code/instantclient_11_2/ojdbc6.jar -DgeneratePom=true

To get maven on Macs:

    $ brew update
    $ brew install maven
    
## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

profiles-example.clj may be used as a template for /profiles.clj.

To start a web server for the application, run:

    lein run

## License

Copyright © 2016 FIXME
