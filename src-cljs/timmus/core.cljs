(ns timmus.core
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


(defn summit-component []
  [:div "Summit Electric"])
(defn simple-component []
  [:div "I love Jennifer"])

(defn render-simple []
  ;; (r/render [#'navbar] (.getElementById js/document "navbar"))
  ;; (r/render [#'simple-component]
  (r/render [simple-component]
            (js/document.getElementById "simple")))
                      ;; (.-body js/document)))

(defn render-simple2 [ele]
  (r/render [simple-component] ele))
;; (.-body js/document)))

(defn ^:export call-from-js [method-name]
  (render-simple)
  (.log js/console method-name)
  )


(def window-number (atom 0))

(defn make-window [content-fn hash]
  (let [base
        {:showModal false
         :modalOpacity 0.5
         :icon "http://www.fstoke.me/favicon.ico"
         :title (str "Window #" (swap! window-number inc))
         :content "lots and lots of content"
         :footerContent "footer content"
         :width 200
         :height 160
         ;; :maxWidth 400
         ;; :maxHeight 300
         :x (+ 80 (rand-int 500))
         :y (+ 80 (rand-int 500))

         ;; :onOpen #(swap! num-opened inc)
         :onClose #(.log js/console "closed")
         }
        win (.window js/jQuery (clj->js (merge base hash)))
        ;; $id (.id (.getContainer win))
        win-id (.attr (.getContainer win) "id")
        $ele (.getElementById js/document win-id)
        $content (.item (.getElementsByClassName $ele "window_frame") 0)
        ;; $content (.item (.getFrame win) 0)
        ]
    (r/render [content-fn] $content)
    win))

(defn simple-windows [n]
  (when (> n 0)
    (let [win (make-window summit-component {:width 250})]
      (js/setTimeout
       #(.close win)
       (rand-int 2000)))
    (js/setTimeout #(simple-windows (dec n)) (rand-int 1000))
    ))
  ;; (dotimes [x n] (make-window :test {:content "asdfs"})))

(defn dialog-test []
  [:div
   ;; [:input {:type "button" :value "simple component"
   ;;          :on-click #(render-simple)}]
   [:input {:type "button" :value "Make Window--simple component"
            ;; :on-click #(make-window :test {:content (str "window #" )})}]
            :on-click #(simple-windows 300)}]
   [:div#simple]
   ;; [:input {:type "button" :value "Make Window"
   ;;          :on-click #(make-window :test {:content (str "window #" )})}]
   ]
            ;; :on-click #(make-window {:content [:b (str "window #" )]})}]]
            ;; :on-click #(dotimes [n 2] (make-window {:content (str "window #" n)}))}]]
  )


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
    [:div.row
     [:div.col-md-4
      "this is the story of timmus... work in progress"]
     [:div.col-md-4
      "Already set up for bootstrap"]
     [:div.col-md-4
      "in three columns"]
     ]
    ;; (if @platt-prices
    ;;   (show-platt-prices))
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
