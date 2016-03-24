(println "loading summit.punchout.order-message")

(ns summit.punchout.order-message
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            ;;[clojure.xml :as x]
            [hiccup.core :as hiccup]
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
            [korma.core :as k]

            [com.rpl.specter :as s]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.punchout.core :refer :all]
            [summit.db.relationships :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.ts.core :refer :all]
            ))


(defn order-header [punchout-request]
  (let [punchout (parse-string (:params punchout-request))
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
;; (order-header (find-punchout-request 4))

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

;; other params: buyer-cookie, operationAllowed
(defn create-order-message [order-request punchout-request]
  ;; [buyer-cookie operation total-price items]
  (let [cart (find-entity :carts (:cart_id order-request))
        cust (find-entity :customers (:customer_id cart))
        items (find-entity-by :line_items :cart_id (:cart_id order-request))
        ]
    [:Message {:inReplyTo (:payload_id punchout-request)}
     [:PunchOutOrderMessage {:operationAllowed "create"}  ;; create disallows inspect/edit. May want to allow these in the future. p. 90
      [:BuyerCookie (:buyer_cookie punchout-request)]
      [:PunchOutOrderMessageHeader {:operationAllowed (:operation punchout-request)}
       [:Total
        [:Money {:currency "USD"} (:total_price order-request)]]]
      ]
     (map item->hiccup items)
     ]
    ))
;; (let [order-request (find-order-request 4185)]
;;   (create-order-message order-request (find-punchout-request (:punchout_id order-request))))

(defn cxml-order-message [order-request punchout-request]
  (let [header (order-header punchout-request)
        msg (create-order-message order-request punchout-request)]
    (cxml header msg)))
;; (let [order-request (find-order-request 4185)]
;;   (create-cxml (cxml-order-message order-request (find-punchout-request (:punchout_id order-request)))))
;; (let [order-request (find-order-request 4185)]
;;   (cxml-order-message order-request (find-punchout-request (:punchout_id order-request))))

(defn send-order-message [order-request punchout-request order-message]
  (log-punchout nil :to :order-message order-message)
  (let [
        response 3 ;;(http-post (:browser_form_post punchout-request) order-message) 
        ]
    (log-punchout nil :from :order-message (:body response))
    ))

(defn process-order-request [order-num]
  (let [order-request (find-order-request order-num)
        punchout-request (find-punchout-request (:punchout_id order-request))
        order-message (create-order-message order-request punchout-request)
        ]
    (send-order-message order-request order-message)
    ))


(println "done loading summit.punchout.order-message")
