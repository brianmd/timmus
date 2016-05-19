(ns summit.sap.project
  (:require [summit.utils.core :refer :all]
            ;; [summit.sap.types :refer :all]
            [summit.sap.core :refer :all]))

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

(examples
 (ppn (projects 1000092))

 (transform-project (first result))
 (transform-project (second result))

 (def projects-fn (find-function :dev :Z_O_ZVSEMM_KUNAG_ASSOC_PROJ))
 (ppn (function-interface projects-fn))
 (push projects-fn {:i_kunag (as-document-num account-num)})
 (execute f)
 (time
  (do
    (def result (projects 1000092))
    result))
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
