;; ;; https://github.com/liebke/zookeeper-clj

;; (ns brianmd.zookeeper
;;   (:require [zookeeper :as zk]
;;             [zookeeper.util :as util]
;;             [zookeeper.data :as data]
;;             )
;;   (:import java.net.InetAddress))

;; (def client (zk/connect "127.0.0.1:2181"))
;; ;;    OR
;; (def client (zk/connect "127.0.0.1" :watcher (fn [event] (println event))))
;; ;;    OR
;; # to start tunnel: grutunnel 2182 2181 21820
;; (def client (zk/connect "127.0.0.1:2182" :watcher (fn [event] (println event))))

;; ;; basically log in. see acl section for more options
;; (zk/add-auth-info client "digest" "david:secret")

;; (zk/create client "/parent-node" :persistent? true)
;; (zk/exists client "/parent-node")



;; (def result-promise (zk/create client "/parent-node3" :persistent? true :async? true))
;; @result-promise 
;; (zk/exists client "/parent-node3")


;; (zk/create client "/parent-node/child-node")
;; (zk/exists client "/parent-node/child-node")
;; (zk/children client "/parent-node")

;; ;; create-all is like mkdirp
;; (zk/create-all client "/parent/child-" :sequential? true)
;; (zk/create-all client "/parent/child-" :sequential? true)
;; (zk/exists client "/parent")
;; (zk/children client "/parent")
;; (first (zk/children client "/parent"))
;; (zk/delete client (str "/parent/" (first (zk/children client "/parent"))))
;; (zk/delete-all client "/parent")

;; (util/extract-id (first (zk/children client "/parent")))
;; (util/sort-sequential-nodes (zk/children client "/parent"))


;; (def version (:version (zk/exists client "/parent")))

;; (zk/set-data client "/parent" (.getBytes "hello world" "UTF-8") version)
;; (zk/data client "/parent")
;; (String. (:data (zk/data client "/parent")) "UTF-8")

;; (zk/set-data client "/parent" (data/to-bytes (pr-str {:a 1, :b 2, :c 3})) version)
;; (read-string (data/to-string (:data (zk/data client "/parent"))))


;; ;;;;;;;;;;;;;;;;;;;;;;;;;;;   ACL

;; ;; scheme, id value, and a set of permissions
;; (zk/acl "world" "anyone" :read :create :delete :admin :write)
;; (zk/acl "ip" "127.0.0.1" :read :create :delete :admin :write)
;; (zk/acl "host" "thinkrelevance.com" :admin :read :write :delete :create)
;; (zk/acl "auth" "" :read :create :delete :admin :write)

;; (zk/world-acl :read :delete :write)
;; (zk/ip-acl "127.0.0.1")
;; (zk/digest-acl "david:secret" :read :delete :write)
;; (zk/host-acl "thinkrelevance.com" :read :delete :write)
;; (zk/auth-acl :read :delete :write)

;; ;; only user has permissions on protected-node
;; (zk/create client "/protected-node" :acl [(zk/auth-acl :admin :create :read :delete :write)])



;; ;;;;;;;;;;;;;;;;;;;;;;;;;;   Group Membership

;; (def group-name "/example-group")

;; (when-not (zk/exists client group-name)
;;   (zk/create client group-name :persistent? true))

;; (defn group-watcher [x]
;;   (let [group (zk/children client group-name :watcher group-watcher)]
;;     (prn "Group members: " group)))

;; (defn join-group [name]
;;   (do (zk/create client (str group-name "/" name))
;;       (zk/children client group-name :watcher group-watcher)))

;; (join-group "bob")
;; (join-group "betty")



;; ;;;;;;;;;;;;;;;;;;;;;;;;;;   Leader Election

;; (def root-znode "/election")

;; (when-not (zk/exists client root-znode)
;;   (zk/create client root-znode :persistent? true))

;; (defn node-from-path [path]
;;   (.substring path (inc (count root-znode))))

;; (declare elect-leader)

;; (defn watch-predecessor [me pred leader {:keys [event-type path]}]
;;   (if (and (= event-type :NodeDeleted) (= (node-from-path path) leader))
;;     (println "I am the leader!")
;;     (if-not (zk/exists client (str root-znode "/" pred)
;;                        :watcher (partial watch-predecessor me pred leader))
;;       (elect-leader me))))

;; (defn predecessor [me coll]
;;   (ffirst (filter #(= (second %) me) (partition 2 1 coll))))

;; (defn elect-leader [me]
;;   (let [members (util/sort-sequential-nodes (zk/children client root-znode))
;;         leader (first members)]
;;     (print "I am" me)
;;     (if (= me leader)
;;       (println " and I am the leader!")
;;       (let [pred (predecessor me members)]
;;         (println " and my predecessor is:" pred)
;;         (if-not (zk/exists client (str root-znode "/" pred)
;;                            :watcher (partial watch-predecessor me pred leader))
;;           (elect-leader me))))))

;; (defn join-group []
;;   (let [me (node-from-path (zk/create client (str root-znode "/n-") :sequential? true))]
;;     (elect-leader me)))

;; (join-group)



;; (InetAddress/getLocalHost)
;; (.getCanonicalHostName (InetAddress/getLocalHost))

;; (println 3)
;; (zk/close client)

