(println "loading summit.punchout.hiccup")

(ns summit.punchout.hiccup
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
            [summit.punchout.core :refer :all]
            [summit.db.relationships :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.ts.core :refer :all]
            ))

(defn first-keyword-tagged [loc tag]
   (if (zip/end? loc)
     nil
     (if (= tag (zip/node loc))
       loc
       (recur (zip/next loc) tag))))

(defn subsequence?
  ([big small] (subsequence? big (seq small) 0))
  ([big small lastIndex]
   (if (empty? small)
     true
     (let [i (.indexOf big (first small))]
       (if (<= lastIndex i)
         (recur big (rest small) i)
         false)))))
;(subsequence? (list :cXML :Header :From :Credential :Identity) [:Credential :Header])
;(subsequence? (list :cXML :Header :From :Credential :Identity) [:cXML :Header :Credential])

(defn zparent-tags
  ([z] (zparent-tags (zip/up z) (list)))
  ([z tags]
   (if (nil? z)
     tags
     (let [parent (zip/up z)]
       (recur parent (conj tags (ffirst z)))))))
;(zparent-tags (first-keyword-tagged z :Request))

(defn ztagged
  ([loc tag] (ztagged loc tag []))
  ([loc tag matches]
   (if (zip/end? loc)
     matches
     (recur (zip/next loc) tag (if (= tag (first loc)) (conj matches loc) matches)))))
;(ztagged z :Request)
;(count (ztagged z :Identity))
;(zparent-tags (first (ztagged z :Identity)))
;(.indexOf (list :cXML :Header :From :Credential :Identity) :Credential)

(println "--------- a")

(defn ztags [loc & tags]
  "returns first element with parents matching the tags or nil if none"
  (let [matches (ztagged loc (last tags))
        match (filter #(subsequence? (zparent-tags %) tags) matches)]
    (if match (zip/up (first match)))))
;; (ztags z :PunchOutSetupRequest)
;; (ztags z :PunchOutSetupRequest :Extrinsic)
;; (ztags z :PunchOutSetupRequest :Extrinsic)
;; (ztags z :PunchOutSetupRequest :Extrinsic #(= "FirstName" (:name (second %))))
;; (zattrs-match (ztags z :PunchOutSetupRequest) :name "FirstName")

(defn ztags-node [loc & tags]
  "return node of matched ztags"
  (let [match (apply ztags (conj tags loc))]
    (if match (zip/node match))))

(defn ztags-attrs [loc & tags]
  (let [match (apply ztags (conj tags loc))]
    (if match (second (zip/node match)))))


(defn ztags-val [loc & tags]
  (let [match (apply ztags-node (conj tags loc))]
    (if match (nth match 2))))
;; (ztags-val z :Header :From :Credential :Identity)
;; (ztags-val z :From :Identity)
;; (ztags-val z :Sender :Identity)
;; (ztags-val z :Sender :Identityyyy)


(defn find-first-tagged [v tag]
  (first (filter #(= tag (first %)) v)))

(defn find-extrinsic-named [v name]
  (first (filter #(and (= :Extrinisc (first %)) (= name (:name (second %)))))))


(println "--------- b")

(defn find-tag [v & tags]
  (if (= 0 (count tags))
    v
    (let [result (find-first-tagged (subvec v 2) (first tags))]
      (if (= 1 (count tags))
        result
        (recur result (rest tags))))
    ))

(defn thirdd [x] (nth x 2))

(defn find-tag-val [& args]
  (nth (apply find-tag args) 2))

(defn find-request-type [hic]
  (-> (find-tag hic [:Request]) thirdd first))
(defn zfind-request-type [z]
  (-> (find-tag (first z) [:Request]) thirdd first))

;; ;; (find-request-type m)
;; (zfind-request-type z)
;; (find-tag m :Request)
;; (find-tag m :Header)
;; (find-tag m :Header :From)
;; (find-tag m :Header :To)
;; (find-tag m :Header :To :Credential :Identity)
;; (find-tag m :Header :Sender)
;; (find-tag m :Header :From :Credential)
;; (find-tag m :Header :From :Credential :Identity)
;; (find-tag-val m :Header :From :Credential :Identity)


;(extract-punchout-data y)

(defn extract-punchout-data2 [z]
  (let [extract (partial ztags-val z)
        request-type (zfind-request-type z)
        extrinsic (partial ztags-val (ztags z request-type))
        request-tag (ztags-node z :Request)]
    (println "request-tag---------------" request-tag)
    {:request {:type request-type
               :payloadID (:payloadid (ztags-attrs z :cXML))
               :operation-allowed (:operation (ztags z (keyword request-type)))
               :browser-form-post (extract :BrowserFormPost :URL)
               }
     ;:broker (find-tag-val hic :Header :From :Credential :Identity)
     :broker {:id  (extract :From :Identity)}
     :to {:id  (extract :To :Identity)}
     ;; :to {:id (extract :To :Identity])}
     :company {:id  (extract :Sender :Identity)
               :auth (extract :Sender :SharedSecret)
               :agent (extract :Sender :UserAgent)}
     :contact {:email (extract :Contact :Email)
               :name (extract :Contact :Name)
               }
     :user {:email (ztags z request-type)
            :first-name (extract (html/attr= :name "FirstName"))
            :last-name (extract (html/attr= :name "LastName"))
            :unique-name (extract (html/attr= :name "UniqueName"))
            :user (extract (html/attr= :name "User"))
            :business-unit (extract (html/attr= :name "BusinessUnit"))
            :buyer-cookie (extract [:BuyerCookie])
            }
     })
  )

;; (def m  (soup/parse "test/punchout-request.xml"))
;; (def z (zip/vector-zip m))
;; z 

;; ;; (-> z zip/down)
;; (ztags-node z :Request :BuyerCookie)
;; (ztags-node z :BuyerCookie)
;; (ztags-attrs z :cXML)
;; (ztags-node z :Requestttt)

;; (ztags z :Header :From :Credential :Identity)
;; (ztags z :Request)
;; (zip/node (ztags z :Header :From :Credential :Identity))
;; (ztags-node z :Header :From :Credential :Identity)
;; (zip/node (ztags-node z :Header :From :Credential :Identity))

#_(defn extract-header-data [enlive-parsed]
  (let [extract (partial extract-content enlive-parsed)
        request-type (zfind-request-type enlive-parsed)
        ]
    {:request {:type request-type
               :payloadID (-> (html/select enlive-parsed [:cXML]) first :attrs :payloadid)
               :browser-form-post (extract [:BrowserFormPost :URL])
               }
     :from {:id (extract [:From :Identity])}
     :to {:id (extract [:To :Identity])}
     :sender {;; :credential-domain (extract [:Sender :Credential (html/attr=)])
              :id (extract [:Sender :Identity])
              :auth (extract [:Sender :SharedSecret])
              :agent (extract [:Sender :UserAgent])}

     :broker {:id (extract [:From :Identity])}
     :company {:id (extract [:Sender :Identity])
               :auth (extract [:Sender :SharedSecret])
               :agent (extract [:Sender :UserAgent])}
     :contact {:email (extract [:Contact :Email])
               :name (extract [:Contact :Name])
            }
     :user {:email (extract [(html/attr= :name "UserEmail")])
            :first-name (extract [(html/attr= :name "FirstName")])
            :last-name (extract [(html/attr= :name "LastName")])
            :unique-name (extract [(html/attr= :name "UniqueName")])
            :user (extract [(html/attr= :name "User")])
            :business-unit (extract [(html/attr= :name "BusinessUnit")])
            :buyer-cookie (extract :BuyerCookie)
            }
     }))


(println "done loading summit.punchout.hiccup")
