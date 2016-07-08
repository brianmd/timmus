(ns summit.step.ts-leaf
  (:require
    [clojure.data.csv :as csv]
    [clojure.java.io :as io]
    [clojure.data.codec.base64 :as b64]
    [clojure.data.xml :as xml]

    [summit.utils.core :refer :all]
    [summit.step.xml-output :refer :all]
    )
  (:import
    [java.security MessageDigest]
   )
  )

(declare
  create-ts-leaf-hierarchy
    get-leaf-class-names
      process-ts-items-with
        extract-leaf-class-names titleize-all get-leaf-class remove-empty-strings titleize
    create-leaf-category-tuples
      make-hashes make-id-hash sha1 make-tuples
    output-tuples
      ;doxml
      ts-leaf-tag-names
  )

(def ts-dir (str step-input-path "trade-service/"))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-ts-leaf-hierarchy []
  (->
    (get-leaf-class-names)
    (create-leaf-category-tuples)
    (output-tuples)
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-leaf-class-names []
  (->
    (process-ts-items-with extract-leaf-class-names)
    remove-empty-strings
    titleize-all
    ))

          ; pipe [ leaf-name* ]

(defn create-leaf-category-tuples [leaf-names]
  (->>
    leaf-names
    (make-hashes "TS_LEAF_")
    (make-tuples "TS_LEAF_")
    )
  )

          ; pipe [ [ id parentId name ]* ]

(defn output-tuples [tuples]
  (xml/emit-str
    (doxml ts-leaf-tag-names tuples)
    )
  )



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   get-leaf-class-names functions

(defn process-ts-items-with [fn]
  (with-open [in-file (io/reader (str ts-dir "item.txt"))]
    (let [lines (csv/read-csv in-file :separator \tab :quote \~)]
      (fn lines)
      )))

(defn extract-leaf-class-names
  [lines]
  (let [leaves (atom #{})]
    (->> lines
         (rest)
         (map get-leaf-class)
         (take 25)
         ;(logit)
         (map #(swap! leaves conj %))
         (doall)
         )
    @leaves))

(defn get-leaf-class [arr]
  ;(logit 27
    (nth arr 27))
    ;)

(defn remove-empty-strings [strings]
  (disj strings ""))

(defn titleize-all [words]
  (set
    (map titleize words)))

(defn titleize
  "Capitalize every word in a string"
  [s]
  (->> (clojure.string/split (str s) #"\b")
       (map clojure.string/capitalize)
       clojure.string/join))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   process-ts-items-with functions

(defn make-hashes [prefix strings]
  ;(println prefix strings)
  (let* [len (count prefix)
         h (atom {})
         max-chars (- 40 len)]
    (doall
      (map (fn [x]
             (swap! h assoc x
                    (str
                      prefix
                      (if (> (count x) max-chars)
                        (sha1 (.toUpperCase x))
                        (.toUpperCase x)
                        )))
                      )
           strings)
      )
    @h
    ))

(defn leaf-parent [name]
  (subs name 0 1))

(defn make-tuples [prefix hashes]
  (let [tuples (into [] (for [[name id] hashes] [id (str prefix (leaf-parent name)) name]))]
    (into tuples (for [[name _] hashes] [(str prefix (leaf-parent name)) nil (leaf-parent name)]))
    ))

(defn- bytes->hex [original]
  (apply str (map #(format "%02x" %) original)))

(defn bytes->base64 [original]
  (String. (b64/encode original) "UTF-8"))

(defn- sha1
  "sha1 base64 digest"
  [s]
  (let [md (MessageDigest/getInstance "SHA1")
        bytes (.getBytes s "ascii")]
    (.update md bytes)
    (bytes->base64 (.digest md))))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;   output-tuple functions

(def blue-hierarchy
  {:type "Product"
   :rootType "Product"
   :baseId "Summit_Member_Records"
   :baseName "Summit Member Records"
   :baseParentId "Product hierarchy root"
   })

(def sap-tag-names
  (assoc blue-hierarchy
    :rootHierarchyId "SAP_Member_Records"
    :rootHierarchyName "SAP Member Records"
    :rootUserTypeId "SAP_Hierarchy_Root"
    :productIdPrefix "SAP_UNSPSC_"
    :userTypePrefix "SAP_"
    ))

(def ts-unspsc-tag-names
  (assoc blue-hierarchy
    :rootHierarchyId "TS_Member_Records"
    :rootHierarchyName "TS Member Records"
    :rootUserTypeId "TS_Hierarchy_Root"
    :productIdPrefix "TS_UNSPSC_"
    :userTypePrefix "TS_"
    :userTypeIds ["Segment" "Family" "Class" "Commodity"]
    ))

(def idw-unspsc-tag-names
  (assoc blue-hierarchy
    :rootHierarchyId "IDW_Member_Records"
    :rootHierarchyName "IDW Member Records"
    :rootUserTypeId "IDW_Hierarchy_Root"
    :productIdPrefix "IDW_UNSPSC_"
    :userTypePrefix "IDW_"
    :userTypeIds ["Segment" "Family" "Class" "Commodity"]
    ))



(def yellow-hierarchy
  {:type "Classification"
   :rootType "Classification 1"
   :baseId "Summit_Member_Records"
   :baseName "Summit Member Records"
   :baseParentId "Product hierarchy root"
   :userTypePrefix nil
   :userTypeId "Category"
   })

(def ts-leaf-tag-names
  (assoc yellow-hierarchy
    :rootHierarchyId "TS_Leaves"
    :rootHierarchyName "Trade Service Leaf Hierarchy"
    :rootUserTypeId "TS_LeafRoot"
    :idPrefix "TsLeaf-"
    ))

(def unspsc-tag-names
  (assoc yellow-hierarchy
    :rootHierarchyId "UNSPSC_Hierarchy"
    :rootHierarchyName "UNSPSC Hierarchy"
    :rootUserTypeId "UNSPSC_Root"
    :idPrefix "UNSPSC_"
    :userTypeIds ["Segment", "Family", "Class", "Commodity"]
    ))


