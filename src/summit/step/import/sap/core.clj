(ns summit.step.import.sap.core
  (:require [clojure.string :as str]
            [summit.utils.core :refer :all]
            ))

(def sap-input-path (str step-input-path "sap/"))
(def sap-output-path (str step-output-path "sap/"))
