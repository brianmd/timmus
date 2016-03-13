(ns summit.step.import.sap.material
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.sap.core :refer :all]
            [summit.step.import.product-selectors :refer [slurp-source-ids]]
            ))

;; Create sap material record and constructor

(def material-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
(def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
(def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(def material-col-info-array
  [:matnr "MARA-MATNR" String [:required :digits]
   :type "MARA-MTART" String [:required (lookup-validator material-types)]
   :title "MAKT-MAKTX" String [:required]
   :uom "MARA-MEINS" String [(lookup-validator uom-types)]
   :summit-part "MARA-BISMT" String []

   :pp-restrict "MARA-MSTAE" String []

   :category-type "MARA-MTPOS_MARA" String [(lookup-validator category-types)]
   :ean11 "MARA-EAN11" String [:digits]
   :using-upc "MARA-NUMTP" String []
   :generic-non-stock? "MARA-ZZGNONSTK" String []
   :ts-item-pik "MARA-ZZTS-ITEM-PIK" String []
   :batch? "MARA-XCHPF" String []
   :mfr-part-num "MARA-MFRPN" String []
   :mfr-id "MARA-MFRNR" String []
   :delivery-unit "MVKE-SCMNG" String []
   :category-group "MVKE-MTPOS" String [(lookup-validator category-types)]         ; mvke => sales data for material
   :descript "STXH/STXL-GRUN_TXT" String []
   ])

(def material-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 material-col-info-array)))
  )

(def material-col-names
  (->> material-col-info-array (partition 4) (map first)))

(make-record SapMaterial material-col-names)
;(apply ->SapMaterial (range 17))

(defn sap-material [cols]
  (let [errors (validate-with* cols [[#(= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)]])]
    (if (not-empty errors)
      (do (logit errors cols "----") (println) nil)
      (let [record (apply ->SapMaterial (map str/trim cols))
            errs (validate-record material-col-info record)]
        (if (not-empty errs)
          (do (logit errs cols "----") nil)
          record)
        )
      )))



;; Xml creation code

#_(defn sap-material-attributes [item]
  [:Values
        (reduce
          (fn [attrs [name col]]
            (conj attrs [:Value
                         {:AttributeID (:dbid col)}
                         (escape-html (name item))]))
          () material-col-info)])

#_(defn sap-material-hiccup-orig [item]
  {:tag :Product
   :attrs {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   :content
   [(material-attributes item)]})

(defn sap-material-hiccup [item]
  [:Product
   {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   (material-attributes material-col-info item)
   ;; (sap-material-attributes item)
   ]
   ;:content
  )

(defn sap-material-xml [item]
  (println
    (hiccup/html (sap-material-hiccup item)))
  item)



;; Read sap material file

(defn transform-sap-material [item]
  (assoc item
         :matnr (as-short-document-num (:matnr item))
         ;; :mfr-id (as-short-document-num (:mfr-id item))
         ))

(defn process-sap-material [lines]
  (let [categories (atom #{})]
    (->> lines
         rest
         (remove nil?)
         ;; (filter #(contains? matched-products (first %)))
         (filter (comp matched-products first))
         ;; (take 2)
         ;; logit-plain
         (map sap-material)
         ;; (remove nil?)
         (map transform-sap-material)
         (map sap-material-xml)
         (map :matnr)
         sap-set-exported
         (dorun)
         )
    ))
;; (time (write-sap-file))
;; (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)

(defn process-sap-file-with [filename fn]
  (process-file-with (str sap-input-path filename) fn))

(defn write-sap-file []
  (with-open [w (clojure.java.io/writer (str sap-output-path "material.xml"))]
    (binding [*out* w]
      (println (opening))
      (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
      (println (closing))
      )))

;; (time (write-sap-file))
;(process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
;(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material))

(println "finished loading summit.step.import.sap.material")

