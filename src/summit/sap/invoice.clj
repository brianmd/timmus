(ns summit.sap.invoice
  (:require [clojure.set :as set]
            [summit.utils.core :refer :all]
            [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]

            [korma.core :as k]))

(defn get-raw-invoices
  ([env cust-id from-date to-date] (get-raw-invoices env cust-id from-date to-date nil))
  ([env cust-id from-date to-date job-acct]
   (let [f (find-function env :z_o_invoices_query)]
     (push f {:i-customer (as-customer-num cust-id)
              :if_invoices "X"
              :i_from_date from-date
              :i_to_date to-date
              :i_job_account job-acct})
     (execute f)
     (pull-map f :et-invoices-summary)
     )))


(defn get-raw-invoice-detail
  [env cust-id acct-id]
  (let [f (find-function env :z_o_invoices_query)]
    (ppn "before push")
    (push f {:i-customer (if cust-id (as-customer-num cust-id))
             :i_invoice (as-document-num acct-id)
             :if_invoices "X"
             :if_returns "X"
             :if_details "X"
             :if_addresses "X"
             :if_texts "X"
             })
    (ppn "before execute")
    (execute f)
    (ppn "before pull")
    (into {}
          (map (fn [fld] [fld (pull-map f fld)]) [:et-invoices-summary :et-invoices-detail :et-addresses :et-text]))
    ;; (map #(pull-map f %) [:et_invoices_detail])
    ;; (pull-map f :et-invoices-summary)
    ))


(examples
 (def summaries (get-raw-invoices :qas 1000736 "2013-12-01" "2013-12-10"))
 (ppn "" "" "-------" "summaries" summaries)

 (def detail (get-raw-invoice-detail :qas 1000736 9003836219))
 (ppn "" "" "------" "detail" detail)
 )

