;; This file uses the mdm database to select products in idw/ts/sap that
;; can be matched. The distribution function selects 20% that will match across
;; the board, 20% that match across two of the three sources, and ~6% from each
;; source that won't match any other source.
;;
;; Note: a fresh database is assumed. There could be other matches
;;       if the database already has records.

(ns summit.step.import.product-selectors
  (:require [clojure.string :as str]
            [clojure.edn :as edn]

            [summit.utils.core :refer :all]
            ;; [summit.step.import.core :refer :all]
            ))

(def punduit-prods-sql "select j.*
  from mdm.idw_item i
  join mdm.idw_manufacturer m on m.id=i.manufacturer_id
  inner join mdm.join_product j on j.idw_index=i.idw_index
  where m.id=211 and j.matnr is not null and j.item_pik is not null and j.idw_index is not null")

;; (def prods (vec (exec-sql punduit-prods-sql)))

(defn round [x]
  (java.lang.Math/floor (double x)))

(defn convert-row-num [row-num num-rows]
  (round (* num-rows row-num (double 0.01))))

(defn convert-range [a-range num-rows]
  [(convert-row-num (first a-range) num-rows) (convert-row-num (second a-range) num-rows)])

(defn select-ranges [num-rows rows & ranges]
  (mapcat #(apply subvec (vec rows) (convert-range % num-rows)) ranges))
;; (def prods (exec-sql (str punduit-prods-sql " limit 100")))
;; (select-ranges 5 prods [0 20] [30 44])

(defn distribution-per-100
  "10 with all, 10 with pairs, 4 idw, 3 sap, and 3 ts"
  [num-rows prods]
  {:idw (sort (map :idw_index (select-percentage-ranges num-rows prods [0 40] [60 87])))
   :sap (sort (map :matnr     (select-percentage-ranges num-rows prods [0 60] [87 94])))
   :ts  (sort (map :item_pik  (select-percentage-ranges num-rows prods [0 20] [40 80] [94 100])))
   :prods prods
   })
;; (distribution-per-100 5 prods)

(defn get-source-ids [sql total-rows prod-distribution-fn]
  (let [prods (vec (exec-sql (str punduit-prods-sql " limit " total-rows)))]
    (prod-distribution-fn total-rows prods)
    ))
;; (get-source-ids punduit-prods-sql 50 distribution-per-100)
;; (-> (get-source-ids punduit-prods-sql 50 distribution-per-100) :ts count)

(defn spit-source-ids [filename sql num-golden-rows prod-distribution-fn]
  (let [ids (get-source-ids sql num-golden-rows prod-distribution-fn)
        path (str step-input-path "product-selections/")]
    (spit (str path filename ".edn") (with-out-str (clojure.pprint/pprint ids)))))

(defn slurp-source-ids [filename]
  (let [path (str step-input-path "product-selections/")]
    (read-string (slurp (str path filename ".edn")))))

;; (spit-source-ids "punduit" punduit-prods-sql 50 distribution-per-100)
;; (slurp-source-ids "punduit")

