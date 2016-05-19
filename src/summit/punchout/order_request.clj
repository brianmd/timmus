(println "loading summit.punchout.order-request")

(ns summit.punchout.order-request
  (:require [clojure.string :as str]

            [net.cgrand.enlive-html :as html]
            [korma.core :as k :refer [database limit where values insert order fields]]

            ;; [incanter.charts :as c]
            ;; [incanter.core :as i]
            ;; [incanter.distributions :as d]
            ;; [incanter.stats :as stats]
            ;; [incanter.svg :as svg]

            [resque-clojure.core :as resque]
            [clj-time.core :as t]
            [clj-time.local :as l]

            [com.rpl.specter :as s]

            [summit.utils.core :refer :all]
            [summit.punchout.core :refer :all]
            [summit.db.relationships :refer :all]
            ))


(def ^:dynamic *db-name* :bh-local)

(defn create-cart [cart-map item-maps]
  (let [db (find-db *db-name*)]
    (when-let [cart-id (:generated_key
                        (insert cart (database db)
                                (values (assoc cart-map
                                               :created_at (db-timenow)
                                               :updated_at (db-timenow)
                                               ))))]
      (doseq [item item-maps]
        (insert line-item (database db)
                (values (assoc item
                               :cart_id cart-id
                               :created_at (db-timenow)
                               :updated_at (db-timenow)
                               ))))
      cart-id))
    )

;; (def cart-map {:service_center_id 7 :customer_id 28})
;; (def items-vals [{:product_id 128035 :quantity 7}
;;                  {:product_id 14528 :quantity 22}
;;                  ])
;; (create-cart cart-map items-vals)

(defn create-order [cust-id ])



(defn parse-line-item [p]
  {:supplier-part-num (extract-content p [:SupplierPartID])
   :matnr (extract-content p [:SupplierPartAuxiliaryID])
   :unit-price (extract-content p [:UnitPrice :Money])
   :quantity (-> p :attrs :quantity)
   :line-num (-> p :attrs :lineNumber)
   })
;; (map parse-line-item (hselect preq [:ItemOut]))

(defn parse [p]
  (let [header-attrs (:attrs (hdetect p [:OrderRequestHeader]))]
    {
     :po-num (:orderid header-attrs)
     :shipping :is-it-Shipping/Description-or-ShipTo/CarrierIdentifier?
     :order-date 3
     :type 3
     :requisitionId 3
     :wait-til-complete? 3
     :total-cost 3

     ;; :ship-to (->address p :ShipTo)
     ;; :bill-to (->address p :BillTo)
     ;; :terms-of-delivery (->address p :TermsOfDelivery)
     ;; :delivery-comments (hselect p [:TermsOfDelivery :Comments])
     :delivery-comments (hselect p [:TermsOfDelivery [:Comments (html/attr= :xml:lang "en-US")]])
     ;; :shipping-instructions (select-content-text p [:ShippingInstructions])
     ;; :contacts (hselect p [:Contact])


     :how-about-TransportInformation? 3

     :start-ship 3
     :end-ship 3

     :items {:item-1 3}
     }))

;; (def req (slurp "test/mocks/order-request.xml"))
;; (def preq (xml->map req))

;; (parse preq)

;; (hselect p [:TermsOfDelivery])
;; (parse p)
;; (->  (parse p) :shipping-instructions)


;; )

(resque/configure {:host "localhost" :port 6379})

(defn resque-order [order-num]
  (resque/enqueue "bourne.insummit.com.mail" "WorkQueue::FinalizeOrderTask" order-num)
  )

;; 1. build cart and line-items
;; 2. submit task

;; (resque/enqueue "bourne.insummit.com.mail" "WorkQueue::FinalizeOrderTask" 4183)
;; not this one ...(resque/enqueue "bourne.insummit.com.mail" "WorkQueue::SubmitSapOrderTask" 4183)


(examples
 (pp (mapv order-vitals (last-orders 5)))
 (plot-order-dollars)
 )

(println "done loading summit.punchout.order-request")

