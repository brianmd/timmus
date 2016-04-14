(ns summit.step.restapi
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [com.rpl.specter :as s :refer [ALL]]
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
            [summit.punchout.hiccup :refer :all]

            [summit.utils.core :refer :all]

            [summit.step.import.product-selectors :refer :all]
            [summit.step.import.idw.product :as idw]
            [summit.step.import.ts.product :as ts]
            [summit.step.import.sap.product :as sap]
            ))



(defn filter-tags-named [tag-name tag-value eles]
  (filter #(= tag-value (tag-name %)) eles)
  )

(defn filter-tag-named [tag-name tag-value eles]
  (first (filter-tags-named tag-name tag-value eles)))


(def manufacturers-cache (atom {}))
(def products-cache (atom {}))

(defn reset-caches []
  (reset! manufacturers-cache {})
  (reset! products-cache {}))

(def base-url "http://stibo-dev.insummit.com/restapi/")
(def base-url "http://stibo-prd-01.insummit.com/restapi/")
(def context "?context=Context1")

(set-clojurized-keyword "MfrUCC" :mfr-ucc)
(set-clojurized-keyword "MfrUCCRegistered" :mfr-ucc-registered)
(set-clojurized-keyword "MfrUCC" :mfr-ucc)

(defn step-authentication []
  {:basic-auth ["Stepsys" "stepsys"]})

(defn get-url-content [url]
  (let [url (str base-url url context)
        _   (ppn url)
        response (clj-http.client/get url (step-authentication))]
    (if (= 200 (:status response))
      (xml->map (:body response))
      nil)))


(defn entity-url [id]
  (str "entities/" id))

(defn manufacturer-url [id]
  (entity-url id))

(defn download-manufacturer-attrs [id]
  (first (get-url-content (manufacturer-url id))))

(defn parse-manufacturer-attrs [m]
  (let [attrs (clojurize-map-keywords (:attrs m))
        content (:content m)
        name (ffirst (s/select [ALL #(= (:tag %) :Name) :content] content))
        name (->> content (filter-tag-named :tag :Name) :content first)
        value-tags (->> content (filter-tag-named :tag :Values) :content)
        values (map #(vector (keyword (:AttributeID (:attrs %))) (first (:content %))) value-tags)
        ]
    {:name name
     :attrs attrs
     :specs (clojurize-map-keywords (into {} values))}
    (merge
     (clojurize-map-keywords (into {} values))
     (clojure.set/rename-keys attrs {:user-type-id :type})
     {:name name}
     )
    ))

(defn download-manufacturer-children [id]
  (get-url-content (str (manufacturer-url id) "/children")))

(defn parse-manufacturer-children [raw]
  (let [raw (:content (first raw))]
    {:golden-children 
     (s/select [ALL :attrs #(= "Golden Manufacturer" (:UserTypeId %)) :ID] raw)
     :children
     (s/select [ALL :attrs #(= "Manufacturer" (:UserTypeId %)) :ID] raw)
     })
  )

(defn manufacturer-attrs [id]
  (parse-manufacturer-attrs (download-manufacturer-attrs id)))

(defn manufacturer-children [id]
  (let [raw (download-manufacturer-children id)]
    (parse-manufacturer-children raw)))

(defn force-manufacturer [id]
  (let [a (manufacturer-attrs id)
        c (manufacturer-children id)
        m (merge a c)]
    m))

(defn force-manufacturer [id]
  (let [a (future (manufacturer-attrs id))
        c (future (manufacturer-children id))
        m (merge @a @c)]
    m))

(defn manufacturer [id]
  (if-let [m (@manufacturers-cache id)]
    m
    (let [m (force-manufacturer id)]
      (swap! manufacturers-cache assoc id m)
      m)))

(defn root-manufacturer []
  (manufacturer "Manufacturer"))

(defn golden-manufacturer? [m]
  (case (:type m)
    ("Golden Manufacturer" "Manufacturer Root") true
    false))
(examples
 (reset-caches)
 (assert (golden-manufacturer? (manufacturer "GoldenMfr8260")))
 (assert (golden-manufacturer? (manufacturer "Manufacturer")))
 (assert-false (golden-manufacturer? (manufacturer "TsMfr3534"))))
(examples
 (ppn (force-manufacturer "GoldenMfr8260"))
 (ppn (force-manufacturer "TsMfr3534"))
 (ppn (manufacturer "GoldenMfr8260"))
 (ppn (manufacturer "TsMfr3534"))
 (ppn (manufacturer "Manufacturer")))

;; (:type (manufacturer "GoldenMfr8260"))
;; (:type (manufacturer "Manufacturer"))

(examples
 (download-manufacturer-children "GoldenMfr8260")
 (ppn (download-manufacturer-children "GoldenMfr8260"))
 (ppn (root-manufacturers))
 )



(defn product-url [id]
  (str "products/" id))

(defn download-product [id]
  (first (get-url-content (product-url id))))

;; (defn product-children [id]
;;   (get-url (str (product-url id) "/children")))

(defn parse-product [p]
  (let [attrs (:attrs p)
        sources (s/select [ALL #(= (:tag %) :ProductCrossReference) :attrs #(= (:Type %) "GoldenRecord") :ProductID] (:content p))]
    {:id (:ID attrs)
     :parent-id (:ParentID attrs)
     :source-ids sources
     ;; :attrs attrs
     ;; :raw p
     })
  )
(examples
 (def ppp (download-product "MEM_GLD_102633"))
 (parse-product ppp))


(defn product [id]
  (let [p (download-product id)]
    (parse-product p)))

(examples
 (def ppp (download-product "MEM_GLD_102633"))
 (ppn (download-product "Unclassified_Golden_Records"))
 (ppn (product "MEM_GLD_102633"))
 )
