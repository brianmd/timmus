(ns timmus.step.xml-output
  (:require
    [clojure.java.io :as io]
    [clojure.xml :as c-xml]
    [clojure.data.xml :as xml]
    )
  )

(declare category-to-xml categories-to-xml)

(defn top-level-categories [categories]
  (filter #(nil? (second %)) categories))

(defn category-children [categories category]
  (let [id (first category)]
    (filter #(= id (second %)) categories)))

(defn ensure-not-struct-map [x]
  (if (= (type x) clojure.lang.PersistentStructMap)
    (into {} x)
    x))

(defn sans-content [node]
  (if (associative? node)
    (dissoc (ensure-not-struct-map node) :content)
    node))

(defn log-xml [node]
  (c-xml/emit-element (sans-content node))
  node)

(defn as-short-xml [node]
  (clojure.string/trim ; remove trailing \n
    (with-out-str (c-xml/emit-element (sans-content node)))))


(defn timenow []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z") (new java.util.Date))
  )

(defn doxml [tags all-categories]
  (as->                                                     ; read in reverse order
                                                            ; each item is nested inside the item below it
    (categories-to-xml tags all-categories (top-level-categories all-categories) 0)
    %
    [(xml/element
       (:type tags)
       {:ID (str (:rootType tags) " root")
        :UserTypeID (str (:rootType tags) " user-type root")
        :Selected "false"
        }
       %
       )]
    [(xml/element
       (str (:type tags) "s")
       {}
       %
       )]
    (xml/element
      :STEP-ProductInformation
      {:ExportTime (timenow)
       :ExportContext "EN All USA"
       :ContextID "EN All USA"
       :WorkspaceID "Main"
       :UseContextLocale "false"
       }
      %)
    ))


(defn categories-to-xml
  ([tags all-categories]
   (categories-to-xml tags all-categories (top-level-categories all-categories) 0))
  ([tags all-categories categories level]
   (map #(category-to-xml tags all-categories %) categories (+ level 1))))

(defn category-to-xml [tags all-categories category level]
    (apply xml/element
           (concat
             [(:type tags)
              {:ID (first category)
               :UserTypeID (if (:userTypePrefix tags)
                             (str (:userTypePrefix tags) (nth (:userTypeIds tags) level))
                             (:userTypeId tags))}
              (xml/element :Name {} (nth category 2))
              ]
             (categories-to-xml tags all-categories (category-children all-categories category) level)
             )))


