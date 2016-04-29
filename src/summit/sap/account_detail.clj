(ns summit.sap.account-detail
  (:require [clojure.set :as set]
            [summit.utils.core :refer :all]
            [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]

            [korma.core :as k]))

(defn execute-account-detail [detail-fn account-number]
    (push detail-fn
          {:customerno (as-document-num account-number)
           :pi-salesorg "S001"})
    (execute detail-fn)
    )

(defn transform-account-detail [detail-fn]
  (let [detail (pull-map detail-fn :pe-companydata)
        result (select-keys detail [:name :city :street :city :district :region :country :tel1-numbr :postl-cod1 :e-mail :tel1-ext :fax-number :fax-extens])
        result (set/rename-keys result {:street :address :district :county :region :state :tel1-numbr :phone :tel1-ext :phone_ext :postl-cod1 :zip :e-mail :email :fax-number :fax :fax-extens :fax_ext})]
    (merge
     result
     {:account-number (as-integer (pull detail-fn :customerno))}
     (if (or (empty? (:postl-cod2 detail)) (empty? (:po-box detail)))
       {:billing_address (:address result)
        :billing_zip (:zip result)}
       {:billing_address (str "PO Box " (:po-box detail))
        :billing_zip (:postl-cod2 detail)}))))

(defn raw-account-detail [account-number]
  (let [detail-fn (find-function *sap-server* :bapi_customer_getdetail1)]
    (execute-account-detail detail-fn account-number)
    (pull-map detail-fn :pe-companydata)))

(defn account-detail [account-number]
  (let [detail-fn (find-function *sap-server* :bapi_customer_getdetail1)]
    (execute-account-detail detail-fn account-number)
    (transform-account-detail detail-fn)))

(defn bh-update-account [db account-num-or-map]
  (if-not (map? account-num-or-map)
    (bh-update-account db (account-detail account-num-or-map))
    (k/update :accounts
              (k/database db)
              (k/set-fields (dissoc account-num-or-map :account-number))
              (k/where {:account_number (:account-number account-num-or-map)}))
    ))
;; (bh-update-account (find-db :bh-dev) 1000736)

(defn bh-update-all-accounts [db]
  (let [account-nums (k/select :accounts (k/database db) (k/fields [:account_number]) (k/where {:city nil}))
        account-nums (map :account_number account-nums)]
    (dorun (map #(bh-update-account db %) account-nums))))
;; (time (bh-update-all-accounts (find-db :bh-local)))

(examples
 (time (bh-update-all-accounts (find-db :bh-local)))
 (def dkd (account-detail 1000736))
 (def dkd (account-detail 1027846))
 (ppn dkd)
 (ppn (raw-account-detail 1027846))
 )

(examples
 (account-detail 1000736)
 (account-detail 1027846)
 (ppn (account-detail 1027846))
 (ppn (account-detail 1027846))

 (def qqq (find-function *sap-server* :bapi_customer_getdetail1))
 (push qqq {:customerno (as-document-num 1000736)})
 (execute qqq)
 (find-field qqq :pe-companydata)
 (pull-map qqq :pe-companydata)
 (field-definition (find-field qqq :pe-companydata))
 (map first (last (field-definition (find-field qqq :pe-companydata))))
 (ppn (field-definition (find-field qqq :pe-companydata)))

 (pull-map qqq :pe-companydata)
 )
