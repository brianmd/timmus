(ns summit.essence
  (:require [korma.core :as k]
            [summit.utils.core :refer :all]
            [summit.step.restapi :as restapi]
            ))

;; (defrecord Essence [data])

;; (defn new-essence [id]
;;   (->Essence (atom {:id id})))

;; (new-essence 3)

(defn id->atom [id]
  (atom {:id id}))
;; (id->atom (as-integer "  044"))


;; Note: Functions ending with a bang are forced (do not use the cache)
(defprotocol Essence
  (id [this])
  (cache [this])
  (clear-cache [this])
  (get-val [this k])
  (set-val [this k v])
  (get-cached [this k f])
  )

(defn essence [this]
  (id this))

(extend-protocol Essence
  Object
  ;; (id [this] (:id (deref (:cache this))))
  (cache [this] (deref (:cache this)))
  (clear-cache [this] (reset! (:cache this) {:id (id this)}))
  (get-val [this k] ((deref (:cache this)) k))
  (set-val [this k v] (swap! (:cache this) assoc k v))
  (id [this] (get-val this :id))
  (get-cached [this k f]
    (if-let [result (get-val this k)]
      result
      (let [result (f this)]
        (set-val this k result)
        result
        ))))


; source =>
{:context "Context1"
 :server :dev}



;;           Product essence is Matnr

(defprotocol BlueHarvestEssence
  (bh-id [this])
  )

(defprotocol BlueHarvestProduct
  (bh-attrs! [this])
  (bh-attrs [this])
  (bh-manufacturer [this]))

(defprotocol MdmProduct
  (mdm-attrs [this])
  (mdm-manufacturer [this]))

(defprotocol StepEssence
  (step-id [this]))

(defprotocol StepProduct
  (step-attrs! [this])
  (step-attrs [this])
  (step-others [this])
  (step-golden [this])
  )

;; essence of Product is matnr
(defrecord Product [cache]
  StepEssence
  (step-id [this] (str "MEM_SAP_" (id this)))
  StepProduct
  (step-attrs [this]
    (restapi/product (step-id this)))

  BlueHarvestEssence
  (bh-id [this] (get-cached this :bh-id #(as-matnr (id %))))
  BlueHarvestProduct
  (bh-attrs! [this]
    (first (k/select :products (k/where {:matnr (bh-id this)}))))
  (bh-attrs [this]
    (get-cached this :bh-attrs bh-attrs!))
  )

(defn make-product [m]
  "map should contain minimally an id"
  (->Product (atom m)))


(examples


(def bhp (make-product {:id 2856162}))
;; (def bhp (SapProduct. (id->atom 2856162)))
(bh-id bhp)
(bh-attrs bhp)

(def bhp (make-product {:bh-id "000000000002856162"}))
(bh-attrs bhp)
(cache bhp)

(def sapprod (SapProduct. (id->atom 1327768)))
(id sapprod)
(step-id sapprod)
(step-attrs sapprod)
(restapi/product "MEM_GLD_102633")
(restapi/product "MEM_SAP_1327768")
(ppn (restapi/download-product "MEM_SAP_1327768"))
(ppn (restapi/download-product "MEM_GLD_150247"))

(defn new-sap-product [id]
  (SapProduct. (atom {:id (as-integer id)})))

;; (defn assoc-essence [this k v]
;;   (swap! (:essence this) assoc k v))


;; (:impls Essence)

;; (new-sap-product "44")
;; (def x23 (new-sap-product "44"))
;; (assoc-essence x23 :t 7)
;; x23 


)



(defprotocol BlueHarvestManufacturer
  (bh-attrs! [this])
  (bh-attrs [this])
  )

(defprotocol StepManufacturer
  (step-attrs! [this])
  (step-attrs [this])
  (step-children [this])
  (step-children! [this])
  (step-golden-children [this])
  (step-source-children [this])
  (step-golden [this])
  (step-sap [this])
  (step-ts [this])
  (step-idw [this])
  (step-parent-id [this])
  (step-parent [this])
  (step-type [this])
  )


(defrecord Manufacturer [cache]
  StepEssence
  (step-id [this] (get-val this :step-id))
  StepManufacturer
  (step-attrs! [this]
    (restapi/manufacturer-attrs (step-id this)))
  (step-attrs [this]
    (get-cached this :step-attrs step-attrs!))
  (step-parent-id [this]
    (:parent-id (step-attrs this)))
  (step-parent [this]
    (get-cached this :parent (fn [_] (new-manufacturer {:step-id (step-parent-id this)}))))
  (step-children! [this]
    (restapi/manufacturer-children (step-id this)))
  (step-children [this]
    (get-cached this :step-children step-children!))
  (step-golden-children [this]
    (:golden-children (step-children this)))
  (step-source-children [this]
    (:children (step-children this)))
  (step-type [this]
    (if (= "Golden Manufacturer" (:type (step-attrs this)))
      :golden-manufacturer
      (clojurize-keyword (:mfr-source (step-attrs this)))))

  BlueHarvestEssence
  (bh-id [this] (get-val this :bh-id))
  BlueHarvestManufacturer
  (bh-attrs! [this]
    (first (k/select :manufacturers (k/where {:id (bh-id this)}))))
  (bh-attrs [this]
    (get-cached this :bh-attrs bh-attrs!))
  )

(defn new-manufacturer [m]
  (->Manufacturer (atom m)))
;; (def gldmfr (new-manufacturer {:step-id "GoldenMfr110202"}))


(examples


(def gldmfr (new-manufacturer {:step-id (first (:golden-children rootmfrs))}))
(def gldmfr (new-manufacturer {:step-id "GoldenMfr110202"}))

(def bhmfr (new-manufacturer {:bh-id 1 :step-id "TsMfr1061"}))
(bh-attrs bhmfr)
(step-attrs bhmfr)
(step-type bhmfr)
(step-parent-id bhmfr)
(step-parent bhmfr)
(step-source-children (step-parent bhmfr))
(bh-id bhmfr)
(ppn (ancestors (class {})))


(step-attrs gldmfr)
(step-attrs! gldmfr)
(step-parent gldmfr)
(step-children gldmfr)
(step-golden-children gldmfr)
(step-source-children gldmfr)
(cache gldmfr)
(clear-cache gldmfr)


;; then step-id would return value based on type: :sap=>"MEM_SAP_{id}", etc.

(ancestors Manufacturer)
(def rootmfrs (restapi/root-manufacturer))
(ppn rootmfrs)
(keys rootmfrs)
(count (:children rootmfrs))
(count (:golden-children rootmfrs))

(def gldmfr (->Manufacturer (id->atom (first (:golden-children rootmfrs)))))
(id gldmfr)
(cache gldmfr)
(clear-cache gldmfr)
(step-attrs gldmfr)
(step-children gldmfr)

)

