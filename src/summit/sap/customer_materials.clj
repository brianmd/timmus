(ns summit.sap.customer-materials
  (:require [summit.utils.core :refer :all]
            ;; [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]))

(defn extract-matnr-custno [v]
  ((juxt #(as-integer (nth % 3)) #(nth % 7)) v))

(defn customer-materials [account-num]
  (let [f (find-function :prd :bapi_custmatinfo_getlist)]
    (push f {:customerrange [["I" "EQ" (as-document-num account-num)]]})
    (execute f)
    (map extract-matnr-custno (pull f :customermaterialinfodetail))
    ))

(examples
 (def mat-dat (customer-materials 1007135))
 (second mat-dat)
 (ppn mat-dat)
 )

