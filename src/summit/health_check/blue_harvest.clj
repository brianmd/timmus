(ns summit.health-check.blue-harvest
  (require [
            summit.utils.core :refer :all
            ])
  (:require [clojure.string :as str]))

(def ^:private bh-ports (range 7300 7312))

(defmacro bh-ssh [& args]
  `(ssh "blue-harvest.prod" ~@args))

(defn- get-bh-index-page [port]
  (slurp (str "http://blue-harvest.prod:" port "/")))

(defn- find-brown-truck-version [port]
  (let [content (get-bh-index-page port)]
    (last (re-find #"\"/brown-truck/brown-truck-(.*).css\"" content))))

(defn instances-same? []
  (= 1 (->> bh-ports (pmap find-brown-truck-version) set count)))


;; (map find-brown-truck-version bh-ports)
;; (def versions (map find-brown-truck-version bh-ports))
;; (set versions)  ;; should contain exactly one version



(defn- resque-worker-processes []
  (let [processes (bh-ssh "ps -aux" {:seq true})
        resque-processes (filter #(if (re-find #"resq.*Waiting for production" %) %) processes)
        ]
    (map #(second (str/split % #"\s+")) resque-processes)
    ))
;; (pp (resque-worker-processes))

(defn- resque-worker-okay? []
  (= (count (resque-worker-processes)) 1))


(defn restart-resque-worker []
  (doseq [p-id (resque-worker-processes)]
    (bh-ssh (str "kill " p-id)))
  (bh-ssh "rm /var/www/apps/blue-harvest/current/tmp/pids/resque_worker_mailqueue.pid")
  (pp "Wait for monit to restart resque. Could take up to five minutes."))




(defn all-okay? []
  (let [result {
                :bh-ports-same? (instances-same?)
                :resque-worker-okay? (resque-worker-okay?)
                }]
   (assoc result :all-okay? (every? identity (vals result)))))

(examples
 (all-okay?)
 )

