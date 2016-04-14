;; component which opens other components in windows

(ns murphydye.components
  (:require [reagent.core :as r]
            [re-com.core           :refer [h-box v-box box selection-list label title checkbox p line hyperlink-href]]
            [re-com.selection-list :refer [selection-list-args-desc]]

            [murphydye.websockets :as ws]
            [murphydye.window :as win]
            [timmus.punchout-demo :as punchout]
            [timmus.entity :refer [entities-editor]]
            [murphydye.chatr :refer [chatr-component] :as chatr]
            ))

(defn init! []
  (chatr/init!))

(defn on-query-select [x]
  (.log js/console x))

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

(defn test-links []
  [:div
   [:a {:on-click #(win/new-window
                    punchout/punchout-demo
                    {:title "Punchout"
                     :x     23  :y      10
                     :width 400 :height 400
                     })} "Punchout Demo"]
        ;; [recom-layout-test]
        ;; [line
        ;;  :size  "3px"
        ;;  :color "red"]
        " "
   [:a {:on-click #(win/new-window-url
                    {:url   "https://www.google.com/"
                     :title "google"}
                    )} "google"]
        " "
   [:a {:on-click #(win/new-window-url
                    {:url   "http://www.w3schools.com/"
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
  ])

(defn components []
  (win/qgrowl "starting components")
  (let [v (r/atom nil)]
    (fn []
      [:div
       [:span
        [win/dialog-test]
        ;; [:input {:type "button" :value "Chatr"
        ;;          :on-click #(win/new-window chatr-component {:title "Chatr" :x 50 :y 100 :width 400 :height 400})}]
        [:input {:type "button" :value "Create Mfr Lookup"
                 :on-click #(win/new-window-url {:url "http://10.9.0.105:3449/api/manufacturerlookup":title "Create Manufacturer Lookup" :x 50 :y 100 :width 400 :height 400})}]
        ]
       [test-links]
       ]
      )))

