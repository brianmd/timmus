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
            [timmus.punchout-demo :as punchout]
            [timmus.entity :refer [entities-editor]]
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

(defn on-query-select [x]
  (.log js/console x))

(def fight-club
  [{:id "1" :label "1st RULE: You do not talk about FIGHT CLUB." :short "1st RULE"}
   {:id "2" :label "2nd RULE: You DO NOT talk about FIGHT CLUB." :short "2nd RULE"}
   {:id "3" :label "3rd RULE: If someone says \"stop\" or goes limp, taps out the fight is over." :short "3rd RULE"}
   {:id "4" :label "4th RULE: Only two guys to a fight." :short "4th RULE"}
   {:id "5" :label "5th RULE: One fight at a time." :short "5th RULE"}
   {:id "6" :label "6th RULE: No shirts, no shoes." :short "6th RULE"}
   {:id "7" :label "7th RULE: Fights will go on as long as they have to." :short "7th RULE"}
   {:id "8" :label "8th RULE: If this is your first night at FIGHT CLUB, you HAVE to fight." :short "8th RULE"}])

(defn query-win []
  (let [
        ;; query-keys (r/atom ["fetching query names"])
        query-keys (r/atom fight-club)
        selections (r/atom #{})
        ]
    (win/process-url query-keys "/api/admin/queries" {})
    (fn []
      [:div
       "queries: "
       [selection-list
        :choices query-keys
        :model selections
        :on-change on-query-select]
       (str @query-keys)
       ])))

(defn dialog-test []
  (fn []
    [:div
     [:input {:type "button" :value "Make Windows"
              :on-click #(win/windows-test 10)}]
     [:input {:type "button" :value "Show Queries"
              :on-click #(win/new-window query-win {:title "Queries"})}]
     [:div#simple]
     ]
    ))

(defn recom-layout-test []
  [v-box
   :class "debug"
   :children
   [[box :child [title :level :level2 :label "Header"]]
    [h-box
     :height "100px"
     :children
     [[box :size "70px" :child [:h4 "Nav"]]
      [box
       :size "1"
       :child
       [:span
        "Content"
        [:br]
        [hyperlink-href :label "google" :target "_blank" :tooltip "google in new window" :tooltip-position :right-center :href "https://www.google.com/"]]]]]
    [box :child "Footer"]]])


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
        [:a.navbar-brand {:href "#/"} "timmus"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]
         [nav-link "#/csr" "CSR" :csr collapsed?]
         [nav-link "#/math" "Math" :math collapsed?]
         ]]])))

;(defn about-page []
;    [:div.container
;        [:div.row
;             [:div.col-md-12
;                   "this is the story of timmus... work in progress"]]])

; http://localhost:3449/api/platt-prices

(defn show-platt-prices []
  [:div
   (timmus.math/show-table @platt-prices)
   ;(show-table platt-prices)
   ])
 
(defn about-page []
  [:div.container
   [dialog-test]
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
   [:div
    [:a {:on-click #(win/new-window
                     punchout/punchout-demo
                     {:title "Punchout"
                      :x 23 :y 10
                      :width 400 :height 400
                      })} "Punchout Demo"]
    ;; [recom-layout-test]
    ;; [line
    ;;  :size  "3px"
    ;;  :color "red"]
    " "
    [:a {:on-click #(win/new-window-url
                     {:url "https://www.google.com/"
                      :title "google"}
                     )} "google"]
    " "
    [:a {:on-click #(win/new-window-url
                     {:url "http://www.w3schools.com/"
                      :title "Url in a Window"}
                     )} "Url-in-a-Window"]
    " "
    [:a {:on-click #(win/new-window
                     recom-layout-test
                     {:title "Re-com test"
                      :width 600 :height 400})} "Re-com test"]
    " "
    [:a {:on-click #(win/new-window
                     entities-editor
                     {:title "Entities"
                      :width 400 :height 400})} "Entities"]
    ]

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

(secretary/defroute "/about" []
  (session/put! :page :about))

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
  (hook-browser-navigation!)
  (mount-components))

;(logit "all done ....")
