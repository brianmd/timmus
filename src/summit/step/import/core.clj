(ns summit.step.import.core
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            ;; [hiccup.core :as hiccup]
            ;; [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            ;; [clojure.java.io :as io]
            ;; [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            ;; [summit.step.xml-output :refer :all]


            ;; [net.cgrand.enlive-html :as html]
            [summit.punchout.core :refer :all]


            [summit.utils.core :refer :all]
            [summit.step.import.product-selectors :refer :all]
            ))


#_(comment



(def golden-search "http://Stepsys:stepsys@stibo-dev.insummit.com/restapi/basicsearch/MEM_GLD*?context=Context1")
(def golden-search "http://stibo-dev.insummit.com/restapi/products/MEM_GLD_101306?context=Context1")
(def golden-retrieval "http://stibo-dev.insummit.com/restapi/products/MEM_GLD_101306?context=Context1")
(def golden-search "http://Stepsys:stepsys@stibo-dev.insummit.com/restapi/products/MEM_GLD_101306?context=Context1")

(defn step-authentication []
  {:basic-auth ["Stepsys" "stepsys"]})

(defn get-golden-ids []
  (let [body (:body (clj-http.client/get "http://stibo-dev.insummit.com/restapi/basicsearch/MEM_GLD_*?context=Context1" (step-authentication)))
        parsed (punchout->map body)
        products (select parsed [:Product])
        ids (doall (map (fn [prod] (:id (:attrs prod))) products))
        ]
    ids))
;; (get-golden-ids)

(defn get-product [id]
  (let [body (:body (clj-http.client/get (str "http://stibo-dev.insummit.com/restapi/products/" id "?context=Context1") (step-authentication)))
        parsed (punchout->map body)
        ]
    (detect parsed [:Product])
  ))
;; (get-product "MEM_GLD_101278")

(defn get-product-references [id]
  (let [product (get-product id)
        references (select product [:ProductCrossReference])
        ids (doall (map (fn [prod] (:productid (:attrs prod))) references))
        ]
    ids))
;; (get-product-references "MEM_GLD_101278")



(defn get-golden-references []
  (let [ids (get-golden-ids)
        refs (into {} (doall (map (fn [id] [id (get-product-references id)]) ids)))]
    refs))
;; (get-golden-references)
;; (def xy (get-golden-references))
;; (def xz (vals xy))
;; (type xz)
;; xz
;; (map #(into {} (map step-id->source-id %)) xz)
;; (first xy)

(defn step-id->source-id [id]
  (let [tokens (reverse (str/split id #"_"))
        source-name (case (second tokens)
                      "SAP" :sap
                      "IDW" :idw
                      "TS" :ts)]
    [source-name (first tokens)]))
;; (step-id->source-id "MEM_IDW_1933")

(defn get-matching-source-products []
  (let [refs (get-golden-references)
        matches (doall (vals refs))]
    (map #(into {} (map step-id->source-id %)) xz)))
;; (get-matching-source-products)


)





(make-record ColumnInfo '(name dbid type validators))

#_(comment


(defn reset-exported-products []
  (def exported-products (atom {:ts [] :idw [] :sap []}))
  )
;; (reset-exported-products)

(defn sap-set-exported [x] (swap! exported-products assoc :sap (set (mapv read-string x))) x)
(defn ts-set-exported [x] (swap! exported-products assoc :ts (set (mapv read-string x))) x)
(defn idw-concat-exported [x] (swap! exported-products assoc :idw (set (concat (:idw exported-products) (mapv read-string x)))) x)

(defn calculate-expected-golden-match [exported match]
  (into {}
        (filter (fn [[k v]]
                  (if (contains? 
                       (case k
                         :matnr (:sap exported)
                         :idw_index (:idw exported)
                         :item_pik (:ts exported))
                       (as-integer v))
                    [k v]))
                match)))
;; (calculate-expected-golden-match @exported-products (first matches))
;; (calculate-expected-golden-match @exported-products {})

(defn calculate-expected-golden-matches [exported matches]
  (filter not-empty (map (partial calculate-expected-golden-match exported) matches)))
;; (calculate-expected-golden-matches @exported-products matches)
;; (clojure.pprint/pprint (calculate-expected-golden-matches @exported-products matches))
;; (spit "junk" (with-out-str (calculate-expected-golden-matches @exported-products matches)))







;; (defn create-source-product-xml [source-ids-file idw-files]
;;   (def matches (:prods (slurp-source-ids source-ids-file)))
;;   (reset-exported-products)
;;   ;; (dorun (map summit.step.import.idw.material/write-idw-file idw-files))
;;   ;; (time (summit.step.import.ts.material/write-ts-file))
;;   ;; (time (summit.step.import.sap.material/write-sap-file))
;;   (clojure.pprint/pprint (calculate-expected-golden-matches @exported-products matches))
;;   )
;; (create-source-product-xml "punduit" ["Panduit14374598.csv"])

;; (swap! exported-products assoc :sap (mapv read-string (:sap @exported-products)))
;; (swap! exported-products assoc :sap (set (:sap @exported-products)))
;; (keys @exported-products)
;; (swap! exported-products assoc :ts (map read-string (:ts @exported-products)))
;; (set (:ts @exported-products))

)


(defn process-file-with [path-and-filename fn]
  (with-open [in-file (io/reader path-and-filename)]
    (let [lines (csv/read-csv in-file :separator \| :quote \^)]
      (fn lines)
      )))

(defn lookup-validator [lookup-table]
  [#(contains? lookup-table %) #(str "Value not in lookup table: " %)])

(defn validate-with [val fns]
  "fns => [assertion-fn msg-fn]"
  (if (not ((first fns) val))
    ((second fns) val)))

(defn validator-fns [fns-or-keyword]
  (if (keyword? fns-or-keyword)
    (validators fns-or-keyword)
    fns-or-keyword))
                                        ;(validator-fns :required)

(defn validate-with* [val fns*]
  (let [val-validator (partial validate-with val)
        validator-fns (map validator-fns fns*)]
    (remove nil? (map val-validator validator-fns))))

;(assert (=
;    (validate-with* "" (-> material-col-info :matnr :validators))
;    '("This field is required.")
;    ))
;(validate-with* "" (-> material-col-info :type :validators))
;(validate-with* "asdf" (-> material-col-info :type :validators))
;(validate-with* "HAWA" (-> material-col-info :type :validators))

(defn validate-colname-with* [colname val fns*]
  (let [msgs (validate-with* val fns*)]
    (if (not-empty msgs)
      [colname msgs])))
;(validate-colname-with* :matnr "" [:required :digits])
;(validate-colname-with* :matnr "122" [:required :digits :test-failure])

(defn validate-col [cols-info record col-name]
  (let [col-info (col-name cols-info)
        validators (:validators col-info)
        ]
    (validate-colname-with* col-name (col-name record) validators)))

(defn validate-record [cols-info record]
  (let [v (partial validate-col cols-info record)]
    (into {}
          (->>
            (map v (keys cols-info))
            (remove nil?)
            ))))
;(validate-record material-col-info (makeit))

(defn material-attributes [material-col-info item]
  [:Values
   (reduce
    (fn [attrs [name col]]
      (if (:dbid col)
        (conj attrs [:Value
                     {:AttributeID (:dbid col)}
                     (escape-html (name item))])
        attrs))
    () material-col-info)])

(defn top-tag []
  (with-out-str
    (x/emit
     (xml/sexp-as-element
      (stepxml-top-tag)
      ))))

(defn opening []
  (->>
   (str/split-lines (top-tag))
   (take 3)
   (str/join "\n")))

(defn closing []
  (->>
   (str/split-lines (top-tag))
   reverse
   (take 2)
   reverse
   (str/join "\n")))

