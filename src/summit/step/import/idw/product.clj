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
   :upc "UPC" String []
   :gtin "GTIN" String []
   :ean13 "EAN" String []
   :catalog-num "PART_NUMBER" String []
   :idw-index "idw_index" String []
   :unspsc "UNSPSC" String []
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
  (let [parent-id (let [p (:unspsc item)]
                    (if (or (nil? p) (= p """"))
                      "IDW_Member_Records"
                      (str "IDW_UNSPSC_" p)))]
    [:Product
     {:ID (str "MEM_IDW_" (:idw-index item))
      :UserTypeID "IDW_Member_Record"
      :ParentID parent-id}
     (product-attributes product-col-info item)
     ]))

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
  (pp "processing idw product")
  (let [categories (atom #{})
        ;; matched-products (set (:idw (slurp-source-ids "current")))
        ]
    (->> lines
         rest
         (remove nil?)
         ;; (take 2)
         ;; logit-plain
         ;; pp
         (map #(select-ranges % [3 4] [6 14] [16 19] [34 35]))
         ;; (take 200)
         ;; (filter #(contains? matched-products (nth % 6)))
         ;; (filter (comp *matched-products* as-integer idw-index-of))
         ;; ;; logit-plain
         (map idw-product)
         (map transform-idw-product)
         ;; ;; logit-plain
         (map idw-product-xml)
         (map :idw-index)
         ;; pp
         idw-concat-exported
         (dorun)
         )
    ))

(defn process-idw-file-with [filename fn]
  (process-file-with (str idw-input-path filename) fn))

(defn write-idw-file [filename]
  (with-open [w (clojure.java.io/writer (str idw-output-path "product.xml"))]
    (binding [*out* w]
      (println (opening))
      (process-idw-file-with filename process-idw-product)
      (println (closing))
      )))

(examples
 (time (write-idw-file "Panduit14374598.csv"))
 (time (write-idw-file "Arlington14798724.csv"))
 (time (write-idw-file "HubbellWiringDevice-Kellems14490913.csv"))
 (time (write-idw-file "MilwaukeeElectricTool14870760.csv"))
 )


(println "finished loading summit.step.import.idw.product")
