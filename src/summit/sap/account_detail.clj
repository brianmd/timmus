(println "loading summit.sap.account_detail")

(ns summit.sap.account-detail
  (:require [clojure.set :as set]
            [korma.core :as k]

            [summit.utils.core :as utils]
            [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]

            [timmus.db.core :refer [*db*]]
            ))

(defn push-inputs
  ;; ([detail-fn] (partial push-inputs detail-fn))
  ([detail-fn account-number]
   (push detail-fn
         {:customerno (utils/as-document-num account-number)
          :pi-salesorg "S001"})
   detail-fn)
  )

(defn pull-outputs [detail-fn]
  (let [detail (pull-map detail-fn :pe-companydata)
        result (select-keys detail [:name :city :street :city :district :region :country :tel1-numbr :postl-cod1 :e-mail :tel1-ext :fax-number :fax-extens])
        result (set/rename-keys result {:street :address :district :county :region :state :tel1-numbr :phone :tel1-ext :phone_ext :postl-cod1 :zip :e-mail :email :fax-number :fax :fax-extens :fax_ext})]
    (merge result {:raw detail}))
  )



;; (defn raw-account-detail [server account-number]
;;   (let [detail-fn (find-function server :bapi_customer_getdetail1)]
;;     (push-inputs detail-fn account-number)
;;     (execute detail-fn)
;;     (pull-outputs detail-fn)
;;     ))

;; (defn transform-account-detail [detail-fn]
;;   (let [detail (pull-map detail-fn :pe-companydata)
;;         result (select-keys detail [:name :city :street :city :district :region :country :tel1-numbr :postl-cod1 :e-mail :tel1-ext :fax-number :fax-extens])
;;         result (set/rename-keys result {:street :address :district :county :region :state :tel1-numbr :phone :tel1-ext :phone_ext :postl-cod1 :zip :e-mail :email :fax-number :fax :fax-extens :fax_ext})]
;;     (merge
;;      result
;;      {:account-number (as-integer (pull detail-fn :customerno))}
;;      (if (or (empty? (:postl-cod2 detail)) (empty? (:po-box detail)))
;;        {:billing_address (:address result)
;;         :billing_zip (:zip result)}
;;        {:billing_address (str "PO Box " (:po-box detail))
;;         :billing_zip (:postl-cod2 detail)}))
;;     detail))

(defn transform [m account-number]
  (let [detail (:raw m)]
    (merge
     m
     {:account-number (utils/->int account-number)}
     (if (or (empty? (:postl-cod2 detail)) (empty? (:po-box detail)))
       {:billing_address (:address m)
        :billing_zip (:zip m)}
       {:billing_address (str "PO Box " (:po-box detail))
        :billing_zip (:postl-cod2 detail)}))
    ))
    ;; detail))

;; (find-function :qas :bapi_customer_getdetail1)
(defn account-detail
  ([account-number] (account-detail *sap-server* account-number))
  ([server account-number]
   (let [f (find-function server :bapi_customer_getdetail1)]
     (->
      f
      (push-inputs account-number)
      execute
      pull-outputs
      (transform account-number)
      (dissoc :raw)
      )
     )))
;; (account-detail :qas 1000736)
;; (def ff (account-detail :prd 1000736))
;; (pull-map ff :pe-companydata)
;; (pull ff :pe-companydata)
;; (def ff (find-function :qas :bapi_customer_getdetail1))
;; (push-inputs ff {:customerno 1000736})
;; (execute ff)
;; (pull-map ff :pe-companydata)
;; (account-detail :qas 1000736)

   ;; (transform-account-detail
   ;;  (raw-account-detail server account-number))))
   ;; (let [detail-fn (find-function server :bapi_customer_getdetail1)]
   ;;   (execute-account-detail detail-fn account-number)
   ;;   (transform-account-detail detail-fn))))

(def ^:private num-updated (atom 0))
(defn bh-update-account
  ([account-num-or-map] (bh-update-account account-num-or-map *db*))
  ([account-num-or-map db] (bh-update-account account-num-or-map db *sap-server*))
  ([account-num-or-map db server]
   (if-not (map? account-num-or-map)
     (let [acct (if (map? account-num-or-map) account-num-or-map (account-detail server account-num-or-map))
           flds (utils/conj-db-updated (dissoc acct :id :account-number))
           ]
       (swap! num-updated inc)
       (k/update :accounts
                 (k/database db)
                 (k/set-fields flds)
                 (k/where {:account_number (:account-number acct)}))
       ))))
;; (bh-update-account 1000736)
;; (account-detail :qas 1000736)
;; (bh-update-account 1000736 (find-db :bh-dev))
;; (time (bh-update-account 1000736 (find-db :bh-prod) :prd))


(defn bh-update-all-accounts
  ([] (bh-update-all-accounts *db*))
  ([db] (bh-update-all-accounts db *sap-server*))
  ([db server]
   ;; (let [account-nums (k/select :accounts (k/database db) (k/fields [:account_number]) (k/where {:city nil}))
   (reset! num-updated 0)
   (let [account-nums (k/select :accounts (k/database db) (k/fields [:account_number]))
         account-nums (map :account_number account-nums)]
     (dorun (map #(bh-update-account % db server) account-nums)))))

(utils/examples
 (def updating (future (bh-update-all-accounts (utils/find-db :bh-prod) :prd)))
 (println updating)
 num-updated

 (time (bh-update-all-accounts (find-db :bh-local) :qas))
 (def dkd (account-detail :qas 1000736))
 (def dkd (account-detail 1027846))
 (ppn dkd)
 ;; (ppn (raw-account-detail :qas 1027846))
 ;; (ppn (raw-account-detail :prd 1000736))
 )

(utils/examples
 (account-detail :qas 1000736)
 (account-detail 1027846)
 (ppn (account-detail :qas 1027846))
 (ppn (account-detail 1027846))

 (def qqq (find-function *sap-server* :bapi_customer_getdetail1))
 (push qqq {:customerno (utils/as-document-num 1000736)})
 (execute qqq)
 (find-field qqq :pe-companydata)
 (pull-map qqq :pe-companydata)
 (field-definition (find-field qqq :pe-companydata))
 (map first (last (field-definition (find-field qqq :pe-companydata))))
 (ppn (field-definition (find-field qqq :pe-companydata)))

 (pull-map qqq :pe-companydata)
 )

(println "done loading summit.sap.account_detail")
