(println "loading timmus.routes.services")

(ns timmus.routes.services
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
            ;; [clj-http.client :as client]
            [config.core :refer [env]]

            ;[compojure.core :refer [defroutes GET]]
            ;[compojure.core :refer [GET]]
            [cheshire.generate :refer [add-encoder encode-str remove-encoder]]

            [dk.ative.docjure.spreadsheet :as xls]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            [summit.sales-associate.order-spec :refer [send-spec-email]]


            ;; [timmus.db.core :refer [*db*]]
            [brianmd.db.store-mysql :as mysql]
            [summit.punchout.core :as p]
            [summit.punchout.punchout :refer [process-punchout-request-str ]]
            [summit.punchout.order-message :as order-message]
            [summit.punchout.order-request :as p3]
            [summit.papichulo.core :refer [papichulo-url papichulo-creds create-papi-url papichulo-url-with-creds]]
            [summit.health-check.blue-harvest :as bh]
            [summit.step.manufacturer-lookup :refer [create-manufacturer-lookup-tables]]

            [summit.sap.project :refer [projects project]]
            [summit.bh.queries :as bh-queries]
            ))
;(-> @((-> customer :rel) "cart") :fk-key)

;; (def fffff (into [] (map :matnr (k/select :mdm.price (k/fields :matnr)))))
;; (first fffff)
;; (type fffff)
;; (spit "/Users/bmd/code/kupplervati/matnrs.edn" fffff )

#_(comment
(.reset (:body aa))
(def bb (slurp (:body aa)))
bb 
(:body aa)
(p/process-punchout-request-str (:body aa))
)







(defn create-fake-xls-stream []
  (let [wb (xls/create-workbook "Price List"
                            [["Name" "Price"]
                             ["Foo Widget" 100]
                             ["Bar Widget" 200]])
        sheet (xls/select-sheet "Price List" wb)
        header-row (first (xls/row-seq sheet))]
    (do
      (xls/set-row-style! header-row (xls/create-cell-style! wb {:background :yellow,
                                                         :font {:bold true}}))
      (with-open [w (clojure.java.io/output-stream "spread2.xlsx")]
        (xls/save-workbook! w wb)))))
;; (create-fake-xls-stream)






(def inspector-vals (atom {:a 9}))

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
  (let [custs (k/select customer (k/limit 2))]
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




;; (println "\n\n\nhelp me, i'm in services.clj")
;; (println (k/select :customers (k/database (find-db :bh-local)) (k/where {:email "brian@murphydye.com"})))
;; (println "done with select command\n\n\n")

(defn separately-log-request
  "stores request in its own file as edn"
  ([req] (separately-log-request req "request"))
  ([req filename]
   (let [hash (req-sans-unprintable req)]
     (spit (str "log/" (short-timenow) "-"
                (clojure.string/replace filename "/" "-")
                ".clj")
           (pr-str hash)))))

(defn load-log-request [filename]
  (read-string (slurp (str "log/" filename ".clj"))))

(defonce log-request-chan (chan (sliding-buffer 30)))

;; (->
;;  (load-log-request "20160221142345-0700--api-punchout-rew")
;;  :cookies
;;  keys)

(defn log-request [& args]
  (>!! log-request-chan args))

(defn log-request-loop [& args]
  (go-loop []
    (println "wait for incoming log request ...")
    (let [args (<! log-request-chan)]
      (apply do-log-request args)
      (separately-log-request (first args) (:uri (first args)))
      (recur)
      )))




;; (log-request-loop)



#_(defn forward-request [to-uri req]
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

            ;; (GET "/papi/*" req
            ;;       (let [response (forward-request (papichulo-url-with-creds) req)]
            ;;         response))

            (GET "/admin/queries" req
              (ok (query-keys)))

            (GET "/all-okay" req
                {:status 200,
                 :body {:bh-ok (bh/all-okay?)}})

            (GET "/last-orders/:n" req
                :path-params [n :- Long]
                {:status 200,
                 :body (mapv bh-queries/order-vitals (bh-queries/last-orders n))})

            (GET "/project/:id" req
              :path-params [id :- Long]
              {:status 200,
               :headers {"Content-Type" "text/json; charset=utf-8"},
               :body (project id)}
              )

            (GET "/projects/:id" req
              :path-params [id :- Long]
              {:status 200,
               ;; :headers {"Content-Type" "application/edn; charset=utf-8"},
               :body (projects id)}
              )

            (GET "/punchout/order-message/:id" req
              :path-params [id :- Long]
              (ppn "in services.clj")
              (ppn "/punchout/order-message/" id)
              (do-log-request req "punchout-order-message-get")
                (try
                  (do-log-request
                   (if false
                     {:status 200,
                      :headers {"Content-Type" "text/json; charset=utf-8"},
                      ;; :body (order-message/order-message (order-message/retrieve-order-data id))
                      :body (order-message/order-message-hiccup (order-message/retrieve-order-data id))
                      }
                     {:status 200,
                      :headers {"Content-Type" "application/xml; charset=utf-8"},
                      :body (order-message/order-message-xml (order-message/retrieve-order-data id))
                      })
                   "punchout-order-message-get-response"
                   )
                  (catch Exception e
                    {:status 404
                     :headers {"Content-Type" "text/xml; charset=utf-8"},
                     :body {:error (str "error: " (.getMessage e))}
                     })))

            (POST "/punchout/order-message" req
              ;; curl -H "Content-Type: application/json" -X POST -d '{"id":4667}' http://localhost:3449/api/punchout/order-message
              :body-params [id :- Long]
              (do-log-request req "punchout-order-message-post")
              (let [
                    ;; id (order-message/last-city-order-num)
                    ;; order-request (p/find-order-request id)
                    ;; punchout-request (p/find-punchout-request (:punchout_id order-request))
                    ;; hiccup (cxml-order-message order-request punchout-request)
                    ]
                ;; (do-log-request {:id id} "punchout")
                ;; (do-log-request req "punchout")
                (order-message/submit-order-message id)
                (do-log-request
                 {:status 200,
                  :headers {"Content-Type" "text/json"}
                  :body {:id id}}
                 "punchout-order-message-post-response")
                ;; (try
                ;;   (do-log-request
                ;;    {:status 200,
                ;;     :headers {"Content-Type" "text/xml; charset=utf-8"},
                ;;     :body (order-message/order-message-xml (order-message/retrieve-order-data id))
                ;;     ;; :body (p/create-cxml hiccup)
                ;;     }
                ;;    "punchout"
                ;;    )
                ;;   (catch Exception e
                ;;     {:status 404
                ;;      :headers {"Content-Type" "text/xml; charset=utf-8"},
                ;;      :body {:error (str "error: " (.getMessage e))}
                ;;      }))
                ))

            (GET "/punchout" req
              (println "in get punchout")
              (do-log-request req "punchout-get")
              (do-log-request
               {:status 200
                :headers {"Content-Type" "text/xml; charset=utf-8"}
                :body (p/create-cxml (p/pong-response))
                } "punchout-response"))

            (POST "/punchout" req
              (do-log-request req "punchout-post")
              (do-log-request {:punchout (localtime)})
                   (println "in post punchout")
                   (def qqq req)
                   (binding [*out* *err*]
                     (println "in post punchout"))
                   (let [byte? (= (type (:body req)) org.httpkit.BytesInputStream)
                         req (if byte? (assoc req :body (slurp (:body req))) req)]
                     ;; (do-log-request req "punchout")
                     (let [response (process-punchout-request-str (:body req))]
                       ;; text/xml; charset=utf-8
                       (do-log-request {:status 200
                                 :headers {"Content-Type" "text/xml; charset=utf-8"}
                                 :body response} "punchout-response")
                       )))

            (GET "/manufacturerlookup" req
              (do
                (create-manufacturer-lookup-tables)
                {:status 200
                 :headers {"Content-Type" "text/json; charset=utf-8"},
                 :body {:message "http://10.9.0.105:3449/manufacturer-lookup.zip"}}))
                 ;; :body {:message "http://192.168.2.5:3449/manufacturer-lookup.zip"}}))


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
                  (ok (k/select contact-email (k/where {:type "Order"}) (k/order :created_at :desc) (k/limit 5)
                              (k/fields :email :payment_method :type :sap_document_number :total_price :cart_id))))

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
                        compared (k/exec-raw (vector compare-sql) :results)]
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






(println "done loading timmus.routes.services")
