(ns summit.step.import.all-import
  (:require [clojure.string :as str]
;            [clojure.java.io :as io :refer [as-url make-parents]]

;            [clojure.data.xml :as xml]
;            [clojure.xml :as x]
            ;; [hiccup.core :as hiccup]
            ;; [clojure.core.reducers :as r]

;            [clojure.data.csv :as csv]
            ;; [clojure.java.io :as io]
            ;; [clojure.data.codec.base64 :as b64]

            ;; [summit.step.xml-output :refer :all]


            ;; [net.cgrand.enlive-html :as html]
            [summit.punchout.core :refer :all]

            [summit.utils.core :refer :all]

            [summit.step.import.product-selectors :refer :all]
            [summit.step.import.idw.product :as idw]
            [summit.step.import.ts.product :as ts]
            [summit.step.import.sap.product :as sap]
            ))

(def search-context "context=Context1")

(def product-url "http://stibo-dev.insummit.com/restapi/products/")

(def golden-search-url (str "http://stibo-dev.insummit.com/restapi/basicsearch/MEM_GLD*?" search-context))

(defn step-authentication []
  {:basic-auth ["Stepsys" "stepsys"]})

(defn get-golden-ids []
  (let [body (:body (clj-http.client/get golden-search-url (step-authentication)))
        parsed (xml->map body)
        products (select parsed [:Product])
        ids (doall (map (fn [prod] (:ID (:attrs prod))) products))
        ]
    ids))
;; (get-golden-ids)

(defn get-product [id]
  (let [body (:body (clj-http.client/get (str product-url id "?" search-context) (step-authentication)))
        parsed (xml->map body)
        ]
    (detect parsed [:Product])
  ))
;; (get-product "MEM_GLD_101278")

(defn get-product-references [id]
  (let [product (get-product id)
        references (select product [:ProductCrossReference])
        ids (doall (map (fn [prod] (:productid (:attrs prod))) references))
        ]
    ids))
;; (get-product-references "MEM_GLD_101278")


(defn step-id->source-id [id]
  (let [tokens (reverse (str/split id #"_"))
        source-name (case (second tokens)
                      "SAP" :sap
                      "IDW" :idw
                      "TS" :ts)]
    [source-name (as-integer (first tokens))]))
;; (step-id->source-id "MEM_IDW_1933")


(defn get-golden-reference-ids []
  (let [ids (get-golden-ids)
        refs (into {} (doall (map (fn [id] [id (get-product-references id)]) ids)))]
    refs))
;; (get-golden-reference-ids)
;; (vals (get-golden-reference-ids))

(defn get-golden-references []
  (map #(into {} (map step-id->source-id %)) (vals (get-golden-reference-ids)))
  )
;; (def actual (get-golden-references))

;; (def expected (:prods (slurp-source-ids "panduit-50")))
;; expected 
;; actual 
;; (sort expected)
;; (clojure.data/diff (into (sorted-map) expected) (into (sorted-map) actual))

(defn get-matching-source-products []
  (let [refs (get-golden-references)
        matches (doall (vals refs))]
    (map #(into {} (map step-id->source-id %)) matches)))
;; (get-matching-source-products)




(defn calculate-expected-golden-match [exported match]
  (into {}
        (filter (fn [[k v]]
                  (if (contains? 
                       (case k
                         :matnr (:sap exported)
                         :idw_index (:idw exported)
                         :item_pik (:ts exported))
                       (as-integer v))
                    [k v]))
                match)))
;; (calculate-expected-golden-match @exported-products (first matches))
;; (calculate-expected-golden-match @exported-products {})

(defn calculate-expected-golden-matches [exported matches]
  (filter not-empty (map (partial calculate-expected-golden-match exported) matches)))
;; (calculate-expected-golden-matches @exported-products matches)
;; (pp (calculate-expected-golden-matches @exported-products matches))
;; (spit "junk" (with-out-str (calculate-expected-golden-matches @exported-products matches)))


(defn create-source-product-xml [distribution idw-files]
  (reset-exported-products)
  (binding [*matched-products* (set (:idw distribution))]
    (dorun (map idw/write-idw-file idw-files)))
  (binding [*matched-products* (set (:ts distribution))]
    (time (ts/write-ts-file)))
  (binding [*matched-products* (set (:sap distribution))]
    (time (sap/write-sap-file)))
  @exported-products)

(defn convert-key-names
  "convert :matrn=>:sap, etc."
  [m]
  (into {} (for [[source-name id] m]
             [(source-name {:idw_index :idw, :item_pik :ts, :matnr :sap}) id])))

(defn find-one [actual prod]
  (if (empty? prod)
    :no-prod
    (let [[source-type id] (first prod)]
      (some #(if (= (source-type %) id) %) actual))
    )
  )

(defn compare-one [expected actual]
  (if (not= expected actual)
    {:expected expected :actual actual}))

(defn compare-expected-products-aux [expected actual]
  (filter identity
          (map #(compare-one % (find-one actual %)) expected)))

(defn compare-expected-products [expected actual]
  (let [expected (map convert-key-names expected)]
    (compare-expected-products-aux expected actual)))

(defn compare-actual-products [actual expected]
  (let [expected (map convert-key-names expected)]
    (compare-expected-products-aux actual expected)))


#_(comment


)


;; (def distribution (spit-source-ids "hubbell-panduit-500" "Panduit" 500 distribution-per-100))
(def ^:dynamic *current-filename* "current")
(def distribution (spit-source-ids *current-filename* "Hubbell Wiring Device-Kellems" 5000 distribution-per-100))
(def distribution (slurp-source-ids *current-filename*))

(def exported (create-source-product-xml distribution ["Panduit14374598.csv"]))
(def exported (create-source-product-xml distribution ["HubbellWiringDevice-Kellems14490913.csv"]))

(def actual (get-golden-references))
(def expected (calculate-expected-golden-matches exported (:prods distribution)))
;; (def expected (calculate-expected-golden-matches @exported-products (:prods distribution)))

(pp (compare-expected-products expected actual))
(count (compare-expected-products expected actual))

(pp (compare-actual-products actual expected))


(let [sap (:sap (first actual))]
  (filter #(= (:matnr %) sap) expected))


(defn productfiles->usb []
  (let [from "/Users/bmd/data/stibo/output/"
        to "/Volumes/Transcend/jarred/"]
    (doseq [source ["sap" "idw" "ts"]]
      (clojure.java.shell/sh "cp" (str from source "/product.xml") (str to source "-product.xml")))))

;; (productfiles->usb)


)

;; mfr-id=124 => Hubbell Wiring Device-Kellems
(def match-by-mfr-part-num-sql
  "select j.*
from mdm.join_product j
join mdm.idw_item i on i.idw_index=j.idw_index
join mdm.sap_material s on s.matnr=j.matnr
join mdm.ts_item t on t.item_pik=j.item_pik
where i.manufacturer_id=124
   and (s.mfr_part_num!=i.catalog
   or s.mfr_part_num!=t.mfr_cat_num
   or t.mfr_cat_num!=i.catalog)
")

;; (def abc (exec-sql match-by-mfr-part-num-sql))
;; abc 
