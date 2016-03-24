(ns summit.health-check.blue-harvest)

(def bh-ports (range 7300 7312))

(defn get-bh-index-page [port]
  (slurp (str "http://blue-harvest.prod:" port "/")))

(defn find-brown-truck-version [port]
  (let [content (get-bh-index-page port)]
    (last (re-find #"\"/brown-truck/brown-truck-(.*).css\"" content))))

(defn instances-same? []
  (= 1 (->> bh-ports (pmap find-brown-truck-version) set count)))

(instances-same?)
;; (map find-brown-truck-version bh-ports)
;; (def versions (map find-brown-truck-version bh-ports))
;; (set versions)  ;; should contain exactly one version

