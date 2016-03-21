(ns summit.step.import.ts.core
  (:require [clojure.string :as str]
            ;; [clojure.java.io :as io :refer [as-url make-parents]]

            ;; [clojure.data.xml :as xml]
            ;; [clojure.xml :as x]
            ;; [hiccup.core :as hiccup]
            ;; [clojure.core.reducers :as r]

            ;; [clojure.data.csv :as csv]
            ;; [clojure.java.io :as io]
            ;; [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            ;; [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            ))

(def ts-input-path (str step-input-path "ts/"))
(def ts-output-path (str step-output-path "ts/"))

