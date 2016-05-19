(ns summit.step.import.attribute-definitions
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.ts.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]

            [summit.step.import.ts.a-meta :refer [ts-attributes]]
            [summit.step.import.idw.xls-spec-reader :refer [idea-attributes]]
            ))

(defn merged-specification-attributes []
  (merge @ts-attributes @idea-attributes))

(defn good-specs []
  (remove #(re-matches #"[_(].*" (:id %)) (vals (merged-specification-attributes))))

(defn tagit [entity-name tag-map]
  (str "<" entity-name " "
       (str/join " "
                 (map (fn [[k v]] (str (name k) "=\"" (if (nil? v) "" (str v)) "\"")) tag-map))
       ">"))
(tagit "Attribute" {:ID 23 :MultiValued false :MinValue nil :MaxValue nil :MaxLength nil :InputMask nil})

(defn spec->xml [m]
  (let [id-name (:id m)
        id (str "SP_" id-name)
        id-starts-with-alphanumeric? (re-matches #"[a-zA-Z0-9].*" id-name)
        parent-id (if id-starts-with-alphanumeric?
                    (str "SPG_" (str/upper-case (first id-name)))
                    "SPG_Misc")
        base-type "Text"
        title (:title m)
        descript (:descript m)
        types ["IDW_Member_Record" "TS_Member_Record" "SAP_Member_Record" "TSI_Member_Record"
               "Summit_Enrichment_Record" "GoldenRecordItem"]]
    (str
     (tagit "Attribute" {:ID (escape-html id) :MultiValued false :ProductMode "Normal" :FullTextIndexed false :ExternallyMaintained false :Selected true :Referenced true})
     "\n<Name>" (escape-html (->str (:title m))) "</Name>"
     (tagit "Validation" {:BaseType "Text" :MinValue nil :MaxValue nil :MaxLength nil :InputMask nil})
     "</Validation>"
     "\n<AttributeGroupLink AttributeGroupID=\"" (escape-html parent-id) "\"></AttributeGroupLink>"
     "\n<MetaData><Value AttributeID=\"4875\">" (escape-html (->str descript)) "</Value></MetaData>"
     (str/join "" (map #(str "<UserTypeLink UserTypeID=\"" % "\"></UserTypeLink>") types))
     "</Attribute>"
     )))
(spec->xml
 {:title "Output Formats",
  :type "AN",
  :descript "The organization of Output Information According to Preset Specification",
  :example "Txt, Csv; Screen Display; Screen And Printer",
  :abbv "",
  :id "Output_Formats",
  :source :idea})


(def top-xml
  "<?xml version=\"1.0\" encoding=\"utf-8\"?>
<STEP-ProductInformation ExportTime=\"2016-03-10 08:51:27 -0700\" ExportContext=\"Context1\" ContextID=\"Context1\" WorkspaceID=\"Main\" UseContextLocale=\"false\">")

(defn create-em []
  (spit-fn println (str step-output-path "attribute-definitions.xml")
           (concat [top-xml "<AttributeList>"] (map! #(spec->xml %) (good-specs))
                   ["</AttributeList></STEP-ProductInformation>"])))
;; (create-em)



(defn- spit-merged []
  (spitln (str step-output-path "merged-attributes.csv")
          (sort-by :id (good-specs))))
;; (spit-merged)

(examples
  (clear-humanized-words)
  (clear-clojurized-keywords)
  (def qrs (merged-specification-attributes))

  (take 2 @ts-attributes)
  (take 2 @idea-attributes)
  (take 3 (merged-specification-attributes))
  (take 3 (vals (merged-specification-attributes)))
  (take 5 (good-specs))

  (spit-merged)
  )

(str/upper-case (first "abc"))
(re-matches #"[a-zA-Z0-9].*" ".abc def")
