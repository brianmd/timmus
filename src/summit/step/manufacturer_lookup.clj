(ns summit.step.manufacturer-lookup
  (:require [clojure.string :as str]
            [config.core :refer [env]]
            [clojure.java.io :as io :refer [as-url]]
            [clojure.data.xml :as xml]

            ;; [me.raynes.conch :refer [programs with-programs let-programs] :as sh]
            [me.raynes.conch.low-level :as shell]

            [com.rpl.specter :as s :refer [ALL LAST]]

            [summit.utils.core :refer :all]
            [summit.step.restapi :refer [manufacturer root-manufacturer golden-manufacturer? reset-caches]]
            ))

;; (def step-input-path (-> env :paths :local :step-input-path))
;; (def step-output-path (-> env :paths :local :step-output-path))

;(-> env :paths :local :step-input-path (#(str % "abc/")))

#_(defn filter-tags-named [tag-name tag-value eles]
  (filter #(= tag-value (tag-name %)) eles)
  )

#_(defn get-top-level-manufacturers [manufacturers]
  (->>
    manufacturers
    :content
    (filter-tags-named :tag :Entities)
    first
    :content
    first
    ))

#_(defn make-output-streams [basepath]
  (io/make-parents (str basepath "all.csv"))
  {
   :all (io/writer (io/file (str basepath "all.csv")))
   "IDW" (io/writer (io/file (str basepath "idw.csv")))
   "SAP" (io/writer (io/file (str basepath "sap.csv")))
   "TS" (io/writer (io/file (str basepath "ts.csv")))
   }
  )

#_(defn close-output-streams [streams]
  (map #(.close %) (vals streams)))

#_(defn get-attr [attr-id manuf]
  (filter #(= attr-id (-> :attrs :AttributeID)))
  )
#_(defn get-manufacturer-attribute [attr-name manuf]
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

#_(defn write-manufacturer [streams golden manuf]
  (let [source (get-manufacturer-attribute "MfrSource" manuf)
        sourceId (get-manufacturer-attribute "MfrSourceId" manuf)
        goldenId (-> golden :attrs :ID)]
    (.write (streams :all) (str source "," sourceId "," goldenId "\n"))
    (.write (streams source) (str sourceId "," goldenId "\n"))
    )
  )

;(defn get-id [manuf]
;  (-> manuf :attrs :ID))


#_(defn process-child [streams golden manu]
  (if (golden? manu)
    (doall (map (fn [x] (process-child streams manu x)) (filter-tags-named :tag :Entity (:content manu))))
    (write-manufacturer streams golden manu)
    )
  )

#_(defn create-manufacturer-lookup-tables []
  (let [manufacturers (xml/parse (java.io.FileReader. (str step-input-path "step/manufacturer.xml")))
        golden (get-top-level-manufacturers manufacturers)
        streams (make-output-streams (str step-output-path "lookup-tables/"))]
    (try
      (process-child streams nil golden)
      (finally
        (close-output-streams streams)
        ))))

;(create-manufacturer-lookup-tables)
;*e



(def basepath (str step-output-path "lookup-tables/"))

(defn write-manufacturer [streams golden manuf]
  (let [source (:mfr-source manuf)
        sourceId (:id manuf)
        goldenId (:id golden)]
    ;; (println (str source "," sourceId "," goldenId "\n"))
    (.write (streams :all) (str source "," sourceId "," goldenId "\n"))
    (.write (streams source) (str sourceId "," goldenId "\n"))
    ))

(defn process-child [streams golden-to-use golden]
  ;; (ppn (str "process-child" (:id golden)))
  ;; (println (:golden-children golden))
  ;; (ppn (dissoc golden :children :golden-children))
  ;; (ppn golden)
  (assert (golden-manufacturer? golden))
  (let [golden-to-use golden]
    (map! #(write-manufacturer streams golden-to-use (manufacturer %)) (:children golden))
    (map! #(process-child streams golden-to-use (manufacturer %)) (:golden-children golden))
    ))

(defn zipem []
  (with-programs [zip mv]
    (let [filenames (map! #(str basepath % ".csv") '(idw sap ts))
          zipname (str basepath "manufacturer-lookup")]
      (apply zip zipname filenames)
      (mv (str zipname ".zip") "resources/public/"))))

(defn create-manufacturer-lookup-tables []
  (reset-caches)
  (io/make-parents (str basepath "all.csv"))
  (with-open [all (io/writer (io/file (str basepath "all.csv")))
                 idw (io/writer (io/file (str basepath "idw.csv")))
                 sap (io/writer (io/file (str basepath "sap.csv")))
                 ts (io/writer (io/file (str basepath "ts.csv")))]
    ;; (let [root (root-manufacturer)
    (let [root (-> (root-manufacturer) :golden-children second manufacturer)
          streams {:all all
                   "IDW" idw
                   "SAP" sap
                   "TS" ts}]
      (process-child streams nil root)
      (zipem)
      nil
      )))

(examples
 (time (create-manufacturer-lookup-tables))
 )
