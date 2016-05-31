(ns murphydye.utils.core
  (:require 
            [clojure.string :as str]
            [goog.i18n.DateTimeFormat :as dtf]
            [cljs.pprint :refer [pprint]]
            ;; [clojure.walk :refer :all]
            ))

(defn ppc [& args]
  (println "----------------------")
  (doseq [a args] (println (with-out-str (cljs.pprint/pprint a))))
  (last args))

(defn date? [d]
  (instance? js/Date d))

(def format-map
  (let [f goog.i18n.DateTimeFormat.Format]
    {:full-date (.-FULL_DATE f)
     :full-datetime (.-FULL_DATETIME f)
     :full-time (.-FULL_TIME f)
     :long-date (.-LONG_DATE f)
     :long-datetime (.-LONG_DATETIME f)
     :long-time (.-LONG_TIME f)
     :medium-date (.-MEDIUM_DATE f)
     :medium-datetime (.-MEDIUM_DATETIME f)
     :medium-time (.-MEDIUM_TIME f)
     :short-date (.-SHORT_DATE f)
     :short-datetime (.-SHORT_DATETIME f)
     :short-time (.-SHORT_TIME f)}))

(defn format-date-generic
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
or a formatting string like \"dd MMMM yyyy\""
  [date-format date]
  (.format (goog.i18n.DateTimeFormat.
            (or (date-format format-map) date-format))
           (js/Date. date)))

(defn ->str [a-name]
  (cond
    (string? a-name) a-name
    (number? a-name) a-name
    (date? a-name) (format-date-generic :short-date a-name)
    :else
    (str/replace
     (str/upper-case
      (if (keyword? a-name)
        (name a-name)
        (str a-name)))
     "-" "_")))

(defn ->keyword [a-string]
  (if (keyword? a-string)
    a-string
    (keyword
     (str/lower-case (str/replace (str a-string) "_" "-")))))

(defn ->int [v]
  (if (nil? v)
    nil
    (if (string? v)
      (let [v (.trim v)]
        (if (empty? v)
          nil
          (Math.round. (.parseFloat v))))
      (Math.round. v))))

(defn ->float [v]
  (if (nil? v)
    nil
    (if (string? v)
      (let [v (.trim v)]
        (if (empty? v)
          nil
          (.parseFloat v)))
      v)))

(defn as-integer [string]
  (->int string))

(def map! (comp doall map))
(def maprun (comp dorun map))

(defn third [s]
  (first (next (next s))))


(defonce clojurized-keywords (atom {}))

(defn set-clojurized-keyword [from-key to-key]
  (swap! clojurized-keywords assoc from-key to-key)
  to-key)

(defn clear-clojurized-keywords []
  (reset! clojurized-keywords {}))
;; (clear-clojurized-keywords)

(defn clojurize-keyword!
  "convert mixed case to lower case with hyphens. Considers 'ID' as a token."
  [key]
  (let [skey (if (keyword? key) (name key) (str key))
        s (str/replace skey #"[_\s-()]+" "-")
        s (str/replace s #"[^A-Z-][A-Z]" #(str (first %) "-" (second %)))
        s (str/replace s #"^-+" "")
        s (str/replace s #"-+$" "")
        s (str/lower-case s)
        k (keyword (if (= \- (first s)) (subs s 1) s))]
    k))

(defn clojurize-keyword
  "memoized. can override this function's output with your own via set-clojurized-keyword."
  [key]
  (let [skey (if (keyword? key) (name key) (str key))]
    (if-let [k (@clojurized-keywords skey)]
      k
      (set-clojurized-keyword key (clojurize-keyword! skey)))))

(defn clojurize-map [m]
  (into {}
        (map! (fn [[k v]] [(clojurize-keyword k) v]) m)))
;; (clojurize-map {"ab_cd" 4 "AbcDef" 9})

;; (defn clojurize-map-keywords [m]
;;   (s/transform (s/walker keyword?)
;;                clojurize-keyword
;;                m))

(defn humanize! [s]
  (str/join " "
            (map str/capitalize
                 (-> s clojurize-keyword name (str/split #"-") ))))
;; (assert= "Parent Id" (humanize! "ParentID"))
;; note: we would prefer "Parent ID"

(defonce humanized-words (atom {}))

(defn set-humanized [from-word to-word]
  (swap! humanized-words assoc from-word to-word)
  to-word)

(defn clear-humanized-words []
  (reset! humanized-words {}))
;; (clear-humanized-words)

(defn humanize
  "memoized. can override this function's output with your own via set-humanized."
  [word]
  (let [word (.trim (->str word))
        low-word (str/lower-case word)]
    (if-let [w (@humanized-words low-word)]
      w
      (set-humanized low-word (humanize! word)))))




(defn zero-pad [width string]
  (if string
    (let [s (str (apply str (repeat width "0")) string)]
      (subs s (- (count s) width)))))

(defn as-matnr [string]
  (zero-pad 18 string))

(defn as-document-num [string]
  (zero-pad 10 string))
;; (as-document-num "asdf")

(defn as-short-document-num [string]
  "remove leading zeros"
  (if string (str/replace string #"^0*" "")))
;; (as-short-document-num (as-document-num "00001"))

(defn select-keys2 [m keys]
  (map #(->str (m %)) keys))

(defn select-keys3 [m keys]
  (let [keys (set keys)]
    (into {} (filter (fn [[k v]] (if (contains? keys k) [k v])) m))))

(defn get-unique [maps key]
  (set (map #(% key) maps)))
