(ns murphydye.project
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            ;; [jayq.core :refer [$ css html]]
            ;; [reagent.session :as session]
            ;; [re-com.core           :refer [h-box v-box box selection-list label title checkbox p line hyperlink-href]]
            ;; [re-com.selection-list :refer [selection-list-args-desc]]

            ;; [secretary.core :as secretary :include-macros true]
            ;; [goog.events :as events]
            ;; [goog.history.EventType :as HistoryEventType]
            ;; [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            ;; [re-com.core :as recom :refer [title p input-text input-textarea button selection-list scroller]]
            ;; [re-com.util     :refer [deref-or-value px]]
            ;; [re-com.popover  :refer [popover-tooltip]]
            ;; [re-com.box      :refer [h-box v-box box gap line flex-child-style align-style]]
            ;; [re-com.validate :refer [input-status-type? input-status-types-list regex?
            ;;                          string-or-hiccup? css-style? html-attr? number-or-string?
            ;;                          string-or-atom? throbber-size? throbber-sizes-list] :refer-macros [validate-args-macro]]

            ;; [cljsjs.react-data-grid :as  grid]
            [cljsjs.fixed-data-table]

            [murphydye.utils.core :as utils :refer [ppc]]
            [murphydye.websockets :as ws]
            [murphydye.window :as win]
            ))

(utils/set-humanized "drawing_num" "Drawing #")
(utils/set-humanized "order_num" "Order #")

(defn get-project [proj]
  (let [url (str "/api/project/" (:id @proj))
        handler #(let [
                       filter-keys [:drawing-num :circuit-id]   ;; TODO add expected date as a filter
                       p (:data %)
                       filter-values (into {}
                                           (utils/map!
                                            (fn [k] (vector k (->
                                                               (utils/get-unique p k)
                                                               (disj "")
                                                               (conj "(all)")
                                                               sort)))
                                            filter-keys))
                       ]
                   ;; (ppc (first %))
                   (swap! proj assoc
                          :project (:data %)
                          :ordering (:ordering %)
                          :filter-values filter-values)
                   )
        error-handler #(win/qgrowl (str "Unable to get project id " (:id @proj)))
        options {:timeout 240000  ;; 2 minutes
                 :handler handler
                 :response-format :transit
                 :error-handler error-handler}]
    (GET url options)))

(defn get-projects [db]
  (let [url (str "/api/projects/" (:account-number @db))
        handler #(do
                   (println "got projects ------------------------------------------------------")
                   (println %)
                   ;; (win/static-alert %)
                   (println (type %))
                   (println (first %))
                   (println ((first %) "title"))
                   (println (map (fn [x] (x "title")) %))
                   (println "count:" (count %))
                   (println "account number: " (:account-number @db))
                   (swap! db assoc :projects %)
                   (println (:projects @db))
                   )
        error-handler #(win/qgrowl (str "Unable to get projects for account number " (:account-number @db)))
        options {:timeout 240000  ;; 2 minutes
                 :handler handler
                 :response-format :transit
                 :error-handler error-handler}]
    (win/qgrowl "getting projects")
    (println "getting projects for " (:account-number @db))
    (println "clojurize" (utils/humanize "abc def"))
    (if (empty? (:projects @db))
      (GET url options))
    (println "off request went ...")))


(declare project-component-win)

(defn open-project [project-header]
  (win/new-window (fn [] (project-component-win project-header))
                  {:title "Project Prototype"
                   ;; :x 25 :y 125 :width 1300 :height 600}))
                   :x 25 :y 125 :width 1000 :height 600}))

(defn projects-table-component [projects]
  (let [keys (map identity [:title :account-customer-id :service-center-code :start-date :end-date :status :id])]
    ;; (win/show-maps projects keys {:on-row-click #(ppc (str "clicked" %))})))
    (win/show-maps projects keys {:on-row-click #(open-project {:id (last %) :title (first %)})})))

;; (defn select-project-keys [m keys]
;;   (println m keys)
;;   (map #(m %) keys))

;; (defn projects-table-component2 [projects]
;;   (println "**************************************************************")
;;   (let [keys (map name [:title :account-customer-id :service-center-code :start-date :end-date :status :id])
;;         ;; rows (seq (into [] (map #(select-project-keys % keys) projects)))]
;;         rs (doall (map #(select-project-keys % keys) projects))
;;         ;; rows (list (first rs) keys (second rs) keys)
;;         frow (first rs)
;;         q ["Jarred" "defg"]
;;         ;; rows [(range 1 8) (range 11 18) (range 21 28) '(a b c d e f ghi) ["m" "n" "o" "p" "q" "r" "s"]]
;;         ;; rows [(range 1 8) (range 11 18) (range 21 28) '(a b c d e f ghi) ["m" "n" "o" "p" "q" "r" "s"] [:a :b (first frow) (second frow) :e "f" (first q)]]
;;         ;; rows (cons rows (first rs))
;;         ;; rows (list (first rows))
;;         rows rs
;;         ]
;;     (utils/ppc "rows" (type rows) rows)
;;     (utils/ppc "tttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttttt" frow "ffirst" (first frow) (type (first frow)) (type "abc") (= (first q) (first frow)) (type frow) (type q))
;;     (let [x (vec (map + [1 1] [3 4]))]
;;       (utils/ppc "vec" (type x) x))
;;     (utils/ppc (identity (list keys keys)))
;;     (println (count keys) (count (first rows)))
;;     [win/show-table {:headers keys :rows rows}]
;;      ;; [win/show-table {:headers keys :rows (list keys keys)}]))
;;     ))


(defn show-row-maps [maps keys]
  (if maps
    [:div.row
     [:div.col-md-12
      (win/show-maps maps keys)]]))

(defn attribute-names [m]
  (if m (keys (:attributes m))))

(defn order-header-table [proj]
  (if proj
    ;; (map #(dissoc (:order-header %) :attributes) proj)))
    (map #(:order-header %) proj)))

(defn filters-component [filters filter-values]
  [:div.well
   [:div.row
    [:br]
    [:div.col-md-12
     [:h2 (str "Filters")]]]
   [:div.row
    [:div.col-md-3
     [:b "Drawing #"] [:br]
     [:select {
               :value (:drawing-num @filters)
               :on-change #(let [v (-> % .-target .-value)]
                             (win/qgrowl v)
                             (swap! filters assoc :drawing-num v))}
      (for [c (:drawing-num filter-values)]
        ^{:key c} [:option c])
      ]]
    [:div.col-md-3
     [:b "Circuit #"] [:br]
     [:select {
               :value (:circuit-id @filters)
               :on-change #(let [v (-> % .-target .-value)]
                             (win/qgrowl v)
                             (swap! filters assoc :circuit-id v))}
      (for [c (:circuit-id filter-values)]
        ^{:key c} [:option c])
      ]]
    ]
   [:br]])




;; (def jquery (js* "$"))
;; (def jquery (js* "jQuery"))


;; (defn table-mounter [this]
;;   (.DataTable
;;    (js/$ (r/dom-node this))
;;    (clj->js {:bDestroy true :fnDrawCallback (r/force-update this)})))

;; (defn datatable-updater [this]
;;   (.DataTable
;;    (js/$ (r/dom-node this))
;;    (clj->js {:bDestroy true})))

(defn project-component [proj]
  (let [filters (r/atom {:drawing-num "(all)" :circuit-id "(all)"})]
    (.log js/console "hello")
    (.dir js/console "ReactDataGrid")
    (r/create-class
     {
      :display-name "project-component"
      ;; :component-did-mount table-mounter
      ;; :component-did-update table-mounter
      ;; #(let [self (js* "this")]
      ;;                         (win/qgrowl "mounted")
      ;;                         (.log js/console self)
      ;;                         ;; (.DataTable (js/jQuery "table") {:bDestroy true :fnDrawCallback (fn [x] (.forceUpdate x))})
      ;;                         (.DataTable (js/jQuery "table")
      ;;                                     (clj->js {:sPaginationType "bootstrap" :bAutoWidth false :bDestroy true
      ;;                                               ;; :fnDrawCallback (fn [] (r/force-update-all))
                              ;;                       }))
                              ;; )

      ;; :component-did-update #(do
      ;;                          (win/qgrowl "updated")
      ;;                          (.log js/console "{{{{{{}}}}}}" (js/jQuery "table"))
      ;;                          (.DataTable (js/jQuery "table") (clj->js {:paging false :aaSorting [] :bDestroy true :fnDrawCallback (fn [] (r/force-update-all))}))
      ;;                          )

      ;; :component-did-update #(do (win/qgrowl "updated") (.log js/console "{{{{{{}}}}}}" ($ "table")))
      ;; :component-did-update #(do (win/qgrowl "updated") (println (js/jQuery "table")))
      ;; :component-did-update #(do (win/qgrowl "updated") (.DataTables ($ :table)))
;; $("table").DataTable({"paging": false, "aaSorting": []})
      ;; :component-did-update #(do (win/qgrowl "updated") (-> (jquery "table") (.DataTables)))
      ;; :component-did-update #(do (win/qgrowl "updated") ("table" js/jQuery. .DataTables))
      :reagent-render
      (fn proj-comp-fn [proj]
        (ppc @proj)
        (let [p (:project @proj)
              p (if-not (= "(all)" (:drawing-num @filters))
                  (let [v (:drawing-num @filters)]
                    (filter #(= v (:drawing-num %)) p))
                  p)
              p (if-not (= "(all)" (:circuit-id @filters))
                  (let [v (:circuit-id @filters)]
                    (filter #(= v (:circuit-id %)) p))
                  p)
              ;; data (:data p)
              order-keys (:order-header (:ordering @proj))
              item-keys (:order-item (:ordering @proj))
              delivery-keys (:delivery (:ordering @proj))
              orders (map #(utils/select-keys3 % order-keys) p)
              items (map #(utils/select-keys3 % item-keys) p)
              deliveries (map #(utils/select-keys3 % delivery-keys) p)
              ]
          (ppc "" "--------------" "first project" (first p))
          (ppc "" "--------------" "order keys" order-keys)
          (ppc "" "--------------" "first orders" (first orders))
          (ppc "filters:" (:filter-values @proj))
          ;; [:div.container
          [:div.container
           [:div.row [:div.col-md-12 [:h1.center (str "Project Prototype: " (:title (:project-header @proj)))]]]
           [filters-component filters (:filter-values @proj)]
           [:div.row [:br] [:div.col-md-12 [:h2 (str "Orders")]]]
           [:div.order-table
            [win/show-maps
             (sort-by :schedule-date (set orders))
             ;; (sort-by :schedule-date (set (order-header-table p)))
             ;; (concat [:drawing-num :order-num :schedule-date :expected-date :document-date] (attribute-names (:order-header (first p))))
             (rest (:order-header (:ordering @proj)))
             ]]
           [:div.row [:br] [:div.col-md-12 [:h2 (str "Line Items")]]]
           [:div.item-table
            [win/show-maps
             (sort-by :order-num p)
             ;; (sort-by :order-num (set (map #(:order-item %) p)))
             (identity (:order-item (:ordering @proj)))
             ]]
           [:div.row [:br] [:div.col-md-12 [:h2 (str "Deliveries")]]]
           [win/show-maps
            (sort-by :delivery (set deliveries))
            ;; (sort-by :delivery (set (map #(:delivery %) p)))
            (identity (:delivery (:ordering @proj)))
            ]
           ;; ]
           ]))})))

;; https://gist.github.com/ducky427/10551a3346695db6a5f0

(def Table (r/adapt-react-class js/FixedDataTable.Table))
(def ColumnGroup (r/adapt-react-class js/FixedDataTable.ColumnGroup))
(def Column (r/adapt-react-class js/FixedDataTable.Column))
(def Cell (r/adapt-react-class js/FixedDataTable.Cell))

(defn gen-table
  "Generate `size` rows vector of 4 columns vectors to mock up the table."
  [size]
  (mapv (fn [i] [i                                                   ; Number
                 (rand-int 1000)                                     ; Amount
                 (rand)                                              ; Coeff
                 (rand-nth ["Here" "There" "Nowhere" "Somewhere"])]) ; Store
        (range 1 (inc size))))

;;; using custom :cellDataGetter in column for cljs persistent data structure
;;; is more efficient than converting row to js array in table's :rowGetter
(defn getter [k row] (get row k))

(defn home-page []
  (let [table  (gen-table 10)]
    (ppc table)
    [:div
     [Table {:width        600
             :height       400
             :rowHeight    30
             ;; :rowGetter    #(get table %)
             :rowsCount    (count table)
             :groupHeaderHeight 50
             :headerHeight 50}
      [ColumnGroup {:fixed true
                    :header [Cell "Col Group"]
                    ;; :width 400
                    }
       [Column {:fixed false
                :header [Cell "Col 1"]
                ;; :cell "<Cell>Column 1 static content</Cell>"
                :cell [Cell "Column 1 static content"]
                :height 14
                :width 200
                :flexGrow 2
                }]
       [Column {:fixed false
                :header [Cell "Col 2"]
                ;; :cell (fn [& args] (println args) [Cell (str args)])
                :cell (fn [args]
                        (let [{:keys [columnKey height width rowIndex] :as arg-map} (js->clj args :keywordize-keys true)]
                          (println arg-map)
                          [Cell (str "Row " rowIndex) "."]))
                ;; :cell (fn [{:keys [:columnKey :height :width :rowIndex]}] [Cell rowIndex])
                ;; :cell (fn [{:keys [:columnKey :height :width :rowIndex]}] [Cell rowIndex])
                :isResizable true
                :height 14
                :width 200
                :flexGrow 1
                }]
       ]
      ;; [Column {:label "Number" :dataKey 0 :cellDataGetter getter :width 100}]
      ;; [Column {:label "Amount" :dataKey 1 :cellDataGetter getter :width 100}]
      ;; [Column {:label "Coeff" :dataKey 2 :cellDataGetter getter :width 100}]
      ;; [Column {:label "Store" :dataKey 3 :cellDataGetter getter :width 100}]
      ]]))
;; header={<Cell>Col 1</Cell>}
;; cell={<Cell>Column 1 static content</Cell>}
;; width={2000}

(defn projects-component [db]
  [:div
   [home-page]])



;; https://gist.github.com/sebmarkbage/ae327f2eda03bf165261
;; React.createFactory(require('MyComponent'))
(defn fixed-table []
  (React.createFactory js/FixedDataTable.Table))

(defn fixed-column []
  (React.createFactory js/FixedDataTable.Column))

(defn simple-tablee []
  (.log js/console "fixed data table:")
  (.dir js/console js/FixedDataTable)
  (.dir js/console (fixed-table))
  (.dir js/console (fixed-column))
  (let [rows [[1] [4] [7]]]
    [Table {:rowHeight 50 :rowsCount (count rows) :width 5000 :height 5000 :headerHeight 50}
     [Column {:header "<Cell>Col 1</Cell>" :width 200 :cell "static value"}]]))

(defn simple-table []
  (.log js/console "fixed data table:")
  (.dir js/console js/FixedDataTable)
  (.dir js/console js/FixedDataTable.Table)
  [:b "hello"])

(defn projects-componentttt [db]
  (let [value (r/atom (:account-number @db))]
    (fn projs-comp-fn [db]
      (win/qgrowl "rendering projects-component")
      [:div.container
       ;; [simple-tablee]
       [:div
        [:div.row
         [:div.col-md-12 [:h1.center "Projects Prototype"]]]]
       [:div.row.well
        [:div.col-md-6.right [:b "Account #:"]]
        [:div.col-md-6
         [:input.form-control
          {:type :text
           :style {:margin "4px" :width "150px"}
           :placeholder "Type an account number and press [Enter]"
           :value @value
           :on-change #(do
                         (println "prev temp account num:" @value)
                         (reset! value (-> % .-target .-value)))
           :on-key-down
           #(when (= (.-keyCode %) 13)
              (swap! db assoc :account-number @value :projects [] :project)
              (.log js/console (str "requesting projects for: " (:account-number @db)))
              (get-projects db)
              )
           }]
         ]
        ]
       [:div.row
        [:div.col-md-12
         [:br]]]

       [:div.row
        [:div.col-md-12
         (projects-table-component (:projects @db))]]
       ])))

(defn projects-component-win []
  (let [db (r/atom {:account-number 1000092
                    :projects []})]
    ;; (get-projects db)
    (fn projs-comp-win-fn []
      [projects-component db])))




(defn project-component-win [project-header]
  (let [project (r/atom {:id (:id project-header)
                         :project-header project-header
                         :project nil})]
    (get-project project)
    (fn proj-comp-win-fn []
      [project-component project])))
