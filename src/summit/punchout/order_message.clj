(println "loading summit.punchout.order-message")

(ns summit.punchout.order-message
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            ;;[clojure.xml :as x]
            [hiccup.core :as hiccup :refer [h] :rename {h escape}]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]

            [net.cgrand.enlive-html :as html]
            [cheshire.core :refer [generate-string parse-string]]
            [pl.danieljanus.tagsoup :as soup]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.zip :as zip]
            [config.core :refer [env]]
            [korma.core :refer [database limit where values insert order] :as k]

            [com.rpl.specter :as s]
            [clj-http.client :as client]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.punchout.core :as p]
            [summit.db.relationships :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.ts.core :refer :all]
            ))


(defn order-header [order]
  (let [punchout-request (:punchout-request order)
        _ (ppn "punch-req" punchout-request "params" (:params punchout-request))
        punchout (parse-string (:params punchout-request))
        _ (ppn "punchout" punchout)
        from ((punchout "broker") "id")
        to ((punchout "company") "id")
        ]
    [:Header
     [:From
      [:Credential {:domain "DUNS"}
       [:Identity from]]]
     [:To
      [:Credential {:domain "DUNS"}
       [:Identity to]]]
     [:Sender
      [:Credential {:domain "DUNS"}
       [:Identity from]]
      [:UserAgent "Summit cXML Application"]]
     ]))
;; (order-header (p/find-punchout-request 4))

(defn item->hiccup [item]
  (let [prod (find-entity :products (:product_id item))
        cents (if-let [c (:price_cents item)] c 4.23)
        cents (if (= 0 cents) 4.23 cents)]
    [:ItemIn {:quantity (:quantity item)}
     [:ItemID
      [:SupplierPartID (:product_id item)]
      [:SupplierPartAuxiliaryID (:matnr prod)]]
     [:ItemDetail
      [:UnitPrice [:Money {:currency "USD"} cents]]
      [:Description {:xml:lang "en"}
       [:ShortName (escape (:name prod))]
       (escape (:LongDescription prod))]
      [:UnitOfMeasure (:uom prod)]
      [:URL (str "https://www.summit.com/store/products/" (:product_id item))]
      (if false [:Classification {:domain "UNSPSC"} (:unspsc prod)])
      ]]))

(defn load-order-info [order-num]
  (let [order-request (find-entity :contact_emails order-num)
        punchout-request (if-let [punchout-id (:punchout_id order-request)]
                           (find-entity :punchouts (:punchout_id order-request)))
        cart (find-entity :carts (:cart_id order-request))
        cust (find-entity :customers (:customer_id cart))
        items (find-entity-by :line_items :cart_id (:cart_id order-request))
        ]
    {:order-num order-num
     :cart cart
     :items items
     :customer customer
     :punchout-request punchout-request}))
;; (load-order-info 4183)
;; (:punchout-request (load-order-info 4183))


(println "--------- f")

(defn items-price [items]
  23)

;; other params: buyer-cookie, operationAllowed
;; (defn create-order-message-from [{:keys [cart customer items punchout-request]}]
(defn create-order-message-from [{:keys [items punchout-request]}]
  ;; [buyer-cookie operation total-price items]
  [:Message {:inReplyTo (:payload_id punchout-request)}
   ;; create disallows inspect/edit. May want to allow these in the future. p. 90
   [:PunchOutOrderMessage {:operationAllowed "create"}
    [:BuyerCookie (:buyer_cookie punchout-request)]
    [:PunchOutOrderMessageHeader {:operationAllowed (:operation punchout-request)}
     [:Total
      [:Money {:currency "USD"} (items-price items)]]]
    ]
   (map item->hiccup items)
   ]
  )


;; create-order-message is defined below

;; (defn create-order-message [order-request punchout-request]
;;   (let [cart (find-entity :carts (:cart_id order-request))
;;         cust (find-entity :customers (:customer_id cart))
;;         items (find-entity-by :line_items :cart_id (:cart_id order-request))
;;         ]
;;     [:Message {:inReplyTo (:payload_id punchout-request)}
;;      [:PunchOutOrderMessage {:operationAllowed "create"}  ;; create disallows inspect/edit. May want to allow these in the future. p. 90
;;       [:BuyerCookie (:buyer_cookie punchout-request)]
;;       [:PunchOutOrderMessageHeader {:operationAllowed (:operation punchout-request)}
;;        [:Total
;;         [:Money {:currency "USD"} (:total_price order-request)]]]
;;       ]
;;      (map item->hiccup items)
;;      ]
;;     ))
;; (let [order-request (p/find-order-request 4185)]
;;   (create-order-message order-request (p/find-punchout-request (:punchout_id order-request))))


;; (defn cxml-order-message [order-request punchout-request]
;;   (ppn "in cxml-order-message")
;;   (let [header (order-header punchout-request)
;;         _ (ppn "header" header)
;;         msg (create-order-message order-request punchout-request)
;;         _ (ppn "msg" msg)
;;         ]
;;     (p/cxml header msg)))
;; (let [order-request (p/find-order-request 4185)]
;;   (create-cxml (cxml-order-message order-request (p/find-punchout-request (:punchout_id order-request)))))
;; (let [order-request (p/find-order-request 4185)]
;;   (p/cxml-order-message order-request (p/find-punchout-request (:punchout_id order-request))))

(defn send-order-message [order-request punchout-request order-message]
  (p/log-punchout nil :to :order-message order-message)
  (let [
        response 3 ;;(http-post (:browser_form_post punchout-request) order-message) 
        ]
    (p/log-punchout nil :from :order-message (:body response))
    ))

;; (defn process-order-request [order-num]
;;   (let [order-request (p/find-order-request order-num)
;;         punchout-request (p/find-punchout-request (:punchout_id order-request))
;;         order-message (create-order-message order-request punchout-request)
;;         ]
;;     (send-order-message order-request order-message)
;;     ))


;; (k/select service-center)
;; (k/select customer (database (find-db :bh-local)) (where {:email "abq@murphydye.com"}))
;; (dselect customer  (where {:email "abq@murphydye.com"}))
#_(defn last-city-order-num []
  (let [db (find-db :bh-local)
        customer-id (:id (ddetect customer (database db) (where {:email "abq@murphydye.com"})))
        cart-id (:id (ddetect cart (database db) (where {:customer_id customer-id}) (order :created_at :DESC) (limit 1)))]
    (println "cust-id" customer-id " cart-id " cart-id)
    ;; cart-id
    (:id (ddetect contact-email (database db) (where {:type "Order" :cart_id cart-id})))
    ))
;; (last-city-order-num)
#_(defn set-order-to-max-punchout []
  (let [db (find-db :bh-local)
        max-id (-> (exec-sql db "select max(id) id from punchouts") first :id)]
    (exec-sql db (str "update contact_emails set punchout_id=" max-id " where id=" (last-city-order-num)))))
;; (set-order-to-max-punchout)


;; (find-entity :carts 750336)
;; (find-entity :customers 6079)
;; (:active_punchout_id (find-entity :customers 6079))

(defn retrieve-order-data [order-id]
  (try
    (let [order (find-entity :contact_emails order-id)
          punchout-id (:punchout_id order)
          cart (find-entity :carts (:cart_id order))
          customer (find-entity :customers (:customer_id cart))
          items (find-entity-by :line_items :cart_id (:id cart))
          punchout-request (p/find-punchout-request punchout-id)
          ]
      (if-not punchout-id
        (throw (Exception. (str "Order " order-id " is not in a punchout session"))))
      (if-not punchout-request
        (throw (Exception. (str "No corresponding punchout request for order #" order-id))))
      {:order order
       :cart cart
       :customer customer
       :items items
       :punchout-request punchout-request}
      )
    ;; (catch Exception e (throw (Exception. (str "Cart #" cart-id " had invalid data."))))
    ))
;; (retrieve-order-data 4667)
;; (find-entity-by :line_items :cart_id 750337)
;; (ppn (retrieve-order-data 4667))

(defn order-message-hiccup [order-message-data]
  (println "\n\n\n      order-message-hiccup     \n\n\n")
  (let [head (order-header order-message-data)
        msg (create-order-message-from order-message-data)]
    [head msg]))

(defn order-message-xml [order-message-data]
  (let [data (order-message-hiccup order-message-data)]
    (p/create-cxml (p/cxml (first data) (second data)))))

(examples
 (order-message-xml (retrieve-order-data 4667))
 (order-message (retrieve-order-data 4667))
 (def xyz punchout-request)
 (def xyy url)
 (println "order message id: " id)
 (println "order request " order-request)
 (println "punchout-request" punchout-request)
 (println "hiccup " hiccup)
 (do-log-request
  (client/post
   url
   {:headers {:content-type :xml}
    :body (p/create-cxml hiccup)})
  "punchout")
 )


(defn order-message [order-message-data]
  (println "in order-message")
  (let [str (order-message-xml order-message-data)
        url (:browser_form_post (:punchout-request order-message-data))]
    {:method :post
     :url url
     :cxml str}))






;; (ppn (order-message (retrieve-4667-data 750337)))




;; (defn create-order-message [order-id]
;;   (let [
;;         order-request (p/find-order-request order-id)
;;         _ (ppn "order-request" order-request)
;;         punchout-request (p/find-punchout-request (:punchout_id order-request))
;;         _ (ppn "punchout-request" punchout-request)
;;         hiccup (cxml-order-message order-request punchout-request)
;;         _ (ppn "hiccup" hiccup)
;;         url (:browser_form_post punchout-request)]
;;     (p/create-cxml hiccup)))
;; (create-order-message 4667)
;; (order-message-xml (retrieve-order-data 4667))
;; (order-header (retrieve-order-data 4667))
;; (create-order-message-from (retrieve-order-data 4667))


(defn submit-order-message [order-id]
  (let [order-data (retrieve-order-data order-id)
        url (:browser_form_post (:punchout-request order-data))
        xml (order-message-xml order-data)]
        ;; id (last-city-order-num)
        ;; id order-id
        ;; order-request (p/find-order-request id)
        ;; punchout-request (p/find-punchout-request (:punchout_id order-request))
        ;; hiccup (cxml-order-message order-request punchout-request)
        ;; url (:browser_form_post punchout-request)]
    ;; (def xyz punchout-request)
    ;; (def xyy url)
    ;; (println "order message id: " id)
    ;; (println "order request " order-request)
    ;; (println "punchout-request" punchout-request)
    ;; (println "hiccup " hiccup)
    (do-log-request
     (client/post
      url
      {:headers {:content-type :xml}
       :body xml})
     "punchout")
    ))

;; (p/find-order-request 4193)
;; (timmus.routes.services/do-log-request {:b "fd"} "punchout")
;; (timmus.routes.services/do-log-request {:a "asdff"} "punchout")
;; (submit-order-message 444)
;; xyy 


;; (:browser_form_post xyz)

;;   {:status 200,
;;    :headers {"Content-Type" "text/xml; charset=utf-8"},
;;    :body (p/create-cxml hiccup)})
;; (client/get "https://www.google.com/")
;; (client/post "http://site.com/api"
;;              {:basic-auth ["user" "pass"]
;;               :body "{\"json\": \"input\"}"
;;               :headers {"X-Api-Version" "2"}
;;               :content-type :json
;;               :socket-timeout 1000  ;; in milliseconds
;;               :conn-timeout 1000    ;; in milliseconds
;;               :accept :json})

(println "done loading summit.punchout.order-message")
