;; notes:
;; - need to add the timezone so database records are read in UTC
;;   :jvm-opts ["-Duser.timezone=UTC -server"]

(defproject timmus "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [domina "1.0.3"]
                 [selmer "1.0.4"]
                 [markdown-clj "0.9.87"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "1.0.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.2"]
                 [org.webjars/font-awesome "4.5.0"]
                 [org.webjars.bower/tether "1.1.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [compojure "1.5.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.0"]
                 [mount "0.1.10"]
                 [cprop "0.1.7"]
                 [luminus/config "0.8"]
                 [org.clojure/tools.cli "0.3.3"]
                 [luminus-nrepl "0.1.4"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 ;; [luminus-immutant "0.1.9" :exclusions [org.jboss.logging/jboss-logging]]
                 [luminus-immutant "0.1.9"]
                 [luminus-migrations "0.1.0"]
                 ;; [migratus "0.8.9"]
                 [conman "0.4.8"]
                                        ;[conman "0.3.0" :exclusions [org.slf4j/slf4j-api org.slf4j/slf4j-log4j12]]
                 [mysql/mysql-connector-java "5.1.6"]

                 [buddy "0.11.0"]
                 [buddy/buddy-core "0.13.0"]
                 [buddy/buddy-sign "1.1.0"]

                 [org.clojure/clojurescript "1.8.40" :scope "provided"]
                 ;; [reagent "0.6.0-alpha"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.22"]
                 [reagent-utils "0.1.7"]
                 [secretary "1.2.3"]
                 ;; [org.clojure/core.async "0.2.374" :exclusions [org.clojure/core.memoize]]
                 [cljs-ajax "0.5.4"]
                 [metosin/compojure-api "1.0.2"]
                 ;; [luminus-log4j "0.1.3" :exclusions [org.slf4j/slf4j-api org.slf4j/slf4j-log4j12]]
                 [luminus-log4j "0.1.3"]

                 ;; clj libraries

                 [com.taoensso/carmine "2.12.2"]
                 [clj-http "2.1.0"]
                 [org.clojure/data.csv "0.1.3"]
                 [semantic-csv "0.1.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [org.clojure/data.xml "0.0.8"]
                 ;; [im.chit/hara.reflect "2.2.17"]
                 [me.raynes/conch "0.8.0"]
                 [potemkin "0.4.3"]
                 [snipsnap "0.1.0" :exclusions [org.clojure/clojure]]
                 ;; [org.clojure/java.jdbc "0.4.2"]  removed because korma requires 0.3.7
                 [org.clojure/java.jdbc "0.3.7"]
                 [korma "0.4.2"]
                 [com.draines/postal "1.11.3"]              ; email support
                 [org.clojure/data.xml "0.0.8"]
                 [com.cemerick/url "0.1.1"]
                 [incanter "1.5.7"]
                 [dk.ative/docjure "1.10.0"]    ; access to excel


                 ;; cljs libraries

                 [org.webjars/jquery "2.2.0"]
                 [org.webjars/jquery-ui "1.11.4"]
                 [org.webjars/jquery-ui-themes "1.11.4"]
                 [org.webjars/jquery-window "5.03"]
                 [siren "0.2.0"]
                 [org.clojars.frozenlock/reagent-table "0.1.3"]
                 ;; [org.webjars/react-data-grid "0.14.16"]
                 ;; [org.webjars.bower/react-data-grid "0.13.21"]
                 [cljsjs/fixed-data-table "0.6.0-1"]

                 ;; [cljsjs/react-grid-layout "0.12.4-0"] ; for dashboard
                 ;; [org.webjars/codemirror "5.12"]

                 ;; [jayq "2.5.4"] ; for jquery
                 ;; [org.webjars/datatables "1.10.11"]

                 ;; [com.taoensso/tower "3.1.0-beta4"] ; i18n
                 ;; [ring "1.4.0" :exclusions [ring/ring-jetty-adapter]]
                 ;; [ch.qos.logback/logback-classic "1.1.3"] ; for easier log setup
                 ;; [com.taoensso/encore "2.36.0"]  ; core clojure utils
;; [metosin/ring-swagger-ui "2.1.4-0"]
                 ;this one caused errors:
                 ;[luminus-immutant "0.1.0"]
                 ;; [ring-cors "0.1.7"]


                 [clojurewerkz/neocons "3.1.0"]  ; neo4j

                 ;; [com.datomic/datomic-pro "0.9.5344" :exclusions [joda-time org.slf4j/jul-to-slf4j org.slf4j/slf4j-nop org.slf4j/slf4j-over-log4j org.slf4j/slf4j org.slf4j/slf4j-log4j12]]

                 ;; [funcool/cats "1.2.1"]
                 ;; [org.clojure/algo.monads "0.1.5"]
                 [clj-time "0.11.0"]
                 [re-com "0.8.0"]
                 [re-frame "0.7.0"]
                 ;[criterium "0.4.3"]                        ; benchmarking

                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]

                 ;; [com.h2database/h2 "1.4.190"]
                 ;; [gyptis "0.2.2" :exclusions [io.aviso/pretty com.taoensso/timbre]]   ;; vega plots, conflicts w/ clj-ns-browser
                 ;; [medley "0.7.1"]  ;lightweight library of useful Clojure functions
                 [com.taoensso/carmine "2.12.2"]
                 [clj-tagsoup/clj-tagsoup "0.3.0" :exclusions [org.clojure/clojure]]
                 ;; [lein-ancient "0.6.8"]
                 ;; [com.jakemccrary/lein-test-refresh "0.13.0"]
                 ;; [zookeeper-clj "0.9.4"]
                 ;; [zookeeper-clj "0.9.4" :exclusions [org.slf4j/slf4j-log4j12]]
                 [com.rpl/specter "0.9.2"]
                 [resque-clojure "0.3.0"]
                 ;; [local/ojdbc6 "11.2.0.4"]    ;; oracle

                 [argo "0.1.2"]   ;; for json api

                 ;; https://github.com/binaryage/cljs-devtools-sample/blob/master/project.clj
                 ;; [binaryage/devtools "0.7.0"]

                 ;; [com.murphydye/utils "0.1.0-SNAPSHOT"]
                 ]

  :min-lein-version "2.0.0"
  :uberjar-name "timmus.jar"
  :jvm-opts ["-Duser.timezone=UTC -server"]
  :resource-paths ["resources" "target/cljsbuild" "jars/*"]

  :main timmus.core
  :migratus {:store :database}

  :plugins [
            [lein-environ "1.0.2"]
            [lein-cprop "1.0.1"]
            [migratus-lein "0.2.6"]
            [lein-cljsbuild "1.1.1"]
            [lein-uberwar "0.2.0"]
            [lein-expand-resource-paths "0.0.1"]
            ]
  :uberwar
  {:handler timmus.handler/app
   :init timmus.handler/init
   :destroy timmus.handler/destroy
   :name "timmus.war"}
  
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src-cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}

  :profiles
  {:uberjar {:omit-source true
             :env {:production true}
              :prep-tasks ["compile" ["cljsbuild" "once"]]
              :cljsbuild
              {:builds
               {:app
                {:source-paths ["env/prod/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false
                  :closure-warnings
                  {:externs-validation :off :non-standard-jsdoc :off}}}}} 

             :aot :all
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :prod    [:project/prod :profiles/prod]
   :project/dev  {:dependencies [[prone "1.0.2"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.1"]
                                 [com.cemerick/piggieback "0.2.1"]
                                 [lein-figwheel "0.5.3-2" :exclusions [org.clojure/core.memoize]]
                                 [org.clojure/tools.namespace "0.2.11"]
                                 [org.clojure/java.classpath "0.2.0"]
                                 [mvxcvi/puget "1.0.0"]
                                 ]
                  :plugins [[lein-figwheel "0.5.2" :exclusions [org.clojure/clojure org.clojure/core.memoize]] [org.clojure/clojurescript "1.7.228" :exclusions [org.clojure/clojure]]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:source-paths ["env/dev/cljs"]
                      :compiler
                      {:main "timmus.app"
                       :asset-path "/js/out"
                       :optimizations :none
                       :source-map true}}}}
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :server-ip "0.0.0.0"  ;; default is localhost
                   :nrepl-port 7002
                   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                   :css-dirs ["resources/public/css"]
                   :ring-handler timmus.handler/app}
                  :source-paths ["env/dev/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns timmus.core
                                 :port 7003
                                 :host "0.0.0.0"
                                 :timeout 320000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]
                  ;;when :nrepl-port is set the application starts the nREPL server on load
                  :env {:dev        true
                        :port       3007
                        ;; :nrepl-port 7000
                        }}
   :project/test {:env {:test       true
                        :port       3001
                        ;; :nrepl-port 7001
                        }}
   :profiles/dev {}
   :profiles/test {}})
