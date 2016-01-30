(ns timmus.db.core
  (:require
    [clojure.java.jdbc :as jdbc]
    ;[conman.core :as conman]
    [config.core :refer [env]]
    [mount.core :refer [defstate]]
    [korma.core :refer :all]
    ;[korma.db :refer [create-db default-connection]]
    [korma.db :refer :all]
    )
  (:import [java.sql
            BatchUpdateException
            PreparedStatement])
  )

#_(def pool-spec
  {:adapter    :mysql
   :init-size  1
   :min-idle   1
   :max-idle   4
   :max-active 32})

;(defn connect! [] (create-db db-spec))

(defn connect! []
  (let [dbconfig (-> env :db :blue-harvest :local)]
    (defdb db (mysql {:db (:dbname dbconfig) :user (:user dbconfig) :password (:password dbconfig)}))
    ))

(defstate ^:dynamic *db*
          :start (connect!))

(default-connection *db*)

#_(defn connect! []
  (let [conn (atom nil)]
    (conman/connect!
      conn
      (assoc
        pool-spec
        :jdbc-url (env :database-url)))
    conn))

#_(defn disconnect! [conn]
  (conman/disconnect! conn))

(defstate ^:dynamic *db*
          :start (connect!)
          ;:stop (disconnect! *db*)
          )

;(conman/bind-connection *db* "sql/queries.sql")

(defn to-date [sql-date]
  (-> sql-date (.getTime) (java.util.Date.)))

(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Date
  (result-set-read-column [v _ _] (to-date v))

  java.sql.Timestamp
  (result-set-read-column [v _ _] (to-date v)))

(extend-type java.util.Date
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (java.sql.Timestamp. (.getTime v)))))

