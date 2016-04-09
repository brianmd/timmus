(ns timmus.routes.punchout
   (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            ;; [korma.core :refer :all]
            [korma.core :as k]
            [clojure.set]
            [clojure.string :as str]
            [clojure.walk :refer :all]
            [cheshire.core :refer :all]
            ;[clj-time.core :as t]
            ;[clj-time.format :as f]
            [net.cgrand.enlive-html :as html]
            [clojure.core.async :as  a
             :refer [>! <! >!! <!! go go-loop chan close! thread alts! alts!! timeout
                     buffer sliding-buffer dropping-buffer]]

            [cemerick.url :refer [url-encode url]]
            [clj-http.client :as client]
            [config.core :refer [env]]

            ;[compojure.core :refer [defroutes GET]]
            ;[compojure.core :refer [GET]]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]


            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            [summit.sales-associate.order-spec :refer [send-spec-email]]


            ;; [timmus.db.core :refer [*db*]]
            [brianmd.db.store-mysql :as mysql]
            [summit.punchout.core :as p]
            [summit.punchout.punchout :refer [process-punchout-request-str ]]
            [summit.punchout.order-message :refer [cxml-order-message] :as order-message]
            [summit.punchout.order-request :as p3]
            [summit.papichulo.core :refer [papichulo-url papichulo-creds create-papi-url papichulo-url-with-creds]]
            ))



(defapi punchout-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Punchout API"
                           :description "cXML Punchout"}}}}

  (context "/punchout" []
    :tags ["thingie"]

    (GET "/punchout/order-message/:id" req
      :path-params [id :- Long]
      (do-log-request req "punchout")
      (let [
            ;; id (order-message/last-city-order-num)
            ;; order-request (p/find-order-request id)
            ;; punchout-request (p/find-punchout-request (:punchout_id order-request))
            ;; hiccup (cxml-order-message order-request punchout-request)
            ]
        ;; (println "order message id: " id)
        {:status 200,
         :headers {"Content-Type" "text/xml; charset=utf-8"},
         ;; :body (p/create-cxml hiccup)
         :body (order-message/order-message-xml id)
         }
        ;; (ok (p/create-cxml hiccup))
        ))

    (GET "/punchout" req
      (println "in get punchout")
      (do-log-request req "punchout")
      ;; (separately-log-request req (:uri req))
      ;; (log-now {:status 200
      (do-log-request {:status 200
                       :headers {"Content-Type" "text/xml; charset=utf-8"}
                       :body (p/create-cxml (p/pong-response))
                       }) "punchout")

    (POST "/punchout" req
      (println "in post punchout")
      (def qqq req)
      (binding [*out* *err*]
        (println "in post punchout"))
      (do-log-request req "punchout")
      (let [byte? (= (type (:body req)) org.httpkit.BytesInputStream)
            req (if byte? (assoc req :body (slurp (:body req))) req)]
        (def aa req)
        (let [response (process-punchout-request-str (:body req))]
          (do-log-request {:status 200
                           :headers {"Content-Type" "text/xml; charset=utf-8"}
                           :body response} "punchout")
          )
        ))






    (GET "/plus" []
      :return       Long
      :query-params [x :- Long, {y :- Long 1}]
      :summary      "x+y with query-parameters. y defaults to 1."
      (ok (+ x y)))

    (GET "/hello" req
      :summary  "echoes the request"
      (ok {:hello "world"}))
    ))



