(ns summit.step.import.idw.xls-spec-reader
  (:require [clojure
             [set :as set :refer [difference]]
             [string :as str]]
            [clojure.java.io :as io]
            [dk.ative.docjure.spreadsheet :as xls]

            [summit.step.import.idw.core :refer :all]
            [summit.utils.core :refer :all]))

(defonce ^:private wkbook nil)
(defn- workbook []
  (if (nil? wkbook)
    (def wkbook (xls/load-workbook (str idw-input-path "schema4.0.xlsx"))))
  wkbook)
(defonce ^:private uniq-attr-sheet nil)
(defn- unique-attr-sheet []
  (if (nil? uniq-attr-sheet)
    (def uniq-attr-sheet (xls/select-sheet #"Unique.*" (workbook))))
  uniq-attr-sheet)

(defonce idea-attributes
  (delay
   (let [attr-sheet (unique-attr-sheet)
         rows (drop 6 (xls/select-columns {:B :title :C :type :D :descript :E :example :F :abbv} attr-sheet))
         rows (map #(update % :title str/trim) rows)
         ]
     (into {} (map! #(let [h (humanize (:title %))
                           id (humanized->id h)]
                       [id (assoc % :title h :id id :source :idea)]) rows)))))
    ;; (into {} (map! #(vector (:title %) %) rows))))
;; (take 2 (idea-schema))


;; these are used merely for comparison to actual data loaded in the premdm
;; (defn- mdm-idw-attributes []
;;   (map! :name (exec-sql "select distinct name from mdm.idw_attribute order by name")))
(defonce mdm-idw-attributes
  (delay
   (map! :name (exec-sql "select distinct name from mdm.idw_attribute order by name"))))
(defn- normalize-string [s]
  (if (string? s)
    (str/lower-case (str/trim s))
    s))
(defn- spit-differences-between-ideaspreadsheet-and-premdm []
  (let [mdm (->> @mdm-idw-attributes (map normalize-string) set)
        idea (->> @idea-attributes keys (map normalize-string) set)
        extra-mdm (sort (difference mdm idea))
        extra-idea (sort (difference idea mdm))
        ]
    (spitln (str idw-output-path "idea-extras.csv") extra-idea)
    (spitln (str idw-output-path "mdm-extras.csv") extra-mdm)
    ;; (with-open [out (clojure.java.io/writer (str idw-output-path "idea-extras.csv"))]
    ;;   (binding [*out* out]
    ;;     (maprun println extra-idea)))
    ;; (with-open [out (clojure.java.io/writer (str idw-output-path "mdm-extras.csv"))]
    ;;   (binding [*out* out]
    ;;     (maprun println extra-mdm)))
    ;; (take 2 extra-idea)
    ;; (count extra-idea)
    (str "updated files (idea-extras.csv/mdm-extras.csv) in the " idw-output-path " directory")
    ))
(examples
 (spit-differences-between-ideaspreadsheet-and-premdm)
 (count @idea-attributes)
 (count @mdm-idw-attributes)
 )



"
jarred
IDW_Member_Record
TS_Member_Record
SAP_Member_Record
TSI_Member_Record
Summit_Enrichment_Record
GoldenRecordItem

"
