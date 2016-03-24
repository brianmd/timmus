(ns summit.bh.mbrainzExperiment
  (require [datomic.api :refer [q db] :as d]
           [clojure.pprint :refer :all]
           [brianmd.db.store-mysql :as s]
           ))

(defn psorted [x] (pprint (sort x)))

(clojure.pprint/pprint (:attributes (:customers s/entity-definitions)))
(defn attr-names-for [tbl] (-> s/entity-definitions tbl :attributes keys))
(defn relationships-for [tbl]
  (-> s/entity-definitions tbl :relationships))
(defn attr-for [tbl attr]
  (-> s/entity-definitions tbl :attributes attr))
(attr-names-for :customers)
(relationships-for :customers)
(attr-for :customers :email)
{:max-length 255, :default "", :name :email, :type "varchar", :charset "utf8_unicode_ci", :seq 2, :entity :customers, :primary? false, :required true}
(s/entity-definitions :customers :first_name)
(keys (s/entity-definitions :customers))
(-> s/entity-definitions :customers :first_name)

(defn ->attr-name [tbl attr]
  (keyword (str (name tbl) "/" (name attr))))
;; (->attr-name :customers :id)

(defn attr-type [d]
  (case (:type d)
      ("char" "varchar") :db.type/string
      ("int") :db.type/long
      ))


(defn attr-as-datomic [tbl attr]
  (let [d (attr-for tbl attr)]
    {:db/id #db/id[:db.part/db]
     :db/doc (str (name tbl) " " (name attr))
     :db/valueType (attr-type d)
     :db/ident (->attr-name tbl attr)
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity
     :db/index true
     :db.install/_attribute :db.part/db
     }))
(attr-as-datomic :customers :first_name)

(def uri "datomic:sql://mbrainz?jdbc:mysql://localhost:3306/datomic?user=datomic_txr&password=qaDYAuNWHbU9oqN9DqaDYAuNWHbU9oqN9D")
(d/delete-database uri)
(d/create-database uri)         ;; see above command

(def conn (d/connect uri))

(def path "/Users/bmd/Downloads/mbrainz-sample/samples/seattle/")
(def schema-tx (read-string (slurp (str path "seattle-schema.edn"))))

(set! *print-length* 250)
{:db/id #db/id[:db.part/db]
 :db/ident :artist/name
 :db/valueType :db.type/string
 :db/cardinality :db.cardinality/one
 :db/fulltext true
 :db/index true
 :db/doc "The artist's name"
 :db.install/_attribute :db.part/db}
[{:db/id #db/id[:db.part/db]
  :db/ident :artist/gid
  :db/valueType :db.type/uuid
  :db/cardinality :db.cardinality/one
  :db/unique :db.unique/identity
  :db/index true
  :db/doc "The globally unique MusicBrainz ID for an artist"
  :db.install/_attribute :db.part/db}
 
 ]

;; display first statement
(first schema-tx)



;; submit schema transaction
;; @(d/transact conn schema-tx)



;; parse seed data edn file
(def data-tx (read-string (slurp (str path "seattle-data0.edn"))))

;; display first three statements in seed data transaction
(first data-tx)
(second data-tx)
(nth data-tx 2)



;; submit seed data transaction
;; @(d/transact conn data-tx)
