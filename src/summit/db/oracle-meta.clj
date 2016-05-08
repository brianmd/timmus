(ns summit.db.oracle-meta
  (:require [clojure.string :as str]
            [summit.utils.core :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.sap.core :refer :all]
            ;; ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            ;; [summit.step.import.product-selectors :refer :all]

            ;; [summit.sap.core :as sap]
            ;; [summit.sap.price :as price]
            ;; [summit.sap.customer-materials :refer [customer-materials]]

            [summit.utils.core :as utils :refer :all]
            ;; [summit.db.relationships :as rel :refer :all]

            [korma.core :as k]
            [korma.db :refer [defdb oracle]]
            ))

(defdb step-db
  ;; (oracle {:subname "@//stibo-prd-db.insummit.com:1521/STEPSYS"
  ;; (oracle {:subname "@//stibo-db.insummit.com:1521/step"
  (oracle {:subname "@//stibo-prd-db.insummit.com:1521/step"
           :user "STEPVIEW"
           :password "stepview"
           :naming {:keys str/lower-case :fields str/lower-case}
           ;; :host "stibo-prd-db.insummit.com"
           ;; :port 1521
           }))

(defn view-descriptions [schema-name]
  (exec-sql step-db (str "SELECT * FROM ALL_OBJECTS where OBJECT_TYPE='VIEW' and owner='" schema-name "' order by object_name")))

(defn synonym-descriptions [schema-name]
  (exec-sql step-db (str "SELECT * FROM ALL_OBJECTS where OBJECT_TYPE='SYNONYM' and owner='" schema-name "' order by object_name")))

(defn table-descriptions [schema-name]
  (exec-sql step-db (str "SELECT * FROM ALL_OBJECTS where OBJECT_TYPE='TABLE' and owner='" schema-name "' order by object_name")))


(def oracle-info (atom {}))

(defn get-oracle-info [owner type]
  (exec-sql step-db (str "select * from all_" type "s where owner='" (str/upper-case owner) "'")))
;; (get-oracle-info "stepview" "table")
(get-oracle-info "stepsys" "table")

(defn store-all-oracle-info []
  (for [type ["table" "view" "synonym" "dependencie"]
        owner ["stepsys" "stepview"]]
    (do (println type owner)
        (swap! oracle-info assoc [(keyword owner) (keyword type)] (get-oracle-info owner type))))
    )
;; (store-all-oracle-info)

(defn get-oracle [tbl-name]
  (exec-sql step-db (str "select * from " (->str tbl-name))))



(examples

(maprun #(swap! oracle-info assoc % (get-oracle %))
        [:all-tables :all-views :all-synonyms
         :all-dependencies :all-constraints
         :all-cons-columns :all-objects :database-properties
         :all-nested-table-cols :all_source :all-tab-cols
         :all-indexes :all-ind-columns :all-ind-expressions
         ])
(spit "oracle-meta" @oracle-info)

(exec-sql step-db "select * from all_views where view_name='VT_NODE'")
({:oid_text_length nil, :view_type_owner nil, :superview_name nil, :view_name "VT_NODE", :type_text_length nil, :oid_text nil, :type_text nil, :read_only "N", :text_length 69M, :editioning_view "N", :owner "STEPSYS", :view_type nil, :text "select nodeid id, nodetype, usertypeid subtypeid, name\n    from node\n"})

(->>
 (synonym-descriptions "STEPVIEW")
 (map :object_name)
 (take 20))
(->>
 (table-descriptions "STEPSYS")
 (map :object_name)
 (take 20))

)
