(ns summit.papichulo.core
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [korma.core :refer :all]
            [clojure.set]
            [clojure.string :as str]
            [clojure.walk :refer :all]
            [cheshire.core :refer :all]
            ;[clj-time.core :as t]
            ;[clj-time.format :as f]
            [net.cgrand.enlive-html :as html]

            [summit.db.relationships :refer :all]
            ;; [summit.sales-associate.order-spec :refer [send-spec-email]]



            ;[cheshire.generate :refer [add-encoder encode-str remove-encoder]]

            [cemerick.url :refer [url-encode url]]
            [clj-http.client :as client]
            [config.core :refer [env]]


            ;[compojure.core :refer [defroutes GET]]

            [summit.utils.core :refer :all]
            [timmus.db.core :refer [*db*]]
            [brianmd.db.store-mysql :as mysql]
            ))

(defn papichulo-url []
  (-> (default-env-setting :papichulo) :url))

(defn papichulo-creds []
  (let [papichulo (default-env-setting :papichulo)]
    [(:username papichulo) (:password papichulo)]))

(defn papichulo-url-with-creds []
  (let [tokens (str/split (papichulo-url) #"//")
        creds (str/join (papichulo-creds) ":")]
    (str (first tokens) "//" creds (second tokens))))

(defn create-papi-url [fn-name args]
  (str
   (papichulo-url)
   "bapi/show?function_name="
   fn-name
   "&args=" (url-encode (generate-string args))))

(defn remove-$ [price]
  (read-string (re-find #"[0-9.]+" price)))

(defn get-price [file]
  [(re-find #"[^.]+" (.getName file))
   (-> (html/select (htmlfile->enlive file) [:span.ProductPriceOrderBox]) first :content first)
   ])





(defn junk-unescape-fat-arrow-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&amp;" "&")
      (replace "&lt;" "<")
      (replace "&gt;" ">")
      (replace "&quot;" "\"")
      (replace "=>" ":")
      ))

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
  ;; (let [creds (-> env :papichulo vals)
  (let [creds (default-env-setting :papichulo vals)
        url (create-papi-url fn-name args)
        response (client/get url {:basic-auth creds})
        parsed (html->enlive (:body response))]
    (-> (html/select parsed [:ul#returnsJson])
        first
        :content
        (as-> eles
              (filter map? eles)
              (map (comp parse-string unescape-html first :content) eles)
              )
        )
    ))

(defn find-attribute [entity from-attr to-attr val]
  (-> (select entity (where {from-attr val}) (fields to-attr)) first to-attr))

;(find-attribute :products :upc :matnr (first upcs))

(defn matnr->upc [matnr]
  (find-attribute :products :matnr :upc matnr))
(defn upc->matnr [upc]
  (-> (select :products (where {:upc upc}) (fields :matnr)) first :matnr)
  )
;(defn upc->matnr-qty [matnr]
;  (-> (select :products (where {:upc upc}) (fields :matnr :min-qty)) first (fn [x] ((juxt :matnr :min_qty) x)))
;  )


;(defn get-internet-prices [matnr-qtys]
;  (let [mqs (map (fn [mq] (if (string? mq) [mq 1] mq)) matnr-qtys)
;        args {:i_kunnr        "0001027925"
;              :i_werks        "ZZZZ"
;              :it_price_input (map (fn [[matnr qty]] [(as-matnr matnr) qty]) mqs)}
;        response (call-papi "z_o_complete_pricing" args)]
;    (map (fn [p]
;           (let [extended-price (p "NETWR")
;                 requested-qty (p "KWMENG")
;                 pricing-qty (p "KPEIN")
;                 price (if requested-qty (/ (* extended-price pricing-qty) requested-qty) 0)]
;             {:matnr (p "MATNR")
;              :extended-price extended-price
;              :requested-qty requested-qty
;              :pricing-qty pricing-qty
;              :unit (p "KMEIN")
;              :price price }))
;         (content-for-name response "ET_PRICE_OUTPUT"))
;    ))

(defn get-internet-prices [prods]                           ; can pass array of matnrs instead of prod maps
  (let [mqs (map (fn [mq] (if (string? mq) [mq 1] [(:matnr mq) (:min_qty mq)])) prods)
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
;(get-internet-prices z)

(def ipprice-safety-net [])
(defn safely-prices [matnr-qtys]
  (def ipprice-safety-net [])
  (let [x (get-internet-prices matnr-qtys)]
    (def ipprice-safety-net (conj ipprice-safety-net x))
    x))

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



;(def prods
;  (select product (where {:upc [not= ""]}) (limit 999999) (fields :upc :matnr :min_qty)))
;(def prods
;  (select product (where {:upc [not= ""] :min_qty [> 1]}) (limit 999999) (fields :upc :matnr :min_qty)))
;(map (comp #(vector (first %) %) vals) z)
;(count prods)
;(take 5 prods)

;(def prods
;  (select product (where {:upc [not= ""] :min_qty [> 1]}) (limit 999999) (fields :upc :matnr :min_qty)))
;(def upc->matnrs
;  (doall (into {} (map (comp #(vector (first %) %) vals)
;                       (select product (where {:upc [not= ""]}) (fields :upc :matnr :min_qty)))))))
;(def all-upcs (vals upc->matnrs))
;(upc->matnrs "045242309825")
;(take 5 all-upcs)
;(time (pmap (fn [x] (download-platt x) nil) (drop 50000 all-upcs))))
;(java.util.Date.)
;(count all-upcs)
;(def upc->matnrs
;  (doall (into {} (map (comp #(vector (first %) %) vals)
;                       (select product (where {:upc [not= ""]}) (fields :upc :matnr :min_qty)))))))
;(def matnrs (vals upc->matnrs))
;(time
;  (def sapprices (doall (mapcat get-internet-prices (partition-all 10 (take 100 (vals upc->matnrs)))))))
;(time
;  (def sapprices (doall (mapcat safely-prices (partition-all 10 (take 999999 matnrs))))))

;sapprices
;ipprice-safety-net
;(count sapprices)
;(spit "sapprices.edn" (with-out-str (pr sapprices)))
;(def x (read-string (slurp "sapprices.edn")))
;(take 5 x)


;(delete :mdm.prices (where {:source 'sap'}))

(defn insert-price-hash [x]
  (insert :mdm.prices (values x)))

(def files :not-loaded-yet)

;; TODO: should filter out already loaded upcs
;; Note: this is untested!
(defn load-platt-prices [product-files]                             ; files is calculated above
  (def prices (map get-price files))
  (def p (map #(vector (first %) (remove-$ (second %))) prices))
  (def price-hashes (doall (pmap (fn [price] {:source "platt" :upc (first price) :price (second price)}) p)))
  (def partitioned (doall (partition 300 price-hashes)))
  (map (fn [x] (map insert-price-hash x)) partitioned)
  )


(defn load-sap-prices []
  (def upc->matnrs
    (doall (into {} (map (comp #(vector (first %) %) vals)
                         (select product (where {:upc [not= ""]}) (fields :upc :matnr :min_qty))))))
  (def matnrs (vals upc->matnrs))
  (def sapprices (doall (mapcat safely-prices (partition-all 10 (take 999999 matnrs)))))

  (def sappriceinsert
    (map
      (fn [x] (let [upc (matnr->upc (:matnr x))
                    qty (last (upc->matnrs upc))]
                {:source "sap" :upc upc :price (* (:price x) qty)}))
      (take 999999 sapprices)))
  (def partitioned (doall (partition 300 sappriceinsert)))
  (map (fn [x] (map insert-price-hash x)) partitioned)
  )



(def compare-sql "select p1.upc, p1.price sap, p2.price platt, (p1.price-p2.price)/p2.price*100 increase from mdm.prices p1 join mdm.prices p2 on p1.upc=p2.upc where p1.source='sap' and p2.source='platt' and p1.price>0 order by increase")

#_(comment

(def compared (exec-raw (vector compare-sql) :results))
(take 5 compared)







;(take 30 all-upcs)
;(count all-upcs)

platt-product-pages

(def platt-product-pages "/Users/bmd/data/crawler/platt/product")
(def directory (clojure.java.io/file platt-product-pages))
(def all-files (file-seq directory))
(def files (filter #(re-find #"\.html$" (.getName %)) all-files))
(def upcs (map #(re-find #"[^.]+" (.getName %)) files))
upcs
(def matnrs (map upc->matnrs upcs))

;(def prod (htmlfile->enlive (first files)))
;files
;prod
;(-> (html/select prod [:span.ProductPriceOrderBox]) first :content first)

;((juxt :a :b) {:a 4 :b 7})
;(upc->matnr (first upcs))
(get-internet-prices [["2718442" 5] ["2718433" 22] ["2718433" 1000]])
(get-internet-prices ["2718442" "0000000000000000000000002718433"])
(def sapprices (mapcat get-internet-prices (partition 10 matnrs)))
sapprices
(get-internet-prices (logit (take 1 matnrs)))
(get-internet-prices (vec (take 10 (drop 1000 matnrs))))
;(-> (select :products (where {:upc (first upcs)}) (fields :matnr)) first :matnr)
;(select :products (limit 5))

;(get-price (first files))
;(def prices (map get-price files))

;(def p (map #(vector (first %) (remove-$ (second %))) prices))
;(first p)
;p
;(def price-hashes (doall (pmap (fn [price] {:source "platt" :upc (first price) :price (second price)}) p)))
;(take 5 price-hashes)
;(map (fn [x] (insert :mdm.prices x)) (partition 300 (values price-hashes)))

;(def partitioned (doall (partition 300 price-hashes)))
;(last partitioned)
;(map (fn [x] (map insert-price-hash x)) partitioned)
;(insert :mdm.prices (values price-hashes))
;(select :mdm.prices)
;(take 5 upcs)
;(def sapprices (mapcat get-internet-prices (partition 10 upcs)))
;sapprices







;(take 5 sapprices)
;(take 5 sappriceinsert)
;(map vals (take 5 sappriceinsert))

;(def sappriceinsert
;  (map
;    (fn [x] (let [upc (matnr->upc (:matnr x))
;                  qty (last (upc->matnrs upc))]
;              {:source "sap" :upc upc :price (* (:price x) qty)}))
;    (take 999999 sapprices)))
;sappriceinsert
;3
;(vec (first (first partitioned)))
;(insert-price-hash (vec (first) (first partitioned))
;(def partitioned (doall (partition 300 sappriceinsert)))
;(last partitioned)
;(map (fn [x] (map insert-price-hash x)) partitioned)

;(get-internet-prices (logit (take 10 upcs)))
;(get-internet-prices (take 1 upcs))



;(def saporder (get-order-detail "2991654"))
;(as-document-num "asdf")
;saporder


;(-> saporder first :content )
;(filter map? (-> saporder first :content))
;((partial filter map?) (-> saporder first :content))
;(-> saporder first :content first map?)
;(-> saporder first :content (partial filter map?) )


;(-> saporder
;    first
;    :content
;    (as-> eles
;          (filter map? eles)
;          (map (comp parse-string unescape-fat-arrow-html first :content) eles)
;          )
;    )



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
