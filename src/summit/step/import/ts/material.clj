(ns summit.step.import.ts.material
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            ;;[clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.ts.core :refer :all]
            [summit.step.import.product-selectors :refer [slurp-source-ids]]
            ))

;; Create trade service material record and constructor

;; (def material-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
;; (def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
;; (def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})



(def material-col-info-array
  [:item-pik "ITEMPIK" String [:required :digits]
   :item-action "ITEM_ACTION" String []
   :mfr-pik "MFR_PIK" BigInteger []
   :item-country "ITEM_COUNTRY" String []
   :item-industry "ITEM_INDUSTRY" String []
   :item-num "ITEM_NUM" String []
   :ean "EAN" String []
   :upc-flag "UPC_FLAG" String []
   :barcode-type "BARCODE_TYPE" String []
   :mfr-cat-num "MFR_CAT_NUM" String []
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
   :unspsc "TS_MAINTENANCE_MONTH" String []
   :unspsc40 "TS_UNSPSC40" String []
   :brand-name "TS_BRAND_NAME" String []
   :mfr-replaced-by-upc "MFR_REPLACED_BY_UPC" String []
   :mfr-replaced-by-cat-no "MFR_REPLACED_BY_CAT_NO" String []
   :mfr-replaces-upc "MFR_REPLACES_UPC" String []
   :mfr-replaces-cat-no "MFR_REPLACES_CAT_NO" String []
   :product-name "TS_PRODUCT_NAME" String []
   :gtin "TS_GTIN" String []
   :upc "TS_UPC" String []
   ])

(def num-material-cols (count (partition 4 material-col-info-array)))
(def material-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 material-col-info-array)))
  )

(def material-col-names
  (->> material-col-info-array (partition 4) (map first)))

(make-record TsMaterial material-col-names)
;(make-record ^{:col-info material-col-info-array} TsMaterial Material-col-names)
;; ->TsMaterial 

(def ->TsMaterial ^{:col-info material-col-info-array} ->TsMaterial) 
;; (meta ->TsMaterial)

;; TsMaterial 
;; (def x 3)
;; (def ^{:r 99} x 45)
;; ^#{:abc "hey"} #'x 
;; x 
;; (def y (with-meta x {:q 17}))

;; (:col-info TsMaterial)
;; (meta TsMaterial)
;; ((meta #'x) ^{:q 17})
;; (meta #'x)
;; (meta x)

;; (def a-var 2)
;; (def another-var (with-meta a-var {:foo :bar}))

;; (def a ^{:created (System/currentTimeMillis)}
;;   [1 2 3])
;; (meta a)

;(apply ->TsMaterial (range num-material-cols))

(defn vec->rec [rec-def cols]
  (let [errors (validate-with* cols [[#(= (count %) num-material-cols) #(str "Wrong number of columns: " (count %) " instead of " num-material-cols)]])]
    (if (not-empty errors)
                                        ;(do (logit errors (interleave (range 200) cols) "----") (println (interleave (range 200) cols) errors) nil)
      (do (logit-plain errors (interleave (range) cols) "----") (println cols errors) nil)
      (let [record (apply ->TsMaterial (map str/trim cols))
            errs (validate-record material-col-info record)]
        (if (not-empty errs)
          (do (logit-plain errs cols "----") nil)
          record)
        )
      )))

(defn ts-material [cols]
  (let [errors (validate-with* cols [[#(= (count %) num-material-cols) #(str "Wrong number of columns: " (count %) " instead of " num-material-cols)]])]
    (if (not-empty errors)
      ;(do (logit errors (interleave (range 200) cols) "----") (println (interleave (range 200) cols) errors) nil)
      (do (logit errors (interleave (range 200) cols) "----") (println cols errors) nil)
      (let [record (apply ->TsMaterial (map str/trim cols))
            errs (validate-record material-col-info record)]
        (if (not-empty errs)
          (do (logit errs cols "----") nil)
          record)
        )
      )))



;; Xml creation code

#_(defn ts-material-attributes [item]
  [:Values
        (reduce
          (fn [attrs [name col]]
            (let [id (:dbid col)]
              (if id
                (conj attrs [:Value
                             {:AttributeID id}
                             (escape-html (name item))])
                attrs)))
          () material-col-info)])

;(ts-material-attributes y)
;(x/emit-element (ts-material-attributes y))
;(x/emit-element {:tag :hello :attrs {:place "world"}})
;(x/emit-element {:tag :parent
;                 :attrs {:id "22" :name "fritz"}
;                 :content [
;                           {:tag :child :attrs {:id "56"}}
;                           {:tag :child :attrs {:id "57"}}]})

(defn ts-material-hiccup [item]
  [:Product
   {:ID (str "MEM_TS_" (:item-pik item))
           :UserTypeID "TS_Member_Record"
           :ParentID "TS_Member_Records"}
   (material-attributes material-col-info item)
   ]
   ;:content
  )

(defn ts-material-xml [item]
  (println
    (hiccup/html (ts-material-hiccup item)))
  item)



;; Read ts material file

;; (def matched-products (set (:ts (slurp-source-ids "punduit"))))
;; (contains? matched-products 121959245)
(def matched-products (set (map str (:ts (slurp-source-ids "punduit")))))
;; (contains? matched-products "121959245")

(defn transform-ts-material [item]
  item
  ;; (assoc item
  ;;        :mfr-pik (as-short-document-num (:mfr-pik item))
  ;;        )
  )

(defn process-ts-material [lines]
  (let [categories (atom #{})]
    (->> lines
         rest
         (remove nil?)
         (filter #(contains? matched-products (first %)))
         ;; (drop 50)
         ;; (take 5)
         ;; logit-plain
         (map ts-material)
         ;; (remove nil?)
         (map transform-ts-material)
         (map ts-material-xml)
         (doall)
         )
    nil
    ))
;; (process-ts-file-with "item.txt" process-ts-material)
;(time (write-ts-file))

(defn process-ts-file-with [filename fn]
  (let [full-filename (str ts-input-path filename)]
    (with-open [in-file (io/reader full-filename)]
      (let [lines (csv/read-csv in-file :separator \tab)]
        (fn lines)))))
;;  (process-file-with (str ts-input-path filename) fn))

(defn write-ts-file []
  (let [full-filename (str ts-output-path "material.xml")]
    (make-parents full-filename)
    (with-open [w (clojure.java.io/writer full-filename)]
      (binding [*out* w]
        (println (opening))
        (process-ts-file-with "item.txt" process-ts-material)
        (println (closing))
        ))))

;; (time (write-ts-file))
;(process-ts-file-with "STEP_MATERIAL.txt" process-ts-material)
;(def x (process-ts-file-with "STEP_MATERIAL.txt" process-ts-material))


