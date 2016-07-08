(println "loading summit.punchout.order-message")

(ns summit.punchout.order-message
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
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

            [dk.ative.docjure.spreadsheet :as xls]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.punchout.core :as p]
            [summit.db.relationships :refer :all]
            ))


(defn order-header [order]
  (let [punchout-request (:punchout-request order)
        punchout (parse-string (:params punchout-request))
        from ((punchout "sender") "id") ;; broker
        to ((punchout "from") "id")  ;; company
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
(examples
 (order-header (p/find-punchout-request 4))
 (order-header (retrieve-order-data 4668))
 (:punchout-request (retrieve-order-data 4668))
 (:params (:punchout-request (retrieve-order-data 4668)))
 (parse-string (:params (:punchout-request (retrieve-order-data 4668))))
 ((parse-string (:params (:punchout-request (retrieve-order-data 4668)))) "company"))

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
(examples
 (load-order-info 4183)
 (:punchout-request (load-order-info 4183)))


(println "--------- f")

(defn items-price [items]
  23)

(defn create-order-message-from [{:keys [items punchout-request]}]
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


(defn send-order-message [order-request punchout-request order-message]
  (p/log-punchout nil :to :order-message order-message)
  (let [
        response 3 ;;(http-post (:browser_form_post punchout-request) order-message) 
        ]
    (p/log-punchout nil :from :order-message (:body response))
    ))

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

(defn order-message-form [order-id]
  (ppn "in order-message-form" order-id)
  (let [order-data (retrieve-order-data order-id)
        _ (ppn "order-data:" order-data)
        url (:browser_form_post (:punchout-request order-data))
        xml (order-message-xml order-data)]
    (do-log-request xml "punchout-send-order-message")
    {:url url
     :method "post"
     :base64 (base64-encode xml)}
    ))
;; (order-message-form 4818)


(defn submit-order-message [order-id]
  (ppn "in submit-order-message" order-id)
  (let [order-data (retrieve-order-data order-id)
        _ (ppn "order-data:" order-data)
        url (:browser_form_post (:punchout-request order-data))
        xml (order-message-xml order-data)]
    (ppn "url" url)
    (ppn "xml:" xml)
    (ppn "base64" (base64-encode xml))
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
