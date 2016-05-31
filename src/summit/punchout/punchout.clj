(println "loading summit.punchout.punchout")

(ns summit.punchout.punchout
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

            [clojure.tools.logging :as log]))

(defn extract-header-data [enlive-parsed]
  (let [extract (partial extract-content enlive-parsed)
        request-type (request-type enlive-parsed)
        email (if-let [e (extract [:Contact :Email])] e "cityabq@murphydye.com")
        email "axiall@murphydye.com"
        ]
    {:request {:type request-type
               :payloadID (-> (html/select enlive-parsed [:cXML]) first :attrs :payloadID)
               :browser-form-post (extract [:BrowserFormPost :URL])
               :operation (-> (html/select enlive-parsed (vector request-type)) first :attrs :operation)
               ;; :operation-allowed (-> (html/select enlive-parsed (vector request-type)) first :attrs :operation)
               }
     :from {:id (extract [:From :Identity])}
     :to {:id (extract [:To :Identity])}
     :sender {:id (extract [:Sender :Identity])
               :auth (extract [:Sender :SharedSecret])
               :agent (extract [:Sender :UserAgent])}
     }))

(defn extract-punchout-data [enlive-parsed]
  (let [extract (partial extract-content enlive-parsed)
        request-type (request-type enlive-parsed)
        email (if-let [e (extract [:Contact :Email])] e "cityabq@murphydye.com")
        email "axiall@murphydye.com"
        ]
    (merge
     (extract-header-data enlive-parsed)
     {
      :contact {:email email
                :name (extract [:Contact :Name])
                }
      :user {:email email ; (extract [(html/attr= :name "UserEmail")])
             :first-name (extract [(html/attr= :name "FirstName")])
             :last-name (extract [(html/attr= :name "LastName")])
             :unique-name (extract [(html/attr= :name "UniqueName")])
             :user (extract [(html/attr= :name "User")])
             :business-unit (extract [(html/attr= :name "BusinessUnit")])
             :buyer-cookie (extract [:BuyerCookie])
             }})
     ))

(defn validate-punchout-request [hash]
  (and ;; (= (-> hash :from :id) "coupa-t")  ; coupa sends axiall in this location
   (= (-> hash :sender :id) (-> env :punchout-test :id))
   (= (-> hash :sender :auth) (-> env :punchout-test :auth))
   )

  true

  )


;; (request-type y)
;; (:payloadID ())
;; (zfind-request-type y)
;; (extract-punchout-data y)
;; (validate-punchout-request (extract-punchout-data y))
;; y 


(defn create-one-time-password [cust]
  )
;; (def base-punchout-login-url "http://ubkkb140d981.brianmd.koding.io:22222/punchout_login/")
(def base-punchout-login-url "http://ubkkb140d981.brianmd.koding.io:22223/punchout_login/")
;; (def base-punchout-login-url "http://localhost:3000/punchout_login/")

(defn create-onetime-url [onetime]
  (str base-punchout-login-url "onetime-" onetime))

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


;; (def punchhash {:request {:type :PunchOutSetupRequest, :payloadID "1211221788.71299@ip-10-251-122-83", :browser-form-post "https://qa.coupahost.com/punchout/checkout/4"}, :from {:id "coupa-t"}, :to {:id "coupa-t"}, :sender {:id (-> env :punchout-test :id), :auth (-> env :punchout-test :auth), :agent "myagent"}, :contact {:email "matthew.hamilton@axiall.com", :name "jim"}, :user {:email "matthew.hamilton@axiall.com", :first-name "myfirstname", :last-name "mylastname", :unique-name "myuniquename", :user "myuser", :business-unit "mybusinessunit", :buyer-cookie "c64af92dc27e68172e030d3dfd1bc944"}})

;; (k/select :customers (k/database (find-db :bh-local)) (k/where {:email "brian@murphydye.com"}))
;; (k/select :punchouts (k/where {:id 90}))
;; from 
;; (k/select broker (k/where {:authkey "-t"}) (k/with company))

(defn save-punchout-request! [hash cxml]
  ;; (println "about to save punchout request" hash)
  ;; (ppn (-> hash :user :email))
  ;; (println (k/select :customers (k/database (find-db :bh-local)) (k/where {:email "brian@murphydye.com"})))
  (let [email (if-let [e (-> hash :user :email)] (str/lower-case e) "cityabq@murphydye.com")
        email "axiall@murphydye.com"
        ;; _ (println "\n\nemail" email)
        cust (first (k/select :customers (k/database (find-db :bh-local)) (k/where {:email email})))
        ;; _ (println "\n\ncust" cust)
        broker (first (k/select broker
                                (k/where {:authkey (-> hash :from :id)})
                                (k/with company)
                                ))
        ;; _ (println "\n\nbroker" broker)
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
        ;; _ (println "\n\npunch-params" punch-params)
        inserted (k/insert :punchouts (k/values punch-params))
        ]
    ;; (println "\n\ninserted ...")
    (k/update :customers
              (k/set-fields {:active_punchout_id (:generated_key inserted)})
              (k/where {:id (:id cust)}))
    ;; (println "done updating customer" cust)
    ))
;; (save-punchout-request! punchhash "cxml ....")
;; (k/select :contact_emails (k/where {:id 4183}))
;; (sort (keys (first (k/select :contact_emails (k/where {:id 4183})))))
;; (:punchout_id (first  (k/select :contact_emails (k/where {:id 4183}))))
;; (find-entity :contact_emails 4183)
;; (:active_punchout_id (k/select :customers (k/where {:id 5462})))


(defn store-onetime [onetime hash]
  (println "store-onetime" onetime hash)
  (def xyz [onetime hash])
  (wcar* (car/setex (clean-all onetime) 60 (generate-string hash))))
;(generate-string {:a 4 :b "hello"})
;; (println xyz)

(defn create-customer-punchout [hash])

(defn process-punchout-request [string]
  (let [parsed (xml->map string)
        hash (extract-punchout-data parsed)
        valid? (validate-punchout-request hash)]
    (log/info "is punchout valid?" valid?)
    (if valid?
      (let [onetime (uuid)]
        (store-onetime onetime hash)
        (save-punchout-request! hash string)
        (punchout-response nil nil onetime))
      (do
        (logit-plain "process-punchout-request not valid")
        (logit-plain "string" string)
        (logit-plain "hash" hash)
        (throw (Exception. "Punchout request not valid"))))))

(defn process-punchout-request-str [xml-string]
  (create-cxml (process-punchout-request xml-string)))



(def coupa-test-str "asdf")
(def coupa-test-str
  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE cXML SYSTEM \"http://xml.cxml.org/schemas/cXML/1.2.014/cXML.dtd\">
<cXML payloadID=\"1211221788.71299@ip-10-251-122-83\" timestamp=\"Mon May 19 18:29:48 +0000 2008\" xml:lang=\"en-US\">
  <Header>
    <From>
      <Credential domain=\"DUNS\">
        <Identity>coupa-t</Identity>
      </Credential>
    </From>
    <To>
      <Credential domain=\"DUNS\">
        <Identity>coupa-t</Identity>
      </Credential>
    </To>
    <Sender>
      <Credential domain=\"DUNS\">
        <Identity>axiall</Identity>
        <SharedSecret>wi7oie.c</SharedSecret>
      </Credential>
      <UserAgent>myagent</UserAgent>
    </Sender>
  </Header>
  <Request>
    <PunchOutSetupRequest operation=\"create\">
      <BuyerCookie>c64af92dc27e68172e030d3dfd1bc944</BuyerCookie>
      <Extrinsic name=\"FirstName\">myfirstname</Extrinsic>
      <Extrinsic name=\"LastName\">mylastname</Extrinsic>
      <Extrinsic name=\"UniqueName\">myuniquename</Extrinsic>
      <Extrinsic name=\"UserEmail\">myemail</Extrinsic>
      <Extrinsic name=\"User\">myuser</Extrinsic>
      <Extrinsic name=\"BusinessUnit\">mybusinessunit</Extrinsic>
      <BrowserFormPost>
        <URL>https://qa.coupahost.com/punchout/checkout/4</URL>
      </BrowserFormPost>
      <Contact role=\"myrole\">
        <Name xml:lang=\"en-US\">jim</Name>
        <Email>myemail</Email>
      </Contact>
    </PunchOutSetupRequest>
  </Request>
</cXML>")

(defn test-punchout [s]
  ;; (clj-http.client/post "http://localhost:3449/api/punchout" {:debug true :debug-body true :body s}))
  (clj-http.client/post "http://localhost:3449/api/punchout" {:body s}))

(defn test-get-punchout-redirect [s]
  (let [response (test-punchout s)]
    (if-not (= 200 (:status response))
      (throw (Exception. (str "Invalid http response (" (:status response) ") when on PunchoutSetUp"))  ))
    (last (re-find #".*URL>(http[^<]+).*" (:body response)))
    ))

;; (extract-punchout-data (xml->map coupa-test-str))
;; (html/select (xml->map coupa-test-str) [:Request])

;; (test-punchout coupa-test-str)
;; (test-get-punchout-redirect coupa-test-str)

;; (def aaa (test-punchout coupa-test-str))
;; (last (re-find #".*URL>(http[^<]+).*" (:body aaa)))


(println "done loading summit.punchout.punchout")
