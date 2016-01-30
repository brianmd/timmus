(ns timmus.config
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [timmus.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[timmus started successfully using the development profile]=-"))
   :middleware wrap-dev})
