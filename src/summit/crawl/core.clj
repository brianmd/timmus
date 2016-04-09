(ns summit.crawl.core
  (:require [cheshire.core :refer [parse-string]]
            [clj-http.client :as client]
            [clojure.string :as str]
            [config.core :refer [env]]
            [korma.core :as k :refer [fields insert values where]]
            [net.cgrand.enlive-html :as html]
            [summit.utils.core :refer :all]))

;; (def directory (clojure.java.io/file platt-product-pages))
;; (def all-files (file-seq directory))
;; (def files (filter #(re-find #"\.html$" (.getName %)) all-files))
;; (count files)

(def platt-product-pages "/Users/bmd/data/crawler/platt/product")
(examples
 (def directory (clojure.java.io/file platt-product-pages))
 (def all-files (file-seq directory))
 (def files (filter #(re-find #"\.html$" (.getName %)) all-files))
 (count files)
 )

(defn find-attribute [entity from-attr to-attr val]
  (-> (dselect entity (where {from-attr val}) (fields to-attr)) first to-attr))

(defn matnr->upc [matnr]
  (find-attribute :products :matnr :upc matnr))
(defn upc->matnr [upc]
  (:matnr (ddetect :products (where {:upc upc}) (fields :matnr))))
;; (-> (dselect :products (where {:upc upc}) (fields :matnr)) first :matnr))


(def join-sql "select p.matnr,p.upc,p.title,p.sell_quantity,p.uom,p.price platt_price,
   ps.price sap_price,(ps.price-p.price)/greatest(p.price,ps.price)*100 over,
   p.source_id,p.manufacturer,p.part_num,p.created_at
  from mdm.price p
  join mdm.prices ps on ps.upc=p.upc
  where ps.price>0 and ps.source='sap'
  order by over")


"select p.matnr,p.upc,p.title,ps.title sap_title,p.sell_quantity,ps.sell_quantity sap_sell_qty,p.uom,ps.uom sap_uom,p.price platt_price,
   ps.price sap_price,(ps.price-p.price)/greatest(p.price,ps.price)*100 over,
   p.source_id,p.manufacturer,p.part_num,ps.part_num sap_part_num,p.created_at
  from mdm.price p
  join mdm.price ps on ps.upc=p.upc
  where p.source='platt' and ps.price>0 and ps.source='sap'
  order by over
"


(defn platt-str->keyword [s]
  (keyword (str/join "-" (-> (first (str/split s #":")) str/lower-case (str/split #" ")))))
;; (platt-str->keyword "Abc Def:")

(defn platt-row->map [row]
  (let [v (map html/text (:content row))]
    [(platt-str->keyword (first v)) (second v)]))
;; (platt-row->map (first tbl))

(defn platt-rows->map [tbl]
  (into {}
        (map platt-row->map tbl)))

(defn remove-$ [price]
  (read-string (re-find #"[0-9.]+" price)))

(defn platt-vitals-enlive [m]
  (let [
        pre-price (-> (html/select m [:span.ProductPrice]) first :content)
        price (-> pre-price first :content)
        specs1 (platt-rows->map (->> (html/select m [:table.seoSpecTable1 :tr])))
        specs2 (platt-rows->map (->> (html/select m [:table.seoSpecTable2 :tr])))
        uom (last pre-price)
        ]
    {:source "platt"
     :source_id (-> (html/select m [:span.ProductID]) first :content first)
     :price (remove-$ (first price))
     :uom (if (= String (type uom)) (str/trim uom))
     :sell_quantity (-> (html/select m [:p.quantityText :input]) first :attrs :value read-string)
     :title (-> (html/select m [:span.lblProdHeadline]) first :content first)
     :description (html/text (first (html/select m [:div#tabDetailsContent (html/attr= :itemprop "description")])))
     :manufacturer (-> (html/select m [:a.BrandLink]) first :content first)
     :part_num (:platt-cat specs1)
     :specs1 specs1
     :specs2 specs2
     }
    ))

(defn platt-vitals [upc]
  (let [filename (str platt-product-pages "/" upc ".html")
        d (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") "2016-02-16")
        m (htmlfile->enlive filename)]
    (assoc (platt-vitals-enlive m)
           :upc upc
           :matnr (upc->matnr upc)
           :created_at d)
    ))
(examples
 (platt-vitals "012800474103")
 (platt-vitals "04524215583")
 (platt-vitals "04524204507")
 )

(examples ;; from sap
 (defrecord Price [matnr qty unknown-price units sell-by-qty uom price])
 (def prices (read-string (slurp "/Users/bmd/code/kupplervati/prices.edn")))

 (def price-records (map! #(apply ->Price %) (vals prices)))
 (def nonzero-prices (filter #(not= 0.00M (:price %)) price-records))
 (count nonzero-prices)
 (first nonzero-prices)

 (defn sap-price->dbprice [p]
   (let [matnr (:matnr p)
         sap (ddetect :mdm.sap_material (where {:matnr (as-matnr matnr)}))
         p {:source "sap"
            :source_id matnr
            :matnr matnr
            :upc (:upc sap)
            :price (:price p)
            :uom (:uom p)
            :sell_quantity (:sell-by-qty p)
            :title (:title sap)
            :description (:descript sap)
            :manufacturer (:mfg_we_pay_invoice_to sap)
            :part_num (:mfr_part_num sap)
            :created_at (.parse (java.text.SimpleDateFormat. "yyyy-MM-dd") "2016-04-07")
            }]
     ;; (pp sap)
     (pp p)
     (insert :mdm.price (values p))))

 (sap-price->dbprice (first nonzero-prices))
 (dselect :mdm.sap_material (where {:matnr (as-matnr (:matnr (first nonzero-prices)))}))

 (map! sap-price->dbprice nonzero-prices)
 )
(filter #(and (= "048011040288" (:upc %) %)) nonzero-prices)
(filter #(and (= "048011040288" (:upc %) %)) price-records)
(first prices)
(prices 23395)
(apply ->Price (prices 23395))
(:price (apply ->Price (prices 23395)))




(defn save-price [m]
  (let [m (assoc m
                 :specs1 (pr-str (:specs1 m))
                 :specs2 (pr-str (:specs2 m))
                 )
        result (insert :mdm.price (values m))]
    (assoc m :id (:generated_key result))
    ))




(examples
 (save-price (platt-vitals "04524204507"))

 (doseq [upc (drop 4394 upcs)]
   (save-price (platt-vitals upc)))

 (nth upcs 4394)
 (take 2 (drop 4394 upcs))
 (platt-vitals (nth upcs 4394))
 (platt-vitals "013627001923")
 )


(defn get-price [file]
  [(re-find #"[^.]+" (.getName file))
   (-> (html/select (htmlfile->enlive file) [:span.ProductPriceOrderBox]) first :content first)
   ])



(defn call-papi [fn-name args]
  (let [creds (-> env :papichulo vals)
        ;; url (p/create-papi-url fn-name args)
        url 3
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

;; (upc->matnr (first upcs))
;; (take 5 files)
;; (def upcs (map #(re-find #"[^.]+" (.getName %)) files))
;; (take 5 upcs)
;; (find-attribute :products :upc :matnr (first upcs))

;; (def matnrs (map upc->matnr upcs))
;; (get-internet-prices [["2718442" 5] ["2718433" 22] ["2718433" 1000]])
;; (get-internet-prices ["2718442" "0000000000000000000000002718433"])
;; (def sapprices (mapcat get-internet-prices (partition 10 matnrs)))
;; sapprices
;; (get-internet-prices (logit (take 1 matnrs)))
;; (get-internet-prices (vec (take 10 (drop 1000 matnrs))))
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


#_(comment


    (def upcs (map #(re-find #"[^.]+" (.getName %)) files))
    upcs
    (count upcs)
    (partition 2 (take 5 upcs))

    files
    prod


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
          (map (comp parse-string unescape-html first :content) eles)
          )
    )



(def sapprices (get-internet-prices [["2718442" 5] ["2718433" 22]]))
sapprices


(def x (html->enlive (clojure.string/replace (:body sapprice) #"[\n]+" "")))
(def x (html->enlive (:body sapprice)))
x
(-> (html/select x [:ul#returnsJson]) )
(-> (html/select x [:ul#returnsJson]) first :content second :content first unescape--string)
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


;; (upc->matnr (first upcs))
;; (take 5 files)
;; (def upcs (map #(re-find #"[^.]+" (.getName %)) files))
;; (take 5 upcs)
;; (find-attribute :products :upc :matnr (first upcs))
add matnr





(def sappriceinsert (map (fn [x] {:source "sap" :upc (matnr->upc (:matnr x)) :price (:price x)}) sapprices))
sappriceinsert
(insert :mdm.prices (values sappriceinsert))

(get-internet-prices (logit (take 10 upcs)))
(get-internet-prices (take 1 upcs))





)
