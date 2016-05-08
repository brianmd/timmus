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

            ;; [summit.step.xml-output :refer :all]


            ;; [net.cgrand.enlive-html :as html]
            [summit.punchout.core :refer :all]


            [summit.utils.core :refer :all]
            [summit.step.import.product-selectors :refer :all]
            ))




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
;;   ;; (dorun (map summit.step.import.idw.product/write-idw-file idw-files))
;;   ;; (time (summit.step.import.ts.product/write-ts-file))
;;   ;; (time (summit.step.import.sap.product/write-sap-file))
;;   (clojure.pprint/pprint (calculate-expected-golden-matches @exported-products matches))
;;   )
;; (create-source-product-xml "punduit" ["Panduit14374598.csv"])

;; (swap! exported-products assoc :sap (mapv read-string (:sap @exported-products)))
;; (swap! exported-products assoc :sap (set (:sap @exported-products)))
;; (keys @exported-products)
;; (swap! exported-products assoc :ts (map read-string (:ts @exported-products)))
;; (set (:ts @exported-products))

)


(defn transduce-tabbed-file-with [path-and-filename f]
  (with-open [in-file (io/reader path-and-filename)]
    (let [lines (csv/read-csv in-file :separator \tab :quote (char 2))]
      (transduce
       f
       (fn [& _])
       lines)
      )))

(defn transduce-verticalbar-file-with [path-and-filename f]
  (with-open [in-file (io/reader path-and-filename)]
    (let [lines (csv/read-csv in-file :separator \| :quote (char 2))]
      (transduce
       f
       (fn [& _])
       lines)
      )))

(defn process-tabbed-file-with [path-and-filename f]
  (with-open [in-file (io/reader path-and-filename)]
    (let [lines (csv/read-csv in-file :separator \tab :quote (char 2))]
      (f lines)
      )))

(defn process-verticalbar-file-with [path-and-filename f]
  (with-open [in-file (io/reader path-and-filename)]
    ;; (let [lines (csv/read-csv in-file :separator \| :quote \^)]
    ;; we don't want quoted characters, but nil doesn't seem to be allowed
    ;; using a character that should never occur
    (let [lines (csv/read-csv in-file :separator \| :quote (char 2))]
      (f lines)
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
;    (validate-with* "" (-> product-col-info :matnr :validators))
;    '("This field is required.")
;    ))
;(validate-with* "" (-> product-col-info :type :validators))
;(validate-with* "asdf" (-> product-col-info :type :validators))
;(validate-with* "HAWA" (-> product-col-info :type :validators))

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
;(validate-record product-col-info (makeit))

(defn product-attributes [product-col-info item]
  [:Values
   (reduce
    (fn [attrs [name col]]
      (if (:dbid col)
        (conj attrs [:Value
                     {:AttributeID (:dbid col)}
                     (escape-html (name item))])
        attrs))
    () product-col-info)])

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

