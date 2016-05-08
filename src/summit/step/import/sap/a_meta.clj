(ns summit.step.import.sap.a-meta
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

(defn extract-name [v]
  (nth v 2))

(defn distinct-names []
  (println "\n\n\n________________________________________________\n" (now))
  (let [names (atom #{})]
    (process-tabbed-file-with
     a-meta-filename
     (fn [lines]
       (transduce
        (comp
         (drop 1)
         ;; (take 5)
         ;; (map pp)
         (map (fn [row] (swap! names conj (nth row 2))))
         ;; (map (fn [& args] (prn args) (swap! names conj (nth (first args) 2))))
         )
        (fn [& row])
        ;; (fn [& row] (swap! names conj #(nth % 2)))
        lines)))
    names))

(defn distinct-names []
  (let [names (atom #{})]
    (transduce-tabbed-file-with
     a-meta-filename
     (comp
      (drop 1)
      ;; (take 5)
      (map #(swap! names conj (nth % 2)))))
    @names))

;; (def names (time (distinct-names)))
(defn write-distinct-names []
  (with-open [w (clojure.java.io/writer (str ts-output-path "spec-names.xml"))]
    (binding [*out* w]
      (maprun! println (sort (distinct-names))))))
;; (write-distinct-names)

;; (count @names)
;; (sort @names)


;; (def xf (comp (filter odd?) (map inc) (take 2)))
;; (transduce xf println (range 10))
;; (transduce xf + 100 (range 5))
