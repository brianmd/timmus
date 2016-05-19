(ns summit.bh.queries
  (:require ;[korma.db :refer :all]
            [korma.core :as k :refer [database limit where values insert order fields]]
            [clojure.string :as str]
            ;; [mount.core :as mount]

            [incanter.charts :as c]
            [incanter.core :as i]
            [incanter.distributions :as d]
            [incanter.stats :as stats]
            [incanter.svg :as svg]

            [com.rpl.specter :as s]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            ))

(def admins-sql "
select c.*
from roles r
join grants g on r.id=g.role_id
join permissions p on p.id=g.permission_id
join customers c on c.id=r.customer_id
where account_id is null and p.resource='all' and p.action='manage'
order by c.created_at
")

(defn admins
  ([] (admins :bh-prod))
  ([db]
   (exec-sql db admins-sql)))

(examples
 (ppn (->> (admins) (map :email)))
 )

(defn last-orders [n]
  (reverse
   (dselect contact-email (database (find-db :bh-prod)) (where {:type "Order"}) (order :id :DESC) (limit n))))
;; (k/select :blue_harvest_dev.customers (k/database (utils/find-db :bh-local)) (k/where {:id 28}))
;; (k/select :service_centers (k/database (find-db :bh-local)))
;; (-> (k/select* :service_centers)
;;     (k/as-sql))

(defn order-dollars []
  (map (comp #(/ % 100.0) :total_price)
       (dselect contact-email (database (find-db :bh-prod)) (where {:type "Order"}) (fields [:total_price]))))

(defn qqplot-order-dollars []
  (let [os (order-dollars)]
    (-> (c/qq-plot os)
        (i/view))))
;; (qqplot-order-dollars)

(defn boxplot-order-dollars []
  (let [os (order-dollars)]
    (-> (c/box-plot os
                   :series-label "Dollars per Order"
                   :legend true
                   :y-label "$")
        (i/view)
        )))
;; (boxplot-order-dollars)

(defn plot-order-dollars []
  (let [os (order-dollars)]
    (-> (c/xy-plot (range 1000) os
                   :series-label "Dollars per Order"
                   :legend true
                   :x-label "Order #"
                   :y-label "$")
        (c/add-points (range 1000) os)
        (i/view)
        )))

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
 (qqplot-order-dollars)
 (boxplot-order-dollars)
 (plot-order-dollars)
 )

