(ns brianmd.db.experiments
  (:require [gyptis.core :refer :all]
            [gyptis.vega-templates :refer [vertical-x-labels] :as vt]
            ;[gyptis.view :refer [plot!]]
            [gyptis.view :refer :all]
            [clojure.data.json :as json]
            ))

;(:require [gyptis.view :refer [plot!]]
;             [gyptis.core :refer :all]
;             ))

(def data [{:x "n=2", :y 1 :fill "n-1"}
           {:x "n=2", :y 0 :fill "n-2"}
           {:x "n=3", :y 1 :fill "n-1"}
           {:x "n=3", :y 1 :fill "n-2"}
           {:x "n=4", :y 2 :fill "n-1"}
           {:x "n=4", :y 1 :fill "n-2"}
           {:x "n=5", :y 3 :fill "n-1"}
           {:x "n=5", :y 2 :fill "n-2"}
           {:x "n=6", :y 5 :fill "n-1"}
           {:x "n=6", :y 3 :fill "n-2"}])

;(plot! (stacked-bar data))


