(ns timmus.math
  (:require [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
    ;[clojure.core :refer [future]]
            [reagent.core :as r :refer [atom]]
            [cljs.pprint :refer [pprint]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for timeout
            [siren.core :refer [siren! sticky-siren! base-style]]

            [reagent-table.core :as rt]
            [ajax.core :refer [GET POST]]
            )
  (:require-macros [timmus.macros :refer [sleep]])
  )

;(defn sleep [ms]
;  (let [c (chan)]
;    (js/setTimeout (fn [] (close! c)) ms)
;    c))

(def base-api-url "/api/")

(defn show-table [tbl]
  (if tbl
    (let [table (if (:headers tbl)
                  tbl
                  {:headers (tbl "headers") :rows (tbl "rows")})]
      [rt/reagent-table table])))

(def table-element-id (atom 0))
(defn next-table-element-id [] (swap! table-element-id inc))

(defrecord Table [headers rows])
(defrecord TableElement [id query table parent-id children-ids collapsed?])
(def table-elements (atom {}))

;(defn element-handler [response]
;  (.log js/console "got ok response")
;  (.log js/console response)
;  (.log js/console [:a 3 "bc"])
;  (.log js/console (keys response))
;  (.log js/console (response "headers"))
;  (assoc myresponse :response response)
;  (reset! tbl2 response)
;  )

(defn element-error-handler [response]
  (.log js/console "error :(")
  (.log js/console (:headers response))
  )
(defn run-element-query [ele]
  (let [url (str base-api-url "customers")
        handler (fn [response]
                  (println "response" (:id ele) response)
                  (println "header" (response "headers"))
                  (println "rows" (response "rows"))
                  (reset! (:table ele) response)
                  (println "tbl" (:table ele))
                  )]
    (println "boo" (:id ele))
    (println "hoo")
    ;(sleep 2000 (println "slept ..."))
    ;(js/setTimeout (fn [] (println "slept")) 2000)
    (GET url
         {:headers {"Accept" "application/json"}
          :timeout 240000                                   ; 2 minutes
          :handler handler
          :error-handler element-error-handler
          }
         )))
(defn new-table-element [parent-id query]
  (let [ele (TableElement. (next-table-element-id) query (r/atom nil) parent-id (r/atom #{}) (r/atom false))]
    (swap! table-elements assoc (:id ele) ele)
    (if parent-id
      (swap! (:children-ids (table-elements parent-id)) conj ele))
    (run-element-query ele)
    ele
    ;(assoc ele :table (Table. ["a" "b"] [[1 1] [2 2] [3 3]]))
    ))

(defn show-table-element [ele]
  (println "ste tbl:" (:id ele) (deref (:table ele)))
  [:div
   "hello"
   (show-table (deref (:table ele)))])

; url: tablename/column/value[limit/:n][/orderby/:col]



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

(defn make-static-table []
  [rt/reagent-table
   {:headers ["Row 1" "Row 2" "Row 3" "Row 4"]
    :rows [[1 1 1 1]
           [4 3 2 1]
           [2 1 4 3]]}]
  )

(defn get-customers [handler error-handler]
  (let [url (str base-api-url "customers")]
    (GET url
         {:headers {"Accept" "application/json"}
          :timeout 240000                                   ; 2 minutes
          :handler handler
          :error-handler error-handler
          }
         )))

(def ^:export myresponse {})
(def tbl2 (atom nil))
(defn handle [response]
  (.log js/console "got ok response")
  (.log js/console response)
  (.log js/console [:a 3 "bc"])
  (.log js/console (keys response))
  (.log js/console (response "headers"))
  (assoc myresponse :response response)
  (reset! tbl2 response)
  )

(defn error-handler [response]
  (.log js/console "error :(")
  (.log js/console (:headers response))
  )

(get-customers handle error-handler)

(defn math-page []
  (let [tbl (Table. ["R1" "R2" "R3" "R4"]
                    [[1 1 1 1]
                     [4 3 2 1]
                     [2 7 4 3]])
        tbl-ele (new-table-element nil nil)
        ]
    (letfn [(handler [response]
              (.log js/console response)
              (.log js/console (:headers response))
              (assoc myresponse :response response)
              ;(reset! tbl2 response)
              )
            ]

      [:div.container
       [:div.form-group.row
        [:div.col-sm-2]
        [:div.col-sm-8
         [:h2 "Multiplier"]
         [:h2 (:id tbl-ele)]
         [multiplier-component]
         ]]
       ;[:hr]
       [make-static-table]
       [show-table tbl]
       ;[show-table @tbl2]
       [show-table-element tbl-ele]
       ;[show-table-element {table-elements (:id tbl-ele)}]
       ]))
    )

