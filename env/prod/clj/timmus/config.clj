(ns timmus.config
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[timmus started successfully]=-"))
   :middleware identity})
