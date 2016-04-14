;; component which opens other components in windows

(ns murphydye.components
  (:require [reagent.core :as r]
            [murphydye.websockets :as ws]
            [murphydye.window :as win]
            [murphydye.chatr :refer [chatr-component]]
            ))

(defn components []
  (win/qgrowl "starting components")
  (let [v (r/atom nil)]
    (fn []
      [:span
       [win/dialog-test]
       [:input {:type "button" :value "Chatr"
                :on-click #(win/new-window chatr-component {:title "Chatr" :x 50 :y 100 :width 400 :height 400})}]
       ]
      )))

