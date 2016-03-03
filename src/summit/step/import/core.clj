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

            [summit.utils.core :refer :all]
            ))

(make-record ColumnInfo '(name dbid type validators))

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

