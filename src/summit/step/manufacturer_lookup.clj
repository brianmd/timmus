(println "loading summit.step.manufacturer-lookup")

(ns summit.step.manufacturer-lookup
  (:require [clojure.string :as str]
            [config.core :refer [env]]
            [clojure.java.io :as io :refer [as-url]]
            [clojure.data.xml :as xml]

            [me.raynes.conch :refer [programs with-programs let-programs] :as sh]

            [com.rpl.specter :as s :refer [ALL LAST]]

            [summit.utils.core :refer :all]
            [summit.step.restapi :refer [manufacturer root-manufacturer golden-manufacturer? reset-caches]]
            ))

(def basepath (str step-output-path "lookup-tables/"))

(defn write-manufacturer [streams golden manuf]
  (let [source   (:mfr-source manuf)
        sourceId (:id manuf)
        goldenId (:id golden)]
    (.write (streams :all) (str source "," sourceId "," goldenId "\n"))
    (.write (streams source) (str sourceId "," goldenId "\n"))
    ))

(defn process-child [streams golden-ancestor golden]
  (assert (golden-manufacturer? golden))
  (let [use-parent? (= (:mfr-show-parent golden) "true")
        golden-ancestor (if use-parent? golden-ancestor golden)]
    (map! #(write-manufacturer streams golden-ancestor (manufacturer %)) (:children golden))
    (map! #(process-child streams golden-ancestor (manufacturer %)) (:golden-children golden))
    ))

(defn zipem []
  (with-programs [zip mv]
    (let [filenames (map! #(str basepath % ".csv") '(idw sap ts))
          zipname   (str basepath "manufacturer-lookup")]
      (ppn filenames)
      (ppn zipname)
      (apply zip zipname filenames)
      (mv (str zipname ".zip") "resources/public/"))))
;; (zipem)

(defn create-manufacturer-lookup-tables []
  (ppn "creating manufacturer lookup tables ...")
  (reset-caches)
  (io/make-parents (str basepath "all.csv"))
  (with-open [all (io/writer (io/file (str basepath "all.csv")))
              idw (io/writer (io/file (str basepath "idw.csv")))
              sap (io/writer (io/file (str basepath "sap.csv")))
              ts  (io/writer (io/file (str basepath "ts.csv")))]
    (let [root    (root-manufacturer)
          ;; (let [root (-> (root-manufacturer) :golden-children second manufacturer)
          streams {:all  all
                   "IDW" idw
                   "SAP" sap
                   "TS"  ts}]
      (process-child streams nil root)
      ))
  (zipem)
  (ppn "ALL DONE creating manufacturer lookup tables ...")
  nil)

(examples
 (println "running lookup")
 (time (create-manufacturer-lookup-tables))
 )

(println "done loading summit.step.manufacturer-lookup")
