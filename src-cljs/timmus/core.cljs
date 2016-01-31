(ns timmus.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]

            ;[timmus.sales-associate.core]
            [timmus.csr.core :refer [order-spec-page]]
            ;[timmus.sales-associate.core :refer [order-spec-page]]
            )
  (:import goog.History))

;(.clear js/console)

;(.clear js/console)

;(defn logit [& args]
;  (apply println args)
;  (last args))
;
;(logit :top)

(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav>a.navbar-brand
   {:class (when (= page (session/get :page)) "active")
    :href uri
    :on-click #(reset! collapsed? true)}
   title])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "timmus"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]
         [nav-link "#/orderspec" "OrderSpec" :orderspec collapsed?]
         ]]])))

;(defn about-page []
;    [:div.container
;        [:div.row
;             [:div.col-md-12
;                   "this is the story of timmus... work in progress"]]])

 
 (defn about-page []
   [:div.container
    [:div.row
     [:div.col-md-4
      "this is the story of timmus... work in progress"]
     [:div.col-md-4
      "Already set up for bootstrap"]
     [:div.col-md-4
      "in three columns"]
     ]])
;(defn order-spec-page []
;  [:div.container
;   [:div.row
;    [:div.col-md-4
;     "order-spec  page"]
;    [:div.col-md-4
;     "Already set up for bootstrap"]
;    [:div.col-md-4
;     "in three columns"]
;    ]])

;(logit about-page)
;[:div [:a {:href "/order-spec-page"} "go to order spec page"]]

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to timmus"]
    [:p "Time to start building your site!"]
    [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to ClojureScript"]]]
   (when-let [docs (session/get :docs)]
     [:div.row
      [:div.col-md-12
       [:div {:dangerouslySetInnerHTML
              {:__html (md->html docs)}}]]])])

(def pages
  {:home #'home-page
   :about #'about-page
   :orderspec #'order-spec-page
   })

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

(secretary/defroute "/orderspec" []
  (session/put! :page :orderspec))
;(session/put! :page #'order-spec-page))

;(logit "after defroute")
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

;(logit "after fetch-docs!")

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))

;(logit "all done ....")
