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

(defn process-sap-file-with [filename fn]
  (with-open [in-file (io/reader (str sap-path filename))]
    (let [lines (csv/read-csv in-file :separator \| :quote \^)]
      (fn lines)
      )))


(def material-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
(def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
(def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(defn lookup-validator [lookup-table]
  [#(contains? lookup-table %) #(str "Value not in lookup table: " %)])
;((first (lookup-validator material-types)) "SAMM")
[(lookup-validator material-types)]
((first (first [(lookup-validator material-types)])) "SAMM")

(def material-col-info-array
  ;[:matnr "MARA-MATNR" String [:required :digits [:count 17]]
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
   :delivery-unit "MARA-SCMNG" String []
   :category-group "MVKE-MTPOS" String [(lookup-validator category-types)]         ; mvke => sales data for material
   :descript "MARA-STXH/STXL-GRUN_TXT" String []
   ])

(def material-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 material-col-info-array)))
  )
;material-col-info
;(keys material-col-info)
;(-> material-col-info :type)
;(-> material-col-info :matnr :validators)

(def material-col-names
  (->> material-col-info-array (partition 4) (map first)))

(make-record SapMaterial material-col-names)
;(apply ->SapMaterial (range 17))

;(defn validate-with [val fn msg-fn]
;  (if (not (fn val))
;    (msg-fn val)))

(defn validate-with [val fns]
  "fns => [assertion-fn msg-fn]"
  (if (not ((first fns) val))
    ((second fns) val)))

(defn validator-fns [fns-or-keyword]
  (if (keyword? fns-or-keyword)
    (validators fns-or-keyword)
    fns-or-keyword))
;(validator-fns :required)

(defn validate-with* [val fns*]
  (let [val-validator (partial validate-with val)
        validator-fns (map validator-fns fns*)]
    ;(remove nil? (map val-validator validator-fns))))
    (remove nil?
            (map val-validator validator-fns))))

;(validate-with* "1a" [(:required validators) (:digits validators)])
;(validate-with* "1a" [:required :digits])
(assert (=
    (validate-with* "" (-> material-col-info :matnr :validators))
    '("This field is required." "The deck was stacked against bro: ")
    ))
(validate-with* "" (-> material-col-info :type :validators))
(validate-with* "asdf" (-> material-col-info :type :validators))
(validate-with* "DIEN" (-> material-col-info :type :validators))
;(-> material-col-info :type :validators first first)
;((-> material-col-info :type :validators first first) "DIEN")
;((-> material-col-info :matnr :validators first) "")
;((-> material-col-info :matnr :validators first) "")
(-> material-col-info :type :validators)
material-col-info
*e

(defn validate-colname-with* [colname val fns*]
  (let [msgs (validate-with* val fns*)]
    (if (not-empty msgs)
      [colname msgs])))
;(validate-colname-with* :matnr "" [:required :digits])
;(validate-colname-with* :matnr "122" [:required :digits :test-failure])
;(validate-with* (range 16) [[#(= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)]])

(defn validate-col [cols-info record col-name]
  (let [col-info (col-name cols-info)
        validators (:validators col-info)
        ;errors (validate-col-with* :matnr )
        ]
    (validate-colname-with* col-name (col-name record) validators)))

(validate-col material-col-info (makeit) :matnr)
(validate-col material-col-info (makeit) :type)
*e
(defn makeit []
  (sap-material (map str (range 17))))
(makeit)




(defn validate-record [cols-info record]
  (let [v (partial validate-col cols-info record)]
    (into {}
          (->>
            (map v (keys cols-info))
            (remove nil?)
            ))))
;(validate-record material-col-info (makeit))

(defn sap-material [cols]
  (let [errors (validate-with* cols [[#(= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)]])]
    (if (not-empty errors)
      errors
      (let [record (apply ->SapMaterial (map str/trim cols))
            errs (validate-record material-col-info record)]
        (if (not-empty errs)
          errs
          record)
        )
      )))

(defn process-sap-material [lines]
  (let [categories (atom #{})]
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
    ;@categories
    ))

(sap-material [192 2])
(sap-material (map str (range 17)))
(process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material))
(map :matnr x)


