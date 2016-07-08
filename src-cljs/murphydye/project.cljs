(ns murphydye.project
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [ajax.core :refer [GET POST]]
            [cljsjs.fixed-data-table]

            [murphydye.utils.core :as utils :refer [ppc]]
            [murphydye.websockets :as ws]
            [murphydye.window :as win]
            ))

(defn scroll-to-bottom
  ([] (scroll-to-bottom "body"))
  ([selector]
   (let [obj (.item (.getElementsByTagName js/document selector) 0)]
     (set! (.-scrollTop obj) (.-scrollHeight obj)))))

(defn delay-scroll-to-bottom
  ([] (delay-scroll-to-bottom "body"))
  ([selector] (delay-scroll-to-bottom selector 100))
  ([selector delay]
   (.setTimeout js/window
                (fn [] (scroll-to-bottom selector))
                delay)
   ))

;; Set some better names for specific columns
(utils/set-humanized "drawing_num" "Drawing #")
(utils/set-humanized "order_num" "Order #")
(utils/set-humanized "has_messages?" "Alert?")
(utils/set-humanized "matnr" "SAP Material #")
(utils/set-humanized "customer_matnr" "Customer Material #")

(defn get-project [db]
  (let [url (str "/api/project/" (:project-id @db))
        id (:project-id @db)
        handler #(let [
                       filter-keys [:drawing-num :circuit-id]   ;; TODO add expected date as a filter
                       data (:data %)
                       status-lines (:status-lines data)
                       messages (:messages data)
                       filter-lookups (into {}
                                           (utils/map!
                                            (fn [k] (vector k (->
                                                               (utils/get-unique status-lines k)
                                                               (disj "")
                                                               (conj "(all)")
                                                               sort)))
                                            filter-keys))
                       proj {:project status-lines
                             :project-id id
                             :messages messages
                             :ordering (:display-ordering %)
                             :filter-lookups filter-lookups
                             :project-header data
                             }
                       ]
                   (swap! db assoc :project proj)
                   )
        error-handler #(win/qgrowl (str "Unable to get project id " id))
        options {:timeout 240000  ;; 2 minutes
                 :handler handler
                 :response-format :transit
                 :error-handler error-handler}]
    (GET url options)))

(defn get-projects [db]
  (let [url (str "/api/projects/" (:account-number @db))
        handler #(do
                   (swap! db assoc :projects %)
                   )
        error-handler #(win/qgrowl (str "Unable to get projects for account number " (:account-number @db)))
        options {:timeout 240000  ;; 4 * 60 * 1000 = 4 minutes
                 :handler handler
                 :response-format :transit
                 :error-handler error-handler
                 }]
    (println "getting projects for " (:account-number @db) @db)
    (when (empty? (:projects @db))
      (GET url options)
      (println "request has departed ..."))))


(declare project-component-win)

(defn open-project [project-header]
  (win/new-window (fn [] (project-component-win project-header))
                  {:title "Project Prototype"
                   :x 25 :y 125 :width 1000 :height 600}))

(defn projects-table-component [projects filters]
  (let [keys (map identity [:project-name :id])]
    (win/show-maps projects keys {:on-row-click
                                  #(do
                                     (ppc "changing project-id" %)
                                     (swap! filters assoc :project-id (last %))
                                     (ppc "new filters" filters))
                                  })))

(defn select-project-keys [m keys]
  (map #(m %) keys))

(defn show-row-maps [maps keys]
  (if maps
    [:div.row
     [:div.col-md-12
      (win/show-maps maps keys)]]))


(defn attribute-names [m]
  (if m (keys (:attributes m))))

(defn order-header-table [proj]
  (if proj
    (map #(:order-header %) proj)))

(defn filters-component
  "filters: reagent-atom hash of currently selected filters
  filter-lookups: collection of allowable values for the filters"
  [filters filter-lookups]
  [:div.well
   [:div.row
    [:br]
    [:div.col-md-12
     [:h2 (str "Project Filters")]]]
   [:div.row
    [:div.col-md-1]
    [:div.col-md-3
     [:b "Drawing #"] [:br]
     [:select {:value (:drawing-num @filters)
               :on-change #(let [v (-> % .-target .-value)]
                             (swap! filters assoc :drawing-num v :order-num nil :item-num nil))}
      (for [c (:drawing-num filter-lookups)]
        ^{:key c} [:option c])
      ]]
    [:div.col-md-3
     [:b "Circuit #"] [:br]
     [:select {:value (:circuit-id @filters)
               :on-change #(let [v (-> % .-target .-value)]
                             (swap! filters assoc :circuit-id v :order-num nil :item-num nil))}
      (for [c (:circuit-id filter-lookups)]
        ^{:key c} [:option c])
      ]]
    ]
   [:br]])

(defn project-component [db filters]
    (r/create-class
     {
      :display-name "project-component"
      :reagent-render
      (fn proj-comp-fn [db filters]
        (if (:project @db)
          (let [proj (:project @db)
                p (:project proj)
                drawing-num (:drawing-num @filters)
                circuit-id (:circuit-id @filters)
                order-num (:order-num @filters)
                p (if-not (= "(all)" drawing-num)
                    (filter #(= drawing-num (:drawing-num %)) p)
                    p)
                p (if-not (= "(all)" circuit-id)
                    (filter #(= circuit-id (:circuit-id %)) p)
                    p)
                order-keys (:order-header (:ordering proj))
                item-keys (:order-item (:ordering proj))
                delivery-keys (:delivery (:ordering proj))
                orders (map #(utils/select-keys3 % order-keys) p)
                ]
            [:div.container
             [:div.row [:div.col-md-12 [:h1.center (str "Project: " (:project-name @db))]]]
             [filters-component filters (:filter-lookups proj)]
             [:div.row [:br] [:div.col-md-12 [:h2 (str "Orders")]]]
             [:div.order-table
              [win/show-maps
               (sort-by :schedule-date (set orders))
               order-keys
               {:on-row-click #(do
                                 (swap! filters assoc :order-num (nth % 2) :item-seq nil)
                                 (reset! (:item-col-filters @filters) {})
                                 (delay-scroll-to-bottom)
                                 )
                :filterable-cols order-keys}
               ]]
             (when order-num
               (let [line-items (sort-by :item-seq (filter #(= order-num (:order-num %)) p))
                     message-keys [:item-seq :message-type :text]
                     messages (utils/map! #(utils/select-keys3 % message-keys) ((:messages proj) order-num))
                     ]
                 [:div
                  (if (not-empty messages)
                    [:div
                     [:div.row [:br] [:div.col-md-12 [:h2 (str "Messages for Order " order-num)]]]
                     [win/show-maps
                      messages
                      (rest message-keys)
                      ]])

                  [:div.row [:br] [:div.col-md-12 [:h2 (str "Line Items for Order " (:order-num @filters))]]]
                  [:div.item-table
                   [win/show-maps
                    line-items
                    item-keys
                    {:on-row-click #(do
                                      (swap! filters assoc :item-seq (first %))
                                      (delay-scroll-to-bottom))
                     :filterable-cols item-keys
                     :col-filters (:item-col-filters @filters)}
                    ]]
                  (when (:item-seq @filters)
                    (let [seq-num (:item-seq @filters)
                          deliveries (filter #(and
                                               (= seq-num (:item-num %))
                                               (not (= "" (:delivery %)))
                                               )
                                             line-items)
                          keys (:delivery (:ordering proj))
                          deliveries (utils/map! #(utils/select-keys3 % keys) deliveries)
                          ]
                      (if (not-empty deliveries)
                        [:div
                         [:div.row [:br] [:div.col-md-12 [:h2 (str "Deliveries for Item Seq " seq-num)]]]
                         [win/show-maps
                          deliveries
                          keys
                          ]]
                        [:h2 "No Deliveries for Item Seq " seq])))]))
             ;; ]
             ])))}))

(defn project-component-win [project-header]
  (let [project (r/atom {:id (:id project-header)
                         :project-header project-header
                         :project nil
                         :item-col-filters (r/atom {})})]
    (fn proj-comp-win-fn []
      [project-component project])))


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
                :cell (fn [args]
                        (let [{:keys [columnKey height width rowIndex] :as arg-map} (js->clj args :keywordize-keys true)]
                          (println arg-map)
                          [Cell (str "Row " rowIndex) "."]))
                :isResizable true
                :height 14
                :width 200
                :flexGrow 1
                }]
       ]
      ]]))

(defn projects-componentttt [db]
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

(defn target--of [event]
  (-> event .-target ))
(defn value--of [event]
  (-> event .-target .-value))
(defn content--of [event]
  (-> event .-target .-value))

(defn project-from-id [db id]
  (first (filter #(= id (:id %)) (:projects @db))))

(defn header-component [db filters]
  [:div.well
   [:div.row.center-vertically
    [:div.col-md-3.right [:b "Account #:"]]
    [:div.col-md-2
     [:input.form-control
      {:type        :text
       :style       {:margin "4px" :width "150px"}
       :placeholder "Type an account number and press [Enter]"
       :value       (:account-number @filters)
       :on-change   #(do
                       (println "prev temp account num:" @filters)
                       (swap! filters assoc :account-number (-> % .-target .-value)))
       :on-key-down
       #(when (= (.-keyCode %) 13)
          (swap! db assoc
                 :account-number (:account-number @filters)
                 :projects [])
          (swap! filters assoc
                 :drawing-num nil
                 :circuit-id nil
                 :project-id nil)
          ;; (.log js/console (str "requesting projects for: " (:account-number @db)))
          (get-projects db)
          )
       }]]
    [:div.col-md-2.right [:b "Project Name:"]]
    [:div.col-md-4
     [:select
      {:default-value (:project-id @filters)
       :on-change #(let [id (js/parseInt (value--of %))
                         proj (project-from-id db id)]
                     (swap! db assoc :project nil :project-id id :project-name (:project-name proj))
                     (swap! filters assoc :project-id id :project-name (:project-name proj) :drawing-num "(all)" :circuit-id "(all)" :order-num nil :item-num nil)
                     (get-project db)
                     )}
      [:option "<Select Project>"]
      (for [p (:projects @db)]
        [:option {:value (:id p)} (:project-name p)])]]
    ]
   (if (:project-id @filters)
     [:div.row
      [:div.col-md-7]
      [:div.col-md-3 [:a {:href "https://facebook.github.io/fixed-data-table/example-object-data.html" :target "_blank"} "Download"]]])])

(defn blank-row []
  [:div.row
   [:div.col-md-12
    [:br]]])

(defn projects-component [db]
  (let [filters (r/atom {:account-number (:account-number @db)
                         :item-col-filters (r/atom {})})]
    (fn projs-comp-fn [db]
      [:div.container
       ;; [simple-tablee]
       [:div.row
        [:div.col-md-12 [:h1.center "Project Portal Prototype"]]]
       [header-component db filters]
       [blank-row]

       ;; [:div.row
       ;;  [:div.col-md-12
       ;;   ;; (projects-table-component (:projects @db))]]
       ;;   (projects-table-component (:projects @db) filters)]]
       [:div.row
        [:div.col-md-12
         (if-let [project-id (:project-id @filters)]
           [project-component db filters]
           ;; [project-component-win {:id project-id :title (:project-name @filters)}]
           ;; (str "filters: " @filters)
           )]]
       ])))

(defn project-for-account [account-num]
  (let [db (r/atom {:account-number account-num
                    :project-id nil
                    :projects []
                    :filters {}
                    :project nil
                    })]
    (get-projects db)
    db
    ))

(defn new-projects-component []
  (let [db (project-for-account 1000092)]
    (get-projects db)
    (fn projs-comp-win-fn []
      [projects-component db])))




(defn main-component []
  (case :projects
    :projects-win (let [proj-map
                        {:title "Project Portal Prototype"
                         :x 25 :y 105 :width 900 :height 600}
                        ]
                    (win/new-window new-projects-component proj-map))
    :projects (new-projects-component)
    ;; :project-win (open-project {:id 18 :title "Jarred"})
    ;; :project (project-component-win {:id 18 :title "Jarred"})
    )
  )
