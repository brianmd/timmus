(println "loading summit.db.relationships")

(ns summit.db.relationships
  (:require
    [korma.db :refer :all]
    [korma.core :refer :all]
    [clojure.string :as str]
    [mount.core :as mount]

    ; [timmus.db.core :refer :all]
    )
  )

(defn find-entity [name id]
  (first (select name (where {:id id}))))
(defn find-entity-by [name key id]
  (select name (where {key id})))


(declare account account-catalog account-verification-status
         account-verification-status
         attr attr-label attr-value
         broker broker-company
         cart
         catalog catalog-product
         category category-product category-hierarchy
         code-snippet
         company company-account company-product
         comparable-group competitor
         contact-email
         customer customer-note
         external-file grant line-item
         manufacturer permission
         product product-class product-video punchout
         role sales-request-status schema-migration
         service-center solr-category solr-service-center
         stock-status video zcta)


(def bh-entities
  {
   :account account
   :cart cart
   :customer customer
   })

(defentity tables
           (table :information_schema.tables))
;(select tables (limit 1))



(defentity account
           (table :accounts)
           (has-many role {:fk :account_id})
           (has-many contact-email {:fk :account_id})
           (many-to-many customer :roles {:lfk :account_id :rfk :customer_id})
           )

(defentity broker
  (table :brokers)
  (has-many broker-company {:fk :broker_id})
  (many-to-many company :broker_companies {:lfk :broker_id :rfk :company_id}))

(defentity broker-company
  (table :broker_companies)
  (belongs-to broker)
  (belongs-to company))

(defentity cart
           (table :carts)
           ;(name :carts)
           (has-many line-item)
           (has-many contact-email {:fk :cart_id})
           (belongs-to customer {:fk :customer_id})
           (belongs-to service-center {:fk :service_center_id})
           )

(defentity category
           (table :categories)
           (has-many product {:fk :category_id})
           )

(defentity company
  (table :companies)
  (has-many broker-company {:fk :company_id})
  (many-to-many broker :broker_companies {:lfk :company_id :rfk :broker_id}))

(defentity contact-email
  (table :contact_emails)
  (belongs-to account {:fk :account_id})
  (belongs-to cart {:fk :cart_id})
  (belongs-to customer {:fk :customer_id})
  )

(defentity customer
           (table :customers)
           ;(name :customers)
           (has-many cart {:fk :customer_id})
           (has-many contact-email {:fk :customer_id})
           (has-many role {:fk :customer_id})
           (has-many punchout {:fk :customer_id})
           (many-to-many account :roles {:lfk :customer_id :rfk :account_id})
           )


(defentity external-file
           (table :external_files)
           (belongs-to customer {:fk :product_id})
           )
(defentity grant
           (table :grants)
           (belongs-to permission {:fk :permission_id})
           (belongs-to role {:fk :role_id})
           )
(defentity line-item
           (table :line_items)
           (belongs-to cart {:fk :cart_id})
           (has-one product {:fk :line_item_id})
           )
(defentity permission
           (table :permissions)
           (has-many grant {:fk :permission_id})
           )
(defentity product
           (table :products)
           (belongs-to category {:fk :_id})
           (has-many external-file {:fk :product_id})
           (belongs-to line-item {:fk :line_item_id})
           )
(defentity punchout
  (table :punchouts)
  (belongs-to customer {:fk :customer_id}))
(defentity role
           (table :roles)
           (belongs-to account {:fk :account_id})
           (belongs-to customer {:fk :customer_id})
           (has-many grant {:fk :role_id})
           )
(defentity service-center
           (table :service_centers)
           (has-many cart {:fk :service_center_id})
           )



;(def perms (select permission))
;perms

(defn find-permission
  ([role]
   (select permission (where {:resource (name role)})))
  ([role sub-role]
   (first
     (select permission (where {:resource (name role) :action (name sub-role)}))))
  )

(defn global-roles [customer]
  "returns nil-account roles"
  )
(defn account-roles [customer account])


;(mount/start)


;(select customer (limit 1))

#_(comment

(-> (select* customer)
    (limit 1)
    (with role
          (where {:account_id [= nil]})
          (with grant (with permission))
          )
    (select)
    ;(as-sql)
    )

;(-> (select* role)
;    (where {:account_id 3})
;    (as-sql)
;    )

(mount/start)

(name :asdf)
(find-permission "account")
(find-permission :account :admin)
(find-permission :account)

(select customer (limit 1) (with role (with grant)))
(defn many-to-many-fn [ent sub-ent-var join-table opts]
  (let [opts (assoc opts
               :join-table join-table
               :lfk (delay (get opts :lfk (default-fk-name ent)))
               :rfk (delay (get opts :rfk (default-fk-name sub-ent-var))))]
    (rel ent sub-ent-var :many-to-many opts)))
customer
cart
account
(select account (limit 1) (with customer))
(select customer (limit 1) (with account))
(select account)
(def c (assoc customer :name "customers"))
c
(->
  customer
  :rel
  (#(% "account"))
  )
(many-to-many-fn customer account :roles {:lfk :customer_id :rfk :account_id})
;(select customer (limit 1))
(select customer (limit 1) (with cart))
(select customer (limit 1) (with account))
(->
  (select* customer)
  (limit 1)
  (with account)
  (as-sql))

)


;(select customer (limit 1))

#_(comment

;(mount/start)
;(select customer (limit 1))
(select
  customer
  (fields [:id :email :account_id])
  (where {:email "brian@murphydye.com"})
  (with role))
(select
  customer
  (where {:email "brian@murphydye.com"})
  (with role (with grant)))

(select
  customer
  (where {:email "brian@murphydye.com"})
  (with role))
(select
  role
  (with account)
  (limit 1))


(->
  (select* customer)
  )
()

(mount/start)
(select customer (limit 1))
(->
  (select* customer)
  (limit 1)
  ;(fields :id :email)
  (with role
        (fields :id :account_id)
        )
  (select)

  ;first
  ;(map [:id :email :roles])

  ;#(map (map [:id :email :roles]) %)
  ;:roles
  )

)   ;; end comment





(println "mount/start")
(mount/start)
(println "mount/start completed")

(println "done loading summit.db.relationships")
