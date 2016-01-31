(ns timmus.csr.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            ;[clojure.core :refer [future]]
            [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for timeout
            [siren.core :refer [siren! sticky-siren! base-style]]
            ))

(def order-num (r/atom ""))
(def summit-email-address (r/atom ""))
(def alert-message (r/atom nil))
(def alert-message-history (r/atom []))

(defn set-alert-message [status msg]
  (let [global {:width "300px"
                :color "white"}
        local (case status
                :success {:background "green"}
                :warn {:background "yellow"}
                :error {:background "red"}
                {})
        delay (case status
                :success 500
                :warn 3000
                :error 3000
                1000)
        ]
    (swap! alert-message-history conj [status msg])
    (println @alert-message-history)
    (siren! {:style (merge base-style global local) :content (str "<div>" msg "</div>") :delay delay})
    ))

(defn timeout [ms]
  (let [c (chan)]
    (js/setTimeout (fn [] (close! c)) ms)
    c))

(def base-api-url "/api/")

(defn handler [response]
  (set-alert-message :success (str "Success! " response))
  )

(defn error-handler [{:keys [status status-text]}]
  (set-alert-message :error (str "Failed error processing order-spec" status-text))
  ;(.log js/console (str "something bad happened: " status " " status-text))
  )

(defn request-order-spec [email order-num]
  (let [url (str base-api-url "order-spec/" email "/" order-num)]
    (GET url
         {:headers {"Accept" "application/json"}
          ;:params {:message "Hello World"
          ;         :user    "Bob"}
          :handler handler
          :error-handler error-handler}
         )))

(defn ajax-request-order-spec []
  (request-order-spec @summit-email-address @order-num)
  (reset! summit-email-address "")
  (reset! order-num "")
  )

(defn atom-input [value attrs]
  [:input.form-control
   (merge
     attrs
     {:type "text"
      :value @value
      :on-change #(reset! value (-> % .-target .-value))})])

(defn valid-order-spec-data? []
  (and
    (re-matches #"[a-zA-Z0-9.-]+" @summit-email-address)
    (re-matches #"\d+" @order-num)
    ))

(defn order-spec-component []
  [:div.well
   ;[:a {:href "https://www.google.com"} "google"]
   [:div.form-group.row
    [:label.col-sm-4.form-control-label
     {:for "summit-email-address" :style {:text-align "right" :font-weight "bold"}}
     "Send to: "]
    [:div.col-sm-4
     (atom-input
       summit-email-address
       {:type "text" :id "summit-email-address" :class "form-control" :autofocus true :placeholder "Summit username"}
       )]
    [:div.col-sm-4
     "@summit.com"]]
   [:div.form-group.row
    [:label.col-sm-4.form-control-label
     {:for "order-num" :style {:text-align "right" :font-weight "bold"}}
     "Order #: "]
    [:div.col-sm-4
     (atom-input
       order-num
       {:type "number" :id "order-num" :class "form-control" :placeholder "Order #"}
       )]]
   [:div.form-group.row
    [:div.col-sm-4]
    [:div.col-sm-4
     [:button.btn.btn-primary
      {:type "submit"
       ;:disabled (or (= @summit-email-address "") (= @order-num ""))
       :disabled (not (valid-order-spec-data?))
       :on-click #(ajax-request-order-spec)}
      "Request Order Specs"]]]
   (if @alert-message
     (do
       [:div.form-group.row
        [:div.alert.alert-success.alert-dismissible
         {:role "alert"}
         ;[:button.close {:type "button" :data-dismiss "alert" :aria-label "Close"}
         ; [:span.glyphicon.glyphicon-remove {:aria-hidden "true"} "x"]]
         [:strong "Success! "]
         (with-out-str (cljs.pprint/pprint @alert-message))
         ;@alert-message
         ]]
       )
     )
   ])


  ;(GET " http: //localhost:3000/store/service_centers.json")

(defn order-spec-page []
  [:div.container.page-header
   [:h2 "Order Spec Sheet Request"]
   ;[:fieldset.mds-border {:style "padding-top: 20px"}
    [order-spec-component]
    ;]
   ])

