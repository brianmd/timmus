(ns timmus.utils.core
  (:require
    [clj-http.client :as client]
    [cheshire.core :refer :all]
    [clojure.pprint :refer [pprint]]
    [config.core :refer [env]]

    ;[cats.core :as m]
    ;[cats.builtin]
    ;[cats.monad.maybe :as maybe]
    )
  )


(def step-input-path (-> env :paths :local :step-input-path))
(def step-output-path (-> env :paths :local :step-output-path))


(defn logit [& args]
  (apply println args)
  (last args))

(defn as-document-num [string]
  (let [s (str "0000000000" string)]
    (subs s (- (count s) 10))))

(defn bh_login [email pw]
  (let [cred
        {"customer"
         {"email" email, "password" pw}
         "session"
         {"customer"
          {"email" email, "password" pw}
          }}
        params
        {:body         (generate-string cred)
         :content-type :json
         :accept       :json}
        ]
    (client/post
      "https://www.summit.com/store/customers/sign_in.json"
      params)
    ))


(def ^:dynamic level-function-names (list))
(def ^:dynamic levels-to-save 1)
(def ^:dynamic levels-to-print 0)
(def level-results (atom {}))

(defn deflevel-result-handler [result]
  (when (<= (count level-function-names) levels-to-save)
    (swap! level-results assoc level-function-names result))
  (when (<= (count level-function-names) levels-to-print)
    (println "\n------------------------------")
    (println (str "call stack: " level-function-names))
    (pprint result)
    (println "....\n")
    )
  result)

(defmacro defun [name args & body]
  (if (or (> levels-to-save 0) (> levels-to-print))
    (println "adding debug logging to" name)
    (println "using plain defn for" name "(not adding debug logging)")
    )
  (if (or (> levels-to-save 0) (> levels-to-print))
    `(defn ~name ~args
       ;(if (and (> (levels-to-print) 0) (= (count level-function-names) 1))
       ;  (println "\n------------------------------"))
       ;(println "level-function-names:" level-function-names (count level-function-names))
       (if (= (count level-function-names) 0)
         (reset! level-results {}))
       (binding [level-function-names (conj level-function-names '~name)]
         (let [result# (do ~@body)]
           (deflevel-result-handler result#)
           result#))
       )
    `(defn ~name ~args ~@body)
    ))



(defmacro macro->fn [m]
  `(fn [& args#]
     (eval
       (cons '~m args#))))
(defmacro make-record [name cols-names]
  `(apply (macro->fn defrecord) ['~name (->> ~cols-names (map name) (map symbol) vec)]))

(make-record ColumnInfo '(name dbid type validations))

(defprotocol Validator
  "validate "
  (field-validations [rec] "returns map: {field-name [predicate (fn [name val rec] msg) ...] ...}")
  (errors [rec] "returns map: {field-name [msg ...] ...")
  (valid? [rec] "returns true if errors is empty map"))

(def required #(and (not (nil? %)) (not= "" %)))
(def string-integer #(re-matches #"\d*" %))
(def string-float #(re-matches #"[\d.]*" %))
(def alphanumeric? #(re-matches #"[a-zA-Z0-9]*" %))


