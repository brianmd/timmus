(ns timmus.csr.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            ;[clojure.core :refer [future]]
            [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for sleep timeout
            [siren.core :refer [siren! sticky-siren! base-style]]

            [re-com.core :refer [title p input-text input-textarea button]]
            [re-com.util     :refer [deref-or-value px]]
            [re-com.popover  :refer [popover-tooltip]]
            [re-com.box      :refer [h-box v-box box gap line flex-child-style align-style]]
            [re-com.validate :refer [input-status-type? input-status-types-list regex?
                                     string-or-hiccup? css-style? html-attr? number-or-string?
                                     string-or-atom? throbber-size? throbber-sizes-list] :refer-macros [validate-args-macro]]
            [reagent-table.core :as rt]
            ))

(enable-console-print!)
(.log js/console "console print")
(println "println after enable-console-print")

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
                :success 5000
                :warn 10000
                :error 25000
                5000)
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

(defn error-handler [status]
;(defn error-handler [{:keys [status status-text]}]
  (set-alert-message :error (str "Failed error processing order-spec: " (:response status)))
  ;(.log js/console (str "something bad happened: " status " " status-text))
  )
;Failed error processing order-spec: {:status 404, :status-text "Not Found", :failure :error, :response ":emailb@summit.com:order-num111:error-msgNo line items for order #111"}



(defn request-order-spec [email order-num]
  (let [url (str base-api-url "order-spec/" email "/" order-num)]
    (set-alert-message :success (str "Processing order " order-num ". You may submit more requests now."))
    (GET url
         {:headers {"Accept" "application/json"}
          :timeout 240000                                   ; 2 minutes
          ;:params {:message "Hello World"
          ;         :user    "Bob"}
          :handler handler
          :error-handler error-handler}
         )))

(def platt-prices (atom nil))
(defn platt-handler [response]
  (reset! platt-prices response))
(defn request-platt-prices []
  (let [url "/api/platt-prices"]
    (GET url
         {:headers {"Accept" "application/json"}
          :timeout 240000                                   ; 2 minutes
          :handler platt-handler
          :error-handler error-handler}
         )))
;; (request-platt-prices)

(defn ajax-request-order-spec []
  (request-order-spec @summit-email-address @order-num)
  ;(reset! summit-email-address "")
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
(def throbber-args-desc
  [{:name :size     :required false :type "keyword"       :default :regular :validate-fn throbber-size? :description [:span "one of " throbber-sizes-list]}
   {:name :color    :required false :type "string"        :default "#999"   :validate-fn string?        :description "CSS color"}
   {:name :class    :required false :type "string"                          :validate-fn string?        :description "CSS class names, space separated"}
   {:name :style    :required false :type "CSS style map"                   :validate-fn css-style?     :description "CSS styles to add or override"}
   {:name :attr     :required false :type "HTML attr map"                   :validate-fn html-attr?     :description [:span "HTML attributes, like " [:code ":on-mouse-move"] [:br] "No " [:code ":class"] " or " [:code ":style"] "allowed"]}])

(defn throbber
  "Render an animated throbber using CSS"
  [& {:keys [size color class style attr] :as args}]
  {:pre [(validate-args-macro throbber-args-desc args "throbber")]}
  (let [seg (fn [] [:li (when color {:style {:background-color color}})])]
    [box
     :align :start
     :child [:ul
             (merge {:class (str "rc-throbber loader "
                                 (case size :regular ""
                                            :small "small "
                                            :large "large "
                                            "")
                                 class)
                     :style style}
                    attr)
             [seg] [seg] [seg] [seg]
             [seg] [seg] [seg] [seg]]])) ;; Each :li element in [seg] represents one of the eight circles in the throbber

(defn change-it [ev]
  (.log js/console ev))

(defn order-spec-component []
  [:div.container.well
   ;; [throbber :color "ff0000" :size :large]
   [title :level :level2 :label "Order Spec Sheet Request"]
   [input-text :model summit-email-address :on-change change-it :status :error :placeholder "placeholder text" :status-tooltip "Hey\nbro" :status-icon? true]
   [input-textarea :model (atom "") :on-change change-it :status :error :placeholder "placeholder text" :status-tooltip "This is a long<br>tip" :rows 10]
   [button :disabled? true :class "btn btn-primary" :label "test" :tooltip "press me" :tooltip-position :left-center]
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

(defn csr-page []
  [:div.container.page-header
   [:h2 "Order Spec Sheet Request"]
   ;[:fieldset.mds-border {:style "padding-top: 20px"}
    [order-spec-component]
    ;]
   ])

