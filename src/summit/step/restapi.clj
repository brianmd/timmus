(def dbb
  [{:id "foo" :content "foo-content" :tags []}
   {:id "bar" :content "bar-content" :tags []}
   {:id "baz etc" :content "baz-content" :tags []}])


(ns summit.step.restapi
  (:require [clojure.string :as str]
            [net.cgrand.enlive-html :as html]
            [com.rpl.specter :as s]
                                        ;            [clojure.java.io :as io :refer [as-url make-parents]]

                                        ;            [clojure.data.xml :as xml]
                                        ;            [clojure.xml :as x]
            ;; [hiccup.core :as hiccup]
            ;; [clojure.core.reducers :as r]

                                        ;            [clojure.data.csv :as csv]
            ;; [clojure.java.io :as io]
            ;; [clojure.data.codec.base64 :as b64]

            ;; [summit.step.xml-output :refer :all]


            ;; [net.cgrand.enlive-html :as html]
            [summit.punchout.core :refer :all]

            [summit.utils.core :refer :all]

            [summit.step.import.product-selectors :refer :all]
            [summit.step.import.idw.product :as idw]
            [summit.step.import.ts.product :as ts]
            [summit.step.import.sap.product :as sap]
            ))

(def manufacturers-cache (atom {}))
(def products-cache (atom {}))

(def url "http://stibo-dev.insummit.com/restapi/")
(def context "?context=Context1")

(defn step-authentication []
  {:basic-auth ["Stepsys" "stepsys"]})

(defn get-url [url]
  (let [url (str url context)
        response (clj-http.client/get url (step-authentication))]
    (pp url)
    (if (= 200 (:status response))
      (xml->map (:body response))
      nil)))


(defn make-url [s]
  (str url s))

(defn entity-url [id]
  (make-url (str "entities/" id)))

(defn manufacturer-url [id]
  (entity-url id))

(defn download-manufacturer [id]
  (first (get-url (manufacturer-url id))))

(defn parse-manufacturer [m]
  (let [attrs (:attrs m)
        content (:content m)
        ]
    (assoc attrs


           ;;;    warning: code was moved and original line no longer works.
           ;;              Replaced with possibly incorrect new line.
           ;; :name (content-for-tag d :Name)
           :name (content-for-name m :Name)
           )))
;; (parse-manufacturer d)

;; (select-tag d :Values)
;; (def d (download-manufacturer "TsMfr3534"))
;; (ppn d)
;; (ppn (:content d))
;; (:tag (first (:content d)))
;; (ppn (manufacturer "TsMfr3534"))
;; (ppn (manufacturer "GoldenMfr8260"))

(defn manufacturer [id]
  (if-let [m (manufacturers-cache id)]
    m
    (let [m (parse-manufacturer (download-manufacturer id))]
      (when m
        (assoc! manufacturers-cache id m)
        m))))

(defn manufacturer-children [id]
  (get-url (str (manufacturer-url id) "/children")))

(defn root-manufacturers []
  (manufacturer-children "Manufacturer"))

(defn product-url [id]
  (make-url (str "products/" id)))

(defn product [id]
  (get-url (product-url id)))

;; (defn product-children [id]
;;   (get-url (str (product-url id) "/children")))


;; (ppn (product "MEM_GLD_102633"))
;; ;; (ppn (product-children "MEM_GLD_102633"))
;; (ppn (manufacturer "TsMfr3534"))
;; (ppn (manufacturer "GoldenMfr8260"))
;; (ppn (manufacturer-children "Manufacturer"))
;; (ppn (root-manufacturers))
;; (ppn (manufacturer-children "GoldenMfr8260"))
