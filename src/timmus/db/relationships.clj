(ns timmus.db.relationships
  (:require
    [korma.db :refer :all]
    [korma.core :refer :all]
    [clojure.string :as str]

    [timmus.db.core :refer :all]
    )
  )

(declare account cart category contact-email customer external-file line-item product role service-center)

(defentity account
           (table :accounts)
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
           )
(defentity external-file
           (table :external_files)
           (belongs-to customer {:fk :product_id}))
(defentity line-item
           (table :line_items)
           (belongs-to cart {:fk :cart_id})
           (has-one product {:fk :line_item_id})
           )
(defentity product
           (table :products)
           (belongs-to category {:fk :_id})
           (has-many external-file {:fk :product_id})
           (belongs-to line-item {:fk :line_item_id})
           )
(defentity role
           (table :roles)
           )
(defentity service-center
           (table :service_centers)
           (has-many cart {:fk :service_center_id})
           )
(defentity contact-email
           (table :contact_emails)
           (belongs-to customer {:fk :customer_id})
           )

;(select customer (limit 1))


