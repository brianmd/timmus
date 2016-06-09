(ns murphydye.window
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.session :as session]
            [re-com.core :refer [h-box v-box box selection-list label title checkbox p line hyperlink-href]]
            [re-com.selection-list :refer [selection-list-args-desc]]

            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]

            [reagent-table.core :as rt]
            [siren.core :refer [siren! sticky-siren! base-style]]

            [murphydye.utils.core :as utils :refer [ppc]]

                                        ;[timmus.sales-associate.core]
            ;; [timmus.csr.core :refer [csr-page platt-prices]]
            ;; [timmus.math :refer [math-page]]
                                        ;[timmus.sales-associate.core :refer [csr-page]]

            [clojure.string :as str])
  (:import goog.History))

(defn show-reagent-table
  "tbl should be a map with :headers and :rows"
  [tbl]
  (if tbl
    (let [table (if (:headers tbl)
                  tbl
                  {:headers (tbl "headers") :rows (tbl "rows")})]
      [rt/reagent-table table]
      )))

(defn cellize [value]
  (case value
    "TRUE" [:div.center.red
            "X"
            ;; [:span.glyphicon.glyphicon-alert ""]
            ]
    "FALSE" ""
    (str value)))

;; (defn make-css-class [s]
;;   (str/replace (str/replace s " " "")))
  ;; (str/lower-case (str/replace s " " ""))
  ;; (-> string str/lower-case #(str/replace % " " ""))))
  ;; (name (utils/clojurize-keyword string)))

(defn click-header-cell [headers sort-map event]
  ;; (ppc "click-header-cell" sort-map args (count args) (first args))
  ;; (let [v (-> (first args) .-target .-value)]
  ;; (.dir js/console event)
  ;; (ppc "screenx" (.-screenX event) "clientX" (.-clientX event))
  (let [col-name (-> event .-target .-innerHTML)
        col-num (.indexOf headers col-name)
        ascending? (if (= col-num (:col-num @sort-map))
                     (case (:ascending? @sort-map)
                       nil true
                       true false
                       false nil)
                     true)]
    ;; (ppc v col-num ascending?)
    (swap! sort-map assoc :col-num (if-not (nil? ascending?) col-num) :ascending? ascending? :col-name col-name)
    ))

(defn show-table
  "tbl should be a map with :headers and :rows"
  ;; ([tbl] (show-table tbl {}))
  ([tbl options]
   (if tbl
     (let [table (if (:headers tbl)
                   tbl
                   {:headers (tbl "headers") :rows (tbl "rows")})
           headers (map utils/humanize (:headers tbl))
           css-classes (map (comp name utils/clojurize-keyword) (:headers tbl))
           rows (:rows tbl)
           counter (atom 0)
           row-options (atom {})
           sort-info (r/atom {:col-num nil :ascending? nil})
           onclick-fn (:on-row-click options)
           col-filters (r/atom {})
           ]
       (fn [tbl options]
         (let [
               ;; table (if (:headers tbl)
               ;;         tbl
               ;;         {:headers (tbl "headers") :rows (tbl "rows")})
               ;; headers (map utils/humanize (:headers tbl))
               ;; css-classes (map (comp name utils/clojurize-keyword) (:headers tbl))
               rows (:rows tbl)
               ;; counter (atom 0)
               row-options (atom {})
               ;; sort-info (atom {:col-num nil :direction :down})
               sort-col (:col-num @sort-info)
               sort-name (:col-name @sort-info)
               ascending? (:ascending? @sort-info)
               filtered-rows (let [filters @col-filters
                                   rows (atom rows)]
                               ;; (ppc "tbl" rows @col-filters)
                               (ppc filters)
                               ;; (utils/map! (fn [k v] (ppc "k,v" k v)) filters)
                               (utils/map!
                                (fn [[k v]]
                                  (ppc "k,v" k v)
                                  (reset! rows
                                          (filter #(re-find (js/RegExp. v "i") (str (nth % k))) @rows)))
                                filters)
                               @rows)
               sorted-rows (if-let [col sort-col]
                             (if ascending?
                               (sort-by #(nth % col) filtered-rows)
                               (sort-by #(nth % col) #(compare %2 %1) filtered-rows)
                               )
                             filtered-rows)
               ;; onclick-fn (:on-row-click options)
               ]
           (if-let [f (:on-row-click options)]
             (swap! row-options assoc :on-click #(f)))

           ;; (ppc "sort-info" sort-info)
           ;; (println "row options:" @row-options)
           ;; [:table.well.smaller.table.table-striped.table-bordered
           [:div
            [:div.max-height
             [:table.well.smaller.table.table-bordered.centered
              [:thead>tr
               (for [h headers]
                 (let [ascending-col? (if (= h sort-name) ascending?)]
                   ^{:key h} [:th {:on-click (partial click-header-cell headers sort-info)} h (case ascending-col? true "\u25bc" false " \u25b2" nil)]))]
              ;; ^{:key h} [:th {:on-click #(do (.dir js/console %) (.dir js/console (.-target %)) (.dir js/console (-> % .-target .-innerHTML)))} h])]
              ;; ^{:key h} [:th {:on-click #(do (.dir js/console %) (.dir js/console (.-target %)) (set! (.-zzz js/window) (.-target %)))} h])]
              [:tfoot]
              ;; [:tfoot
              ;;  [:tr
              ;;   (for [h headers]
              ;;     ^{:key h} [:td h])]]
              [:tbody
               (if-let [filterable-cols (:filterable-cols options)]
                 [:tr
                  (doall
                   (map (fn [col-name css-class]
                          (if col-name
                            ;; (let [n (.indexOf headers col-name)]
                            (let [n (.indexOf (:headers tbl) col-name)]
                              ;; [:td [:input (name %)]]
                              [:td [:input {:style {:width "100%"}
                                            :className css-class
                                            :type :text
                                            :on-change (fn [event] (let [value (-> event .-target .-value)]
                                                                     (ppc "filter val" value col-name n headers)
                                                                     (swap! col-filters assoc n value)
                                                                     ;; (swap! col-filters assoc x)
                                                                     ))}]])
                            [:td ""]))
                        filterable-cols
                        css-classes))])
               (doall
                (for [row sorted-rows]
                  (let [clicker (if onclick-fn {:on-click #(onclick-fn row)} {})]
                    ;; (let [clicker (if onclick-fn {:on-click #(do (.dir js/console (-> % .-target))(.dir js/console (-> %))(.dir js/console (.type %)) (onclick-fn row))} {})]
                    ^{:key (swap! counter inc)}
                    [:tr.row-hover (merge options clicker)
                     (doall
                      (map (fn [x css-class] ^{:key (swap! counter inc)} [:td {:className css-class} (cellize x)])
                           row
                           css-classes))
                     ])))]
              ]]
             [:br]
             [:br]]
           ))))))

(defn show-maps
  ([maps] (show-maps maps (keys (first maps))))
  ([maps keys] (show-maps maps keys {}))
  ([maps keys options]
   (let [data (doall (map #(utils/select-keys2 % keys) maps))]
     [show-table {:headers keys :rows data} options])))

(defn growl [m]
  (.log js/console m)
  (let [m (if (map? m) m {:message m})
        global (merge
                base-style
                {:width "300px"
                 :color "white"
                 :status :success
                 :message "no message"
                 :delay 1000
                 }
                m)
        local (merge
               (case (:status global)
                 :warn {:background "yellow"}
                 :error {:background "red"}
                 {:background "green"})
               global)
        delay (or
               (:delay local)
               (case (:status local)
                 :success 5000
                 :quick 1000
                 :warn 10000
                 :error 25000
                 5000)
               )
        msg (:message local)
        style (dissoc local :message)]
    ;; (println "local:" local delay msg style)
     (if (= delay :sticky)
       (sticky-siren! {:style style :content (str "<div>" msg "</div>")})
       (siren! {:style style :content (str "<div>" msg "</div>") :delay delay}))))
;; (growl "yea")
;; (growl :success "yea" {:delay 1000})
;; (growl {:status :success :message "sticky" :delay :sticky})

(defn qgrowl [msg] (growl {:message msg :delay 2000}))
(defn alert [msg] (growl {:message msg :delay 2000}))
;; (qgrowl "quick")
(defn static-alert [msg] (growl {:message msg :delay :sticky}))

(def ^:private window-number (atom 0))

(defn on-query-select [x]
  (.log js/console x))

(defn new-window-url
  "map should have minimally :url"
  [m]
  (.window js/jQuery (clj->js m)))

(defn new-window [content-fn hash]
  (let [base
        {:showModal false
         :modalOpacity 0.5
         :icon "http://www.fstoke.me/favicon.ico"
         :title (str "Window #" (swap! window-number inc))
         :content "lots and lots of content"
         :footerContent ""
         :width 200
         :height 160
         :maxWidth 4800
         :maxHeight 4800
         :x (+ 80 (rand-int 500))
         :y (+ 80 (rand-int 500))

         ;; :onOpen #(swap! num-opened inc)
         :onClose #(.log js/console "closed")
         }
        win (.window js/jQuery (clj->js (merge base hash)))
        win-id (.attr (.getContainer win) "id")
        $ele (.getElementById js/document win-id)
        $content (.item (.getElementsByClassName $ele "window_frame") 0)
        ;; $content (.item (.getFrame win) 0)
        ]
    (if (vector? content-fn)
      (r/render content-fn $content)
      (r/render [content-fn] $content)
      )
    win))

(defn windows-test [n]
  (when (> n 0)
    (let [win (new-window [:div "Window Test"] {:width 250})]
      (js/setTimeout
       #(.close win)
       (rand-int 2000)))
    (js/setTimeout #(windows-test (dec n)) (rand-int 1000))
    ))

(defn process-url [atom-val url url-options]
  (let [handler #(reset! atom-val %)
        error-handler #(println %)
        options (merge
                 {:headers {"Accept" "application/json"}
                  :timeout 240000    ; 2 minutes
                  :handler handler
                  :error-handler error-handler}
                 url-options)
        ]
    (println (str "processing " url))
    (GET url options)))



(defn make-window [content-fn hash]
  (new-window content-fn hash))

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
    (process-url query-keys "/api/admin/queries" {})
    (fn query-win-fn []
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
    [:span
     [:input {:type "button" :value "Make Windows"
              :style {:background-color :blue :color :white}
              :on-click #(windows-test 10)}]
     ;; [:input {:type "button" :value "Chatr"
     ;;          :on-click #(new-window chatr-component {:title "Chatr" :x 50 :y 100 :width 400 :height 400})}]
     ;; [:input {:type "button" :value "Show Queries"
     ;;          :on-click #(new-window query-win {:title "Queries"})}]
     ;; [:div#simple]
     ]
    ))

