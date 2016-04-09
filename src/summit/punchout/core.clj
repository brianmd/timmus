(println "loading summit.punchout.core")

;; Data resides in three formats: xml, vector (hiccup), and map (enlive).
;; As we transform the same data in each of these formats, a suffix is sometimes added.
;; For example, order-message-xml, order-message-vector, and order-message-map.
;; 
;; When data is in default format (input as map and output as vector), no suffix is needed.

(ns summit.punchout.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            ;;[clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]

            [net.cgrand.enlive-html :as html]
            [cheshire.core :refer [generate-string parse-string]]
            [pl.danieljanus.tagsoup :as soup]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.zip :as zip]
            [config.core :refer [env]]
            [korma.core :as k]

            [com.rpl.specter :as s]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.ts.core :refer :all]
            ))


(println "--------- d")

(defn xml->map [xml]
  (html/xml-resource (java.io.StringReader. xml)))

(defn log-punchout [other-party direction type msg]
  (k/insert :punchout_logs
            (k/values {:other_party other-party :direction (if (= direction :to) 1 0) :type (str type) :message (str msg) :created_at (db-timenow) :updated_at (db-timenow)}))
  )

;; (def x (slurp "test/mocks/punchout-request.xml"))
;; x 

;; (def y (xml->map x))
;; y 
;; (pp (exec-sql :bh-prod "select * from contact_emails where type='Order' order by created_at desc limit 1"))
;; (pp (exec-sql :bh-prod "select * from contact_emails where type='Order' order by created_at desc limit 1"))
;; (korma.core/select customer (find-db :prod-db) (korma.core/where {:id 4453}))
;; (korma.core/select :contact_emails (korma.core/database (find-db :bh-prod)) (korma.core/where {:id 4453}))
;; (korma.core/select contact-email (korma.core/database (find-db :bh-prod)) (korma.core/where {:id 4453}))




;; Work w/ Jarred to see what other data we should start capturing in our database from cXML.

;; issue with many City of Abq customers on one cart

;; want java stibo class



(defn extract-content [enlive-parsed selector-vector]
  (first (html/select enlive-parsed (conj (vec selector-vector) html/text-node))))

(defn only-maps [x]
  (filter map? x))

(defn request-type [hash]
  (-> (html/select hash [:Request]) first :content only-maps first :tag))

(defn extract-attribute [enlive-parsed selector-vector attribute]
  (attribute (:attrs (hdetect enlive-parsed selector-vector))))

(defn select-content [parsed v]
  (if-let [c (first (hselect parsed v))]
    (:content c)))

(defn empty-string? [s]
  (and (= (type s) String) (re-matches #"^\s+$" s)))

(defn prune-empty-strings [v]
  (filter #(not (empty-string? %)) v))

(defn select-pruned-content [parsed v]
  (if-let [c (first (hselect parsed v))]
    (prune-empty-strings (:content c))))

(defn mapify [v]
  (reduce #(assoc %1 (:tag %2) (str/join (hselect %2 [html/text-node]))) {} v))

(defn select-content-text [parsed v]
  (mapify (hselect parsed v)))

(defn ->address [parsed tag]
  (let [v [tag :Address]
        addr (select-pruned-content parsed v)]
    (merge
     (select-content-text addr [:Name])
     (mapify (select-pruned-content addr [:PostalAddress])))
    ))


(println "--------- e")


(def cxml-leader "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.1.010/cXML.dtd\">
")

(defn create-payload-id []
  (str (short-timenow) "-" (rand-int 999999999) "@summit.com"))

(defn create-timestamp []
  (clean-all (java.util.Date.)))

(defmacro cxml [& body]
  `[:cXML {:version "1.1.007" "xml:lang" "en-US" :payloadID ~(create-payload-id) :timestamp ~(create-timestamp)}
   ~@body
   ]
  )

(defn create-cxml [hiccup]
  (str cxml-leader (hiccup/html hiccup)))


(defn pong-response []
  (let [status 200
        status-str "OK"]
    (cxml
     [:Response
      [:Status {:code status :text status-str}
       "Ping Response Message"]])))

(defn find-order-request [order-num]
  (find-entity :contact_emails order-num))

(defn find-punchout-request [id]
  (find-entity :punchouts id))


(println "done loading summit.punchout.core")
