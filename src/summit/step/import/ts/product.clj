(ns summit.step.import.ts.product
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            ;;[clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.ts.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]
            ))


;; Create trade service product record and constructor

;; (def product-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
;; (def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
;; (def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})



(def product-col-info-array
  [:item-pik "ITEM_PIK" String [:required :digits]
   :item-action "ITEM_ACTION" String []
   :mfr-pik "MFR_PIK" BigInteger []
   :item-country "ITEM_COUNTRY" String []
   :item-industry "ITEM_INDUSTRY" String []
   :item-num "ITEM_NUM" String []
   :ean "EAN" String []
   :upc-flag "UPC_FLAG" String []
   :barcode-type "BARCODE_TYPE" String []
   :mfr-cat-num "PART_NUMBER" String []
   :tsc-cat-num "TSC_CAT_NUM" String []
   :lookup-cat-num "LOOKUP_CAT_NUM" String []
   :alt-cat-num "ALT_CAT_NUM" String []
   :trade-name "TRADE_NAME" String []
   :indv-weight "INDV_WEIGHT" Double []
   :indv-weight-uom "INDV_WEIGHT_UOM" String []
   :gst-flag "GST_FLAG" String []
   :replaced-by-item-pik "REPLACED_BY_ITEM_PIK" BigInteger []
   :replaces-item-pik "REPLACES_ITEM_PIK" BigInteger []
   :country-of-origin "TS_COUNTRY_OF_ORIGIN" String []
   :user-num "USER_NUM" String []
   :item-status "ITEM_STATUS" String []
   :msds "TS_MSDS" String []
   :msds-eff-date "TS_MSDS_EFF_DATE" String []
   :msds-form-num "TS_MSDS_FORM_NUM" String []
   :item-sic-code "TS_ITEM_SIC_CODE" Integer []
   :item-size "TS_ITEM_SIZE" Double []
   :leaf-class  "LEAF_CLASS" String []
   :maintenance-code "TS_MAINTENANCE_CODE" String []
   :maintenance-month "TS_MAINTENANCE_MONTH" String []
   :unspsc "UNSPSC" String []
   :unspsc40 "UNSPSC40" String []
   :brand-name "BRAND_NAME" String []
   :mfr-replaced-by-upc "MFR_REPLACED_BY_UPC" String []
   :mfr-replaced-by-cat-no "MFR_REPLACED_BY_CAT_NO" String []
   :mfr-replaces-upc "MFR_REPLACES_UPC" String []
   :mfr-replaces-cat-no "MFR_REPLACES_CAT_NO" String []
   :product-name "PRODUCT_NAME" String []
   :gtin "GTIN" String []
   :upc "UPC" String []
   ])

(def num-product-cols (count (partition 4 product-col-info-array)))
(def product-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 product-col-info-array)))
  )

(def product-col-names
  (->> product-col-info-array (partition 4) (map first)))

(make-record TsProduct product-col-names)
;(make-record ^{:col-info product-col-info-array} TsProduct product-col-names)
;; ->TsProduct 

(def ->TsProduct ^{:col-info product-col-info-array} ->TsProduct) 
;; (meta ->TsProduct)

;; TsProduct 
;; (def x 3)
;; (def ^{:r 99} x 45)
;; ^#{:abc "hey"} #'x 
;; x 
;; (def y (with-meta x {:q 17}))

;; (:col-info TsProduct)
;; (meta TsProduct)
;; ((meta #'x) ^{:q 17})
;; (meta #'x)
;; (meta x)

;; (def a-var 2)
;; (def another-var (with-meta a-var {:foo :bar}))

;; (def a ^{:created (System/currentTimeMillis)}
;;   [1 2 3])
;; (meta a)

;(apply ->TsProduct (range num-product-cols))

(defn- vec->rec [rec-def cols]
  (let [errors (validate-with* cols [[#(= (count %) num-product-cols) #(str "Wrong number of columns: " (count %) " instead of " num-product-cols)]])]
    (if (not-empty errors)
                                        ;(do (logit errors (interleave (range 200) cols) "----") (println (interleave (range 200) cols) errors) nil)
      (do (logit-plain errors (interleave (range) cols) "----") (println cols errors) nil)
      (let [record (apply ->TsProduct (map str/trim cols))
            errs (validate-record product-col-info record)]
        (if (not-empty errs)
          (do (logit-plain errs cols "----") nil)
          record)
        )
      )))

(defn- ts-product [cols]
  (let [errors (validate-with* cols [[#(= (count %) num-product-cols) #(str "Wrong number of columns: " (count %) " instead of " num-product-cols)]])]
    (if (not-empty errors)
      ;(do (logit errors (interleave (range 200) cols) "----") (println (interleave (range 200) cols) errors) nil)
      (do (logit errors (interleave (range 200) cols) "----") (println cols errors) nil)
      (let [record (apply ->TsProduct (map str/trim cols))
            errs (validate-record product-col-info record)]
        (if (not-empty errs)
          (do (logit errs cols "----") nil)
          record)
        )
      )))


;; Xml creation code

#_(defn ts-product-attributes [item]
  [:Values
        (reduce
          (fn [attrs [name col]]
            (let [id (:dbid col)]
              (if id
                (conj attrs [:Value
                             {:AttributeID id}
                             (escape-html (name item))])
                attrs)))
          () product-col-info)])

;(ts-product-attributes y)
;(x/emit-element (ts-product-attributes y))
;(x/emit-element {:tag :hello :attrs {:place "world"}})
;(x/emit-element {:tag :parent
;                 :attrs {:id "22" :name "fritz"}
;                 :content [
;                           {:tag :child :attrs {:id "56"}}
;                           {:tag :child :attrs {:id "57"}}]})

(defn- ts-product-hiccup [item]
  (let [parent-id (let [p (:unspsc item)]
                    (if (or (nil? p) (= p ""))
                      "TS_Member_Records"
                      (str "TS_" p)))]
    [:Product
     {:ID (str "MEM_TS_" (:item-pik item))
      :UserTypeID "TS_Member_Record"
      :ParentID parent-id}
     (product-attributes product-col-info item)
     ]))

(defn- ts-product-xml [item]
  (println
    (hiccup/html (ts-product-hiccup item)))
  item)



;; Read ts product file

;; (def matched-products (set (:ts (slurp-source-ids "punduit"))))
;; (contains? matched-products 121959245)
;; (def matched-products (set (map str (:ts (slurp-source-ids "punduit")))))
;; (contains? matched-products "121959245")


(defn transform-ts-product [item]
  item
  ;; (assoc item
  ;;        :mfr-pik (as-short-document-num (:mfr-pik item))
  ;;        )
  )

(def ^:private ts-mfr-arlington "1009")
(def ^:private ts-mfr-hubbell "762")
(def ^:private ts-mfr-milwaukee "112")

(defn- keep-good [v]
  (if (= (count v) 40)
    v
    (ppn "bad record:" v (str "count " (count v)))))

(defn- process-ts-product [lines]
  (let [categories (atom #{})
        ;; matched-products (set (:ts (slurp-source-ids "current")))
        ]
    (->> lines
         rest
         (remove nil?)
         ;; (filter #(contains? matched-products (first %)))
         ;; (filter (comp *matched-products* as-integer first))
         ;; (take 5)
         (take 100000)
         (filter keep-good)
         ;; pp
 ;;   (filter #(= ts-mfr-milwaukee (nth % 2)))  ;; mfr-pik
         ;; (drop 50)
         ;; logit-plain
         (map ts-product)
         ;; (remove nil?)
         (map transform-ts-product)
         (map ts-product-xml)
         (map :item-pik)
   ;; ts-set-exported
         (dorun)
         )
    ))
;; (process-ts-file-with "item.txt" process-ts-product)
(examples
 (time (write-ts-file))
 )


(defn process-ts-file-with [filename f]
  (process-tabbed-file-with (str ts-input-path filename) f))
;; (defn process-ts-file-with [filename fn]
;;   (let [full-filename (str ts-input-path filename)]
;;     (with-open [in-file (io/reader full-filename)]
;;       (let [lines (csv/read-csv in-file :separator \tab)]
;;         (fn lines)))))
;;  (process-file-with (str ts-input-path filename) fn))

(defn write-ts-file []
  (let [full-filename (str ts-output-path "product.xml")]
    (make-parents full-filename)
    (with-open [w (clojure.java.io/writer full-filename)]
      (binding [*out* w]
        (println (opening))
        (let [result
              (process-ts-file-with "item.txt" process-ts-product)]
          (println (closing))
          result)))))

(examples
 (time (write-ts-file))
 )
;(process-ts-file-with "STEP_MATERIAL.txt" process-ts-product)
;(def x (process-ts-file-with "STEP_MATERIAL.txt" process-ts-product))


(println "finished loading summit.step.import.ts.product")
