(ns timmus.routes.services
  (:require [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [schema.core :as s]
            [korma.core :refer :all]

            [timmus.db.relationships :refer :all]
            [timmus.sales-associate.order-spec :refer [send-spec-email]]
            ))

(s/defschema Thingie {:id Long
                      :hot Boolean
                      :tag (s/enum :kikka :kukka)
                      :chief [{:name String
                               :type #{{:id String}}}]})

(defapi service-routes
  (ring.swagger.ui/swagger-ui
   "/swagger-ui")
  ;JSON docs available at the /swagger.json route
  (swagger-docs
    {:info {:title "Sample api"}})
  (context* "/api" []
            :tags ["thingie"]

            (GET* "/plus" []
                  :return       Long
                  :query-params [x :- Long, {y :- Long 1}]
                  :summary      "x+y with query-parameters. y defaults to 1."
                  (ok (+ x y)))

            (POST* "/minus" []
                   :return      Long
                   :body-params [x :- Long, y :- Long]
                   :summary     "x-y with body-parameters."
                   (ok (- x y)))

            (GET* "/times/:x/:y" []
                  :return      Long
                  :path-params [x :- Long, y :- Long]
                  :summary     "x*y with path-parameters"
                  (ok (* x y)))

            (POST* "/divide" []
                   :return      Double
                   :form-params [x :- Long, y :- Long]
                   :summary     "x/y with form-parameters"
                   (ok (/ x y)))

            (GET* "/power" []
                  :return      Long
                  :header-params [x :- Long, y :- Long]
                  :summary     "x^y with header-parameters"
                  (ok (long (Math/pow x y))))

            (PUT* "/echo" []
                  :return   [{:hot Boolean}]
                  :body     [body [{:hot Boolean}]]
                  :summary  "echoes a vector of anonymous hotties"
                  (ok body))

            (POST* "/echo" []
                   :return   (s/maybe Thingie)
                   :body     [thingie (s/maybe Thingie)]
                   :summary  "echoes a Thingie from json-body"
                   (ok thingie))

            (GET* "/hello" []
                  :query-params [name :- String]
                  (ok {:message (str "Hello, " name)}))

            (GET* "/customer/:email" []
                  :path-params [email :- String]
                  (ok (select customer (limit 1) (where {:email email}))))

            (GET* "/order-spec/:email/:ordernum" []
                 ;:return Long
                 :path-params [email :- String, ordernum :- Long]
                 :summary "emails specs related to order-num to email"
                  (let [email (str email "@summit.com")]
                    (println "order-spec" email ordernum)
                    (try
                      (send-spec-email email ordernum)
                      (println "sent email for" email ordernum)
                      (ok {:email email :order-num ordernum})
                      (catch Exception e
                        (println "oops" e)
                        (not-found {:email email :order-num ordernum :error-msg (.getMessage e)}))
                      )))
                    ;(Thread/sleep 1000)
                    ;(ok {:email email :order-num ordernum})
                    ;(bad-request {:email email :order-num ordernum})
                    ;(throw "nope")
            )

  (context* "/context" []
            :tags ["context*"]
            :summary "summary inherited from context"
            (context* "/:kikka" []
                      :path-params [kikka :- s/Str]
                      :query-params [kukka :- s/Str]
                      (GET* "/:kakka" []
                            :path-params [kakka :- s/Str]
                            (ok {:kikka kikka
                                 :kukka kukka
                                 :kakka kakka})))))


