(ns summit.step.import.ts.a-meta
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]

            [summit.utils.core :refer :all]
            [summit.step.xml-output :refer :all]

            [summit.step.import.core :refer :all]
            [summit.step.import.ts.core :refer :all]
            [summit.step.import.product-selectors :refer :all]
            ))

(def a-meta-filename (str ts-input-path "a_meta.txt"))
(def a-value-filename (str ts-input-path "a_value.txt"))
(def a-spec-filename (str ts-input-path "a_attribute.txt"))

(defn ts-spec-definitions! []
  (let [specs (atom #{})]
    (into {}
          (transduce-tabbed-file-with
           a-meta-filename
           (comp
            (drop 1)
            ;; (take 5)
            ;; (map #(swap! names conj (nth % 2)))
            (map (fn [x]
                   (let [h (humanize (nth x 2))
                         id (humanized->id h)]
                     [id {:source :ts :id id :source-id (first x) :title h
                          :descript (nth x 3) :type (nth x 4)}])))
            )
           conj))))

(defn ts-spec-values! []
  (let [specs (atom {})]
    (transduce-tabbed-file-with
     a-value-filename
     (comp
      (drop 1)
      ;; (take 5)
      ;; (map #(swap! names conj (nth % 2)))
      (map (fn [x] (swap! specs assoc (->int (nth x 0)) (nth x 2))))
      )
     (fn [& args]))
    (println "done")
    @specs
    ))

(defonce ts-spec-definitions (delay (ts-spec-definitions!)))
(defonce ts-spec-definitions-by-source-id (delay (into {} (map! #(vector (->int (:source-id %)) (:id %)) (vals @ts-spec-definitions)))))
(defonce ts-spec-values (delay (ts-spec-values!)))

;; (pp (first @ts-spec-definitions))
;; (pp (first @ts-spec-definitions-by-source-id))
;; (pp (first @ts-spec-values))
;; (count @ts-spec-definitions)
;; (count @ts-spec-values)

(defn- ts-spec-hiccup [[id val]]
  [:Value
   {:AttributeID (str "SP_" (@ts-spec-definitions-by-source-id (->int id)))}
   (escape-html (@ts-spec-values (->int val)))])

(defn- ts-product-hiccup [item]
  ;; (let [parent-id (let [p (:unspsc item)]
  ;;                   (if (or (nil? p) (= p ""))
  ;;                     "TS_Member_Records"
  ;;                     (str "TS_" p)))]
    [:Product
     {:ID (str "MEM_TS_" (:id item))
      ;; :UserTypeID "TS_Member_Record"
      ;; :ParentID parent-id
      }
     [:Values
      (map! ts-spec-hiccup (:specs item))
      ]
     ])

(defn- ts-print-product-specs-xml [item]
  (if (and (:id item) (not-empty (:specs item)))
    (println
     (hiccup/html (ts-product-hiccup item))
     )))

(defn- ts-product-xml
  ([] (atom {:id nil :specs []}))
  ([accum]
   (ts-print-product-specs-xml @accum)
   accum)
  ([accum item]
   (if (= (:id @accum) (first item))
     (swap! accum update-in [:specs] conj [(second item) (third item)])
     (do
       (ts-print-product-specs-xml @accum)
       (swap! accum assoc :id (first item) :specs [[(second item) (third item)]]))
     )
   accum)
   )



(defn write-ts-spec-values []
  (let [full-filename (str ts-output-path "specs.xml")
        curr-item-pik (atom nil)
        curr-specs (atom {})]
    (make-parents full-filename)
    (with-open [w (clojure.java.io/writer full-filename)]
      (binding [*out* w]
        (println (opening))
        ;; (println "<Products>")
        (transduce-tabbed-file-with
         a-spec-filename
         (comp
          (drop 1)
          (take 25)
          (map #(vector (nth % 6) (nth % 2) (nth % 3)))

          )
         ts-product-xml
         ;; (sans-accumulator pp)
         )
        ;; (process-ts-file-with a-spec-filename process-ts-product)
        (println (closing))
        ))))

;; (write-ts-spec-values)
