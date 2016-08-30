(println "loading summit.sap.project")

(ns summit.sap.project
  (:require [summit.utils.core :refer :all]
            ;; [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]
            [clojure.string :as str]
            [summit.utils.core :as utils]))


;; ----------------------------------
;;     All projects for an account

(def ^:private et-project-fields [:client :id :sold-to :project-name :title :start-date :end-date :service-center-code :status :last-modifier :modified-on])

(defn- transform-triplets [m attr-defs]
  (let [attr-defs (partition 3 attr-defs)]
    (into {}
          (for [attr-def attr-defs]
            (let [web-name (first attr-def)  sap-name (second attr-def)  name-transform-fn (nth attr-def 2)]
              [web-name (name-transform-fn (m sap-name))])))))

(defn- transform-project [v]
  (let [m (into {} (map vector et-project-fields v))
        transformed-m
        (->
         m
         (dissoc :client)
         (assoc :sold-to (->int (:sold-to m))
                :ship-to-ids []
                :id (->int (:id m))
                :status (case (:status m)
                          "A" :active
                          "P" :planning
                          "C" :complete
                          "Z" :cancelled
                          (:status m))))]
    [(:id transformed-m) transformed-m]))

(defn- transform-ship-to [projs v]   ;; v => [:client :project :ship-to]
  (swap! projs update-in [(->int (second v)) :ship-to-ids] conj (->int (nth v 2))))

(defn projects [account-num]
  (let [f (find-function :qas :Z_O_ZVSEMM_KUNAG_ASSOC_PROJ)]
    (push f {:i_kunag (as-document-num account-num)})
    (execute f)
    (let [projs (atom (into {} (map! transform-project (pull f :et-projects))))]
      ;; (ppn projs)
      ;; (map! (partial transform-ship-to projs) (pull f :et-ship-tos))
      (map! (partial transform-ship-to projs) (pull f :et-ship-tos))
      (vals @projs))))
;; (ppn (projects 1000092))
;; (ppn (projects 1002225))


;; ----------------------------------------------------
;;      a single project

(defn transform-attribute-definition [m]
  (let [id (-> (:attr-assign m) str/lower-case (str/replace #"_" "-"))]
    [id {:id id
         :title (:attr-title m)
         :len (->int (:attr-len m))
         :required? (= "X" (:attr-req m))
         :batch-only? (= "X" (:attr-batch-only m))
         :conv (:attr-conv m)}]))

(defn transform-attribute-definitions [v]
  (into {}
        (map transform-attribute-definition v)))

(defn pair-key-vals [proj attr-prefix attr-defs]
  (map #(let [attr-name (str "attr-" (inc %))]
          (vector
           (:title (attr-defs attr-name))
           ((keyword (str attr-prefix attr-name)) proj)))
       (range (count attr-defs))))

(defn transform-message-lists [sap-messages]
  (let [msgs
        (for [msg sap-messages]
          {:order-num (->int (:vbeln msg))
           :item-num (->int (:posnr msg))
           :message-type (:msgtyp msg)
           :text (:message msg)})]
    (group-by :order-num msgs)))
;; (ppn (process-message-lists (pull-map projects-fn :et-message-list)))
;; ((process-message-lists (pull-map projects-fn :et-message-list)) 16811)

(def extractions
  {:order-info [;; :id (swap! id-seq-num inc)
                :project-id :projid ->int
                :order-num :vbeln-va ->int
                ;; :id 
                ;; :has-messages? (contains? messages order-num)
                :drawing-num :bstkd identity
                :schedule-date :edatu identity
                :expected-date :bstdk identity
                :entered-date :audat identity
                ;; ])]
]
   :line-item  [:item-num :posnr-va ->int
                :matnr :matnr ->int
                :customer-matnr :kdmat identity
                ;; :delivery-item :posnr-vl ->int
                :descript :arktx identity
                :circuit-id :circ-id identity
                :requested-qty :kwmeng double
                :delivered-qty :lfimg double
                :picked-qty :picked double
                :total-goods-issue-qty :tot-gi-qty double
                :remaining-qty :remaining double  ;; still to be delivered
                :reserved-qty :resv-qty double
                :uom :vrkme identity
                :inventory-loc :inv-loc identity
                :storage-loc :lgort identity
                :service-center :werks identity
                :trailer-atp :cust-loc-atp double
                :service-center-atp :main-loc-atp double]
   :delivery   [:delivery :vbeln-vl identity]})

(defn- line-item-id [m]
  (str (-> m :order :order-num) "-" (-> m :line-item :item-num)))

(defn- extract-attr-vals
  ([m begin-str] (extract-attr-vals m begin-str 1 {}))
  ([m begin-str index v]
   (let [key (keyword (str begin-str index))
         val (key m)]
     (if (nil? val)
       v
       (extract-attr-vals m begin-str (inc index) (conj v [(keyword (str "attr-" index)) val]))))))

(defn transform-status-line [m]
  {:order (transform-triplets m (:order-info extractions))
   :line-item (transform-triplets m (:line-item extractions))
   :delivery (transform-triplets m (:delivery extractions))
   :order-attr-vals (extract-attr-vals m "zz-zvsemm-vbak-attr-")
   :line-item-attr-vals (extract-attr-vals m "zz-zvsemm-vbap-attr-")
   :delivery-attr-vals (extract-attr-vals m "zz-zvsemm-likp-attr-")
   :raw m})

(defn transform-status-lines [m]
  (let [lines (map transform-status-line m)]
    lines))

(defn retrieve-maps [project-fn]
  (let [attr-defs (partition 3
                             [:status-lines :et-status-lines transform-status-lines
                              :messages :et-message-list transform-message-lists
                              :delivery-attr-defs :et-likp-atts transform-attribute-definitions
                              :order-attr-defs :et-vbak-atts transform-attribute-definitions
                              :line-item-attr-defs :et-vbap-atts transform-attribute-definitions])]
    (into {}
          (for [attr-def attr-defs]
            (let [web-name (first attr-def) sap-name (second attr-def) name-transform-fn (nth attr-def 2)]
              [web-name (name-transform-fn (pull-map project-fn sap-name))]))))
  )

;; (project 1)

(defn- transform-raw-order [m]
  (let [order (:order m)
        ;; line-item-id (-> m :line-item )
        ]
    (assoc
     (clojure.set/rename-keys order {:order-num :id})
     :line-item-id (line-item-id m)
     )
    ))

(defn- merge-order [orders order]
  (let [line-item-ids (apply conj [] (map :line-item-id orders))]
    (merge
     order
     {:line-item-ids (-> line-item-ids set sort)})))

(defn- join-like-orders [orders]
  (let [unique-orders (set (map #(dissoc % :line-item-id) orders))]
    (map #(merge-order (collect-same orders (:id %)) %) unique-orders)))
    ;; (set orders)))

(defn- order->json-api [order]
  (utils/ppn "" "" "--order:" order)
  {:type :order
   :id (:id order)
   :attributes (dissoc order :id :project-id :line-item-ids)
   :relationships {:project {:data {:type :project :id (:project-id order)}}
                   :line-items {:data (map (fn [x] {:type :line-item :id x}) (:line-item-ids order))}
                   }
   })

;; (defn- order [maps]
;;   (let [order :order ])
;;   (map #(assoc (clojure.set/rename-keys % {:order-num :id}) :line-items [])
;;        (set (map :order maps))))

(defn- extract-orders [maps]
  (->> maps
      (map transform-raw-order)
      join-like-orders
      (map order->json-api)
      ))
  ;; (map #(assoc (clojure.set/rename-keys % {:order-num :id}) :line-items [])
  ;;      (set (map :order maps))))


(defn- transform-raw-item [m]
  (let [item (:line-item m)
        order-id (-> m :order :order-num)
        delivery (-> m :delivery :delivery)
        ]
    (assoc item
           :id (line-item-id m)
           :order-id order-id
           :delivery delivery
           )))

(defn- merge-item [items item]
  (let [
        delivery-ids (apply conj [] (filter #(not-empty %) (map :delivery items)))
        delivered-qty (apply + (map :delivered-qty items))
        picked-qty (apply + (map :picked-qty items))
        ]
    (merge
     item
     {:delivery-ids delivery-ids
      :delivered-qty delivered-qty
      :picked-qty picked-qty}
     )))

(defn- collect-same [v id]
  (filter #(= id (:id %)) v))

(defn- join-like-items [items]
  (let [unique-items (set (map #(dissoc % :delivery :delivered-qty :picked-qty) items))]
    (map #(merge-item (collect-same items (:id %)) %) unique-items)))

(defn- line-item->json-api [item]
  {:type :line-item
   :id (:id item)
   :attributes (dissoc item :order-id :delivery-ids)
   :relationships {:order {:data {:type :order :id (:order-id item)}}
                   :deliveries {:data (map (fn [x] {:type :delivery :id x}) (:delivery-ids item))}
                   }
   })

(defn- extract-line-items [maps]
  (->> maps
      (map transform-raw-item)
      join-like-items
      (map line-item->json-api)
      ))

(defn transform-project [project-fn]
  (let [maps (retrieve-maps project-fn)
        status-lines (:status-lines maps)
        order-ids (map #(-> % :order :order-num) status-lines)
        items (extract-line-items status-lines)
        orders (extract-orders status-lines)
        json-orders (map (fn [x] {:type :order :id x}) order-ids)
        ]
    {:data
     {:type :project
      :id nil
      :attributes {:order-attribute-names (:order-attr-defs maps)
                   :line-item-attribute-names (:line-item-attr-defs maps)
                   :delivery-attribute-names (:delivery-attr-defs maps)}
      :relationships {:orders {:data json-orders}}
      }
     :included
     {
      :orders orders
      :line-items items
      }
     :raw maps
     }
    ))

(defn project
  ([project-id] (project :qas project-id))
  ([system project-id]
   (utils/ppn (str "getting project " project-id " on " system))
   (let [project-fn (find-function system :Z_O_ZVSEMM_PROJECT_CUBE)
         id-seq-num (atom 0)]
       ;; note: :attr-conv will tell us the attribute type
       ;; (ppn (function-interface project-fn))
     (push project-fn {:i-proj-id (as-document-num project-id)})
     (execute project-fn)
     (assoc-in
      (transform-project project-fn)
      [:data :id] project-id)
     )))
                 ;; [(first attr-def) (nth attr-def 2) ])))
  ;; (project 1)

(do
  #_(defn project-orig
      ([project-id] (project :qas project-id))
      ([system project-id]
       (let [project-fn (find-function system :Z_O_ZVSEMM_PROJECT_CUBE)
             id-seq-num (atom 0)]
       ;; note: :attr-conv will tell us the attribute type
       ;; (ppn (function-interface project-fn))
         (push project-fn {:i-proj-id (as-document-num project-id)})
         (execute project-fn)
         (let [delivery-attributes (transform-attribute-definitions
                                    (pull-map project-fn :et-likp-atts))
               order-header-attributes (transform-attribute-definitions
                                        (pull-map project-fn :et-vbak-atts))
               order-line-item-attributes (transform-attribute-definitions
                                           (pull-map project-fn :et-vbap-atts))
               status-lines (pull-map project-fn :et-status-lines)
               sap-messages (pull-map project-fn :et-message-list)
               messages (process-message-lists sap-messages)]

           (println "floating type: " (type (:kwmeng (first status-lines))))
           {:display-ordering
            {:order-header
             (concat [:has-messages? :drawing-num :order-num :expected-date :entered-date]
                     (map (fn [[k v]] (:title v)) order-header-attributes))
             :order-item
             (concat [:item-num :matnr :customer-matnr :descript :circuit-id :qty :delivered-qty :picked-qty :total-goods-issue-qty :remaining-qty :uom :schedule-date :trailer-atp :service-center-atp]
                     (map (fn [[k v]] (:title v)) order-line-item-attributes))
             :delivery
             (concat [:delivery]
                     (map (fn [[k v]] (:title v)) delivery-attributes))}
            :ordering
            {:order-header
             (concat [:has-messages? :project-id :drawing-num :order-num :schedule-date :expected-date :entered-date]
                     (map (fn [[k v]] (:title v)) order-header-attributes))
             :order-item
             (concat [:item-num :matnr :customer-matnr :delivery-item :descript :circuit-id :qty :delivered-qty :picked-qty :total-goods-issue-qty :remaining-qty :reserved-qty :uom :inventory-loc :storage-loc :service-center :trailer-atp :service-center-atp]
                     (map (fn [[k v]] (:title v)) order-line-item-attributes))
             :delivery
             (concat [:delivery]
                     (map (fn [[k v]] (:title v)) delivery-attributes))}
            :data
            {:messages messages

             :status-lines
             (for [s status-lines]
             ;; (into {}
             ;; {
             ;; :id (swap! id-seq-num inc)
             ;; :order-header
               (let [order-num (->int (:vbeln-va s))]
                 (merge
                  {:id (swap! id-seq-num inc)
                   :project-id (->int (:projid s))
                   :order-num order-num
                   :has-messages? (contains? messages order-num)
                   :drawing-num (:bstkd s)
                   :schedule-date (:edatu s)
                   :expected-date (:bstdk s)
                   :entered-date (:audat s)}
                  (into {} (pair-key-vals s "zz-zvsemm-vbak-" order-header-attributes))
                ;; )

                ;; :order-item
                ;; (merge
                  {:item-num (->int (:posnr-va s))
                   :matnr (->int (:matnr s))
                   :customer-matnr (:kdmat s)
                   :delivery-item (->int (:posnr-vl s))
                   :descript (:arktx s)
                   :circuit-id (:circ-id s)
                   :qty (double (:kwmeng s))
                   :delivered-qty (double (:lfimg s))
                   :picked-qty (double (:picked s))
                   :total-goods-issue-qty (double (:tot-gi-qty s))
                   :remaining-qty (double (:remaining s))  ;; still to be delivered
                   :reserved-qty (double (:resv-qty s))
                   :uom (:vrkme s)
                   :inventory-loc (:inv-loc s)
                   :storage-loc (:lgort s)
                   :service-center (:werks s)
                   :trailer-atp (double (:cust-loc-atp s))
                   :service-center-atp (double (:main-loc-atp s))}
                  (into {} (pair-key-vals s "zz-zvsemm-vbap-" order-line-item-attributes))
                ;; )

                ;; :delivery
                ;; (merge
                  {:delivery (:vbeln-vl s)}
                  (into {} (pair-key-vals s "zz-zvsemm-likp-" delivery-attributes))))
             ;; }
)}}

         ;; )
))))
  ;; (project 18)
  ;; (pp "" "" "----------------" "proj 19" (project 19))
  ;; (ppn (project 18))
)

;; (def x (project 1))
;; (keys x)
;; (-> x :data keys)
;; (-> x :data :messages)
;; (-> x :data :status-lines)

(examples
 (ppn
  "-----"
  (def proj18 (project 18))
  proj18)
 (project 19)
 (ppn (map :delivery (project 18)))

 (ppn (projects 1000092))

 (def projects-fn (find-function :dev :Z_O_ZVSEMM_KUNAG_ASSOC_PROJ))
 (ppn (function-interface projects-fn))
 (push projects-fn {:i_kunag (as-document-num account-num)})
 (execute f)
 (time
  (do
    (def result (projects 1000092))
    result))
 (transform-project (first result))
 (transform-project (second result))

 (:ship-tos result) (def projects-fn (find-function :dev :Z_O_ZVSEMM_PROJECT_CUBE))
 ;; note: :attr-conv will tell us the attribute type
 (def cube (function-interface projects-fn))
 (ppn cube)
 (keys cube)
 (map first (:imports cube))
 (map first (:exports cube))
 (map first (:tables cube))
 (ppn cube)

 (push projects-fn {:i-proj-id (as-document-num 18)})
 (execute projects-fn)
 (pull projects-fn :et-likp-atts)
 :delivery-attributes
 (pull-map projects-fn :et-likp-atts)
 :status-lines
 (ppn (pull-map projects-fn :et-status-lines))
 :order-header-attributes
 (pull-map projects-fn :et-vbak-atts)
 :order-line-item-attributes
 (pull-map projects-fn :et-vbap-atts)
 :message-list
 (ppn (pull-map projects-fn :et-message-list))

 (pull-map projects-fn :et-status-lines :et-vak-atts))

(println "done loading summit.sap.project")
