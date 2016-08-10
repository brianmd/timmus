(ns summit.sap.routes
  (:require [compojure.core :refer [defroutes routes wrap-routes]]
            ;; [timmus.layout :refer [error-page]]
            ;; [timmus.routes.home :refer [home-routes]]
            ;; [timmus.routes.services :refer [service-routes]]
            ;; [timmus.routes.punchout :refer [punchout-routes]]
            ;; [timmus.middleware :as middleware]
            ;; [clojure.tools.logging :as log]
            ;; [compojure.route :as route]
            ;; [config.core :refer [env]]
            ;; [mount.core :as mount]
            ;;                             ; [ring.middleware.cors :refer [wrap-cors]]
            ;; [luminus.logger :as logger]

            [compojure.core :refer [context routes]]
            [argo.core :refer [defapi defresource]]

            [clojure.core.memoize :as memo]
            [clojure.core.cache :as cache]

            [summit.sap.project :as project]

            [summit.utils.core :refer :all]
            ;; [timmus.config :refer [defaults]]
            ;; [timmus.routes.websockets :refer [websocket-routes]]
            ))

;; (def id (memo/ttl identity {} ))
;; (def id (memo/ttl identity {} :ttl/threshod 1))
;; (id 42)
;; (memo/snapshot id)

;; (do
;;   (def C (cache/ttl-cache-factory {:q 5 :r 2} :ttl 2000))
;;   (def D (assoc C :s 12))
;;   (def E (assoc D :t 14))
;;   (assoc E :aa 2)
;;   (cache/has? C :q)
;;   )
;; (cache/hit C :q)

;; (def sleepy #(do (Thread/sleep %2) %))

;; (-> C (assoc :a 1)
;;     (assoc :b 2)
;;     (sleepy 2500)
;;     (assoc :c 3))


;; (assoc C :a 5)
;; (assoc C :b 2)
;; (def q (-> C (assoc :a 1) (assoc :b 2)))
;; [q C]
;; (type q)


(def ^:private prefix "/sap")

;; (defresource heroes {})
;; (defapi json-api {:resources [heroes]})
;; json-api

(defn get-all []
  (println "get-all")
  [{:id 1 :a 3 :b 4} {:id 2 :q 99 :r 98}])

(defn get-one [id]
  (println "get-one" id)
  {:id id :name "Santa Claus" :street "1 North Pole"})

(defn validate-new [body]
  nil)

(defn neww [body]
  {:new 3 :body 4})

(defn validate-update [body]
  (println "validating update " body)
  nil)

(defn update [id body]
  (println "updating " id body)
  )

(defn delete [id]
  (println "deleting " id))

(defresource heroes
  {
   :find (fn [req]
           {:data (get-all)})

   :get (fn [req]
          {:data (get-one (-> req :params :id))})  ; will raise 404 if :data is nil

   :create (fn [req]
             (if-let [errors (validate-new (:body req))]
               {:errors errors}
               {:data (neww (:body req))}))

   :update (fn [req]
             (if-let [errors (validate-update (:body req))]
               {:errors errors}
               {:data (update (-> req :params :id) (:body req))}))

   :delete (fn [req]
             (when-let [errors (delete (-> req :params :id))]
               {:errors errors}))
   })

(defresource projects
  {
   ;; :primary-key :project-name

   :find (fn [req]
           (ppn "getting all projects")
           (let [account-num (-> req :params :filter :account)
                 result (project/projects account-num)]
             (ppn "got em")
             (ppn req)
             (ppn "params" (-> req :params))
             {
              :links {:self (str prefix "/projects")}
              :data result
              })
           )

   :get (fn [req]
          (ppn "getting one")
          (let [id (-> req :params :id)
                ;; id 1
                result (:data (project/project id))]
            (ppn "result" result)
            (ppn "id" (-> req :params :id))
            {:data result})
            ;; {:data (first result)})
            ;; {:data {:a 4}})
          )
   })

(alter-var-root #'argo.core/base-url (fn [_#] prefix))
(def sap-routes
  (routes (context prefix [] heroes projects))
  )

(ppn "prefix" prefix)
(ppn "done loading sap.routes")
(ppn sap-routes)
;; (defapi sap-routes
;;   {:base-url "/sap"
;;    :resources [heroes]
;;    })
