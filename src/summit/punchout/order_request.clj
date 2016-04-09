(println "loading summit.punchout.order-request")

(ns summit.punchout.order-request
  (:require [clojure.string :as str]

            [net.cgrand.enlive-html :as html]
            [korma.core :refer [database limit where values insert order]]

            [resque-clojure.core :as resque]
            [clj-time.core :as t]
            [clj-time.local :as l]

            [com.rpl.specter :as s]

            [summit.utils.core :refer :all]
            [summit.punchout.core :refer :all]
            [summit.db.relationships :refer :all]
            ))

#_(comment


(def r (slurp "test/mocks/order-request.xml"))
r 

(def p (xml->map r))
p 

(hselect p [:BillTo])
(hselect p [:BillTo :Name])
(hselect p [:BillTo :PostalAddress])
(hselect-content p [:BillTo :Address])
(hselect-content p [:BillTo :PostalAddress])
(hselect-pruned-content p [:BillTo :PostalAddress])
(hselect (-> (select-pruned-content p [:BillTo :PostalAddress]) vec) html/text-node)
(hselect (-> (select-pruned-content p [:BillTo :PostalAddress]) first vec) html/text-node)
(->  (hselect-pruned-content p [:BillTo :PostalAddress]))
(->  (hselect-pruned-content p [:BillTo :PostalAddress]) first )
(str/join (hselect (->  (hselect-pruned-content p [:BillTo :PostalAddress]) first ) [html/text-node]))

(hdetect p [:OrderRequestHeader])
(:attrs (hdetect p [:OrderRequestHeader]))


(mapify (select-pruned-content p [:BillTo :PostalAddress]))
(->address p :BillTo)

)



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

(def cart-map {:service_center_id 7 :customer_id 28})
(def items-vals [{:product_id 128035 :quantity 7}
                 {:product_id 14528 :quantity 22}
                 ])
;; (create-cart cart-map items-vals)

(defn create-order [cust-id ])

{:id 3
 :cart_id 3
 :created_at 3
 :updated_at 3
 :email 3
 :message 3
 :contact_preference "email"
 :include_order_items 1
 :include_request_info 0
 }
;; (write-sql :bh-local cart (values cart-vals))
;; (insert cart (database (find-db *db-name*)) (values cart-vals))

;; (pp (exec-sql :bh-local "select * from contact_emails where type='Order' limit 1"))
;; (dselect customer (database (find-db *db-name*)) (limit 1))





(defn last-orders [n]
  (reverse
   (dselect contact-email (database (find-db :bh-prod)) (where {:type "Order"}) (order :id :DESC) (limit n))))

(defn last-order []
  (first
   (dselect contact-email (database (find-db :bh-prod)) (where {:type "Order"}) (order :id :DESC) (limit 1))))

(defn last-order-sans-json []
  (dissoc (last-order) :sap_json_result))

;; (defn order-vitals [o]
;;   (let [o (mapv #(get o %) [:id :total_price :email :name :created_at :sap_document_number])]
;;     (assoc o 1 (/ (nth o 1) 100.0))))

(defn orders-by [email]
  (dselect contact-email (database (find-db :bh-prod)) (where {:type "Order" :email email}) (order :id :DESC) (limit 1)))
;; (order-vitals (last-order))
;; (count (orders-by (:email (last-order))))
;; (:created_at (ddetect customer (where {:email (:email (last-order))})))

(defn order-vitals [o]
  (let [o (into {} (s/select [s/ALL #(contains? #{:id :total_price :email :name :created_at :sap_document_number} (first %))] o))]
    (assoc o
           :total_price (/ (:total_price o) 100.0)
           :created (localtime (:created_at o)))))
;; (def ooo (last-order))
;; (order-vitals ooo)
;; (pp (mapv order-vitals (last-orders 5)))
;; (pp (order-vitals (last-order)))
;; (pp (last-order-sans-json))

(examples
 (pp (mapv order-vitals (last-orders 5)))
 )



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



(println "done loading summit.punchout.order-request")

