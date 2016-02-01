(ns timmus.math.core
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
    ;[clojure.core :refer [future]]
            [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for timeout
            [siren.core :refer [siren! sticky-siren! base-style]]

            [reagent-table.core :as rt]
            ))

(defn atom-input [value attrs]
  [:input.form-control
   (merge
     attrs
     {:type "text"
      :value @value
      :on-change #(reset! value (-> % .-target .-value))})])


(defn random-pair []
  [(rand-int 9) (rand-int 9)])
(def correct-answers (r/atom 0))
(def multiplier-pair (r/atom (random-pair)))
(def multiplier-answer (r/atom ""))
(def multiplier-is-correct (r/atom nil))
(defn get-multiplier-answer [] (apply * @multiplier-pair))
(defn reset-multiplier! []
  (reset! multiplier-pair (random-pair))
  (reset! multiplier-answer ""))
(defn multiplier-component []
  [:div
   "Multiply these two numbers:"
   [:br]
   (first @multiplier-pair) "*" (second @multiplier-pair) " = "
   [atom-input multiplier-answer]
   [:br]
   (if (= @multiplier-answer (str (get-multiplier-answer)))
     (do
       (swap! correct-answers inc)
       (reset-multiplier!)
       "Correct!!!"
       ; (println "correct")
       (if (= 0 (mod @correct-answers 10))
         (.play (js/Audio. "http://murphydye.com/applause.mp3"))
         (.play (js/Audio. "http://murphydye.com/bottleopen.mp3"))
         )
       )
     ; "Not quite right :("
     )
   [:div "Number correct: " [:b @correct-answers]]
   ])

(defn make-table []
  [rt/reagent-table
   {:headers ["Row 1" "Row 2" "Row 3" "Row 4"]
    :rows [[1 1 1 1]
           [4 3 2 1]
           [2 1 4 3]]}]
  )

(defn math-page []
  ;(breakpoint)
  [:div.container
   ;[:div.form-group.row
   ; [:div.col-sm-2]
   ; [:div.col-sm-8
   ;  [:h2 "Multiplier"]
   ;  ]]
   [:div.form-group.row
    [:div.col-sm-2]
    [:div.col-sm-8
     [:h2 "Multiplier"]
     [multiplier-component]
     ]]
   ;[:hr]
   ;[make-table]
   ])

