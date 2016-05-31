(ns summit.db.step
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

            [config.core :refer [env]]

            [korma.core :as k]
            [korma.db :refer [defdb oracle]]))

(defdb step-db
  ;; (oracle {:subname "@//stibo-prd-db.insummit.com:1521/STEPSYS"
  ;; (oracle {:subname "@//stibo-db.insummit.com:1521/step"
  (oracle {:subname "@//stibo-prd-db.insummit.com:1521/step"
           :user (-> env :db :step-prd :user)
           :password (-> env :db :step-prd :password)
           :naming {:keys str/lower-case :fields str/lower-case}
           ;; :host "stibo-prd-db.insummit.com"
           ;; :port 1521
           }))

;; (exec-sql step-db"SELECT * FROM user_cons_columns")


(defn get-golden-product [source-id]
  (let [source-product (first (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:NAME source-id})))
        ;; golden-record-link 1788734
        golden-record-link-type (:LINKTYPEID (first (k/select :STEPSYS.VT_REFERENCETYPE (k/database step-db) (k/where {:ID "GoldenRecord"}))))
        link (first (k/select :STEPSYS.VT_PRODUCTREFERENCELINK (k/database step-db) (k/where {:TARGETID (:ID source-product) :REFERENCETYPEID golden-record-link-type})))]
    ;; (ppn source-product)
    ;; (ppn golden-record-link-type)
    ;; (ppn link)
    (first (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:ID (:SOURCEID link)})))))
;; (get-golden-product "MEM_IDW_11163656")

;; these works
(examples
 (exec-sql step-db "select nodeid id, name from node n where nodetype='c'")
 (exec-sql step-db "select count(*) cnt from node n where nodetype='c'")
 (exec-sql step-db "select * from node n where nodetype='c'")
 (exec-sql step-db "SELECT * FROM ALL_OBJECTS where OBJECT_TYPE='VIEW' and owner='STEPSYS' order by object_name")
 )
;; but this doesn't
;; (k/select :node (k/database step-db) (k/where {:nodetype "c"}))

;; [({:ID 1955749M, :SUBTYPEID 1753550M, :NAME "MEM_IDW_11163656"}) ()]

;; (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:ID 1674015}))
;; ({:ID 1674015M, :SUBTYPEID 1673959M, :NAME "ProductOverridesRoot"})
;; (k/select :STEPSYS.VT_SUBTYPE (k/database step-db) (k/where {:ID 1673959}))

;; (k/select :STEPSYS.VT_PRODUCTREFERENCELINK (k/database step-db) (k/where {:SOURCEID 2015657}))
;; ({:ID 2015658M, :SOURCEID 2015657M, :TARGETID 1955749M, :QUALIFIERID -5M, :WORKSPACE 1378163M, :SEQNO 0M, :REFERENCETYPEID 1788734M, :SUPPRESSING 0M})
;; (k/select :STEPSYS.VT_REFERENCETYPE (k/database step-db) (k/where {:LINKTYPEID 1788734}))
;; ({:ID "GoldenRecord", :LINKTYPEID 1788734M, :ISDESCRIPTION 0M, :MULTIVALUE 1M, :REVISED 1M})
;; (k/select :STEPSYS.VT_REFERENCETYPE (k/database step-db) (k/where {:ID "GoldenRecord"}))

;; (k/select :STEPSYS.VT_PRODUCTREFERENCELINK (k/database step-db) (k/where {:TARGETID 1955749}))
;; (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:ID 2015657}))
;; (assert=
;;  (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:ID 1955749}))
;;  (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:NAME "MEM_IDW_11163656"})))

;; (k/select :STEPSYS.VT_PRODUCT (k/database step-db) (k/where {:SOURCEID 2015657}))

;; (k/select :customers (k/database (utils/find-db :bh-local)) (k/limit 1))

;; (-> (k/select* :VT_PRODUCT)
;;     (k/database step-db)
;;     (k/where {:ID 1674015})
;;     (k/limit 5)
;;     (k/as-sql)
;;     )
;; "SELECT \"VT_PRODUCT\".* FROM \"VT_PRODUCT\" WHERE (\"VT_PRODUCT\".\"ID\" = ?) LIMIT 5"

;; (-> (k/select* :users)
;;     (k/where {:first "john"
;;             :last "doe"})
;;     (k/as-sql))



