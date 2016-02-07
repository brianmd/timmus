(ns timmus.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [korma.core :refer :all]
            [clojure.set]
            ;[clojure.string :as str]
            [clojure.walk :refer :all]
            [cheshire.core :refer :all]
            ;[clj-time.core :as t]
            ;[clj-time.format :as f]

            [timmus.db.relationships :refer :all]
            [timmus.sales-associate.order-spec :refer [send-spec-email]]



            ;[cheshire.generate :refer [add-encoder encode-str remove-encoder]]


            [compojure.core :refer [defroutes GET]]

            [timmus.utils.core :refer :all]
            [timmus.db.core :refer [*db*]]
            [brianmd.db.store-mysql :as mysql]
            ))
;customer
;mysql/entity-definitions
;cart
;(-> customer :rel)
;@((-> customer :rel) "cart")
;(-> @((-> customer :rel) "cart") :fk-key)


(s/defschema Thingie {:id Long
                      :hot Boolean
                      :tag (s/enum :kikka :kukka)
                      :chief [{:name String
                               :type #{{:id String}}}]})


(defn custs []
  (let [custs (select customer (limit 2))]
    {:headers (vec (map name (keys (first custs))))
     :rows    [(vec (range 31))]
     ;:rows (map vals custs)
     })
  ;(let [custs (select customer (limit 20))]
  ;  {:headers (keys custs) :rows (vals custs)})
  )
;(custs)
;(let [custs (select customer (limit 2))]
;  {:headers (keys (first custs)) :rows (map vals custs)})


;; test a route ...
;(defn stringify-all [x]
;  (postwalk #(if(keyword? %)(name %) %) x))

;(def example-route (GET "/" [] "<html>...</html>"))
;(def example-route (GET "/" req req))
;(stringify-all [:a "b" 3])
;(stringify-all {:server-port 80
;                :server-name "127.0.0.1"
;                :remote-addr "127.0.0.1"
;                :uri "/"
;                :scheme :http
;                :headers {}
;                :request-method :get})
;(def example-route (GET "/" req (stringify-keys req)))
;(def example-route (GET "/" req (do (println req) (println (stringify-all req)))))
;(def example-route (GET "/" req (do req)))
;(def example-route (GET* "/" req (do (println req) (println (stringify-all req)) (stringify-all req))))
;(stringify-all)
;(example-route {:server-port 80
;                :server-name "127.0.0.1"
;                :remote-addr "127.0.0.1"
;                :uri "/"
;                :scheme :http
;                :headers {}
;                :request-method :get})
;
;(walk #(* 2 %) identity [1 2 3 4 5])
;(walk (fn [[k v]] [k (* 10 v)]) identity {:a 1 :b 2 :c 3})
;(walk (fn [[k v]] [k (* 10 v)]) identity {:a 1 :b 2 :c 3})
;(def thing {:page/tags [{:tag/category "lslsls"}]})
;(postwalk #(if(keyword? %)(keyword (name %)) %) thing)
;(postwalk #(if(keyword? %)(name %) %) thing)
;(keyword? :a)

(defapi service-routes
  (ring.swagger.ui/swagger-ui
   "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Sample api"}})
  (context* "/api" []
            :tags ["thingie"]

            (GET* "/plus" []
                  :return       Long
                  :query-params [x :- Long, {y :- Long 1}]
                  :summary      "x+y with query-parameters. y defaults to 1."
                  (ok (+ x y)))

            (POST* "/minus" []
                   :return      Long
                   :body-params [x :- Long, y :- Long]
                   :summary     "x-y with body-parameters."
                   (ok (- x y)))

            (GET* "/times/:x/:y" []
                  :return      Long
                  :path-params [x :- Long, y :- Long]
                  :summary     "x*y with path-parameters"
                  (ok (* x y)))

            (POST* "/divide" []
                   :return      Double
                   :form-params [x :- Long, y :- Long]
                   :summary     "x/y with form-parameters"
                   (ok (/ x y)))

            (GET* "/power" []
                  :return      Long
                  :header-params [x :- Long, y :- Long]
                  :summary     "x^y with header-parameters"
                  (ok (long (Math/pow x y))))

            (GET* "/echo" req
                  :summary  "echoes the request"
                  ["compojure.api.middleware/options",
                   "cookies",
                   "remote-addr",
                   "ring.swagger.middleware/data",
                   "params",
                   "flash",
                   "route-params",
                   "headers",
                   "async-channel",
                   "server-port",
                   "content-length",
                   "form-params",
                   "compojure/route",
                   "websocket?",
                   "session/key",
                   "query-params",
                   "content-type",
                   "path-info",
                   "character-encoding",
                   "context",
                   "uri",
                   "server-name",
                   "query-string",
                   "body",
                   "multipart-params",
                   "scheme",
                   "request-method",
                   "session"]
                  (let [bad-params [                        ; these throw errors when json-izing
                                    :compojure.api.middleware/options
                                    :async-channel
                                    ]
                        x (apply dissoc (concat [req] bad-params))]
                    (ok (clean-all x))
                   )
                  )

            (PUT* "/echo" []
                  :return   [{:hot Boolean}]
                  :body     [body [{:hot Boolean}]]
                  :summary  "echoes a vector of anonymous hotties"
                  (ok body))

            (POST* "/echo" []
                   :return   (s/maybe Thingie)
                   :body     [thingie (s/maybe Thingie)]
                   :summary  "echoes a Thingie from json-body"
                   (ok thingie))

            (GET* "/hello" []
                  :query-params [name :- String]
                  (ok {:message (str "Hello, " name)}))

            (GET* "/customers" []
                  ;(let [custs (select customer (limit 20))]
                  ;  (ok {:headers (keys custs) :rows (vals custs)}))
                  (println "getting customers ...")
                  (let [c (custs)]
                    (println c)
                    (ok c)
                    )
                  )

            (GET* "/db/column-info" []
                  (println "&&&&&&&&&&&&&&&&&&&&&&&&&&^^^^^^^^^^^")
                  (println mysql/entity-definitions)
                  (ok (clean-all mysql/entity-definitions)))

            (GET* "/db/query/:entity/:attribute-name/:val" []
                  :path-params [entity :- String, attribute-name :- String, val :- String]
                  (println mysql/entity-definitions)
                  (println "getting entity" entity " ...")
                  (ok (clean-all (mysql/attribute-query (keyword entity) (keyword attribute-name) val)))
                  )

            (GET* "/customer/:email" []
                  :path-params [email :- String]
                  (ok (mysql/attribute-query :customers :email email)))

            (GET* "/order-spec/:email/:ordernum" []
                 ;:return Long
                 :path-params [email :- String, ordernum :- Long]
                 :summary "emails specs related to order-num to email"
                  (let [email (str email "@summit.com")]
                    (println "order-spec" email ordernum)
                    (try
                      (send-spec-email email ordernum)
                      (println "sent email for" email ordernum)
                      (ok {:email email :order-num ordernum})
                      (catch Exception e
                        (println "oops" e)
                        (not-found {:email email :order-num ordernum :error-msg (.getMessage e)}))
                      )))
                    ;(Thread/sleep 1000)
                    ;(ok {:email email :order-num ordernum})
                    ;(bad-request {:email email :order-num ordernum})
                    ;(throw "nope")
            )

  (context* "/context" []
            :tags ["context*"]
            :summary "summary inherited from context"
            (context* "/:kikka" []
                      :path-params [kikka :- s/Str]
                      :query-params [kukka :- s/Str]
                      (GET* "/:kakka" []
                            :path-params [kakka :- s/Str]
                            (ok {:kikka kikka
                                 :kukka kukka
                                 :kakka kakka})))))


