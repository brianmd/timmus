(ns timmus.utils.core
  (:require
    [clj-http.client :as client]
    [cheshire.core :refer :all]
    )
  )

(defn logit [& args]
  (apply println args)
  (last args))

(defn as-document-num [string]
  (let [s (str "0000000000" string)]
    (subs s (- (count s) 10))))

(defn bh_login [email pw]
  (let [cred
        {"customer"
         {"email" email, "password" pw}
         "session"
         {"customer"
          {"email" email, "password" pw}
          }}
        params
        {:body         (generate-string cred)
         :content-type :json
         :accept       :json}
        ]
    (client/post
      "https://www.summit.com/store/customers/sign_in.json"
      params)
    ))
