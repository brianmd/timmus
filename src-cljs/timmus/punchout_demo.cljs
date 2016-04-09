(ns timmus.punchout-demo
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]

                                        ;[timmus.sales-associate.core]
            [timmus.csr.core :refer [csr-page platt-prices]]
            [timmus.math :refer [math-page]]
                                        ;[timmus.sales-associate.core :refer [csr-page]]

            [murphydye.window :as win]
            ))

(defn checkmark []
  [:span {:style {:display "inline-block" :color "green" :margin "0px 10px"}} "✔"])

(defn show-completion [completed key]
  (if (contains? @completed key)
    [checkmark]
    [:span]))

(defn click-selector [sel]
  (println sel)
  (.click (.getElementById js/document sel)))

(defn submit-selector [sel]
  (println sel)
  (.submit (.getElementById js/document sel)))

(defn logout [completed results]
  (let [signout-url "http://localhost:3000/store/customers/signout"]
    (swap! completed conj :logged-out)))

(defn punchout [completed results]
  (let [comp (js/$ "#punchin")
        url (str "http://localhost:3000/punchout_login/asdf")]
    (.open js/window url)
    (swap! completed conj :punched-out)
    )
  )

(defn order-message [completed results order-num]
  (swap! completed conj :order-message)
  (.open js/window (str "http://localhost:3449/api/punchout/order-message/" order-num)))

(defn purchase-order [completed results]
  (swap! completed conj :purchase-order)
  )


(defn issues []
  [:div.container
   ])

(defn punchout-demo []
  (let [completed (r/atom #{})
        results (r/atom {})]
    [:div.container {:style {:margin-left 10}}
     [:h3 "Punchout Demo"]
     [:table
      [:tbody
       [:tr
        [:td "1. " [:a {:on-click #(logout completed results)} "Log out "] [:span {:style {:color "red"}} "(manual)"]]
        [:td [show-completion completed :logged-out]]
        [:td ]]
       [:tr
        [:td "2. " [:a {:on-click #(punchout completed results)} "Punch out"] " (log in via portal)"]
        [:td [show-completion completed :punched-out]]
        [:td ""]
        ]
       [:tr
        ;; [:td "3. " [:a {:on-click #(order-message completed results 4192)} "Order Message"]]
        [:td "3. " [:a {:on-click #(order-message completed results 747009)} "Order Message"]]
        [:td [show-completion completed :order-message]]
        [:td]]
       [:tr
        [:td "4. " [:a {:on-click #(purchase-order completed results)} "Purchase Order"]
         [:ul
          [:li "show resque worker " [:span {:style {:color "red"}} "(manual)"]]
          [:li "receive xml"]
          [:li "create cart, line items, and order"]
          [:li "tell resque worker to process"]
          ]]
        [:td [show-completion completed :purchase-order]]
        [:td]]
       [:tr
        [:td [:i "5. Invoice (not yet implemented)"]]
        [:td]
        [:td]]
       ]]
     ]))
