(println "loading summit.step.import.idw.product")

(ns summit.step.import.idw.product
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.idw.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]
            ))

;; Create sap product record and constructor

;; (def product-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
;; (def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
;; (def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(def product-col-info-array
  [;; :seller-id-qualifier nil nil []
   :seller-id "IDW_MANUID" String []

   :item-ctrl-number "IDW_ITEM_CTRL" String []
   :upc "IDW_UPC" String []
   :gtin "IDW_GTIN" String []
   :ean13 "IDW_EAN" String []
   :catalog-num "IDW_catalog" String []
   :idw-index "idw_index" String []
   :unspsc "IDW_UNSPSC" String []
   :igcc "IDW_IGCC" String []

   :update-status "IDW_STATUS" String []
   :time-stamp "IDW_STATUSDT" String []
   :country-of-origin "IDW_CNTRYOFORIGIN" String []

   :msds-flag "IDW_MSDSFLAG" String []
   ])

(def product-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 product-col-info-array)))
  )

(def product-col-names
  (->> product-col-info-array (partition 4) (map first)))

(def num-columns (count product-col-names))

;; (def IdwProduct (apply create-struct product-col-names))
(make-record IdwProduct product-col-names)
;; (apply ->IdwProduct (range 12))

(defn idw-product [cols]
  (let [errors (validate-with* cols [[#(= (count %) num-columns) #(str "Wrong number of columns: " (count %) " instead of " num-columns)]])]
    (if (not-empty errors)
      (do (logit-plain errors cols "----") (println) nil)
      (let [record (apply ->IdwProduct (map str/trim cols))
            errs (validate-record product-col-info record)]
        (if (not-empty errs)
          (do (logit-plain errs cols "----") nil)
          record)
        )
      )))



;; Xml creation code

#_(defn product-attributes [product-col-info item]
  [:Values
   (reduce
    (fn [attrs [name col]]
      (if col
        (conj attrs [:Value
                     {:AttributeID (:dbid col)}
                     (escape-html (name item))])
        attrs))
    () product-col-info)])

;(sap-product-attributes y)
;(x/emit-element (sap-product-attributes y))
;(x/emit-element {:tag :hello :attrs {:place "world"}})
;(x/emit-element {:tag :parent
;                 :attrs {:id "22" :name "fritz"}
;                 :content [
;                           {:tag :child :attrs {:id "56"}}
;                           {:tag :child :attrs {:id "57"}}]})

(defn idw-product-hiccup [item]
  [:Product
   {:ID (str "MEM_IDW_" (:idw-index item))
           :UserTypeID "IDW_Member_Record"
           :ParentID "IDW_Member_Records"}
   (product-attributes product-col-info item)
   ]
   ;:content
  )

(defn idw-product-xml [item]
  (println
    (hiccup/html (idw-product-hiccup item)))
  item)



;; Read sap product file

;; (def matched-products (set (:idw (slurp-source-ids "punduit"))))
;; (def matched-products (set (map str (:idw (slurp-source-ids "punduit")))))
;; (contains? matched-products 9566224)
;; matched-products 

(defn transform-idw-product [item]
  item)
  ;(assoc item :matnr (as-short-document-num (:matnr item))))

(defn idw-index-of [item]
  (nth item 6))

(defn process-idw-product [lines]
  (let [categories (atom #{})
        ;; matched-products (set (:idw (slurp-source-ids "current")))
        ]
    (->> lines
         rest
         (remove nil?)
         ;; (take 2)
         ;; logit-plain
         (map #(select-ranges % [3 4] [6 14] [16 19] [34 35]))
         ;; logit-plain
         ;; (filter #(contains? matched-products (nth % 6)))
         (filter (comp *matched-products* as-integer idw-index-of))
         ;; (take 5)
         ;; logit-plain
         (map idw-product)
         (map transform-idw-product)
         ;; logit-plain
         (map idw-product-xml)
         (map :idw-index)
         idw-concat-exported
         (dorun)
         )
    ))
;; (time (write-idw-file "Panduit14374598.csv"))
;; (process-idw-file-with "Panduit14374598.csv" process-idw-product)

;; (process-idw-file-with "KleinTools14353859.csv" process-idw-product)
;; (-> x first)
;; (-> x first count)


(defn process-idw-file-with [filename fn]
  (process-file-with (str idw-input-path filename) fn))

(defn write-idw-file [filename]
  (with-open [w (clojure.java.io/writer (str idw-output-path "product.xml"))]
    (binding [*out* w]
      (println (opening))
      (process-idw-file-with filename process-idw-product)
      (println (closing))
      )))

;; (time (write-idw-file "Panduit14374598.csv"))
;; (process-idw-file-with "KleinTools14353859.csv" process-idw-product)
;(def x (process-idw-file-with "STEP_MATERIAL.txt" process-idw-product))




#_(comment

   :reserved-1 nil nil []
   :reserved-2 nil nil []
   :reserved-3 nil nil []
   :reserved-4 nil nil []
   :reserved-5 nil nil []
   :attr-name-1 nil String []
   :attr-value-1 nil String []
   :attr-1-uom nil String []
   :attr-name-2 nil String []
   :attr-value-2 nil String []
   :attr-2-uom nil String []
   :attr-name-3 nil String []
   :attr-value-3 nil String []
   :attr-3-uom nil String []
   :attr-name-4 nil String []
   :attr-value-4 nil String []
   :attr-4-uom nil String []
   :attr-name-5 nil String []
   :attr-value-5 nil String []
   :attr-5-uom nil String []
   :attr-name-6 nil String []
   :attr-value-6 nil String []
   :attr-6-uom nil String []
   :attr-name-7 nil String []
   :attr-value-7 nil String []
   :attr-7-uom nil String []
   :attr-name-8 nil String []
   :attr-value-8 nil String []
   :attr-8-uom nil String []
   :attr-name-9 nil String []
   :attr-value-9 nil String []
   :attr-9-uom nil String []
   :attr-name-10 nil String []
   :attr-value-10 nil String []
   :attr-10-uom nil String []
   :attr-name-11 nil String []
   :attr-value-11 nil String []
   :attr-11-uom nil String []
   :attr-name-12 nil String []
   :attr-value-12 nil String []
   :attr-12-uom nil String []
   :attr-name-13 nil String []
   :attr-value-13 nil String []
   :attr-13-uom nil String []
   :attr-name-14 nil String []
   :attr-value-14 nil String []
   :attr-14-uom nil String []
   :attr-name-15 nil String []
   :attr-value-15 nil String []
   :attr-15-uom nil String []
   :attr-name-16 nil String []
   :attr-value-16 nil String []
   :attr-16-uom nil String []
   :attr-name-17 nil String []
   :attr-value-17 nil String []
   :attr-17-uom nil String []
   :attr-name-18 nil String []
   :attr-value-18 nil String []
   :attr-18-uom nil String []
   :attr-name-19 nil String []
   :attr-value-19 nil String []
   :attr-19-uom nil String []
   :attr-name-20 nil String []
   :attr-value-20 nil String []
   :attr-20-uom nil String []
   :attr-name-21 nil String []
   :attr-value-21 nil String []
   :attr-21-uom nil String []
   :attr-name-22 nil String []
   :attr-value-22 nil String []
   :attr-22-uom nil String []
   :attr-name-23 nil String []
   :attr-value-23 nil String []
   :attr-23-uom nil String []
   :attr-name-24 nil String []
   :attr-value-24 nil String []
   :attr-24-uom nil String []
   :attr-name-25 nil String []
   :attr-value-25 nil String []
   :attr-25-uom nil String []
   :attr-name-26 nil String []
   :attr-value-26 nil String []
   :attr-26-uom nil String []
   :attr-name-27 nil String []
   :attr-value-27 nil String []
   :attr-27-uom nil String []
   :attr-name-28 nil String []
   :attr-value-28 nil String []
   :attr-28-uom nil String []
   :attr-name-29 nil String []
   :attr-value-29 nil String []
   :attr-29-uom nil String []
   :attr-name-30 nil String []
   :attr-value-30 nil String []
   :attr-30-uom nil String []
   :attr-name-31 nil String []
   :attr-value-31 nil String []
   :attr-31-uom nil String []
   :attr-name-32 nil String []
   :attr-value-32 nil String []
   :attr-32-uom nil String []
   :attr-name-33 nil String []
   :attr-value-33 nil String []
   :attr-33-uom nil String []
   :attr-name-34 nil String []
   :attr-value-34 nil String []
   :attr-34-uom nil String []
   :attr-name-35 nil String []
   :attr-value-35 nil String []
   :attr-35-uom nil String []
   :attr-name-36 nil String []
   :attr-value-36 nil String []
   :attr-36-uom nil String []
   :attr-name-37 nil String []
   :attr-value-37 nil String []
   :attr-37-uom nil String []


)

(println "finished loading summit.step.import.idw.product")
