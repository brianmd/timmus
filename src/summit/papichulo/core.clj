(ns summit.papichulo.core
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


(defn remove-$ [price]
  (read-string (re-find #"[0-9.]+" price)))

(defn get-price [file]
  [(re-find #"[^.]+" (.getName file))
   (-> (html/select (htmlfile->enlive file) [:span.ProductPriceOrderBox]) first :content first)
   ])





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




;(comment


(def all-upcs (map #(-> % vals first)
                   (select product (where {:upc [not= ""]}) (fields :upc))))
(def matnrs (map upc->matnr upcs))
;(take 30 all-upcs)
;(count all-upcs)
;(time (pmap download-platt (drop 10000 all-upcs)))
;(time (pmap (fn [x] (download-platt x) nil) (drop 10000 all-upcs))))

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
