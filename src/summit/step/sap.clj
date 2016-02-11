(ns summit.step.sap
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            ))

(def sap-path (str step-input-path "sap/"))

(defn process-sap-file-with [filename fn]
  (with-open [in-file (io/reader (str sap-path filename))]
    (let [lines (csv/read-csv in-file :separator \| :quote \^)]
      (fn lines)
      )))

(def material-types #{"HAWA" "KITS" "ZATB" "ZCMF" "ZFIM" "ZLOT"})
(def category-types #{"DIEN" "LUMF" "NLAG" "NORM" "SAMM" "ZFEE" "ZLAG" "ZTBF" "ZZLL"})
(def uom-types #{"BAG" "BX" "EA" "FT" "LB" "M" "P2" "RL"})

(defn lookup-validator [lookup-table]
  [#(contains? lookup-table %) #(str "Value not in lookup table: " %)])

(def material-col-info-array
  ;[:matnr "MARA-MATNR" String [:required :digits [:count 17]]
  [:matnr "MARA-MATNR" String [:required :digits]
   :type "MARA-MTART" String [:required (lookup-validator material-types)]
   :title "MAKT-MAKTX" String [:required]
   :uom "MARA-MEINS" String [(lookup-validator uom-types)]
   :summit-part "MARA-BISMT" String []

   :pp-restrict "MARA-MSTAE" String []

   :category-type "MARA-MTPOS_MARA" String [(lookup-validator category-types)]
   :ean11 "MARA-EAN11" String [:digits]
   :using-upc "MARA-NUMTP" String []
   :generic-non-stock? "MARA-ZZGNONSTK" String []
   :ts-item-pik "MARA-ZZTS-ITEM-PIK" String []
   :batch? "MARA-XCHPF" String []
   :mfr-part-num "MARA-MFRPN" String []
   :mfr-id "MARA-MFRNR" String []
   :delivery-unit "MVKE-SCMNG" String []
   :category-group "MVKE-MTPOS" String [(lookup-validator category-types)]         ; mvke => sales data for material
   :descript "STXH/STXL-GRUN_TXT" String []
   ])

(def material-col-info
  (into {} (map (fn [x] [(first x) (apply ->ColumnInfo x)]) (partition 4 material-col-info-array)))
  )

(def material-col-names
  (->> material-col-info-array (partition 4) (map first)))

(make-record SapMaterial material-col-names)
;(apply ->SapMaterial (range 17))

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

(defn sap-material [cols]
  (let [errors (validate-with* cols [[#(= (count %) 17) #(str "Wrong number of columns: " (count %) " instead of " 17)]])]
    (if (not-empty errors)
      (do (logit errors cols "----") (println) nil)
      (let [record (apply ->SapMaterial (map str/trim cols))
            errs (validate-record material-col-info record)]
        (if (not-empty errs)
          (do (logit errs cols "----") nil)
          record)
        )
      )))

;(defn bookshelf-from [data]
;  (xml/element :books {}
;               (reduce
;                 (fn [books b]
;                   (conj books (xml/element :book {:author (:author b)} (:title b))))
;                 () data)))
;x
;(def y (first x))
;y
;material-col-info
;(keys material-col-info)
;(-> material-col-info :matnr)
;(-> material-col-info :matnr :dbid)
;(defn sap-material-attributes [item]
;  (xml/element :Values {}
;               (reduce
;                 (fn [attrs [name col]]
;                   (conj attrs (xml/element :Value {:AttributeID (:dbid col)} (name item))))
;                 () material-col-info)))
;(sap-material-attributes y)
;(xml/emit-str (sap-material-attributes y))
;(xml/emit (sap-material-attributes y) *out*)
;(xml/emit-char-seq (sap-material-attributes y) "UTF-8")
;y

(defn sap-material-attributes [item]
  [:Values
        (reduce
          (fn [attrs [name col]]
            (conj attrs [:Value
                         {:AttributeID (:dbid col)}
                         (escape-html (name item))]))
          () material-col-info)])
;(defn sap-material-attributes-orig [item]
;  {:tag :Values
;   :content
;        (reduce
;          (fn [attrs [name col]]
;            (conj attrs {:tag     :Value
;                         :attrs   {:AttributeID (:dbid col)}
;                         :content [(escape-html (name item))]}))
;          () material-col-info)})
;(time (write-sap-file))
;*e

;(sap-material-attributes y)
;(x/emit-element (sap-material-attributes y))
;(x/emit-element {:tag :hello :attrs {:place "world"}})
;(x/emit-element {:tag :parent
;                 :attrs {:id "22" :name "fritz"}
;                 :content [
;                           {:tag :child :attrs {:id "56"}}
;                           {:tag :child :attrs {:id "57"}}]})

(defn sap-material-hiccup-orig [item]
  {:tag :Product
   :attrs {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   :content
   [(sap-material-attributes item)]})

(defn sap-material-hiccup [item]
  [:Product
   {:ID (str "MEM_SAP_" (as-short-document-num (:matnr item)))
           :UserTypeID "SAP_Member_Record"
           :ParentID "SAP_Member_Records"}
   (sap-material-attributes item)
   ]
   ;:content
  )

(defn sap-material-xml [item]
  (println
    (hiccup/html (sap-material-hiccup item)))

  ;(xml/emit (logit (sap-material-hiccup item)) *out*)
  ;(xml/emit-element (logit (sap-material-hiccup item)))
  item)

(defn transform-sap-material [item]
  (assoc item :matnr (as-short-document-num (:matnr item))))

;(time (write-sap-file))
;x/*state*
;x/*sb*
;(str x/*sb*)
;(x/emit-element {:tag :hello :content ["world"]})
;(x/emit {:tag :hello :content ["world"]})
;(x/emit-element (sap-material-attributes y))
;(sap-material-hiccup y)
;(sap-material-xml y)
;y

(defn process-sap-material [lines]
  (let [categories (atom #{})]
    (->> lines
         rest
         (take 50)
         ;(take 5)
         ;logit
         (map sap-material)
         (remove nil?)
         ;logit
         (map transform-sap-material)
         (map sap-material-xml)
         ;(rest)
         ;(map get-leaf-class)
         ;(take 25)
         ;(map #(swap! leaves conj %))
         (doall)
         )
    nil
    ))

(def blue-hierarchy
  {:type "Product"
   :rootType "Product"
   :baseId "Summit_Member_Records"
   :baseName "Summit Member Records"
   :baseParentId "Product hierarchy root"
   })

;(defn create-sap-material-stepxml [tags all-categories]
;  (as->                                                     ; read in reverse order
;    (categories-to-xml tags all-categories (top-level-categories all-categories) 0); each item is nested inside the item below it
;    %
;    [(xml/element
;       (:type tags)
;       {:ID (str (:rootType tags) " root")
;        :UserTypeID (str (:rootType tags) " user-type root")
;        :Selected "false"
;        }
;       %
;       )]
;    [(xml/element
;       (str (:type tags) "s")
;       {}
;       %
;       )]
;    (xml/element
;      :STEP-ProductInformation
;      {:ExportTime (timenow)
;       :ExportContext "EN All USA"
;       :ContextID "EN All USA"
;       :WorkspaceID "Main"
;       :UseContextLocale "false"
;       }
;      %)
;    ))

;(sap-material [192 2])
;(sap-material (map str (range 17)))
;(with-open [w (clojure.java.io/writer  "/Users/bmd/Downloads/sap.xml")]
;   (.write w (str "hello" "world")))

(defn top-tag []
  (with-out-str
    (x/emit
      (xml/sexp-as-element
        (stepxml-top-tag)
        ))))

(defn opening []
  (str/join "\n"
    (take 3 (str/split-lines (top-tag)))))

(defn closing []
  ((comp str/join reverse take2 reverse) (str/split-lines (top-tag))))
  ;((comp take2 reverse) (str/split-lines (top-tag))))
  ;reverse
  ;(-> (top-tag) str/split-lines (reverse #(take 2 %) reverse)))
;((comp str/join take2 reverse) (str/split-lines (top-tag)))
;(-> (str/split-lines (top-tag)) (fn [x] reverse x) vec)
;(-> (str/split-lines (top-tag)) #(reverse %) (fn [x] (take 2 x)))
(opening)
(closing)
(type (top-tag))
(top-tag)

(defn write-sap-file []
  (println "in write-sap")
  (with-open [w (clojure.java.io/writer "/Users/bmd/Downloads/sap.xml")]
    (binding [*out* w]
      (println (opening))
      (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
      (println (closing))
      )))
;(time (write-sap-file))
;(process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
;(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material))
;(map :matnr x)

;<STEP-ProductInformation ExportTime="2016-02-05 12:18:34" ExportContext="Context1" ContextID="Context1" WorkspaceID="Main" UseContextLocale="false">
;  <Products>
;    <Product ID="MEM_SAP_100378" UserTypeID="SAP_Member_Record" ParentID="SAP_Member_Records">
;      <Values>
;        <Value AttributeID="MARA-MFRPN">Manu Part num</Value>
;(xml/sexp-as-element
;  [:foo {:foo-attr "foo value"}
;   [:bar {:bar-attr "bar value"}
;    [:baz {} "The baz value"]]])

;(stepxml-opening-tag)
;(xml/emit-str (stepxml-opening-tag))
;(x/emit-element
;  (stepxml-opening-tag)
;  )

;(def djy
;  {"name" {:first-name "Dave"
;          :last-name "Yarwood"}
;   :age 28
;   :hobbies ["music" "languages" "programming"]})
;
;(get-in djy ["name" :last-name])


