(println "loading summit.punchout.core")

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
            [clojure.pprint :refer :all]

            [net.cgrand.enlive-html :as html]
            [cheshire.core :refer [generate-string]]
            [pl.danieljanus.tagsoup :as soup]
            [taoensso.carmine :as car :refer (wcar)]
            [clojure.zip :as zip]
            [config.core :refer [env]]
            [korma.core :as k]


            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.db.relationships :refer :all]
            ;; [summit.step.import.core :refer :all]
            ;; [summit.step.import.ts.core :refer :all]
            ))

(defn first-keyword-tagged [loc tag])
(defn first-keyword-tagged [loc tag])

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


(println "--------- d")

(defn punchout->map [xml]
  (html/html-resource (java.io.StringReader. xml)))
;; (def x (slurp "test/punchout-request.xml"))
;; x 

;; (def y (punchout->map x))
;; y 

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


(defn extract-content [enlive-parsed selector-vector]
  (first (html/select enlive-parsed (conj (vec selector-vector) html/text-node))))

(defn only-maps [x]
  (filter map? x))

(defn request-type [hash]
  (-> (html/select hash [:Request]) first :content only-maps first :tag))

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

(defn extract-punchout-data [enlive-parsed]
  (let [extract (partial extract-content enlive-parsed)
        request-type (request-type enlive-parsed)
        ]
    {:request {:type request-type
               :payloadID (-> (html/select enlive-parsed [:cXML]) first :attrs :payloadid)
               :browser-form-post (extract [:BrowserFormPost :URL])
               :operation (-> (html/select enlive-parsed (vector request-type)) first :attrs :operation)
               ;; :operation-allowed (-> (html/select enlive-parsed (vector request-type)) first :attrs :operation)
               }
     :broker {:id (extract [:From :Identity])}
     :to {:id (extract [:To :Identity])}
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
            :buyer-cookie (extract [:BuyerCookie])
            }
     }))

(defn validate-punchout-request [hash]
  (and ;; (= (-> hash :broker :id) "coupa-t")  ; coupa sends axiall in this location
   (= (-> hash :company :id) (-> env :punchout-test :id))
   (= (-> hash :company :auth) (-> env :punchout-test :auth))
   )

  true

  )


;; (request-type y)
;; (:payloadID ())
;; (zfind-request-type y)
;; (extract-punchout-data y)
;; (validate-punchout-request (extract-punchout-data y))
;; y 

(defn uuid [] (java.util.UUID/randomUUID))
;; (uuid)


(println "--------- e")

(defn create-one-time-password [cust]
  )


(def cxml-leader "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.1.010/cXML.dtd\">
")
;; (def base-punchout-login-url "http://ubkkb140d981.brianmd.koding.io:22222/punchout/login/")
(def base-punchout-login-url "http://ubkkb140d981.brianmd.koding.io:22223/punchout/")

(defn create-onetime-url [onetime]
  (str base-punchout-login-url "onetime-" onetime))

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

(defn punchout-response [cust hash onetime]
  (let [url (create-onetime-url onetime)
        status 200
        status-str "OK"]
    (cxml
     [:Response
      [:Status {:code status :text status-str}]
      [:PunchOutSetupResponse
       [:StartPage
        [:URL url]]]])))
;; (punchout-response nil nil "4123479 43212341 1234")
;; (create-cxml (punchout-response nil nil "4123479 43212341 1234"))


;; (def punchhash {:request {:type :PunchOutSetupRequest, :payloadID "1211221788.71299@ip-10-251-122-83", :browser-form-post "https://qa.coupahost.com/punchout/checkout/4"}, :broker {:id "coupa-t"}, :to {:id "coupa-t"}, :company {:id (-> env :punchout-test :id), :auth (-> env :punchout-test :auth), :agent "myagent"}, :contact {:email "matthew.hamilton@axiall.com", :name "jim"}, :user {:email "matthew.hamilton@axiall.com", :first-name "myfirstname", :last-name "mylastname", :unique-name "myuniquename", :user "myuser", :business-unit "mybusinessunit", :buyer-cookie "c64af92dc27e68172e030d3dfd1bc944"}})

(defn save-punchout-request! [hash cxml]
  (let [cust (first (k/select :customers (k/where {:email (-> hash :user :email str/lower-case)})))
        broker (first (k/select broker
                                (k/where {:authkey (-> hash :broker :id)})
                                (k/with company)
                                ))
        punch-params {:request_type (-> hash :request :type str)
                      :operation (-> hash :request :operation str)
                      :payload_id (-> hash :request :payloadID)
                      :buyer_cookie (-> hash :user :buyer-cookie)
                      :browser_form_post (-> hash :request :browser-form-post)
                      :params (generate-string hash)
                      :cxml cxml
                      :customer_id (:id cust)
                      :broker_id (:id broker)
                      :company_id (-> broker :companies first :id)
                      :created_at (db-timenow)
                      :updated_at (db-timenow)
                      }
        inserted (k/insert :punchouts (k/values punch-params))
        ]
    (k/update :customers
              (k/set-fields {:active_punchout_id (:generated_key inserted)})
              (k/where {:id (:id cust)}))
    ))
;; (save-punchout-request! punchhash "cxml ....")
;; (k/select :contact_emails (k/where {:id 4183}))
;; (sort (keys (first (k/select :contact_emails (k/where {:id 4183})))))
;; (:punchout_id (first  (k/select :contact_emails (k/where {:id 4183}))))
;; (find-entity :contact_emails 4183)
;; (:active_punchout_id (k/select :customers (k/where {:id 5462})))

(defn store-onetime [onetime hash]
  (wcar* (car/set onetime (generate-string hash))))
;(generate-string {:a 4 :b "hello"})

(defn create-customer-punchout [hash])

(defn process-punchout-request [string]
  (let [parsed (punchout->map string)
        hash (extract-punchout-data parsed)
        valid? (validate-punchout-request hash)
        onetime (if valid? (uuid))]
    (when (not valid?)
            (logit-plain "process-punchout-request not valid")
            (logit-plain "string" string)
            (logit-plain "hash" hash))
    (when valid?
      (store-onetime onetime hash)
      (save-punchout-request! hash string)
      (punchout-response nil nil onetime))
    ))

(defn process-punchout-request-str [xml-string]
  (create-cxml (process-punchout-request xml-string)))



;; ---------------------------------------------------------------------------------  order message

(defn order-header []
  [:Header
   [:From
    [:Credential {:domain "DUNS"}
     [:Identity "asdf"]]]
   [:To
    [:Credential {:domain "DUNS"}
     [:Identity "rrrr"]]]
   [:Sender
    [:Credential {:domain "DUNS"}
     [:Identity "wwww"]]
    [:UserAgent "Summit cXML Application"]]
   ])

(defn item->hiccup [item]
  (let [prod (find-entity :products (:product_id item))]
    [:ItemIn {:quantity (:quantity item)}
     [:ItemID
      [:SupplierPartID (:product_id item)]
      [:SupplierPartAuxiliaryID (:matnr prod)]]
     [:ItemDetail
      [:UnitPrice [:Money {:currency "USD"} (:price_cents item)]]
      [:Description {:xml:lang "en"}
       [:ShortName (:name prod)]
       (:LongDescription prod)]
      [:UnitOfMeasure (:uom prod)]
      [:URL (str "https://www.summit.com/store/product/" (:product_id item))]
      (if false [:Classification {:domain "UNSPSC"} (:unspsc prod)])
      ]]))

(defn load-order-info [order-num]
  (let [order-request (find-entity :contact_emails order-num)
        punchout-request (if-let [punchout-id (:punchout_id order-request)]
                           (find-entity :punchouts (:punchout_id order-request)))
        cart (find-entity :carts (:cart_id order-request))
        cust (find-entity :customers (:customer_id cart))
        items (find-entity-by :line_items :cart_id (:cart_id order-request))
        ]
    {:order-num order-num
     :cart cart
     :items items
     :customer customer
     :punchout-request punchout-request}))
;; (load-order-info 4183)
;; (:punchout-request (load-order-info 4183))


(println "--------- f")

;; other params: buyer-cookie, operationAllowed
(defn get-order-message [order-num]
  ;; [buyer-cookie operation total-price items]
  (let [order-request (find-entity :contact_emails order-num)
        punchout-request (if-let [punchout-id (:punchout_id order-request)]
                           (find-entity :punchouts (:punchout_id order-request)))
        cart (find-entity :carts (:cart_id order-request))
        cust (find-entity :customers (:customer_id cart))
        items (find-entity-by :line_items :cart_id (:cart_id order-request))
        ]
    [:Message {:inReplyTo (:payload-id punchout-request)}
     [:PunchOutOrderMessage {:operationAllowed "create"}  ;; create disallows inspect/edit. May want to allow these in the future. p. 90
      [:BuyerCookie (:buyer_cookie punchout-request)]
      [:PunchOutOrderMessageHeader {:operationAllowed (:operation punchout-request)}
       [:Total
        [:Money {:currency "USD"} (:total_price order-request)]]]
      ]
     (map item->hiccup items)
     ]
    ))
;; (get-order-request 4183)

(println "done loading summit.punchout.core")
