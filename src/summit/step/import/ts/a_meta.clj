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
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.ts.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]
            ))

(def a-meta-filename (str ts-input-path "a_meta.txt"))
(def a-value-filename (str ts-input-path "a_value.txt"))
(def a-spec-filename (str ts-input-path "a_attribute.txt"))

(defn ts-spec-definitions! []
  (let [attrs (atom #{})]
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
  (let [attrs (atom {})]
    (transduce-tabbed-file-with
     a-value-filename
     (comp
      (drop 1)
      ;; (take 5)
      ;; (map #(swap! names conj (nth % 2)))
      (map (fn [x] (swap! attrs assoc (->int (nth x 0)) (nth x 2))))
      )
     (fn [& args]))
    (println "done")
    @attrs
    ))

(def ts-spec-definitions (delay (ts-spec-definitions!)))
(def ts-spec-values (delay (ts-spec-values!)))

;; (pp (first @ts-spec-definitions))
;; (count @ts-spec-definitions)
;; (count @ts-spec-values)

(defn- ts-spec-hiccup [id val]
  [:Value {:SpecID id} val])

(defn- ts-product-hiccup [item]
  (let [parent-id (let [p (:unspsc item)]
                    (if (or (nil? p) (= p ""))
                      "TS_Member_Records"
                      (str "TS_" p)))]
    [:Product
     {:ID (str "MEM_TS_" (:item-pik item))
      ;; :UserTypeID "TS_Member_Record"
      ;; :ParentID parent-id
      }
     [:Values
      (map! ts-spec-hiccup (:specs item))
      ]
     ]))

(defn- ts-product-xml [item]
  (println
   (hiccup/html (ts-product-hiccup item)))
  item)

(defn- ts-product-xml [item]
  (println
   (hiccup/html (ts-product-hiccup item)))
  item)



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
          (take 5)
          (map #(vector (nth % 6) (nth % 2) (nth % 3)))

          )
         ;; ts-product-xml
         (sans-accumulator println)
         )
        ;; (process-ts-file-with a-spec-filename process-ts-product)
        (println (closing))
        ))))

(write-ts-spec-values)
