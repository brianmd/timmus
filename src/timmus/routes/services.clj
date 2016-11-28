(println "loading timmus.routes.services")

(ns timmus.routes.services
  (:require [brianmd.db.store-mysql :as mysql]
            [cemerick.url :refer [url url-encode]]
            [cheshire
             [core :refer :all]
             [generate :refer [add-encoder encode-str remove-encoder]]]
            [clj-http.client :as client]
            [clojure
             [string :as str]
             [walk :refer :all]]
            [clojure.core.async
             :as
             a
             :refer
             [<!
              <!!
              >!
              >!!
              alts!
              alts!!
              buffer
              chan
              close!
              dropping-buffer
              go
              go-loop
              sliding-buffer
              thread
              timeout]]
            [clojure.java.io :as io]
            ;; [compojure.api.sweet :refer :all]
            [compojure.api.sweet :refer [defapi context GET POST PUT]]
            [config.core :refer [env]]
            [korma.core :as k]
            [ring.util
             [http-response :refer :all]
             [response :refer [redirect]]]
            [schema.core :as s]
            [summit.bh.queries :as bh-queries]
            [summit.db.relationships :refer :all]
            [summit.health-check.blue-harvest :as bh]
            [summit.papichulo.core
             :refer
             [create-papi-url
              papichulo-creds
              papichulo-url
              papichulo-url-with-creds]]
            [summit.punchout
             [core :as p]
             [order-message :as order-message]
             [order-request :as order-request]
             [punchout :refer [process-punchout-request-str]]]
            [summit.sales-associate.order-spec :refer [send-spec-email]]
            [summit.sap.project :refer [project projects]]
            [summit.step.manufacturer-lookup
             :refer
             [create-manufacturer-lookup-tables]]
            [summit.utils.core :refer :all]
            [timmus.routes.fake-axiall :refer [fake-axiall-punchout-request]]))

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




(def punchout-url "http://localhost:3449/api/punchout")
(def punchout-url "http://localhost:4007/api/punchout")



(defn add-product-to-empty-cart [email product-id]
  (let [cust (find-by-colname :customers :email email)
        cart-id (:main_cart_id cust)
        _ (if-not cart-id (throw (Exception. (str "Customer " email "does not have a cart."))))
        line-items (if cart-id (select-by-colname :line_items :cart_id cart-id))]
    (ppn "-----" "line-items" line-items)
    (if (empty? line-items)
      (k/insert  :line_items (k/values {:product_id product-id :cart_id cart-id :quantity 1 :created_at (db-timenow) :updated_at (db-timenow)}))
      )))
;; (add-product-to-empty-cart "'axiall@murphydye.com'" 31)

(defn process-fake-axiall-punchout []
  (ppn "faking")
  (let [request (fake-axiall-punchout-request)
        url "http://localhost:3449/api/punchout"]
    (ppn "request" request "url" url)
    (ppn
     (client/post
      url
      {:headers {"Content-Type" "application/xml; charset=utf-8"},
       :body request}))
    (ppn "posted")
    (add-product-to-empty-cart "axiall@murphydye.com" 31)
    )
  )

;; (process-fake-axiall-punchout)







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
   (let [hash (req-sans-unprintable req)
         filename (str "log/" (short-timenow) "-"
              (clojure.string/replace filename "/" "-")
              ".clj")
         ]
     (io/make-parents filename)
     (spit filename (pr-str hash)))))

(defn load-log-request [filename]
  (read-string (slurp (str "log/" filename ".clj"))))

(defonce log-request-chan (chan (sliding-buffer 30)))

;; (->
;;  (load-log-request "20160221142345-0700--api-punchout-rew")
;;  :cookies
;;  keys)

(defn log-request [& args]
  (>!! log-request-chan args))

;; (defn log-request-loop [& args]
;;   (go-loop []
;;     (println "wait for incoming log request ...")
;;     (let [args (<! log-request-chan)]
;;       (apply do-log-request args)
;;       (separately-log-request (first args) (:uri (first args)))
;;       (recur)
;;       )))




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
                 :body (mapv bh-queries/sorted-order-vitals (bh-queries/last-orders n))})



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



            (GET "/punchout/axiall" req
              (let [url (process-fake-axiall-punchout)
                    url "http://meta.murphydye.com:11000/punchout_login/axiall"
                    ]
                (redirect url 302)))

            (POST "/punchout/po" req
              (let [byte? (= (type (:body req)) org.httpkit.BytesInputStream)
                    req (if byte? (assoc req :body (slurp (:body req))) req)]
                (do-log-request req "punchout/po")
                (do-log-request {:punchout-po (localtime)})
                {:status 200,
                 :headers {"Content-Type" "application/xml; charset=utf-8"},
                 :body (order-request/xml-response 3)
                 }

                )
              )

            (POST "/punchout/accept-order-message/:id" req
              :path-params [id :- Long]
              ;; :form-params [cXML-urlencoded :- String]
              (ppn "in /punchout/accept/accept-order-message/" id)
              (ppn "-------" "------" "cXML-urlencoded" (:cXML-urlencoded (:params req)))
              (do-log-request req "punchout-accept-order-message")
              {:status 200,
               ;; :headers {"Content-Type" "application/xml; charset=utf-8"},
               :headers {"Content-Type" "text/json; charset=utf-8"},
               :body {:ok 200}
               })

            (GET "/punchout/order-message-form/:id" req
              :path-params [id :- Long]
              (ppn "/punchout/order-message-form/" id)
              (do-log-request req "punchout-order-message-form-get")
              (try
                (do-log-request
                 {:status 200,
                  :headers {"Content-Type" "text/json; charset=utf-8"},
                  :body (order-message/order-message-form id)
                  }
                 "punchout-order-message-form-get-response"
                 )
                (catch Exception e
                  {:status 404
                   :headers {"Content-Type" "text/xml; charset=utf-8"},
                   :body {:error (str "error: " (.getMessage e))}
                   })))

            (GET "/punchout/order-message/:id" req
              :path-params [id :- Long]
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
            ;; (POST "/punchout/order-message" []
              ;; curl -H "Content-Type: application/json" -X POST -d '{"id":4667}' http://localhost:3449/api/punchout/order-message
              :body-params [id :- Long]
              (ppn "in services, order-message")
              (ppn (str "/punchout/order-message/" id))
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
                  ;; :headers {"Content-Type" "application/xml; charset=utf-8"},
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
              (ppn "/punchout (post)")
              (let [byte? (= (type (:body req)) org.httpkit.BytesInputStream)
                    req (if byte? (assoc req :body (slurp (:body req))) req)]
                (do-log-request req "punchout-post")
                (do-log-request {:punchout (localtime)})
                (println "in post punchout")
                (def qqq req)
                (binding [*out* *err*]
                  (println "in post punchout"))
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
                  (println @mysql/entity-definitions)
                  (ok (clean-all @mysql/entity-definitions))
                  )

            (GET "/entities/query/:entity-name/:attribute-name/:val" []
                  :path-params [entity-name :- String, attribute-name :- String, val :- String]
                  ;(println @mysql/entity-definitions)
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
