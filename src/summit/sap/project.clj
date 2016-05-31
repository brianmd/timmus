(println "loading summit.sap.project")

(ns summit.sap.project
  (:require [summit.utils.core :refer :all]
            ;; [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]
            [clojure.string :as str]))

(def ^:private et-project-fields [:client :id :sold-to :account-customer-id :title :start-date :end-date :service-center-code :status :last-modifier :modified-on])

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
                          (:status m)))
         )]
    [(:id transformed-m) transformed-m]))

(defn- transform-ship-to [projs v]   ;; v => [:client :project :ship-to]
  (swap! projs update-in [(->int (second v)) :ship-to-ids] conj (->int (nth v 2))))

(defn projects [account-num]
  (let [f (find-function :dev :Z_O_ZVSEMM_KUNAG_ASSOC_PROJ)]
    (push f {:i_kunag (as-document-num account-num)})
    (execute f)
    (let [projs (atom (into {} (map! transform-project (pull f :et-projects))))]
      (ppn projs)
      (map! (partial transform-ship-to projs) (pull f :et-ship-tos))
      (vals @projs))
    ))
;; (ppn (projects 1000092))



;; ----------------------------------------------------
;;      a single project

(defn transform-attribute-definition [m]
  (let [id (-> (:attr-assign m) str/lower-case (str/replace #"_" "-"))]
    [id {:id id
         :title (:attr-title m)
         :len (->int (:attr-len m))
         :required? (= "X" (:attr-req m))
         :batch-only? (= "X" (:attr-batch-only m))
         :conv (:attr-conv m)
         }]))

(defn transform-attribute-defintions [v]
  (into {}
        (map transform-attribute-definition v)))

(defn pair-key-vals [proj attr-prefix attr-defs]
  (map #(let [attr-name (str "attr-" (inc %))]
          (vector
           (:title (attr-defs attr-name))
           ((keyword (str attr-prefix attr-name)) proj)
           ))
       (range (count attr-defs))))

(do
  (defn project [project-id]
    (let [project-fn (find-function :dev :Z_O_ZVSEMM_PROJECT_CUBE)
          id-seq-num (atom 0)]
      ;; note: :attr-conv will tell us the attribute type
      ;; (ppn (function-interface project-fn))
      (push project-fn {:i-proj-id (as-document-num project-id)})
      (execute project-fn)
      (let [
            delivery-attributes (transform-attribute-defintions
                                 (pull-map project-fn :et-likp-atts))
            order-header-attributes (transform-attribute-defintions
                                     (pull-map project-fn :et-vbak-atts))
            order-line-item-attributes (transform-attribute-defintions
                                        (pull-map project-fn :et-vbap-atts))
            status-lines (pull-map project-fn :et-status-lines)
            ]

        (println "floating type: " (type (:kwmeng (first status-lines))))
        {
         :display-ordering
         {
          :order-header
          (concat [:drawing-num :order-num :schedule-date :expected-date :document-date]
                  (map (fn [[k v]] (:title v)) order-header-attributes))
          :order-item
          (concat [:item-seq :matnr :customer-matnr :delivery-item :descript :circuit-id :qty :delivered-qty :picked-qty :total-goods-issue-qty :remaining-qty :reserved-qty :uom :inventory-loc :storage-loc :service-center :trailer-atp :service-center-atp]
                  (map (fn [[k v]] (:title v)) order-line-item-attributes))
          :delivery
          (concat [:delivery]
                  (map (fn [[k v]] (:title v)) delivery-attributes))
          }
         :ordering
         {
          :order-header
          (concat [:project-id :drawing-num :order-num :schedule-date :expected-date :document-date]
                  (map (fn [[k v]] (:title v)) order-header-attributes))
          :order-item
          (concat [:item-seq :matnr :customer-matnr :delivery-item :descript :circuit-id :qty :delivered-qty :picked-qty :total-goods-issue-qty :remaining-qty :reserved-qty :uom :inventory-loc :storage-loc :service-center :trailer-atp :service-center-atp]
                  (map (fn [[k v]] (:title v)) order-line-item-attributes))
          :delivery
          (concat [:delivery]
                  (map (fn [[k v]] (:title v)) delivery-attributes))
          }
         :data
        (for [s status-lines]
          ;; (into {}
           ;; {
            ;; :id (swap! id-seq-num inc)
            ;; :order-header
            (merge
             {
              :id (swap! id-seq-num inc)
              :project-id (->int (:projid s))
              :order-num (->int (:vbeln-va s))
              :drawing-num (:bstkd s)
              :schedule-date (:edatu s)
              :expected-date (:bstdk s)
              :document-date (:audat s)
              }
             (into {} (pair-key-vals s "zz-zvsemm-vbak-" order-header-attributes))
              ;; )

            ;; :order-item
            ;; (merge
             {
              :item-seq (->int (:posnr-va s))
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
              :service-center-atp (double (:main-loc-atp s))
              }
            (into {} (pair-key-vals s "zz-zvsemm-vbap-" order-line-item-attributes))
            ;; )

            ;; :delivery
            ;; (merge
             {
              :delivery (:vbeln-vl s)
              }
             (into {} (pair-key-vals s "zz-zvsemm-likp-" delivery-attributes))
             )
            ;; }
           )}
 
       ;; )
        )
      ))
  ;; (project 18)
  ;; (pp "" "" "----------------" "proj 19" (project 19))
  ;; (ppn (project 18))
  )

(examples
 (ppn
  "-----"
  (project 18)
  )
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

 (:ship-tos result)


 (def projects-fn (find-function :dev :Z_O_ZVSEMM_PROJECT_CUBE))
 ;; note: :attr-conv will tell us the attribute type
 (ppn (function-interface projects-fn))
 (push projects-fn {:i-proj-id (as-document-num 18)})
 (execute projects-fn)
 (pull projects-fn :et-likp-atts)
 :delivery-attributes
 (pull-map projects-fn :et-likp-atts)
 (pull-map projects-fn :et-status-lines)
 :order-header-attributes
 (pull-map projects-fn :et-vbak-atts)
 :order-line-item-attributes
 (pull-map projects-fn :et-vbap-atts)

 (pull-map projects-fn :et-status-lines :et-vak-atts)
 )

(println "done loading summit.sap.project")
