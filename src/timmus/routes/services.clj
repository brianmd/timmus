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
            [net.cgrand.enlive-html :as html]

            [timmus.db.relationships :refer :all]
            [timmus.sales-associate.order-spec :refer [send-spec-email]]



            ;[cheshire.generate :refer [add-encoder encode-str remove-encoder]]

            [cemerick.url :refer [url-encode url]]
            [clj-http.client :as client]
            [config.core :refer [env]]


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
(select contact-email (where {:type "Order"}) (order :created_at :desc) (limit 5))
(select contact-email (limit 5))

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

            (GET* "/orders" []
                  ;(ok (select contact-email (where {:type "Order"}) (order :created_at :desc) (limit 5))))
                  (ok (select contact-email (where {:type "Order"}) (order :created_at :desc) (limit 5)
                              (fields :email :payment_method :type :sap_document_number :total_price :cart_id))))

            (GET* "/customers" []
                  ;(let [custs (select customer (limit 20))]
                  ;  (ok {:headers (keys custs) :rows (vals custs)}))
                  (println "getting customers ...")
                  (let [c (custs)]
                    (println c)
                    (ok c)
                    )
                  )

            (GET* "/entities/definitions" []
                  (println "&&&&&&&&&&&&&&&&&&&&&&&&&&^^^^^^^^^^^")
                  ;(println mysql/entity-definitions)
                  (ok (clean-all mysql/entity-definitions)))

            (GET* "/entities/query/:entity-name/:attribute-name/:val" []
                  :path-params [entity-name :- String, attribute-name :- String, val :- String]
                  ;(println mysql/entity-definitions)
                  (println "getting entity" [entity-name attribute-name val] " ...")
                  (ok (clean-all (mysql/attribute-query (keyword entity-name) (keyword attribute-name) val)))
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


;(select product (limit 5) )
;(select product (limit 5) (where {:upc [not= ""]}) (fields :upc))
;(map #(-> % vals first) (select product (limit 5) (where {:upc [not= ""]}) (fields :upc)))
;
;(take 30 all-upcs)
;
;slow-download-platt
;(map download-platt (take 2 all-upcs))


;(def all-upcs (map #(-> % vals first)
;                   (select product (where {:upc [not= ""]}) (fields :upc))))
;(count all-upcs)
;(time (pmap download-platt (drop 10000 all-upcs)))
;(time (pmap (fn [x] (download-platt x) nil) (drop 10000 all-upcs))))




(comment


(def platt-product-pages "/Users/bmd/data/crawler/platt/product")
(def directory (clojure.java.io/file platt-product-pages))
(def all-files (file-seq directory))
(def files (filter #(re-find #"\.html$" (.getName %)) all-files))
(def upcs (map #(re-find #"[^.]+" (.getName %)) files))
upcs
(count upcs)
(partition 2 (take 5 upcs))

(def prod (htmlfile->enlive (first files)))
files
prod
(-> (html/select prod [:span.ProductPriceOrderBox]) first :content first)

(defn remove-$ [price]
  (read-string (re-find #"[0-9.]+" price)))

(defn get-price [file]
  [(re-find #"[^.]+" (.getName file))
   (-> (html/select (htmlfile->enlive file) [:span.ProductPriceOrderBox]) first :content first)
   ])

(select customer (limit 1) (with account))

;(get-price (first files))
;(def prices (map get-price files))
;
;(def p (map #(vector (first %) (remove-$ (second %))) prices))
;p
;(def price-hashes (doall (map (fn [price] {:source "platt" :upc (first price) :price (second price)}) p)))
(take 5 price-hashes)
;(insert :mdm.prices (values price-hashes))
;(select :mdm.prices)
(take 5 upcs)
(def sapprices (mapcat get-internet-prices (partition 10 upcs)))
sapprices







(def sappriceinsert (map (fn [x] {:source "sap" :upc (matnr->upc (:matnr x)) :price (:price x)}) sapprices))
sappriceinsert
(insert :mdm.prices (values sappriceinsert))

(get-internet-prices (logit (take 10 upcs)))
(get-internet-prices (take 1 upcs))






(defn unescape-fat-arrow-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&amp;" "&")
      (replace "&lt;" "<")
      (replace "&gt;" ">")
      (replace "&quot;" "\"")
      (replace "=>" ":")
      ))

(defn create-papi-url [fn-name args]
  (str
    "http://localhost:4000/bapi/show?function_name="
    fn-name
    "&args=" (url-encode (generate-string args))))

(defn content-for-name [coll name]
  (let [section (first (filter #(= name (% "name")) coll))
        ]
    (case (section "type")
      "TABLE" (->> (section "table") second)
      section)
    )
  )
;(content-for-name saporder "ET_ORDERS_SUMMARY")
;(content-for-name saporder "ET_ORDERS_DETAIL")

(defn call-papi [fn-name args]
  (let [creds (-> env :papichulo vals)
        url (create-papi-url fn-name args)
        response (client/get url {:basic-auth creds})
        parsed (html->enlive (:body response))]
    (-> (html/select parsed [:ul#returnsJson])
        first
        :content
        (as-> eles
              (filter map? eles)
              (map (comp parse-string unescape-fat-arrow-html first :content) eles)
              )
        )
    ))

(defn find-attribute [entity from-attr to-attr val]
  (-> (select entity (where {from-attr val}) (fields to-attr)) first to-attr))

(find-attribute :products :upc :matnr (first upcs))

(defn matnr->upc [matnr]
  (find-attribute :products :matnr :upc matnr))
(defn upc->matnr [upc]
  (-> (select :products (where {:upc upc}) (fields :matnr)) first :matnr)
  )
(defn upc->matnr-qty [matnr]
  (-> (select :products (where {:upc upc}) (fields :matnr :min-qty)) first (fn [x] ((juxt :matnr :min_qty) x)))
  )
((juxt :a :b) {:a 4 :b 7})
;(upc->matnr (first upcs))

(defn get-internet-prices [matnr-qtys]
  (let [mqs (map (fn [mq] (if (string? mq) [mq 1] mq)) matnr-qtys)
        args {:i_kunnr        "0001027925"
              :i_werks        "ZZZZ"
              :it_price_input (map (fn [[matnr qty]] [(as-matnr matnr) qty]) mqs)}
        response (call-papi "z_o_complete_pricing" args)]
    (map (fn [p]
           (let [extended-price (p "NETWR")
                 requested-qty (p "KWMENG")
                 pricing-qty (p "KPEIN")
                 price (if requested-qty (/ (* extended-price pricing-qty) requested-qty) 0)]
             {:matnr (p "MATNR")
              :extended-price extended-price
              :requested-qty requested-qty
              :pricing-qty pricing-qty
              :unit (p "KMEIN")
              :price price }))
         (content-for-name response "ET_PRICE_OUTPUT"))
    ))

(def matnrs (map upc->matnr upcs))
(get-internet-prices [["2718442" 5] ["2718433" 22] ["2718433" 1000]])
(get-internet-prices ["2718442" "0000000000000000000000002718433"])
(def sapprices (mapcat get-internet-prices (partition 10 matnrs)))
sapprices
(get-internet-prices (logit (take 1 matnrs)))
(get-internet-prices (vec (take 10 (drop 1000 matnrs))))
;(-> (select :products (where {:upc (first upcs)}) (fields :matnr)) first :matnr)
;(select :products (limit 5))

(defn get-order-detail [order-num]
  (let [args {
              "i_order"      (as-document-num order-num)
              "if_orders"    "X"
              "if_details"   "X"
              "if_texts"     "X"
              "if_addresses" "X"
              }
        response (call-papi "z_o_orders_query" args)
        ]
    ;(content-for-name response "ET_ORDERS_SUMMARY")
    ;(content-for-name response "ET_ORDERS_DETAIL")
    ;(content-for-name response "ET_SCHED_LINES")
    response
    ))


(def saporder (get-order-detail "2991654"))
(as-document-num "asdf")
saporder


(-> saporder first :content )
(filter map? (-> saporder first :content))
((partial filter map?) (-> saporder first :content))
(-> saporder first :content first map?)
(-> saporder first :content (partial filter map?) )


(-> saporder
    first
    :content
    (as-> eles
          (filter map? eles)
          (map (comp parse-string unescape-fat-arrow-html first :content) eles)
          )
    )



(def sapprices (get-internet-prices [["2718442" 5] ["2718433" 22]]))
sapprices


(def x (html->enlive (clojure.string/replace (:body sapprice) #"[\n]+" "")))
(def x (html->enlive (:body sapprice)))
x
(-> (html/select x [:ul#returnsJson]) )
(-> (html/select x [:ul#returnsJson]) first :content second :content first unescape-fat-arrow-html parse-string)
(generate-string {:a 3})
(parse-string (generate-string {:a 3}))
(-> (html/select x [:ul#returns]) first :content)

(clojure.string/replace "asdf\nfff" #"\n" "")
(clojure.string/trim " \n ")


(def z {:a 9,,,,, "b" 12})
(z :a)
(z "b")
z
(:a z)
("b" z)


)
