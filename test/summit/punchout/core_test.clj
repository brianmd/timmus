(ns summit.punchout.core-test
  (:require ;[clojure.test :as t]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]

            [config.core :refer [env]]
            [clojure.walk :refer [prewalk]]

            [expectations :refer :all]
            [summit.utils.core :refer :all]
            [summit.punchout.core :refer :all]
            [com.rpl.specter :refer :all]

            [summit.db.relationships :refer :all]
            ))

#_(defn select-keyword [nested-arr keyword]
  (let [i (atom [])]
    (prewalk #(if (and (sequential? %) (= keyword (first %))) (do (swap! i conj %) %) %) nested-arr)
    @i))



;; ---------------------------------------------------------------------------------  process punchout request

(def xml (slurp "test/mocks/punchout-request.xml"))
(def hashxml (xml->map xml))

(expect
 {:request {:type :PunchOutSetupRequest, :payloadID "1211221788.71299@ip-10-251-122-83", :browser-form-post "https://qa.coupahost.com/punchout/checkout/4", :operation "create"}, :broker {:id "coupa-t"}, :to {:id "coupa-t"}, :company {:id (-> env :punchout-test :id), :auth (-> env :punchout-test :auth), :agent "myagent"}, :contact {:email "matthew.hamilton@axiall.com", :name "jim"}, :user {:email "matthew.hamilton@axiall.com", :first-name "myfirstname", :last-name "mylastname", :unique-name "myuniquename", :user "myuser", :business-unit "mybusinessunit", :buyer-cookie "c64af92dc27e68172e030d3dfd1bc944"}}
 (extract-punchout-data hashxml))

(expect true? (validate-punchout-request (extract-punchout-data hashxml)))


(extract-punchout-data hashxml)
(def ha  (extract-punchout-data hashxml))
(-> ha :company)
(korma.core/select broker)
(korma.core/select broker (korma.core/where {:authkey (-> ha :broker :id)}))
(korma.core/select broker (korma.core/where {:authkey (-> ha :broker :id)}))
(korma.core/select :broker_companies (korma.core/where {:company_key (-> ha :company :id)}))
(korma.core/select broker
                   (korma.core/where {:authkey (-> ha :broker :id)})
                   (korma.core/with company)
                   )

;; ---------------------------------------------------------------------------------  test punchout response

(def response-hiccup (process-punchout-request xml))
;; (process-punchout-request xml)
;;    => [:cXML {:payloadID "20160226102632-0700-35546641@summit.com", "xml:lang" "en-US", :version "1.1.007", :timestamp "2016-02-26T17:26:32.144Z"} [:Response [:Status {:code 200, :text "OK"}] [:PunchOutSetupResponse [:StartPage [:URL "http://ubkkb140d981.brianmd.koding.io:22223/punchout/onetime-38c5b2b8-47c1-4cd7-befb-d48730dd868f"]]]]]


(expect 
        (not=
         (select-keyword response-hiccup :URL)
 (process-punchout-request xml) :URL))


;; (process-punchout-request xml)
;; (process-punchout-request-str xml)

;; (def slideshow-str (slurp "http://director.insummit.com/api/get_album_list?api_key=local-1e61b9ec8ff6cedafa425c9e1ca07e47&only_published=1&only_active=1&list_only=0&only_smart=0&exclude_smart=0"))
;; (def slideshow (cheshire.core/parse-string slideshow-str))
;; slideshow 
;; (spit "/Users/bmd/Downloads/slideshow-data" slideshow-str)
;; (= slideshow-str (slurp "/Users/bmd/Downloads/slideshow-data"))

;; ---------------------------------------------------------------------------------  test order message

(def ord-req (get-order-message 4183))

(expect '[:Message
          [:PunchOutOrderMessage [:BuyerCookie "c64af92dc27e68172e030d3dfd1bc944"]
           [:PunchOutOrderMessageHeader {:operationAllowed "create"} [:Total [:Money {:currency "USD"} 0]]]]
          ([:ItemIn {:quantity 2000}
            [:ItemID [:SupplierPartID 29787]
             [:SupplierPartAuxiliaryID "000000000002718433"]]
            [:ItemDetail [:UnitPrice [:Money {:currency "USD"} 0]] [:Description {:xml:lang "en"} [:ShortName "WIRE 12/1C SOL CU THHN RED 2000 COILPK"] nil] [:UnitOfMeasure "FT"] nil]]

           [:ItemIn {:quantity 2} [:ItemID [:SupplierPartID 57577] [:SupplierPartAuxiliaryID "000000000001207495"]] [:ItemDetail [:UnitPrice [:Money {:currency "USD"} 0]] [:Description {:xml:lang "en"} [:ShortName "THHN-4/0-BLU-19STR-CU-CUT LENGTH"] nil] [:UnitOfMeasure "FT"] nil]]
           [:ItemIn {:quantity 1} [:ItemID [:SupplierPartID 57705] [:SupplierPartAuxiliaryID "000000000000017464"]] [:ItemDetail [:UnitPrice [:Money {:currency "USD"} 0]] [:Description {:xml:lang "en"} [:ShortName "A12R124 NEMA3R ENCLOSURE 12"] nil] [:UnitOfMeasure "EA"] nil]])]
        ord-req)



#_(comment
    (expect 3 (count ord-req))
    (expect :Message (first ord-req))
    (expect :PunchOutOrderMessage (first (second ord-req)))

    (def line-items (nth ord-req 2))

    (expect 3 (count line-items))
    (expect
     '(:ItemIn :quantity :ItemID :SupplierPartID :SupplierPartAuxiliaryID :ItemDetail :UnitPrice :Money :currency :Description :xml:lang :ShortName :UnitOfMeasure)
     (select (walker keyword?) (first line-items))
     )
    (expect '[:ItemID [:SupplierPartID 29787] [:SupplierPartAuxiliaryID "000000000002718433"]] (first (select-keyword line-items :ItemID)))
    (expect '(29787 57577 57705) (map second (select-keyword line-items :SupplierPartID)))
    (select (walker keyword?) (first line-items))


(select-keyword items :ItemID)
(29787 57577 57705)
[[:ItemID [:SupplierPartID 29787] [:SupplierPartAuxiliaryID "000000000002718433"]] [:ItemID [:SupplierPartID 57577] [:SupplierPartAuxiliaryID "000000000001207495"]] [:ItemID [:SupplierPartID 57705] [:SupplierPartAuxiliaryID "000000000000017464"]]]

(count ord-req)
(first ord-req)
(second ord-req)
(nth ord-req 2)
(count items)
ord-req 
(select [ALL ALL #(= 0 (mod % 3))]
        [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])
[:ItemIn {:quantity 2000} [:ItemID [:SupplierPartID 29787] [:SupplierPartAuxiliaryID "000000000002718433"]] [:ItemDetail [:UnitPrice [:Money {:currency "USD"} 0]] [:Description {:xml:lang "en"} [:ShortName "WIRE 12/1C SOL CU THHN RED 2000 COILPK"] nil] [:UnitOfMeasure "FT"] nil]]

[:ItemIn {:quantity 2} [:ItemID [:SupplierPartID 57577] [:SupplierPartAuxiliaryID "000000000001207495"]] [:ItemDetail [:UnitPrice [:Money {:currency "USD"} 0]] [:Description {:xml:lang "en"} [:ShortName "THHN-4/0-BLU-19STR-CU-CUT LENGTH"] nil] [:UnitOfMeasure "FT"] nil]]



(get-in (first items) [:ItemIn])

(select [ALL ] (first items))
(select [ALL #(= :ItemID (first %))] (first items))

;; (t/run-all-tests)

)
