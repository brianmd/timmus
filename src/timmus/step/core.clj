(ns step.core
  (:require [clojure.string :as str]
            [config.core :refer [env]]
            [clojure.java.io :as io :refer [as-url make-parents]]
            [clojure.data.xml :as xml]

            [timmus.utils.core :refer :all]
            ))

(def input-path (-> env :paths :local :step-input-path))
(def output-path (-> env :paths :local :step-output-path))

;(-> env :paths :local :step-input-path (#(str % "abc/")))

(defn filter-tags-named [tag-name tag-value eles]
  (filter #(= tag-value (tag-name %)) eles)
  )

(defn get-top-level-manufacturers [manufacturers]
  (->>
    manufacturers
    :content
    (filter-tags-named :tag :Entities)
    first
    :content
    first
    ))

(defn make-output-streams [basepath]
  (make-parents (str basepath "all.csv"))
  {
   :all (io/writer (io/file (str basepath "all.csv")))
   "IDW" (io/writer (io/file (str basepath "idw.csv")))
   "SAP" (io/writer (io/file (str basepath "sap.csv")))
   "TS" (io/writer (io/file (str basepath "ts.csv")))
   }
  )

(defn close-output-streams [streams]
  (map #(.close %) (vals streams)))

(defn get-attr [attr-id manuf]
  (filter #(= attr-id (-> :attrs :AttributeID)))
  )
(defn get-manufacturer-attribute [attr-name manuf]
  (->>
    manuf                                                   ; STEP-ProductInformation
    :content                                                ; (EdgetTypes Values ...
    (filter-tags-named :tag :Values)                        ;
    first
    :content
    (filter-tags-named #(:AttributeID (:attrs %)) attr-name)
    first
    :content
    first
    ))

(defn write-manufacturer [streams golden manuf]
  (let [source (get-manufacturer-attribute "MfrSource" manuf)
        sourceId (get-manufacturer-attribute "MfrSourceId" manuf)
        goldenId (-> golden :attrs :ID)]
    (.write (streams :all) (str source "," sourceId "," goldenId "\n"))
    (.write (streams source) (str sourceId "," goldenId "\n"))
    )
  )

(defn get-id [manuf]
  (-> manuf :attrs :ID))

(defn golden? [manu]
  (contains? #{"Golden Manufacturer" "Manufacturer Root"} (-> manu :attrs :UserTypeID)))

(defn process-child [streams golden manu]
  (if (golden? manu)
    (doall (map (fn [x] (process-child streams manu x)) (filter-tags-named :tag :Entity (:content manu))))
    (write-manufacturer streams golden manu)
    )
  )

(defn create-manufacturer-lookup-tables []
  (let [manufacturers (xml/parse (java.io.FileReader. (str input-path "step/manufacturer.xml")))
        golden (get-top-level-manufacturers manufacturers)
        streams (make-output-streams (str output-path "lookup-tables/"))]
    (try
      (process-child streams nil golden)
      (finally
        (close-output-streams streams)
        ))))

;(create-manufacturer-lookup-tables)
;*e

