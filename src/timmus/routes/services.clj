(ns timmus.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [korma.core :refer :all]
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


            [timmus.db.core :refer [*db*]]
            [brianmd.db.store-mysql :as mysql]
            [summit.punchout.core :as p]
            [summit.punchout.punchout :refer [process-punchout-request-str ]]
            [summit.punchout.order-message :refer [cxml-order-message]]
            [summit.punchout.order-request :as p3]
            [summit.papichulo.core :refer [papichulo-url papichulo-creds create-papi-url papichulo-url-with-creds]]
            ))
;(-> @((-> customer :rel) "cart") :fk-key)

#_(comment
(.reset (:body aa))
(def bb (slurp (:body aa)))
bb 
(:body aa)
(p/process-punchout-request-str (:body aa))
)

(defn map->table [hash]
  {:headers (keys (first hash))
   :rows    (map vals hash)})
;(map->table [{:a 9 :b 99}])


#_(def queries-array
  [:orders "select * from contact_emails where type='ORDER'"
   :number-of-orders "select count(*) from contact_emails where type='ORDER'"
   ])
(def queries-array
  ["Orders" "select * from contact_emails where type='ORDER'"
   "Number of Orders" "select count(*) from contact_emails where type='ORDER'"
   "nnNumber of Orders" "select count(*) from contact_emails where type='ORDER'"
   ])

(defn query-named [name]
  ())

(defn query-keys []
  (map first (partition 2 queries-array)))
;; (query-keys)


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



(defn req-sans-unprintable [req]
  #_["compojure.api.middleware/options",
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
     x))

(defn req->printable [req]
  (clean-all (req-sans-unprintable req)))

(defn separately-log-request
  "stores request in its own file as edn"
  ([req] (separately-log-request req "request"))
  ([req filename]
   (let [hash (req-sans-unprintable req)]
     (spit (str "log/" (short-timenow) "-"
                (clojure.string/replace filename "/" "-")
                ".clj")
           (pr-str hash)))))

(defn log-now [obj]
  "stores request in its own file as edn"
   (let [filename (summit.punchout.core/uuid)]
     (spit (str "log/separate/" filename)
           (pr-str obj)))
  obj)

(defn load-log-request [filename]
  (read-string (slurp (str "log/" filename ".clj"))))

(defonce log-request-chan (chan (sliding-buffer 30)))

;; (->
;;  (load-log-request "20160221142345-0700--api-punchout-rew")
;;  :cookies
;;  keys)

(defn log-request [& args]
  (>!! log-request-chan args))

(defn do-log-request
  ([req] (do-log-request req "requests"))
  ([req filename]
   (spit (str "log/" filename ".log")
         (with-out-str
           (pp
            [(timenow)
             (if (map? req) (req->printable req) req)
             (str "end " (timenow))]))
         :append true)))
;; (do-log-request 3 "requests")

(defn log-request-loop [& args]
  (go-loop []
    (println "wait for incoming log request ...")
    (let [args (<! log-request-chan)]
      (apply do-log-request args)
      (separately-log-request (first args) (:uri (first args)))
      (recur)
      )))




;; (log-request-loop)



(defn forward-request [to-uri req]
  (let [uri (str to-uri ((:params req) :*))
        method (:request-method req)
        base-req (select-keys req [:headers :cookies :body :remote-addr
                                   :multipart-params :query-params :form-params :form-param-encoding
                                   :content-type :content-length :json-opts :transit-opts
                                   :basic-auth :digest-auth :oauth-token])
        ]
    (client/request
     (assoc base-req
            :method method
            :url uri
            :decompress-body false
            :debug true
            :debug-body true
            )
      )))

;; (def x {:a 3 :b "432"})
;; (select-keys x [:a :c])



(defapi service-routes
  {:swagger {:ui "/swagger-ui"
             :spec "/swagger.json"
             :data {:info {:version "1.0.0"
                           :title "Sample API"
                           :description "Sample Services"}}}}
  ;(ring.swagger.ui/swagger-ui
  ; "/swagger-ui")
  ;;JSON docs available at the /swagger.json route
  ;(swagger-docs
  ;  {:info {:title "Sample api"}})
  (context "/api" []
            :tags ["thingie"]

            (GET "/plus" []
                  :return       Long
                  :query-params [x :- Long, {y :- Long 1}]
                  :summary      "x+y with query-parameters. y defaults to 1."
                  (ok (+ x y)))

            (POST "/minus" []
                   :return      Long
                   :body-params [x :- Long, y :- Long]
                   :summary     "x-y with body-parameters."
                   (ok (- x y)))

            (GET "/times/:x/:y" []
                  :return      Long
                  :path-params [x :- Long, y :- Long]
                  :summary     "x*y with path-parameters"
                  (ok (* x y)))

            (POST "/divide" []
                   :return      Double
                   :form-params [x :- Long, y :- Long]
                   :summary     "x/y with form-parameters"
                   (ok (/ x y)))

            (GET "/power" []
                  :return      Long
                  :header-params [x :- Long, y :- Long]
                  :summary     "x^y with header-parameters"
                  (ok (long (Math/pow x y))))

            (GET "/echo/*" req
                  :summary  "echoes the request"
                  (ok (req->printable req))
                  ;; (ok {:a 3})
                  ;; (let [bad-params [                        ; these throw errors when json-izing
                  ;;                   :compojure.api.middleware/options
                  ;;                   :async-channel
                  ;;                   ]
                  ;;       x (apply dissoc (concat [req] bad-params))]
                  ;;   (ok (clean-all (assoc x :abcdef 32)))
                  ;;   ;(ok (clean-all x))
                  ;;  )
                  )

            (GET "/papi/*" req
                  (let [response (forward-request (papichulo-url-with-creds) req)]
                    response))

            (GET "/admin/queries" req
              (ok (query-keys)))

            (GET "/punchout/order-message/:id" req
              :path-params [id :- Long]
              (do-log-request req "punchout")
              (let [order-request (p/find-order-request id)
                    punchout-request (p/find-punchout-request (:punchout_id order-request))
                    hiccup (cxml-order-message order-request punchout-request)]
                {:status 200,
                 :headers {"Content-Type" "text/xml; charset=utf-8"},
                 :body (p/create-cxml hiccup)}
                ;; (ok (p/create-cxml hiccup))
                ))

            (GET "/punchout" req
                  (println "in get punchout")
                  (do-log-request req "punchout")
                  ;; (separately-log-request req (:uri req))
                  (log-now {:status 200
                            :headers {"Content-Type" "text/xml; charset=utf-8"}
                            :body (p/create-cxml (p/pong-response))
                            }))
                  ;; (ok (log-now (req->printable req))))

            (POST "/punchout" req
                   (println "in post punchout")
                   (let [byte? (= (type (:body req)) org.httpkit.BytesInputStream)
                         req (if byte? (assoc req :body (slurp (:body req))) req)]
                     (def aa req)
                     (do-log-request req "punchout")
                     ;; (log-request req)
                     (let [response (process-punchout-request-str (:body req))]
                       ;; text/xml; charset=utf-8
                       (log-now {:status 200
                                 :headers {"Content-Type" "text/xml; charset=utf-8"}
                                 :body response})
                       ;; (ok response)
                       )
                     ;; (separately-log-request req (:uri req))
                     ;; (ok (req->printable req))
                     ))



            (PUT "/echo" []
                  :return   [{:hot Boolean}]
                  :body     [body [{:hot Boolean}]]
                  :summary  "echoes a vector of anonymous hotties"
                  (ok body))

            (POST "/echo" []
                   :return   (s/maybe Thingie)
                   :body     [thingie (s/maybe Thingie)]
                   :summary  "echoes a Thingie from json-body"
                   (ok thingie))

            (GET "/hello" []
                  :query-params [name :- String]
                  (ok {:message (str "Hello, " name)}))

            (GET "/orders" []
                  ;(ok (select contact-email (where {:type "Order"}) (order :created_at :desc) (limit 5))))
                  (ok (select contact-email (where {:type "Order"}) (order :created_at :desc) (limit 5)
                              (fields :email :payment_method :type :sap_document_number :total_price :cart_id))))

            (GET "/customers" []
                  ;(let [custs (select customer (limit 20))]
                  ;  (ok {:headers (keys custs) :rows (vals custs)}))
                  (println "getting customers ...")
                  (let [c (custs)]
                    (println c)
                    (ok c)
                    )
                  )

            (GET "/entities/definitions" []
                  (println "&&&&&&&&&&&&&&&&&&&&&&&&&&^^^^^^^^^^^")
                  ;(println mysql/entity-definitions)
                  (ok (clean-all mysql/entity-definitions)))

            (GET "/entities/query/:entity-name/:attribute-name/:val" []
                  :path-params [entity-name :- String, attribute-name :- String, val :- String]
                  ;(println mysql/entity-definitions)
                  (println "getting entity" [entity-name attribute-name val] " ...")
                  (ok (clean-all (mysql/attribute-query (keyword entity-name) (keyword attribute-name) val)))
                  )

            (GET "/customer/:email" []
                  :path-params [email :- String]
                  (ok (mysql/attribute-query :customers :email email)))

            (GET "/platt-prices" []
                  (println "in platt-prices")
                  (let [compare-sql "select p1.upc, p1.price sap, p2.price platt, (p1.price-p2.price)/p2.price*100 increase from mdm.prices p1 join mdm.prices p2 on p1.upc=p2.upc where p1.source='sap' and p2.source='platt' and p1.price>0 order by increase"
                        compared (exec-raw (vector compare-sql) :results)]
                    (println "upc comparison count: " (count compared))
                    (ok (map->table compared))))

            (GET "/order-spec/:email/:ordernum" []
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

  (context "/context" []
            :tags ["context*"]
            :summary "summary inherited from context"
            (context "/:kikka" []
                      :path-params [kikka :- s/Str]
                      :query-params [kukka :- s/Str]
                      (GET "/:kakka" []
                            :path-params [kakka :- s/Str]
                            (ok {:kikka kikka
                                 :kukka kukka
                                 :kakka kakka}))))
  )






