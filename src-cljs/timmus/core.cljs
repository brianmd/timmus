(ns timmus.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]

            [re-com.core           :refer [h-box v-box box selection-list label title checkbox p line hyperlink-href]]
            [re-com.selection-list :refer [selection-list-args-desc]]

            ;[timmus.sales-associate.core]
            [timmus.csr.core :refer [csr-page platt-prices]]
            [timmus.math :refer [math-page]]
            ;[timmus.sales-associate.core :refer [csr-page]]


            [murphydye.window :as win]
            [murphydye.components :as md-components]
            ;; [timmus.punchout-demo :as punchout]
            [timmus.entity :refer [entities-editor]]
            ;; [murphydye.chatr :as chatr]
            )
  (:import goog.History))




;(.clear js/console)

;(.clear js/console)

;(defn logit [& args]
;  (apply println args)
;  (last args))
;
;(logit :top)

;; (def query-keys (r/atom "fetching query names"))
;; (win/process-url query-keys "/api/admin/queries" {})



(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav>a.navbar-brand
   {:class (when (= page (session/get :page)) "active")
    :href uri
    :on-click #(reset! collapsed? true)}
   title])




(defn react-test []
  [:div
   [:b "react test"]])
  ;; ($.window)({
  ;;         title: "Cyclops Studio",
  ;;         url: "http://apps.fstoke.me/"
  ;;         })

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "Summit"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/projects" "Projects" :projects collapsed?]
         [nav-link "#/csr" "CSR" :csr collapsed?]
         ;; [nav-link "#/math" "Math" :math collapsed?]
         ]]])))

;(defn about-page []
;    [:div.container
;        [:div.row
;             [:div.col-md-12
;                   "this is the story of timmus... work in progress"]]])

; http://localhost:3449/api/platt-prices

(defn show-platt-prices []
  [:div
   (win/show-table @platt-prices {})
   ;(show-table platt-prices)
   ])
 
(defn projects-page []
  [:div.container
   [md-components/components]
  ;; [dialog-test]
   ;; [:div.row
   ;;  [:div.col-md-4
   ;;   "this is the story of timmus... work in progress"]
   ;;  [:div.col-md-4
   ;;   "Already set up for bootstrap"]
   ;;  [:div.col-md-4
   ;;   "in three columns"]
   ;;  ]
   ;; (if @platt-prices
   ;;   (show-platt-prices))
   ;; [:div
   ;;  [:a {:href "https://www.google.com/" :target "_blank"} "google"]]

   ;; :on-click #(win/new-window query-win {:title "Queries"})}]
   ])

;(defn csr-page []
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
;[:div [:a {:href "/csr-page"} "go to order spec page"]]

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to Summit"]
    [:p "This is an experimental site intended for quick prototypes."]
    ;; [:p "Time to start building your site!"]
    ;; [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]
    ]
   ;; [:div.row
   ;;  [:div.col-md-12
   ;;   [:h2 "Welcome to ClojureScript"]]]
   ;; (when-let [docs (session/get :docs)]
   ;;   [:div.row
   ;;    [:div.col-md-12
   ;;     [:div {:dangerouslySetInnerHTML
   ;;            {:__html (md->html docs)}}]]])
   ])

(def pages
  {:home #'home-page
   :projects #'projects-page
   :csr #'csr-page
   :math #'math-page
   })

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/projects" []
  (session/put! :page :projects))

(secretary/defroute "/csr" []
  (session/put! :page :csr))

(secretary/defroute "/math" []
  (session/put! :page :math))

;(session/put! :page #'csr-page))

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
  (md-components/init!)
  (hook-browser-navigation!)
  (mount-components))

;(logit "all done ....")
