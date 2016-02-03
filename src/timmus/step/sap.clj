(ns timmus.step.sap
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]
            [clojure.data.xml :as xml]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [clojure.pprint :refer :all]

            [timmus.step.xml-output :refer :all]

            [timmus.utils.core :refer :all]
            ))

(def sap-path (str step-input-path "sap/"))

(def material-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
(def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(def material-col-info-array
  [:matnr "MARA-MATNR" String [required string-integer]
   :type "MARA-MTART" String [#(contains? material-types %)]
   :title "MAKT-MAKTX" String [required]
   :uom "MARA-MEINS" String [#(contains? uom-types %)]
   :summit-part "MARA-BISMT" String []

   :pp-restrict "MARA-MSTAE" String []

   :category-type "MARA-MTPOS_MARA" String []
   :ean11 "MARA-EAN11" String []
   :using-upc "MARA-NUMTP" String []
   :generic-non-stock? "MARA-ZZGNONSTK" String []
   :ts-item-pik "MARA-ZZTS-ITEM-PIK" String []
   :batch? "MARA-XCHPF" String []
   :mfr-part-num "MARA-MFRPN" String []
   :mfr-id "MARA-MFRNR" String []
   :delivery-unit "MARA-SCMNG" String []
   :category-group "MVKE-MTPOS" String [#(contains? material-types %)]         ; mvke => sales data for material
   :descript "MARA-STXH/STXL-GRUN_TXT" String []
   ])

;(def material-col-info
;  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 material-col-info-array)))
;  )

(def material-col-names
  (->> material-col-info-array (partition 4) (map first)))

(make-record SapMaterial material-col-names)
(apply ->SapMaterial (range 17))

(defn validate-with [fn msg-fn val]
  (if (fn val)
    (msg-fn val)))

(defn errors-using [fns val]
  (let [errs (map #(validate-with (first %) (second %) val) (partition 2 fns))
        errs (remove nil? errs)]
    (if (not-empty errs) errs)))

(defn sap-material [cols]
  (if-let [errors (errors-using [#(not= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)
                                 ]
                               cols)]
    (println (str "Matnr " (first cols) " has the following errors: " errors))
    (apply ->SapMaterial cols)
    ))

(defn process-sap-file-with [filename fn]
       (with-open [in-file (io/reader (str sap-path filename))]
         (let [lines (csv/read-csv in-file :separator \| :quote \^)]
           (println "hey")
           (fn lines)
           )))

;(defn process-material [x]
;  (->
;    (println x)
;    (take 3)
;    )
;  )

;(defn process-material-file []
;  (process-sap-file-with "STEP_MATERIAL.txt"))

(defn process-sap-material [lines]
  (println "in printem")
  (let [leaves (atom #{})]
    (println leaves)
    (->> lines
         rest
         (take 5)
         (map sap-material)
         (remove nil?)
         ;(rest)
         ;(map get-leaf-class)
         ;(take 25)
         ;(map #(swap! leaves conj %))
         (doall)
         )
    ;@leaves
    ))


;(process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
;(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material))
;(map :matnr x)

;
;(map (fn [x] [x (env x)])
;     (sort (keys env))
;     )


