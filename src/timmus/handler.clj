(ns timmus.handler
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            [timmus.layout :refer [error-page]]
            [timmus.routes.home :refer [home-routes]]
            [timmus.routes.services :refer [service-routes]]
            [timmus.routes.punchout :refer [punchout-routes]]
            [timmus.middleware :as middleware]
            [clojure.tools.logging :as log]
            [compojure.route :as route]
            [config.core :refer [env]]
            [mount.core :as mount]
            ; [ring.middleware.cors :refer [wrap-cors]]
            [luminus.logger :as logger]

            [argo.core :refer [defapi defresource]]

            [timmus.config :refer [defaults]]
            [timmus.routes.websockets :refer [websocket-routes]]
            [summit.sap.routes :refer [sap-routes]]
            ))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (logger/init env)
  (doseq [component (:started (mount/start))]
    (log/info component "started"))
  ((:init defaults)))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (log/info "timmus is shutting down...")
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (log/info "shutdown complete!"))

(def app-routes
  (routes
    (var sap-routes)
    (var websocket-routes)
    (var service-routes)
    ;; (var punchout-routes)
    (wrap-routes #'home-routes middleware/wrap-csrf)
    ;(wrap-cors :access-control-allow-origin [#".*"]
    ;  :access-control-allow-methods [:get :put :post :delete])
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
