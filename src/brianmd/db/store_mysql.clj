(ns brianmd.db.store-mysql
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.jdbc :as sql]
    [clojure.core.async
     :as a
     :refer [>! <! >!! <!! go chan buffer close! thread
             alts! alts!! timeout]]
    [korma.db :refer :all]
    [korma.core :refer :all]
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]

    [timmus.utils.core :refer :all]
    [timmus.db.core :refer :all]
    [timmus.db.core :refer [*db*]]
    )
  )

;;;;;;;   NOTE:  have set :naming in db.core to convert all attribute names to lowercase.
;;;;;;;          if revert this, will need to uppercase TABLE_NAME and COLUMN_NAME below

;(type *db*)
;*db*
;(exec-raw ["SELECT * FROM customers WHERE id > ?" [5]] :results)
;(exec-raw ["SELECT * FROM customers WHERE id=28"] :results)
;(select :customers (modifier "DISTINCT") (fields :state))
;(select :customers (fields :state "count(*) cnt") (group :state) (order "cnt"))
;(select :customers (fields :state "count(*) cnt") (group :state) (order :state :desc))
;(select :customers (fields :state "count(*) cnt") (group :state) (order :cnt :desc))
;(select :customers (fields :id) (limit 5))

(defentity schema-entities
           (table "information_schema.tables"))
(defentity schema-attributes
           (table "information_schema.columns"))
(defentity key-attribute-usage
           (table "information_schema.key_column_usage"))

(defn current-db-name []
  (-> *db* :pool deref :datasource .getJdbcUrl (str/split #"/") last)
  )
(defn get-dbname-entityname [full-entity-name]
  (let [tokens (str/split full-entity-name #"\.")
        dbname (if (> (count tokens) 1) (first tokens) (current-db-name))]
    (println tokens)
    [dbname (last tokens)]))

;(select schema-attributes (limit 1))
;(select schema-entities (limit 1))
(defn entities-for*
  ([] (entities-for* (current-db-name)))
  ([dbname]
   (->
     (select* schema-entities)
     (where {:table_schema dbname}))))
(defn entities-for
  ([] (entities-for (current-db-name)))
  ([dbname]
   (select (entities-for* dbname))))
(defn entity-names-for
  ([] (entity-names-for (current-db-name)))
  ([dbname]
   (->>
     (select (-> (entities-for* dbname) (fields :table_name)))
     (map :table_name))))

(defn attributes-for* [full-entity-name]
  (let [[dbname entity-name] (get-dbname-entityname full-entity-name)]
    (->
      (select* schema-attributes)
      (where {:table_schema dbname :table_name entity-name})
      )))
(defn attributes-for [entity-name]
  (select (attributes-for* entity-name)))
(defn attribute-names-for [entity-name]
  (->>
    (select (-> (attributes-for* entity-name) (fields :column_name)))
    (map :column_name)))

;const fieldTypeString = function() {
;const fieldTypes = {
;                    1: 'tinyint',
;                    3: 'int',
;                    4: 'float',
;                    5: 'double',
;                    8: 'bigint',
;                    10: 'date',
;                    11: 'time',
;                    253: 'string',
;                    12: 'datetime',
;                    246: 'decimal',
;                    252: 'text',  // or mediumblob
;                    254: 'char'
;                    };
;
;return function(fieldTypeNum, name, flags) {


(entities-for "blue_harvest_dev")
;(entity-names-for "blue_harvest_dev")
;(entity-names-for)
;(entity-names-for "information_schema")
;(attribute-names-for "account_catalogs")
;(attribute-names-for "information_schema.attributes")
;(attributes-for "customers")
;(def z (first (filter #(= "address1" (:attribute_name %)) (attributes-for "customers"))))
;(def y (first (filter #(= "id" (:attribute_name %)) (attributes-for "customers"))))
;(def x (first (filter #(= "email" (:attribute_name %)) (attributes-for "customers"))))
;z
;(attribute-defs-for "information_schema.attributes")

;(filter #(= "latitude" (:attribute_name %)) (attributes-for "service_centers"))
;(attribute-defs-for "service_centers")
(defn attribute-def [col]
  ;(println "-------" (:attribute_name col))
  (let [col-type-tokens (str/split (:column_type col) #"[(),]")
        max-length    (or (second col-type-tokens) (:character_maximum_length col))
        ;a (println "max" max-length "tokens" col-type-tokens (type max-length))
        all {
             :name       (keyword (:column_name col))
             :type       (:data_type col)
             ;:type       (first col-type-tokens)
             :max-length (if max-length (if (= String (type max-length)) (Long. max-length) max-length))
             :default    (:column_default col)
             :required   (= "NO" (:is_nullable col))
             :seq        (:ordinal_position col)
             :entity      (keyword (:table_name col))
             :primary?       (= "PRI" (:column_key col))
             }
        ;b (println "all" all)
        others (partition 2 [:collation_name :charset
                             :column_comment :comment
                             :numeric_precision :precision
                             :numeric_scale :scale
                             :extra :extra
                             ])
        good-others (filter #(let [v ((first %) col)]
                               (and (not= "" v) (not (nil? v))))
                            others)
        ]
    ;(println "good" good-others)
    ;(println "g2" (map #(vector (second %) ((first %) col)) good-others))
    (into
      all
      ;(doall
        (map #(vector (second %) ((first %) col)) good-others))
    ))

(defn attribute-defs-for [entity-name]
  (into {}
        (map #(vector (:name %) %)
             (map attribute-def
                  (take 9999
                        (select (attributes-for* entity-name)))))))

;(select
;  (attributes-for* "customers"))
;(attribute-defs-for "customers")

(defn database-attribute-defs
  ([] (database-attribute-defs (current-db-name)))
  ([dbname]
    (into {}
          (map #(vector (keyword %) {:attributes (attribute-defs-for (str dbname "." %))}) (entity-names-for dbname)))))
;(database-attribute-defs)
;(:customers (database-attribute-defs))
;(attribute-defs-for "service_center_types")
;(take 3
;      (select (attributes-for* "customers"))) ))
;(attribute-defs-for "customers")
;(take 1 (drop 5
;              (attributes-for "information_schema.attributes")))
;
;(attribute-defs-for "information_schema.entities")
;(attribute-defs-for "manufacturers")
;
;(attribute-defs-for "mdm.ts_item")
;(s "message")
;(s "id")
;(s "company")
;x
;y
;z
;(Long. 4294967295)
;(type 4294967295)
;(defn s [name]
;  (let [
;        c (first (filter #(= name (:attribute_name %)) (attributes-for "contact_emails")))
;        ]
;    (pprint c)
;    (attribute-def c)
;    ))
;*e
;
;(pprint x)
;  (filter #(= "is_internal" (:attribute_name %)) (attributes-for "customers"))
;  )
;(filter #(= "is_internal" (:attribute_name %)) (attributes-for "customers"))
;(attribute-def z)
;(attribute-def y)
;
;(str/split "vvvv(aaaa)" #"[()]")
;(:attribute_type z)
;(map :attribute_type (attributes-for "customers"))

;*db*
;(current-db-name)
;(-> *db* :pool deref :datasource .getJdbcUrl (str/split #"/") last)


(defn ids-for [entity-name]
  (->>
    (attribute-names-for entity-name)
    (filter #(re-find #"_id$" (str %)))
    (filter #(not (= :source_id %)))                        ; in our use, source_id points to an external id, not internal
    ))
;(ids-for "blue_harvest_dev.attr_labels")
;(ids-for "attr_labels")
;(ids-for "mdm.ts_item")
;
;(select key-attribute-usage (limit 2))
;
;(select key-attribute-usage
        ;(where {:entity_schema "mdm" :referenced_entity_name [not= nil]})
        ;(fields :constraint_name :entity_name :referenced_entity_name :attribute_name :referenced_attribute_name :ordinal_position)
        ;(order :constraint_name)
        ;(order :ordinal_position)
        ;)
(defn relationships
  ([] (relationships (current-db-name)))
  ([dbname]
   (let [constraints
         (select key-attribute-usage
                 ; indexes have null, foreign keys have ids
                 (where {:table_schema dbname :referenced_table_name [not= nil]})
                 (fields :constraint_name :entity_name :referenced_table_name :column_name :referenced_column_name :ordinal_position)
                 (order :constraint_name)
                 (order :ordinal_position)
                 )
         one2many (atom {})
         add-constraint (fn [c]
                          (let [
                                ;key [(:entity_name c) (:referenced_entity_name c)]
                                key (:constraint_name c)
                                tbls [(:table_name c) (:referenced_table_name c)]
                                cols [(:column_name c) (:referenced_column_name c)]
                                ;v [(:attribute_name c) (:referenced_attribute_name c)]
                                v [(:table_name c) (:referenced_table_name c) (:column_name c) (:referenced_column_name c)]
                                o2m (@one2many key)
                                new-v (if o2m
                                        (logit (conj o2m cols))
                                        (vector tbls cols)
                                        )]
                            (swap! one2many
                                   assoc
                                   key
                                   new-v
                                   )
                            ))
         ]
     (doall (map add-constraint constraints))
     @one2many
     )
    ))
;(relationships)
;(relationships "mdm")
;((relationships "mdm") "leaf_class_attr__a_meta_fk")
;(entity-names-for)



;(defn find-relations
;  ([fn dbname]
;   (let [names (entity-names-for dbname)
;         relations (relationships dbname)]
     ;(letfn [(find-matching-entity [name]
     ;          (filter
     ;            (fn [[_ relation]] (= name (fn (first relation))))
     ;            relations
     ;            ))]
       ;(into {}
       ;      (map #(vector (keyword %) (find-matching-entity %)) names)))))
  ;)
; want:
;{:name :blue_harvest_dev
; :entities
;       {:name         :customer
;        :entity_name:  :customers
;        :pk           :id
;        :fields       [
;                       {:name :email :type :string :required :true
;                        }]
;        :has-many     {:carts {:entity-name :cart :match-keys [:id :customer-id]}
;                       }
;        :many-to-many {:accounts {:through :customer-accounts :id: :customer_id :fk_id :account_id}}
;        }
; })

;(defn belongs-relations
;  ([] (belongs-relations (current-db-name)))
;  ([dbname]
;   (find-relations first dbname)))

;(belongs-relations)
;(belongs-relations "mdm")
;(:ts_item (belongs-relations "mdm"))
;(:tss_leaf_class_attribute (belongs-relations "mdm"))
;(map belongs-relations (relationships))
;(filter (fn ([key v] (first (first v)))))
;
;(def z (atom {["1" "2"] [3]}))
;(swap! z assoc ["1" "2"] [5 3]))
;(@z ["1" "2"])
;@z
;(@z ["1" "2"])
;(swap! z assoc 3 7)
;(conj [2 4] 3)
;(map :entity_name (relationships))
;(map #(vector (:entity_name %) (:referenced_entity_name %) (:foreign_key %)) (relationships))
;(compare "asdf" "asdfx")
;(compare "asdf" "asdg")
;(compare "bsdf" "xs")
;(relationships "mdm")
;
;(defn relationships []
;  (let [db-name "blue_harvest_dev"
;        sql (str "select entity_name, referenced_entity_name, attribute_name as 'foreign_key', referenced_attribute_name as 'references', ordinal_position "
;                 "from information_schema.key_attribute_usage "
;                 "where entity_schema='" db-name "' and referenced_entity_name is not null "
;                 "order by entity_name, ordinal_position, attribute_name "
;                 )]
;    (println sql)
;    (q sql)))

(defn relationships-for [entityname]
  (filter #(= (:entity_name %) entityname) (relationships)))

;(defn foreign-ids-for [entityname]
;  (->>
;    (relationships-for entityname)
;    (map :foreign_key)

(defn convert-for-json [val]
  (println "type" val (type val) (= java.util.Date (type val)))
  (cond
    (= java.util.Date (type val)) (str val)
    :else val))

(def entity-names (entity-names-for))
(def entity-definitions (database-attribute-defs))
;entity-names
;(println entity-definitions)
;*e

(defn attribute-query [entity attribute-name value]
  (println [entity attribute-name value])
  (println {(keyword attribute-name) value})
  (let [
        ;tbl (if (keyword? entity)
        ;      (entity bh-entities)
        ;      entity)
        entity-name (keyword entity)
        entity-info (entity-name entity-definitions)
        colname (keyword attribute-name)
        attribute-info (colname entity-info)
        attribute-type (:type attribute-info)
        v (if (contains? #{} attribute-type) (Long. value) value)
        colname (keyword attribute-name)
        ;v (re-matches #".*id" (name colname))
        query (->
                (select* entity-name)
                (where {colname v})
                (limit 100))]
    (println entity colname query)
    (println "\n\nas-sql ------------" (as-sql query))
    (let [result (select query)]
      {
       ;:rows    (vec (map #(map convert-for-json (vals %)) result))
       :rows (vec (clean-all result))
       :headers (vec (map name (keys (first result))))
       :result  result
       :belongs-to 3
       })
    ))

;(attribute-query :customers :id 28)
;(attribute-query :customers :email "brian@murphydye.com")
