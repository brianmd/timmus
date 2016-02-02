(ns timmus.step.sap
  (:require [clojure.string :as str]
            [config.core :refer [env]]
            [clojure.java.io :as io :refer [as-url make-parents]]
            [clojure.data.xml :as xml]

            [timmus.utils.core :refer :all]
            ))

(def step-input-path (-> env :paths :local :step-input-path))
(def step-output-path (-> env :paths :local :step-output-path))

