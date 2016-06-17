(ns summit.punchout.ariba-catalog
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [semantic-csv.core :as scsv ] ;:refer :all]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.sap.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]

            [summit.sap.core :as sap]
            [summit.sap.price :as price]
            [summit.sap.customer-materials :refer [customer-materials]]



            [summit.utils.core :as utils :refer :all]
            [summit.db.relationships :as rel :refer :all]


            [korma.core :as k]
            ;; [korma.db :refer [defdb oracle]]

            [dk.ative.docjure.spreadsheet :as xls]

            ))


;; spreadsheet helper functions

(defn- get-cell [sheet rownum colnum]
  (.getCell (.getRow sheet rownum) colnum))

(defn get-val [sheet rownum colnum]
  (let [cell (get-cell sheet rownum colnum)]
    (if (nil? cell) nil (xls/read-cell cell))))

(defn set-val [sheet rownum colnum value]
  "creates a cell if non-existent. Could make more efficient -- don't create when value==nil"
  (let [row (.getRow sheet rownum)
        cell (or (.getCell row colnum) (.createCell row colnum))]
    (xls/set-cell! cell value)))



(defn get-product-info [matnr]
  (let [prod (find-by-colname :products :matnr matnr)
        descript (:name prod)
        unspsc 39
        files (if prod (select-by-colname :external_files :product_id (:id prod)))]
    (if prod
      {:descript descript
       :unspsc unspsc
       :image (:url (first (filter #(= "Image" (:type %)) files)))
       :thumbnail (:url (first (filter #(= "Thumbnail" (:type %)) files)))
       })))
;; (get-product-info 35377)


;; (get-product-info 30716)


;; (defn create-fake-xls-stream []
;;   (let [wb (xls/create-workbook "Price List"
;;                                 [["Name" "Price"]
;;                                  ["Foo Widget" 100]
;;                                  ["Bar Widget" 200]])
;;         sheet (xls/select-sheet "Price List" wb)
;;         header-row (first (xls/row-seq sheet))]
;;     (do
;;       (xls/set-row-style! header-row (xls/create-cell-style! wb {:background :yellow,
;;                                                                  :font {:bold true}}))
;;       (with-open [w (clojure.java.io/output-stream "spread2.xlsx")]
;;         (xls/save-workbook! w wb)))))

;; (create-fake-xls-stream)


(def dow-account-number 1007135)
(def dow-service-center "CLU1")
(def ariba-network-id "AN1015020056-T")   ;; also known as supplier id

;; (binding [sap/*sap-server* :dev]
;;   (price/prices 1000081 dow-service-center [[6787 100]]))
;; (binding [sap/*sap-server* :prd]
;;   (price/prices dow-account-number dow-service-center [[8322 1]]))

;; (price/prices dow-account-number dow-service-center [[2718442 1]])
;; (price/prices dow-account-number dow-service-center [[8322 1]])
;; (price/prices "0001027925" dow-service-center [[8322 1]])
;; (price/prices 1027925 dow-service-center [[8322 1]])

(defn cache-map [cache-atom calculate-fn key]
  (if-let [v (@cache-atom key)]
    v
    (let [v (calculate-fn key)]
      (swap! cache-atom (fn [hash k] (assoc hash k v)) key)
      v)))
;; (cache-map dow-prices (fn [k] (* k 4)) 22)

(def dow-prices (atom {}))
(def internet-prices (atom {}))

(defn dow-price [matnr]
  (cache-map dow-prices #(last (first (price/prices dow-account-number dow-service-center [[% 1]]))) matnr))

(defn market-price [matnr]
  (cache-map internet-prices #(last (first (price/internet-unit-prices [%]))) matnr))
;; (dow-price 8322)
;; (market-price 8322)

(defn get-unspsc [matnr]
  (let [sql (str "select i.unspsc idw_unspsc,t.unspsc ts_unspsc,t.unspsc40 ts_unspsc40
from mdm.join_product j
join mdm.idw_item i on i.idw_index=j.idw_index
join mdm.ts_item t on t.item_pik=j.item_pik
where j.matnr='" (as-matnr matnr) "';")
        result (first (exec-sql sql))]
    (or (:idw_unspsc result) (:ts_unspsc result))))
(examples (get-unspsc 1000000))

(defn quoteit [s]
  ;; (pr-str s))
  (if s
    (str \"
         (str/replace s #"\"" "\"\"")
         \")
    ""))
;; (quoteit "ab\"def\"qrs")
;; (println "ab\"def\"")
;; (str/replace "ab\"def\"" #"\"" "\\\"")
;; (println (str/replace "ab\"def\"" #"\"" "\\\\\""))
;; (println (str \" (str/replace "ab\"def\"" #"\"" "\\\\\"") \"))
;; (pr "ab\"xyq\"tu")



 
;; (part-info 35379 3)
;; (part-info 3255718 3)
;; (ppn (part-info 3255718 3))
;; (ppn (sort (part-info 3255718 3)))
;; (ppn (sort (part-info 2854664 3)))
;; (ppn (sort (part-info 3 3)))

(defn part-info [matnr cust-part-num]
   ;; (println "\n222 part-info")
   (let [
         mat (as-matnr matnr)
         bh-prod (ddetect :products (k/where {:matnr mat}))
         bh-mfr (ddetect :manufacturers (k/where {:id (:manufacturer_id bh-prod)}))
         product-id (:id bh-prod)
         mdm-prod (ddetect :mdm.sap_material (k/where {:matnr mat}))
         file-urls (dselect :external_files (k/where {:product_id product-id}))
         msds-url (:url (detect #(= "Attachment" (:type %)) file-urls))
         thumbnail-url (:url (detect #(= "Thumbnail" (:type %)) file-urls))
         image-url (:url (detect #(= "Image" (:type %)) file-urls))
         customer-price 0
         internet-price 0
         ;; customer-price (dow-price matnr)
         ;; internet-price (market-price matnr)
         ;; price (if (<= internet-price customer-price) internet-price customer-price)

         ;; temporary fix
         unspsc 39
         ;; unspsc (get-unspsc matnr)
         lead-time 1
         uom (:uom bh-prod)
         short-name (:title mdm-prod)
         ;; price should be money, and printed with a minimum of two decimals
         ;; todo: escape quotes (this is a csv file, after all)
         ]
     {
      ;; :bh-prod bh-prod
      ;; :mdm-prod mdm-prod

      :matnr matnr
      :mfr-name (:name bh-mfr)
      ;; :mfr-name (quoteit (:name bh-mfr))
      :url (if product-id (str "https://www.summit.com/store/products/" product-id))
      :ariba-network-id ariba-network-id
      :part-num (:manufacturer_part_number bh-prod)
      :customer-part-num cust-part-num
      :descript (if (nil-or-empty? (:description bh-prod)) short-name (:description bh-prod))
      ;; :descript (quoteit (:description bh-prod))
      :short-name short-name
      ;; :short-name (quoteit short-name)
      :unspsc unspsc
      :price customer-price
      :market-price (if (< customer-price internet-price) internet-price)
      :uom uom
      :lead-time lead-time
      :msds-url msds-url
      :thumbnail-url thumbnail-url
      :image-url image-url
      :language "en_US"
      :currency "USD"
      :nothing ""
      }))

;; (ppn (part-info 8322 "cust-part-no"))
;; (ppn (part-info 35379 "cust-part-no"))  ; this has been zzzz-deleted.

(def ariba-output-fields [:ariba-network-id :matnr :part-num :descript :unspsc :price :uom :lead-time
                          :mfr-name :url :msds-url
                          :market-price :customer-part-num
                          :nothing :language :currency :short-name :image-url :thumbnail-url])
(def ariba-supplemental-output-fields [nil nil :part-num :descript :unspsc nil nil nil
                          :mfr-name :url :msds-url
                          :market-price :customer-part-num
                          :nothing :language :currency :short-name :image-url :thumbnail-url])

;; (ppn ((apply juxt ariba-output-fields) (part-info 8322 "cust-part-no")))
;; (ppn (map #(if (nil? %) "" %) ((apply juxt ariba-output-fields) (part-info 8322))))
;; (ppn (part-info 83223322))



;; (k/select :products (k/where {:matnr 1000000}))

(defn ariba-catalog-info->str [m]
  (apply str
         (interpose ","
                    (map #(if (nil? %) "" %) ((apply juxt ariba-output-fields) m))
                    ;; [(:ariba-network-id m) (:matnr m)]
                    )))

(defn write-ariba-catalog [products]
  (with-open [out (io/writer (str sap-input-path "ariba-catalog.csv"))]
    (let [supplier-domain-id ariba-network-id
          num-items (count products)
          timestamp "3/23/16"
          comments "none"]
      (dorun
       (map #(.write out (str % "\n"))
            ["CIF_I_V3.0"
             "CHARSET:,UTF-8"
             "LOADMODE:,F"
             "CODEFORMAT:,UNSPSC"
             "CURRENCY:,USD"
             (str "SUPPLIERID_DOMAIN:,NetworkID")
             (str "ITEMCOUNT:," num-items)
             (str "TIMESTAMP:," timestamp)
             "UNUOM:,TRUE"
             (str "COMMENTS:," comments)
             "Supplier ID,Supplier Part ID,Manufacturer Part ID,Item Description,SPSC Code,Unit Price,Unit of Measure,Lead Time,Manufacturer Name,Supplier URL,Manufacturer URL,Market Price,Material_Number,Supplier Part Auxiliary ID,Language,Currency,Short Name,Image,Thumbnail"
             "DATA"
             ]))
      (dorun
       ;; (map (comp #(.write out (ariba-catalog-info->str %) ariba-catalog-info )) catalog-matnr_cust-part-nums))
       (map #(do
               (.write out (ariba-catalog-info->str %))
               (.write out "\n"))
            products))
      (.write out "ENDOFDATA")
      )))

;; (def ariba-catalog-products (->> catalog-matnr_cust-part-nums (take 2) (map! #(apply part-info %))))
;; (write-ariba-catalog ariba-catalog-products)

;; (write-ariba-catalog catalog-matnr_cust-part-nums)




(defn update-ariba-product [sheet rownum prod]
  (ppn "-----" prod rownum)
  (set-val sheet rownum 2  (:part-num prod))
  (set-val sheet rownum 3  (:descript prod))
  ;; (set-val sheet rownum 4  (:unspsc prod))
  (set-val sheet rownum 4  39)
  (set-val sheet rownum 8  (:mfr-name prod))
  (set-val sheet rownum 9  (:url prod))
  (set-val sheet rownum 16 (:short-name prod))
  (set-val sheet rownum 17 (:image prod))
  (set-val sheet rownum 18 (:thumbnail prod))
  )

(defn process-ariba-product [spreadsheet row-num matnr]
  (ppn (str "matnr:" matnr))
  (let [prod (get-product-info matnr)]
    (if prod
      (update-ariba-product spreadsheet row-num prod)
      (ppn (str "no such matnr: " matnr))
      )))

(defn update-ariba-catalog [spreadsheet-basename]
  (let [x (xls/load-workbook (str spreadsheet-basename ".xls"))
        s (xls/select-sheet "Sheet1" x)
        matnrs (map! (comp int :matnr) (filter identity (drop 12 (xls/select-columns {:B :matnr} s))))
        ;; prods (map! #(get-product-info %) matnrs)

        ;; matnrs (take 3 matnrs)

        prods (map! #(part-info % nil) matnrs)
        ;; prods (take 10 ps)
        ;; prods ps
        ]
    (def ps prods)
    (ppn "________________" "______________" (take 40 (drop 12 (xls/select-columns {:D :descript} s))))
    (doall
     ;; (map-indexed (fn [n matnr] (process-ariba-product s (+ n 12) matnr)) matnrs))
     ;; (map-indexed (fn [n matnr] (update-ariba-product s (+ n 12) matnr)) matnrs))
     (map-indexed (fn [n matnr] (update-ariba-product s (+ n 12) matnr)) prods))
    (xls/save-workbook! (str spreadsheet-basename "2" ".xls") x)
    ))
;; (time
;;  (update-ariba-catalog "dow"))

(examples



;; (count catalog-matnrs)
(apply part-info (first catalog-matnr_cust-part-nums))
(ariba-catalog-info->str (apply part-info (first catalog-matnr_cust-part-nums)))
(k/select :service_centers (k/database (find-db :bh-local)))
;; (-> (k/select* :service_centers)
;;     (k/database (find-db :bh-local))
;;     (k/as-sql))
;; (exec-sql (find-db :bh-local) "SELECT `service_centers`.* FROM `service_centers`")
;; (exec-sql (find-db :bh-local) "select * from service_centers")
 (def catalog-matnr_cust-part-nums (customer-materials dow-account-number))
 (map first catalog-matnr_cust-part-nums)
 (def catalog-matnrs [2040 40581 10368 35966])
 (def catalog-matnrs (map first catalog-matnr_cust-part-nums))


 )
