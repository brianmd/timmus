(ns murphydye.chatr
  (:require [reagent.core :as r]
            [murphydye.websockets :as ws]
            [murphydye.window :as win]
            ;; [reagent.session :as session]
            ;; [re-com.core           :refer [h-box v-box box selection-list label title checkbox p line hyperlink-href]]
            ;; [re-com.selection-list :refer [selection-list-args-desc]]

            ;; [secretary.core :as secretary :include-macros true]
            ;; [goog.events :as events]
            ;; [goog.history.EventType :as HistoryEventType]
            ;; [markdown.core :refer [md->html]]
            ;; [ajax.core :refer [GET POST]]
            ;; [re-com.core :as recom :refer [title p input-text input-textarea button selection-list scroller]]
            ;; [re-com.util     :refer [deref-or-value px]]
            ;; [re-com.popover  :refer [popover-tooltip]]
            ;; [re-com.box      :refer [h-box v-box box gap line flex-child-style align-style]]
            ;; [re-com.validate :refer [input-status-type? input-status-types-list regex?
            ;;                          string-or-hiccup? css-style? html-attr? number-or-string?
            ;;                          string-or-atom? throbber-size? throbber-sizes-list] :refer-macros [validate-args-macro]]

            ))


(defrecord person [id name])
(defrecord room [id name people owner])

(defonce messages (r/atom []))

(defn send-message [msg]
  (ws/send-transit-msg!
   {:app :chat :action :speak :message msg}))

(defn message-list []
  [:ul
   (for [[i message] (map-indexed vector (reverse @messages))]
     ^{:key i}
     [:li message])])

(defn message-input []
  (let [value (r/atom nil)]
    (.log js/console "new message-input")
    (fn []
      [:input.form-control
       {:type :text
        :placeholder "type in a message and press enter"
        :value @value
        :on-change #(do
                      (.log js/console (str @value ":changed:" (-> % .-target .-value)))
                      (reset! value (-> % .-target .-value)))
        :on-key-down
        #(when (= (.-keyCode %) 13)
           (.log js/console (str "submitting:" @value))
           (send-message @value)
           (reset! value nil))}])))

(defn chatr-component []
  [:div.container
   [:div.row
    [:div.col-md-12
     [:h2 "Summit Chat"]]]
   [:div.row
    [:div.col-md-4
     ;; [:input {:type "button" :value "Connect Websocket"
     ;;          :on-click #(init!)}]
     ]]
   [:div.row
    [:div.col-sm-6
     [message-input]]]
   [:div.row
    [:div.col-sm-6
     [message-list]]]
   ])


(defn update-messages! [{:keys [message]}]
  ;; (swap! messages #(vec (take 10 (conj % message)))))
  ;; (win/qgrowl (str "incoming msg:" message))
  ;; (swap! messages #(vec (take 10 (conj % message)))))
  (swap! messages #(vec (conj % message))))

(defn init! []
  (win/qgrowl "creating websocket")
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  ;; (mount-components)
  )
