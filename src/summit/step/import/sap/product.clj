(ns summit.step.import.sap.product
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.sap.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]
            ))

;; Create sap product record and constructor

(def product-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
(def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
(def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(def product-col-info-array
  [:matnr "MARA-MATNR" String [:required :digits]
   :type "MARA-MTART" String [:required (lookup-validator product-types)]
   :title "MAKT-MAKTX" String [:required]
   :uom "UOM" String [(lookup-validator uom-types)]
   :summit-part "MARA-BISMT" String []

   :pp-restrict "MARA-MSTAE" String []

   :category-type "MARA-MTPOS_MARA" String [(lookup-validator category-types)]
   :ean11 "UPC" String [:digits]
   :using-upc "MARA-NUMTP" String []
   :generic-non-stock? "MARA-ZZGNONSTK" String []
   :ts-item-pik "MARA-ZZTS-ITEM-PIK" String []
   :batch? "MARA-XCHPF" String []
   :mfr-part-num "PART_NUMBER" String []
   :mfr-id "MARA-MFRNR" String []
   :delivery-unit "MVKE-SCMNG" String []
   :category-group "MVKE-MTPOS" String [(lookup-validator category-types)]         ; mvke => sales data for product
   ;; :descript "STXH/STXL-GRUN_TXT" String []
   :descript "DESCRIPTION" String []
   ])

(def product-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 product-col-info-array)))
  )

(def product-col-names
  (->> product-col-info-array (partition 4) (map first)))

(make-record SapProduct product-col-names)
;(apply ->SapProduct (range 17))

(defn sap-product [cols]
  (let [errors (validate-with* cols [[#(= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)]])]
    (if (not-empty errors)
      (do (logit errors cols "----") (println) nil)
      (let [record (apply ->SapProduct (map str/trim cols))
            errs (validate-record product-col-info record)]
        (if (not-empty errs)
          (do (logit errs cols "----") nil)
          record)
        )
      )))



;; Xml creation code

#_(defn sap-product-attributes [item]
  [:Values
        (reduce
          (fn [attrs [name col]]
            (conj attrs [:Value
                         {:AttributeID (:dbid col)}
                         (escape-html (name item))]))
          () product-col-info)])

#_(defn sap-product-hiccup-orig [item]
  {:tag :Product
   :attrs {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   :content
   [(product-attributes item)]})

(defn sap-product-hiccup [item]
  [:Product
   {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   (product-attributes product-col-info item)
   ;; (sap-product-attributes item)
   ]
   ;:content
  )

(defn sap-product-xml [item]
  (println
    (hiccup/html (sap-product-hiccup item)))
  item)



;; Read sap product file

(defn transform-sap-product [item]
  (assoc item
         :matnr (as-short-document-num (:matnr item))
         ;; :mfr-id (as-short-document-num (:mfr-id item))
         ))

(def file-line-num (atom 0))
(def last-file-line (atom nil))
(defn reset-file-line-num [] (reset! file-line-num 0))
(defn bump-line-num [coll]
  (swap! file-line-num inc)
  (reset! last-file-line (first coll))
  (if (empty? coll)
    coll
    (cons (first coll) (lazy-seq (bump-line-num (rest coll)))))
  )
;; (swap! file-line-num inc)
;; file-line-num 
;; last-file-line 
;; (bump-line-num (range 1000000))

;; (defn bump-line-num [coll]
;;   (when (seq coll)
;;     (if (pred (first coll))
;;       (lazy-cons (first coll) (filter pred (rest coll)))
;;       (recur pred (rest coll)))))
;;   (lazy-seq
;;    (when-let [s (seq coll)]
;;      (swap! file-line-num inc)
;;      (recur (rest s)))))

#_(do
  (reset-file-line-num)
  (doall (take 2 (bump-line-num [4 5 56])))
  @file-line-num
  )

;; (defn mapp
;;   ([f coll]
;;    (lazy-seq
;;     (when-let [s (seq coll)]
;;       (cons (f (first s)) (mapp f (rest s)))))))
;; (mapp inc [3 4 5])

(def mfr-arlington "0092000734")
(def mfr-hubbell "0092003655")
(def mfr-milwaukee "0092005450")

(defn keep-good [v]
  (if (= (count v) 17)
    v
    (ppn "bad record:" v)))

(defn process-sap-product [lines]
  (reset-file-line-num)
  (let [categories (atom #{})
        ;; matched-products (set (:sap (slurp-source-ids "current")))
        ]
    (->> lines
         ;; bump-line-num
         rest
         (remove nil?)
         ;; (filter #(contains? matched-products (first %)))
         ;; logit-plain
         ;; (filter #(> (count %) 10))
         (filter keep-good)
         ;; (filter (comp *matched-products* as-integer first))
;;   (filter #(= mfr-milwaukee (nth % 13)))  ;; arlington. who we pay
         ;; (filter (comp matched-products first))
         ;; (take 200)
         ;; logit-plain
         (map sap-product)
         ;; (remove nil?)
         (map transform-sap-product)
         (map sap-product-xml)
         (map :matnr)
         ;; sap-set-exported
         (dorun)
         )
    ))
;; (time (write-sap-file))
;; (:ts @exported-products)
;; @exported-products 
;; (:sap @exported-products)
;; (process-sap-file-with "STEP_MATERIAL.txt" process-sap-product)

(defn process-sap-file-with [filename fn]
  (process-verticalbar-file-with (str sap-input-path filename) fn))

(defn write-sap-file []
  (with-open [w (clojure.java.io/writer (str sap-output-path "product.xml"))]
    (binding [*out* w]
      (println (opening))
      (process-sap-file-with "STEP_MATERIAL.txt" process-sap-product)
      (println (closing))
      )))

(examples
 (time (write-sap-file))
 )
;(process-sap-file-with "STEP_MATERIAL.txt" process-sap-product)
;(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-product))

(println "finished loading summit.step.import.sap.product")

