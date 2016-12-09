(println "loading summit.punchout.order-message")

(ns summit.punchout.order-message
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [clojure.java.io :as io :refer [as-url make-parents]]
            [config.core :refer [env]]
            [hiccup.core :as hiccup :refer [h] :rename {h escape}]
            [korma.core :as k :refer [database insert limit order values where]]
            [taoensso.carmine :as car :refer [wcar]]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            [summit.punchout.core :as p]
            [summit.step.xml-output :refer :all]
            ))

(defn order-header [order]
  (let [punchout-request (:punchout-request order)
        punchout (parse-string (:params punchout-request))
        from ((punchout "from") "id") ;; broker
        to ((punchout "to") "id")  ;; company
        sender ((punchout "sender") "id")  ;; company
        ;; secret ((punchout "sender") "secret")
        ]
    [:Header
     [:From
      [:Credential {:domain "DUNS"}
       [:Identity to]]]
     [:To
      [:Credential {:domain "DUNS"}
       [:Identity from]]]
     [:Sender
      [:Credential {:domain "DUNS"}
       [:Identity sender]
       ;; [:SharedSecret secret]
       ]
      [:UserAgent "Summit cXML Application"]]
     ]))
(examples
 (order-header (p/find-punchout-request 4))
 (order-header (retrieve-order-data 4668))
 (:punchout-request (retrieve-order-data 4668))
 (:params (:punchout-request (retrieve-order-data 4668)))
 (parse-string (:params (:punchout-request (retrieve-order-data 4668))))
 ((parse-string (:params (:punchout-request (retrieve-order-data 4668)))) "company"))

(defn item->hiccup [cart-item]
  (let [prod (find-entity :products (:product_id cart-item))
        mfr (find-entity :manufacturers (:manufacturer_id prod))
        cents (if-let [c (:price_cents cart-item)] c 4.23)
        cents (if (= 0 cents) 4.23 cents)]
    (ppn "item" cart-item)
    (ppn "prod" prod)
    [:ItemIn {:quantity (:quantity cart-item)}
     [:ItemID
      [:SupplierPartID (->int (:matnr prod))]
      [:SupplierPartAuxiliaryID (:product_id cart-item)]
      ]
     [:ItemDetail
      [:UnitPrice [:Money {:currency "USD"} cents]]
      [:Description {:xml:lang "en"}
       (escape (:name prod))
       ;; [:ShortName (escape (:name prod))]
       ;; (escape (:LongDescription prod))
       ]
      [:ManufacturerName (:name mfr)]
      [:ManufacturerPartID (:manufacturer_part_number prod)]
      [:UnitOfMeasure (:uom prod)]
      [:URL (str "https://www.summit.com/store/products/" (:product_id cart-item))]
      [:Classification {:domain "UNSPSC"} "39000000"]
      ;; (if false [:Classification {:domain "UNSPSC"} (:unspsc prod)])
      ]]))

(defn load-order-info [order-num]
  (let [order-request (find-entity :contact_emails order-num)
        punchout-request (if-let [punchout-id (:punchout_id order-request)]
                           (find-entity :punchouts (:punchout_id order-request)))
        cart (find-entity :carts (:cart_id order-request))
        cust (find-entity :customers (:customer_id cart))
        cart-items (find-entity-by :line_items :cart_id (:cart_id order-request))
        ]
    {:order-num order-num
     :cart cart
     :items cart-items
     :customer customer
     :punchout-request punchout-request}))
(examples
 (load-order-info 4183)
 (:punchout-request (load-order-info 4183)))


(println "--------- f")

(defn items-price [items]
  4.23)

(defn create-order-message-from [{:keys [items punchout-request]}]
  [:Message {:inReplyTo (:payload_id punchout-request)}
   ;; create disallows inspect/edit. May want to allow these in the future. p. 90
   [:PunchOutOrderMessage
    [:BuyerCookie (:buyer_cookie punchout-request)]
    [:PunchOutOrderMessageHeader {:operationAllowed (:operation punchout-request)}
     [:Total
      [:Money {:currency "USD"} (items-price items)]]]
    (map item->hiccup items)
    ]
   ]
  )


(defn send-order-message [order-request punchout-request order-message]
  (p/log-punchout nil :to :order-message order-message)
  (let [
        response 3 ;;(http-post (:browser_form_post punchout-request) order-message) 
        ]
    (p/log-punchout nil :from :order-message (:body response))
    ))

(defn retrieve-order-data [order-id]
  (try
    (ppn "order-id" order-id)
    (let [order (find-entity :contact_emails order-id)
          _ (ppn "order" order)
          punchout-id (:punchout_id order)
          cart (find-entity :carts (:cart_id order))
          _ (ppn "cart" cart)
          customer (find-entity :customers (:customer_id cart))
          items (find-entity-by :line_items :cart_id (:id cart))
          _ (ppn "items" items)
          punchout-request (p/find-punchout-request punchout-id)
          _ (ppn "preq" punchout-request)
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
    (ppn "hiccup" msg)
    [head msg]))

(defn order-message-xml [order-message-data]
  (let [data (order-message-hiccup order-message-data)
        xml (p/create-cxml (p/cxml (first data) (second data)))]
    (println xml)
    xml))

(examples
 (order-message-xml (retrieve-order-data 4667))
 (order-message (retrieve-order-data 4667))
 (def xyz punchout-request)
 (def xyy url)
 (pp 3 4 5)
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

(defn order-message-form [order-id]
  (ppn "in order-message-form" order-id)
  (let [order-data (retrieve-order-data order-id)
        _ (ppn "order-data:" order-data)
        url (:browser_form_post (:punchout-request order-data))
        xml (order-message-xml order-data)]
    (do-log-request xml "punchout-send-order-message")
    {:url url
     :method "post"
     :base64 (base64-encode xml)
     ;; :xmlstr (hiccup/h xml)
     :xmlstr xml
     }))
;; (order-message-form 4818)

;; (hiccup/h "a<b>c\"ef\"")

"  not used for real work  "
(defn submit-order-message [order-id]
  (ppn "deprecated, don't use this!!! in submit-order-message" order-id)
  (let [order-data (retrieve-order-data order-id)
        _ (ppn "order-data:" order-data)
        url (:browser_form_post (:punchout-request order-data))
        xml (order-message-xml order-data)]
    (ppn "url" url)
    (ppn "xml:" xml)
    (ppn "xml-str:" (hiccup/h xml))
    ;; (ppn "base64" (base64-encode xml))
    (do-log-request xml "punchout-send-order-message")
    (do-log-request
     (client/post
      url
      {:form-params {:cXML-urlencoded (base64-encode xml)}}
      )
     "punchout-send-order-message-response")
    ))

;; (order-message-xml (retrieve-order-data 4668))
;; (submit-order-message 4668)

(println "done loading summit.punchout.order-message")
