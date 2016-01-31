(ns timmus.csr.core
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [reagent.core :as r :refer [atom]]
            [ajax.core :refer [GET POST]]
            ))

(defn atom-input [value attrs]
  [:input.form-control
   (merge
     attrs
     {:type "text"
      :value @value
      :on-change #(reset! value (-> % .-target .-value))})])

(def order-num (r/atom ""))
(def summit-email-address (r/atom ""))

(defn order-spec-component []
  [:form
   ;[:a {:href "https://www.google.com"} "google"]
   [:div.form-group.row
    [:label.col-sm-4.form-control-label
     {:for "summit-email-address" :style {:text-align "right" :font-weight "bold"}}
     " Your email address: "]
    [:div.col-sm-4
     (atom-input
       summit-email-address
       {:type "text" :id "summit-email-address" :class "form-control" :autofocus true :placeholder "Summit username"}
       )]
    [:div.col-sm-4
     " @summit.com "]]
   [:div.form-group.row
    [:label.col-sm-4.form-control-label
     {:for "order-num" :style {:text-align "right" :font-weight "bold"}}
     " Order #: "]
    [:div.col-sm-4
     (atom-input
       order-num
       {:type "number" :id "order-num" :class "form-control" :placeholder "Order #"}
       )]]
   [:div.form-group.row
    [:div.col-sm-4]
    [:div.col-sm-4
     [:button.btn.btn-primary
      {:type "submit" :disabled (or (= @summit-email-address "") (= @order-num ""))}
      "Request Order Specs"]]]
   ])


  ;(GET " http: //localhost:3000/store/service_centers.json")

(defn order-spec-page []
  [:div.container
   [:h2 "Order Spec Sheet Request"]
   ;[:fieldset.mds-border {:style "padding-top: 20px"}
    [order-spec-component]
    ;]
   ])

