(ns summit.neo.core
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nodes]
            [clojurewerkz.neocons.rest.labels :as labels]
            [clojurewerkz.neocons.rest.relationships :as relations]
            [clojurewerkz.neocons.rest.cypher :as cypher]
            [clojurewerkz.neocons.rest.batch :as batch]

            [korma.core :as k]

            [config.core :refer [env]]

            [summit.utils.core :refer :all]
            ))

(defn add-label
  ([conn label] (fn [node] (labels/add conn node label)))
  ([conn label node]
   (labels/add conn node label)))

(defn create-node [conn label m]
  (let [n (nodes/create conn m)]
    (add-label conn label n)
    n))

(defn create-nodes [conn label coll]
  (map! (partial create-node conn label) coll))
  ;; (let [node-coll (nodes/create conn coll)]
  ;;   (maprun (add-label conn label) coll)
  ;;   ))

;; (create-node conn "Junk" {:howdy "I'm a little teapot"})
;; (create-node conn "Junk" {:howdy "I'm a little teapot"})
;; (ppn (cypher/tquery conn "MATCH (n:Junk) delete n"))
;; (create-nodes conn "Junk" [{:id 1 :title "Teapot 1" :type :junk} {:title "Teapot 2"}])
;; (pp (cypher/tquery conn "MATCH (n:Junk) RETURN n LIMIT 25"))
;; (pp (->> (cypher/tquery conn "MATCH (n:Junk) RETURN n LIMIT 25") (map (comp vals))))



;; !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

;; (pp (->> (cypher/tquery conn "MATCH (n:Junk) RETURN n LIMIT 25") (map (comp #(select-keys % [:metadata :data]) first vals))))

;; (cypher/tquery conn "MATCH (n:Junk) RETURN n.title LIMIT 2")
;; (cypher/tquery conn "MATCH (n:Product)-[:BOUGHT_WITH]-(e) RETURN n.matnr,e.matnr LIMIT 2")
;; (cypher/tquery conn "MATCH (n:Product)-[r:BOUGHT_WITH]-(e) RETURN n.matnr,e.matnr,r LIMIT 2")

(defn parse-element [m]
  (println "\n\n" (now))
  (println m)
  (let [types (pp (keys m))
        _ (if-not (= (count types) 1) (throw (Exception. (str "too many keys " (keys m)))))
        _ (println "before first")
        type (pp (first types))
        _ (println "after first")
        m (pp (m type))
        result (select-keys m [:metadata :data])
        type (case type
               "n" :node
               "r" :relationship
               type)]
    (assoc-in result [:metadata :type] type)))
;; (cypher-query "match (n:Junk) where not (n)-[]-() return n limit 2")
;; (cypher-query "match (n:Product)-[r]-() return n,r limit 25")
;; (println "\n\n***********************************")
;; (ppn (cypher-query "match (n:Product {matnr: 2761096})-[r]-(e) return n,r,e limit 25"))

(defn cypher-query
  ([query] (cypher-query :default query))
  ([conn query]
   (map identity (cypher/tquery conn query))
   ;; (map parse-element (cypher/tquery conn query))
   ))

;; (cypher-query conn "match (n:Junk-[r]-()) return n,r")






;; (def neo-user (-> env :db :neo4j-prd :user))
;; (def neo-pw (-> env :db :neo4j-prd :password))
;; (def conn (nr/connect (str "http://" neo-user ":" neo-pw "@localhost:7474/db/data/") neo-user neo-pw))

;; ;; (def node (nodes/create conn {:url "http://clojureneo4j.info" :domain "clojureneo4j.info"}))

;; ;; ;; (cy/tquery conn "START person=node({sid}) MATCH person-[:friend]->friend RETURN friend" {:sid (:id amy)})

;; (def q "MATCH (n:Product) RETURN n LIMIT 5")
;; (ppn (cypher/tquery conn q))

;; (println 3)
;; (def node1 (nodes/create conn {:url "http://clojureneo4j.info" :domain "clojureneo4j.info"}))

;; (nodes/create conn
;;               [:TestNode
;;                {:xid 77 :name "Test 1"}
;;                 ])

;; ;; ;; (ppn conn)

;; (def cust-node (nodes/create conn
;;                           [:Customer
;;                            (into {}
;;                                  (filter (fn [[k v]] (if v v))
;;                                          (clojurize-map (clean-all (first (dselect :customers (k/limit 1)))))))]

;; (def cust-node (nodes/create conn
;;                           (into {}
;;                                 (filter (fn [[k v]] (if v v))
;;                                         (clojurize-map (clean-all (first (dselect :customers (k/limit 1)))))))
;;                           ))
;; cust-node 


;; (let [conn  (nr/connect "http://localhost:7474/db/data/")
;;       amy   (nodes/create conn {:username "amy"})
;;       bob   (nodes/create conn {:username "bob"})
;;       rel   (relations/create conn amy bob :friend {:source "college"})]
;;   (ppn (nodes/get conn (:id amy)))
;;   (ppn (nodes/get conn (:id bob))))

;; (clojurize-map (clean-all (first (dselect :customers (k/limit 1)))))

;; (clojurize-keyword "default_ser")
