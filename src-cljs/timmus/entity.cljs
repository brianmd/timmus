(ns timmus.entity
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            ;[clojure.core :refer [future]]
            [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for sleep timeout
            [siren.core :refer [siren! sticky-siren! base-style]]

            [re-com.core :refer [title p input-text input-textarea button selection-list]]
            [re-com.util     :refer [deref-or-value px]]
            [re-com.popover  :refer [popover-tooltip]]
            [re-com.box      :refer [h-box v-box box gap line flex-child-style align-style]]
            [re-com.validate :refer [input-status-type? input-status-types-list regex?
                                     string-or-hiccup? css-style? html-attr? number-or-string?
                                     string-or-atom? throbber-size? throbber-sizes-list] :refer-macros [validate-args-macro]]
            [reagent-table.core :as rt]
            ))

(defn entity-editor [e]
  )

(defn entities-editor3 [entities]
  ;; [:div
   ;; {:style {:width "100%" :height "100%"}}
  [box
   :size "1"
   :child
   [v-box
     :size "1"
     :children
     [[h-box
       :size "1"
       :children
       [[v-box
         :size "1"
         :children
         [[box
           :size "1"
           :child [p "projects"]]
          [box
           :size "20px"
           :child [p "+ -"]]]]
        [v-box
         :size "1"
         :children
         [[p "entities"]
          [p "+ -"]]]
        ]]
      [h-box
       :size "1"
       :children
       [[p "entity attribute"]]]]]])


(def all-projects
  {:projects ["bh" "mdm"]}
  )

(defn make-selection-list [v]
  (for [item v]
    {:id item :label item :short item}))

(defn entities-editor [entities]
  (let [entities all-projects
        projects (r/atom (make-selection-list (:projects entities)))
        selected-project (r/atom #{})
        selected-entity (r/atom nil)]
    [:table.editor
     {:style {:width "100%" :height "100%"}}
     [:tr.top
      [:td.projects.top
       [v-box
        :children
        [
        ;; [selection-list
        ;;   :choices projects
        ;;   :model selected-project
        ;;   :on-change #(reset! selected-project %)]
         [:select
          {:size 5 :default-value "unkown" :on-click #(js/console.log %)} 
          [:option "valid"] 
          [:option "invalid"] 
          [:option "unkown"]]
         "+ -"
         ]]]
      [:td.entities.top
       "entities"]
      [:td.attributes.top
       "attributes"]
      ]
     [:tr
      [:td.definition.bottom
       {:col-span "3"}
       "attribute definition"]]]))
