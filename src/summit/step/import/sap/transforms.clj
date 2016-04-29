(ns summit.step.import.sap.transforms
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url make-parents]]

            [clojure.data.xml :as xml]
            [clojure.xml :as x]
            [hiccup.core :as hiccup]
            [clojure.core.reducers :as r]

            [clojure.data.csv :as csv]
            [semantic-csv.core :as scsv ] ;:refer :all]
            [clojure.java.io :as io]
            [clojure.data.codec.base64 :as b64]
            ;; [clojure.pprint :refer :all]

            [summit.step.xml-output :refer :all]

            [summit.utils.core :refer :all]
            [summit.step.import.core :refer :all]
            [summit.step.import.sap.core :refer :all]
            ;; [summit.step.import.product-selectors :refer [slurp-source-ids]]
            [summit.step.import.product-selectors :refer :all]
            ))

(defn streamr [transducer-fn item-fn coll]
  (transduce transducer-fn
             (fn [& y] (if (= 2 (count y)) (item-fn (second y))))
             coll))
(examples
 (streamr (comp (filter even?) (take 4))
          println
          (range 100))
 )

(def inventory
  {:filename (str sap-input-path "STEP_INVENTORY.txt")
   :fieldnames [:matnr :service-center :mrp-type :status :abc-indicator :delivery-time :tool-identifier :unit-price :unrestricted-stock]})
;; (count (:fieldnames inventory))

(def bought-with
  {:filename (str sap-input-path "STEP_FREQ_BOUGHT.txt")
   :fieldnames [:from-matnr :from-times-bought :to-matnr :to-times-bought :times-bought-together :percentage]
   ;; :cast {:from-matnr ->int :from-times-bought ->int :to-matnr ->int :to-times-bought ->int :times-bought-together ->int}})
   :cast [->int ->int ->int ->int ->int ->float]})

;; (defrecord BoughtWith (mapv (comp symbol name) (:fieldnames bought-with)))

(def bought
  {:filename (str sap-input-path "STEP_TIMES.txt")
   :fieldnames [:matnr :service-center :times-bought]
   ;; :cast {:from-matnr ->int :from-times-bought ->int :to-matnr ->int :to-times-bought ->int :times-bought-together ->int}})
   :cast [->int str/trim ->int]})

(def service-center
  {:filename (str sap-input-path "STEP_SERVICE_CENTER_LIST.txt")
   :fieldnames [:code :state :city :zip :inactive :tax-jurisdiction]})

(defn cast-vector [transforms v]
  (map #(%1 %2) transforms v))


;; (->int nil)
;; (->int "")
;; (defn ->short)
;; (defn ->int [v]
;;   (if (or (nil? v) (empty? v))))

(defn process-file-info [file-info filter-fn process-row-fn]
  (with-open [in-file (io/reader (:filename file-info))]
    (let [fieldnames (:fieldnames file-info)
          num-cols (count fieldnames)]
      (streamr (comp
                (drop 1)
                (filter #(> (count %) 2))
                ;; pp
                (map #(cast-vector (:cast file-info) %))
                (map #(into {} (map vector fieldnames %)))
                filter-fn
                )
               process-row-fn
               (csv/read-csv in-file :separator \| :quote \^)
               ))))

(def bought-products (atom {}))
(def bought-together (atom {}))
(defn add-bought-products [m]
  (swap! bought-products assoc (:from-matnr m) {:matnr (:from-matnr m) :qty (:from-times-bought m)})
  (swap! bought-products assoc (:to-matnr m) {:matnr (:to-matnr m) :qty (:to-times-bought m)})
  (if (< (:from-matnr m) (:to-matnr m)) (swap! bought-together assoc [(:from-matnr m) (:to-matnr m)] (:times-bought-together m)))
  )
(examples
 (time
  (process-file-info bought-with
                     (comp
                      ;; (take 4)
                      )
                     add-bought-products
                     ;; pp
                     ))
 ;; (deref bought-products)
 (count (deref bought-products))
 )

(def service-center-bought (atom {}))
(defn add-service-center-bought [m]
  (swap! service-center-bought assoc [(:matnr m) (:service-center m)] (:times-bought m)))

(examples
 (time
  (process-file-info bought
                     (comp
                      ;; (take 4)
                      )
                     add-service-center-bought
                     ;; pp
                     ))
 (count @service-center-bought)
 )

(defn arg2nd [f]
  (fn [& args]
    (if (= (count args) 2)
      (f (second args)))))
(examples
 (assert-= nil ((arg2nd identity) 3 4 5) ((arg2nd identity) 3))
 (assert-= 4 ((arg2nd identity) 3 4))
 )

(transduce (comp (take 7) (map flatten) (map (partial interpose ",") )) println @service-center-bought)
(transduce (comp (take 7) (map flatten) (map (partial interpose ",") )) (arg2nd println) @service-center-bought)
(transduce (comp (take 7) (map flatten) (map (partial interpose ",") )) (arg2nd #(println (apply str %))) @service-center-bought)
(transduce (comp (take 7) (map flatten) (map (partial interpose ",") )) (arg2nd #(->> % (apply str) println)) @service-center-bought)
(transduce (comp (take 7) (map flatten) (map (partial interpose ","))))

(pp (take 3 @bought-products))
(pp (map second (take 3 @bought-products)))

(pp (take 3 @bought-together))
(pp (map flatten (take 3 @bought-together)))




(with-open [out (io/writer (str sap-input-path  "bought-products.csv"))]
  (transduce (comp
              ;; (take 7)
              (map second)
              (map vals)
              (map (partial interpose ","))
              )
             (arg2nd #(->> % (apply str "\n") (.write out)))
             @bought-products))
"
// load # times products were bought
LOAD CSV
FROM 'file:///Users/bmd/data/stibo/input/sap/bought-products.csv' AS line
WITH line
merge (p:Product {matnr: toint(line[0]), times_bought: toint(line[1])})
;
"

(with-open [out (io/writer (str sap-input-path  "bought-together.csv"))]
  (transduce (comp
              ;; (take 7)
              (map flatten)
              (filter #(> (nth % 2) 1))
              (map (partial interpose ",")))
             (arg2nd #(->> % (apply str "\n") (.write out)))
             @bought-together))

"
USING PERIODIC COMMIT 1000
LOAD CSV
FROM 'file:///Users/bmd/data/stibo/input/sap/bought-together.csv' AS line
WITH line
match (p1:Product {matnr: toint(line[0])})
match (p2:Product {matnr: toint(line[1])})
merge (p1)-[:BOUGHT_WITH {times: toint(line[2])}]->(p2)
;
"

(with-open [out (io/writer (str sap-input-path  "sc-bought.csv"))]
  (transduce (comp
              ;; (take 7)
              (map flatten)
              (map (partial interpose ",")))
             (arg2nd #(->> % (apply str "\n") (.write out)))
             @service-center-bought))

"
LOAD CSV
FROM 'file:///Users/bmd/data/stibo/input/sap/sc-bought.csv' AS line
WITH line
merge (p:Product {matnr: toint(line[0])})
merge (sc:ServiceCenter {code: line[1]})
merge (p)-[:BOUGHT {times: toint(line[2])}]->(sc)
;
"


"
// let's create a score that goes two depths
// should it take into account service center?
// should it take into account percentage?
match (p:Product {matnr: 7662})-[r]-(n) return p,r,n limit 25;
"

;; ((partial map vector (:fieldnames service-center)) (range 6))
;; (def mmm (partial map vector (:fieldnames service-center)))
;; (mmm (range 6))

;; (cast-with {:a ->int} [{:a "12a"}])
;; (defn normalize-bought-with [m]
;;   (cast-with (:cast bought-with) (vector m)))

;; (defn vector-cast [transforms v]
;;   )

;; (cast-with {:from-matnr ->int} [{:from-matnr "000034"}])
;; (cast-with (:cast bought-with) {:from-matnr "000034"})
;;   {:from-matnr (->int (:from-matnr m))})

;; (process-file-info bought-with
;;                    (comp
;;                     ;; (filter #(not= "NM" (:state %)))
;;                     ;; #(cast-with {:from-matnr ->int} %)
;;                     ;; (map normalize-bought-with)
;;                     ;; (partial cast-with {:from-matnr ->int})
;;                     ;; (juxt (:cast bought-with))
;;                     (take 4)
;;                     )
;;                    println
;;                    )
;; (process-file-info service-center
;;                    (comp
;;                     (filter #(not= "NM" (:state %)))
;;                     (take 4))
;;                    println
;;                    )
;; (println "--------")

(examples
 (into [] (comp
           (filter even?)
           (take 4))
       (range 100))
 (sequence (comp
            (filter even?)
            (take 4))
           (range 100))
 (transduce (comp
             (filter even?)
             (take 4))
            println  ;; receives two parameters, the init value (0) or the result of this, and the value from the collection
            0
            (range 100)))







(examples
 (
  (->>
   (take 4)) (range 10))
 #(->>
   %
   (filter not-nm)
   (take 4))



 (let [*2 #(* % 2)
       *4 #(* % 4)]
   (def weird-composition
     (comp
      (filter even?)
      (map *2)
      (map *4))))
 (into [] weird-composition [1 2 3 4])

 (defn process [file-info f]
   (f))

 (process service-center
          (fn [line]
            (->
             line

             ))))
