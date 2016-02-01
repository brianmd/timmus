(ns timmus.db.relationships
  (:require
    [korma.db :refer :all]
    [korma.core :refer :all]
    [clojure.string :as str]
    [mount.core :as mount]

    [timmus.db.core :refer :all]
    )
  )

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
         product product-class product-video
         role sales-request-status schema-migration
         service-center solr-category solr-service-center
         stock-status video zcta)

(defentity account
           (table :accounts)
           (has-many role {:fk :account_id})
           )
(defentity cart
           (table :carts)
           (has-many line-item)
           (belongs-to customer {:fk :customer_id})
           (belongs-to service-center {:fk :service_center_id})
           )
(defentity category
           (table :categories)
           (has-many product {:fk :category_id})
           )
(defentity customer
           (table :customers)
           (has-many cart {:fk :customer_id})
           (has-many contact-email {:fk :customer_id})
           (has-many role {:fk :customer_id})
           (many-to-many account role)
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
(defentity contact-email
           (table :contact_emails)
           (belongs-to customer {:fk :customer_id})
           )




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
(->
  (select* customer)
  (limit 1)
  ;(fields :id :email :roles.id)
  (with role
        (fields :id :account_id)
        )
  (select)

  first
  (map [:id :email :roles])

  ;#(map (map [:id :email :roles]) %)
  ;:roles
  )
)


