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

(defn ts-attributes! []
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

(def ts-attributes (delay (ts-attributes!)))

