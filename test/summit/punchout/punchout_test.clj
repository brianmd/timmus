(ns summit.punchout.punchout-test
  (:require [summit.utils.core :as u]
            [summit.punchout.samples :refer :all]
            [summit.punchout.punchout :as sut]
            [summit.punchout.samples :refer :all]
            [summit.punchout.core :refer :all]
            [clojure.test :refer :all]
            [summit.punchout.core-test :as pcore]
            [korma.core :as k]))


;; "https://broker.com/punchout/checkout/4" (:browser-form-post request)

;; email        (if-let [e (extract [:Contact :Email])] e "cityabq@murphydye.com")
;; email        "axiall@murphydye.com"


(deftest extract-punchout-test
  (let [_ (pcore/ensure-broker-records)
        punch  (sut/extract-punchout (xml->map punchout-test-str))
        h      (:header punch)
        request (:request punch)
        type   (:type punch)

        ;; header attributes
        from   (:from h)
        to     (:to h)
        sender (:sender h)

        ;; request attributes
        contact (:contact request)
        user   (:user request)
        prequest (:request request)
        ]
    (are [expected actual] (= expected actual)
      ;; 4 request
      :PunchOutSetupRequest                     (:type punch)
      [:type :header :request]                  (keys punch)

      "useremail"                               (:email user)
      "contactemail"                            (:email contact)
      "cookie-monster"                          (:buyer-cookie user)
      "https://broker.com/punchout/checkout/1"  (:browser-form-post prequest)
      "the-payload"                             (:payloadID prequest)
      "create"                                  (:operation prequest)

      # ensure original punchout header is provided
      true                                      (map? h)
      [:from :to :sender :type :enlive-request] (keys h)
      :PunchOutSetupRequest                     (:type h)
      "from-company"                            (:id from)
      "to-summit"                               (:id to)
      "Sender"                                  (:id sender)
      "super-secret"                            (:auth sender)
      "myagent"                                 (:agent sender)
      )))


