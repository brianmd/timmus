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
            )
  (:require-macros [timmus.macros :refer [sleep]])
  )

(defn error-handler [response]
  (.log js/console "-------------------------------     error :(")
  (.log js/console (:headers response))
  )

;(defn sleep [ms]
;  (let [c (chan)]
;    (js/setTimeout (fn [] (close! c)) ms)
;    c))

(def base-api-url "/api/")

(def entities (r/atom nil))

(.log js/console "getting entities")
(defn set-entities [hash]
  (.log js/console "------------------   got entities")
  (reset! entities hash)
  )

(defn get-json-entities []
  (GET (str base-api-url "entities/definitions")
      {:headers {"Accept" "application/json"}
       :timeout 240000                                   ; 2 minutes
       :handler set-entities
       :error-handler error-handler
       }
    ))
;; (get-json-entities)

(defn entity-for [entity-name]
  (if @entities (@entities entity-name)))

(defn belongs-to-for [entity]
  ((entity "relationships") "belongs-to")
  )

(defn has-many-for [entity]
  ((entity "relationships") "has-many")
  )

(defn named-belongs-to-for [entity entity-name]
  (filter #(= entity-name (% "name") (belongs-to-for entity))))

(defn named-has-many-for [entity entity-name]
  (filter #(= entity-name (% "name") (has-many-for entity))))

#_(defn show-entity-relationships [all-entities entity-name]
  (let [entity (entity-for entity-name)]
    ;(println entity)
    ;(println "belongs-to-for")
    ;(println (keys entity))
    (println "relationships" (entity "relationships"))
    (println "belongs-to-for" (belongs-to-for entity))
    ;(println (-> entity #(% "relationships")))
    ;(println (-> entity #(% "relationships") #(% "belongs-to")))
    ;(println (belongs-to-for entity))
    [:div
     [:b "belongs-to"]
     (show-relation-group entity (belongs-to-for entity))
     [:b "has-many"]
     (show-relation-group entity (has-many-for entity))
     ]))

(defn show-entity-relationships-orig [e entity-name]
  (let [
        entity (@e entity-name)
        relations (entity "relationships")
        belongs (relations "belongs-to")
        many (relations "has-many")
        belong-names (map #(% "name") belongs)
        many-names (map #(% "name") many)
        ]
    [:div
     [:b "belongs-to"]
     [:ul
      {:style {:margin-left "-40px" :list-style-type :none}}
      (map #(vector :li {:on-click
                         (fn []
                           (js/alert %)
                           (js/alert
                             (str (filter
                                    (fn [x] (= (x "name") %))
                                    belongs))))} (str %)) belong-names)]
     [:b "has-many"]
     [:ul
      {:style {:margin-left "-40px" :list-style-type :none}}
      (map #(vector :li (str %)) many-names)]
     ]
    )
  )
;[:div (str belong-names)]
    ;[:div (str (keys (entity "relationships")))]))

(defn show-entities []
  [:div
   [:h2 "Entities"]
   (when @entities
     [:ul
      (map #(vector :li (str %)) (keys @entities))])])

(defn show-table [tbl]
  (if tbl
    (let [table (if (:headers tbl)
                  tbl
                  {:headers (tbl "headers") :rows (tbl "rows")})]
      [rt/reagent-table table]
      )))

(def table-element-id (atom 0))
(defn next-table-element-id [] (swap! table-element-id inc))

(defrecord Table [headers rows])
(defrecord TableElement [id query entity parent-id children-ids collapsed?])
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
(defn run-element-query [entity]
  (let [query (:query entity)
        url (str base-api-url "entities/query/" (first query) "/" (second query) "/" (nth query 2))
  ;(let [url (str base-api-url "entities/query/" "customers" "/id/28")
        handler (fn [response]
                  ;(println "response" (:id ele) response)
                  ;(println "header" (response "headers"))
                  ;(println "rows" (response "rows"))
                  (reset! (:entity entity) response)
                  ;(println "tbl" (:table ele))
                  )]
    ;(println "boo" (:id ele))
    ;(println "hoo")
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
  ;(println "----------- query " query)
  (let [entity (TableElement. (next-table-element-id) query (r/atom nil) parent-id (r/atom []) (r/atom false))]
    (swap! table-elements assoc (:id entity) entity)
    (if parent-id
      (swap! (:children-ids (table-elements parent-id)) conj entity))
    (run-element-query entity)
    entity
    ;(assoc ele :table (Table. ["a" "b"] [[1 1] [2 2] [3 3]]))
    ))

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

;(get-customers handle error-handler)



;; (def tbl-ele (new-table-element nil ["customers" "id" "28"]))
;; (def my-carts (new-table-element nil ["carts" "customer_id" "28"]))
;; (swap! (:children-ids tbl-ele) conj (:id my-carts))



(def atom-n (r/atom 0))
(defn inc-n [] (swap! atom-n inc))

(defn show-relation-group [entity relation-group]
  (letfn [(process [entity relation] (js/alert (str relation)) (js/alert entity))
          (clicked [entity relation] (fn [] (process entity relation)))]
    [:ul
     {:style {:margin-left "-40px" :list-style-type :none}}
     (map #(vector :li {:key (inc-n) :on-click (clicked entity %)} (% "name")) relation-group)
     ]
    ))

(defn show-table-element [ele]
  ;(println "ste tbl:" (:id ele) (deref (:entity ele)))
  [:div
   [:h2 (first (:query ele))]
   (let [e @(:entity ele)]
     (show-table e))])

(defn show-entity [tbl-ele entity es]
  (let [entity @(:entity tbl-ele)
        es @entities
        eles @table-elements
        child-ids @(:children-ids tbl-ele)]
    [:div
     [:table {:style {:font-size "70%"}}
      [:tr
       [:td
        [:b "belongs-to"]
        ;(if (and @entities @tbl-entity)
        (show-relation-group @(:entity tbl-ele) (belongs-to-for (@entities (first (:query tbl-ele)))))
        ;)
        ]
       [:td
        [:b "has-many"]
        (show-relation-group entity (has-many-for (es (first (:query tbl-ele)))))
        ]
       [:td
        ; sometimes returns error that dom was changed. To fix, open in a new window.
        [show-table-element tbl-ele]
        ]]
      ]
     [:div {:style {:margin-left "30px"}}
      (let [child-eles (map #(eles %) child-ids)]
        ;(map #(vector :span {:key (inc-n)} (show-entity %)) child-eles)
        (map show-entity child-eles)
        )
      ]]))

(defn math-component []
  [:div.form-group.row
   [:div.col-sm-2]
   [:div.col-sm-8
    [:h2 "Multiplier"]
    ;; [:h2 (:id tbl-ele)]
    [multiplier-component]
    ]])


;; uncomment line below if show entity in math-page
;; (get-json-entities)

(defn math-page []
  (let [tbl (Table. ["R1" "R2" "R3" "R4"]
                    [[1 1 1 1]
                     [4 3 2 1]
                     [2 7 4 3]])
        ;tbl-ele (new-table-element nil ["customers" "id" "38"])
        ;entity (:entity tbl-ele)
        ]
    (letfn [(handler [response]
              (.log js/console response)
              (.log js/console (:headers response))
              (assoc myresponse :response response)
              ;(reset! tbl2 response)
              )
            ]

      [:div.container
       [math-component]
       ;; [show-entity tbl-ele @(:entity tbl-ele) @entities]
      ;[show-entities]
       ;[show-table-element {table-elements (:id tbl-ele)}]
       ]))
    )


