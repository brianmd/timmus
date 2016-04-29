(ns summit.bh.queries
  (:require ;[korma.db :refer :all]
            [korma.core :as k]
            [clojure.string :as str]
            ;; [mount.core :as mount]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            ))

(def admins-sql "
select c.*
from roles r
join grants g on r.id=g.role_id
join permissions p on p.id=g.permission_id
join customers c on c.id=r.customer_id
where account_id is null and p.resource='all' and p.action='manage'
order by c.created_at
")

(defn admins
  ([] (admins :bh-prod))
  ([db]
   (exec-sql db admins-sql)))

(examples
 (ppn (->> (admins) (map :email)))
 )
