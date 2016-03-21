;; This file uses the mdm database to select products in idw/ts/sap that
;; can be matched. The distribution function selects 20% that will match across
;; the board, 20% that match across two of the three sources, and ~6% from each
;; source that won't match any other source.
;;
;; Note: a fresh database is assumed. There could be other matches
;;       if the database already has records.


;; Steps:
;; 1. Select which joined products output
;;    this can be either directly or via distribution percentages
;; 2. Save this selection to file
;;    the important piece is :prods
;;    The purpose of saving is to allow manual editing
;; 3. Read one of these files, pass to next step
;; 4. For each source, pass set of ids to source specific processor
;;    (Note that the input for this step can come directly from step 1)
;;    The processors store the product ids they exported into @exported-products
;; 5. Import the source files into STEP.
;;    Deletion of product data prior to this step is preferrable but not required
;; 6. Get the actual data from (get-golden-references)
;; 7. Get the expected from @exported-products and the joined product list passed into step 4.
;; 8. Compare the expected with the actual.


;; Files:
;;   product-selectors-data
;;   source processors/exporters (sap-product, etc.)
;;   product_selectors
;;   comparator/coordinator


(ns summit.step.import.product-selectors
  (:require [clojure.string :as str]
            [clojure.edn :as edn]

            [summit.utils.core :refer :all]
            ;; [summit.step.import.core :refer :all]
            ))

(def ^:dynamic *matched-products* [])

(def joined-products (atom []))
(defn reset-exported-products []
  (def exported-products (atom {:ts [] :idw [] :sap []}))
  )
(reset-exported-products)

(defn sap-set-exported [x]
  (swap! exported-products assoc :sap (set (mapv read-string x))) x)
(defn ts-set-exported [x]
  (swap! exported-products assoc :ts (set (mapv read-string x))) x)
(defn idw-concat-exported [x]
  (swap! exported-products
         assoc :idw (set (concat (:idw exported-products) (mapv read-string x)))) x)

(defn idw-manufacturer-id [name]
  (let [sql (str "select id from mdm.idw_manufacturer where name='" name "'")]
    (:id (first (exec-sql sql)))))
;; (idw-manufacturer-id "Panduit")

(defn products-for-manufacturer-sql [idw-manu-name max-rows]
  (str "select j.*
  from mdm.idw_item i
  join mdm.idw_manufacturer m on m.id=i.manufacturer_id
  inner join mdm.join_product j on j.idw_index=i.idw_index
  where m.id=" (idw-manufacturer-id idw-manu-name) " and j.matnr is not null and j.item_pik is not null and j.idw_index is not null limit " max-rows))

(defn product-selections [idw-manu-name max-rows]
  (let [
        prods (vec (exec-sql (products-for-manufacturer-sql idw-manu-name max-rows)))
        cleaned (map #(assoc % :matnr (as-integer (:matnr %))) prods)
        ]
    cleaned))

(defn distribution-per-100
  "20 with all, 20 with pairs (60 total), 7 idw, 7 sap, and 6 ts"
  [num-rows prods]
  {:idw (sort (map :idw_index (select-percentage-ranges num-rows prods [0 40] [60 87])))
   :sap (sort (map :matnr     (select-percentage-ranges num-rows prods [0 60] [87 94])))
   :ts  (sort (map :item_pik  (select-percentage-ranges num-rows prods [0 20] [40 80] [94 100])))
   :prods prods
   })
;; (distribution-per-100 5 prods)

(defn get-distributed-ids [idw-manu-names total-rows prod-distribution-fn]
  ;; (let [prods (vec (exec-sql (str panduit-prods-sql " limit " total-rows)))]
   (let [prods (product-selections idw-manu-names total-rows)]
     (prod-distribution-fn total-rows prods)
     ))
;; (get-distributed-ids "Panduit" 50 distribution-per-100)
;; (second (get-distributed-ids ["Panduit"] 50 distribution-per-100))
;; (get-distributed-ids ["Panduit"] 50 distribution-per-100)
;; (count (:ts (first (get-distributed-ids ["Panduit"] 50 distribution-per-100))))
;; (-> (get-distributed-ids panduit-prods-sql 50 distribution-per-100) :ts count)

(defn spit-source-ids [filename idw-manu-name num-golden-rows prod-distribution-fn]
  (let [ids (get-distributed-ids idw-manu-name num-golden-rows prod-distribution-fn)
        path (str step-input-path "product-selections/")]
    ;; (spit (str path filename ".edn") (with-out-str (clojure.pprint/pprint ids)))))
    (spit (str path filename ".edn") (pp->str ids))
    ids))

(defn slurp-source-ids [filename]
  (let [path (str step-input-path "product-selections/")
        name (str path filename ".edn")]
    (read-string (slurp name))))

;; (spit-source-ids "panduit-50" "Panduit" 50 distribution-per-100)
;; (spit-source-ids "hubbell-panduit-500" "Panduit" 500 distribution-per-100)
;; (slurp-source-ids "panduit-50")
;; (:prods (slurp-source-ids "panduit-50"))

