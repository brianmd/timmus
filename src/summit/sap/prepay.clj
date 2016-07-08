(ns summit.sap.prepay
  (:require [clojure.set :as set]
            [summit.utils.core :refer :all]
            [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]

            [korma.core :as k]

            [clojure.string :as str]))

(defn raw-account-detail [account-number]
  (let [detail-fn (find-function *sap-server* :bapi_customer_getdetail1)]
    ;; (execute-account-detail detail-fn account-number)
    (pull-map detail-fn :pe-companydata)))

(examples
  (def prepay-fn (find-function :dev :z_o_zpay_prepay))
  (function-interface prepay-fn)
  (push prepay-fn
        {
         :i_sales_order "0000016807"
         :i_amount "206.86"
         :i_token "9411011150031111"
         :i_zip "87104"
         :i_cctype "VISA"
         :i_expiration "01/2018"
         })

  (com.sap.conn.jco.JCoContext/begin (:destination prepay-fn))
  (execute prepay-fn)
  (pp "e-successful" (pull prepay-fn :e_successful))

  (def commit-fn (find-function :dev :bapi-transaction-commit))
  (execute commit-fn)
  ;; (pp (function-interface commit-fn))
  (pp (pull commit-fn :return))

  (com.sap.conn.jco.JCoContext/end (:destination prepay-fn))
  )

(defmacro transaction-test [destination sexp commit-predicate]
  `(try
     (do
       (com.sap.conn.jco.JCoContext/begin ~destination)
       ~sexp
       (if (~commit-predicate)
         (let [commit-fn# (find-function ~destination :bapi-transaction-commit)]
           (execute commit-fn#)
   ;; (ppn (pull commit-fn# :return))
           (let [return-val# (doall (pull commit-fn# :return))]
             (if-not (= "000" (nth return-val# 2))
               (throw (Exception. (str "Commit failed (" return-val# ")"))))))
         ))
     (finally
       (com.sap.conn.jco.JCoContext/end ~destination))))
;; (macroexpand-1
;;  '(transaction-test (find-destination :dev) :sexp (fn [] false)))

(defn prepay [order-num amount card-type token expiration zip]
  (let [
        f (find-function *sap-server* :z_o_zpay_prepay)
        args {
              :i_sales_order (as-document-num order-num)
              :i_amount (str amount)
              :i_cctype (str/upper-case (->str card-type))
              :i_token token
              :i_expiration expiration
              :i_zip (str zip)
              }
        ]
    ;; (ppn args)
    (push f args)
    (transaction-test (:destination f)
                      (execute f)
                      (fn [] true)
                      )
    (pull f :e_successful)
    ))

(examples
 (with-sap-server :dev (prepay 16807 206.86 :visa "9411011150031111" "01/2018" 87104))
 )
