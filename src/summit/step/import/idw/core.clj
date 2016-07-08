(ns summit.step.import.idw.core
  (:require [clojure.string :as str]
            [summit.utils.core :refer :all]
            ))

(def idw-input-path (str step-input-path "idw/"))
(def idw-output-path (str step-output-path "idw/"))
