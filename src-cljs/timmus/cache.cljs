(ns timmus.cache
  ;; (:require-macros [cljs.core.async.macros :refer [go]
  ;;                   reagent.ratom :refer [reaction]
  ;;                   ])
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.session :as session]
            ;; [cljs.core.async.macros :refer-macros [go]]
            [ajax.core :refer [GET POST]]
            [cljs.core.async :refer [chan close!]]          ; for sleep timeout
            [reagent.core :as r]
            ;; [reagent.ratom :refer [reaction]]
            ))

(def db-cache  (r/atom {}))

{:customers {1 {:id 1 :email "a@example.com"}}}

(defn subscribe-entity [entity-type id]
  )


 

