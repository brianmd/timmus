(ns summit.sap.price
  (:require [summit.utils.core :refer :all]
            [summit.sap.types :refer :all]
            [summit.sap.core :refer :all])
  )

(defn convert-price [p]
  (let [p (vec p)]
    (assoc p 0 (as-integer (first p)))))

(defn prices [account-number service-center-code matnr-qty-vec]
  ;; (let [price-fn (find-function :qas :z-o-complete-pricing)
  (let [price-fn (find-function *sap-server* :z-o-complete-pricing)
        v (for [[matnr qty] matnr-qty-vec]
            [(as-matnr matnr) qty])]
    (push price-fn
          {:i-kunnr (as-document-num account-number)
           :i-werks service-center-code
           :it-price-input v})
    (execute price-fn)
    (map! convert-price (pull price-fn :et-price-output))
    ))

(defn internet-prices [matnr-qty-vec]
  (prices internet-account-number internet-service-center-code matnr-qty-vec))

(defn internet-unit-prices [matnrs]
  (internet-prices (map #(vector % 1) matnrs)))

(examples
 (prices internet-account-number internet-service-center-code [[2718442 3]])
 (internet-prices
  [[2718442 3]
   [2718442 9]])
 (internet-unit-prices [2718442 20834])
 )

