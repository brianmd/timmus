(ns summit.utils.core
  (:require
    [clj-http.client :as client]
    [cheshire.core :refer :all]
    [clojure.pprint :refer [pprint]]
    [config.core :refer [env]]
    [clojure.walk :refer :all]
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.java.io :as io]

    [cheshire.generate :refer [add-encoder encode-str remove-encoder]]
    ;[cats.core :as m]
    ;[cats.builtin]
    ;[cats.monad.maybe :as maybe]

    [net.cgrand.enlive-html :as html]
    )
  )


(def step-input-path (-> env :paths :local :step-input-path))
(def step-output-path (-> env :paths :local :step-output-path))

(add-encoder clojure.lang.Delay
             (fn [c jsonGenerator]
               (.writeString jsonGenerator (str c))))

;(map
;  #(add-encoder %
;                (fn [c jsonGenerator]
;                  (.writeString jsonGenerator (str c))))
;  [clojure.lang.Delay org.httpkit.server.AsyncChannel java.lang.Class java.lang.Long])
  ;[clojure.lang.Delay org.httpkit.server.AsyncChannel])

(defn clean-all [x]
  (parse-string (generate-string x)))

(defn stringify-all [x]
  (postwalk str x))
  ;(postwalk #(if(keyword? %)(name %) %) x))

(defn logit [& args]
  (binding [*out* *err*]
    (map pprint args))
  (last args))

(defn as-matnr [string]
  (let [s (str "000000000000000000" string)]
    (subs s (- (count s) 18))))
(defn as-document-num [string]
  (let [s (str "0000000000" string)]
    (subs s (- (count s) 10))))
(as-document-num "asdf")
(defn as-short-document-num [string]
  "remove leading zeros"
  (str/replace string #"^0*" ""))
;(as-short-document-num (as-document-num "1"))

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


(defn col-names [definition-vector]
  (->> definition-vector (partition 4) (map first)))

(defmacro make-record [name cols-names]
  `(apply (macro->fn defrecord) ['~name (->> ~cols-names (map name) (map symbol) vec)]))
;; (defmacro make-record [name definition-vector]
;;   `(apply (macro->fn defrecord) ['~name (->> ~(col-names definition-vector) (map name) (map symbol) vec)]))

(defprotocol Validator
  "validate "
  (field-validations [rec] "returns map: {field-name [predicate (fn [name val rec] msg) ...] ...}")
  (errors [rec] "returns map: {field-name [msg ...] ...")
  (valid? [rec] "returns true if errors is empty map"))

(defn not-re-matches [regex string]
  (not (re-matches regex string)))
(def test-failure #(re-matches #"au9234721324189712345qiouqwre" %))
(def required #(and (not (nil? %)) (not= "" %)))
(def digits #(re-matches #"\d*" %))
(def string-float #(re-matches #"[\d.]*" %))
(def alphanumeric? #(re-matches #"[a-zA-Z0-9]*" %))

(def validators
  {:test-failure [test-failure #(str "The deck was stacked against bro: " %)]
   :digits       [digits #(str "Must be digits only: " %)]
   :required     [required (fn [_] "This field is required.")]
   })

(defn short-timenow []
  (.format (java.text.SimpleDateFormat. "yyyyMMddHHmmssZ") (new java.util.Date))
  )

(defn timenow []
  (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z") (new java.util.Date))
  )

(defn plain-timenow []
  (clean-all (new java.util.Date))
  )

(defn stepxml-top-tag []
  [:STEP-ProductInformation
   {:ExportTime (timenow)
    ;:ExportContext "Context1"
    :ContextID "Context1"
    :WorkspaceID "Main"
    :UseContextLocale "false"
    }
   [:Products]])


;(map * [1 2 3] [4 5 6])
;(for [i [1 2 3] j [4 5 5]] (* i j))
;(mapcat (fn[j] (map #(* % j) [4 5 6])) [1 2 3])
;(mapcat (fn[j] (map (fn[i] (* i j)) [4 5 6])) [1 2 3])
;
;(defn zip
;  [& colls]
;  (apply map vector colls))
;
;(for [[i j] (zip [1 2 3] [4 5 6])] (* i j))
;
;(zip [1 2 3] [4 5 6] [7 8 9 9])
;(interleave [1 2 3] [4 5 6])
;(time (dorun (for [x (range 1000) y (range 10000) :while (> x y)] [x y])))
;(time (doall (for [x (range 10) y (range 10) :while (> x y)] [x y])))
;(time (doall (for [x (range 10) y (range 10) :when (> x y)] [x y])))
;(time (dorun (for [x (range 1000) y (range 10000) :when (> x y)] [x y])))
;(for [[x y] '([:a 0] [:b 2] [:c 0]) :when (= y 0)] x)

(defn is-search-page? [txt]
  (re-find #"Refine By" txt))

(defn platt-page-category [page]
  (cond
    (not-empty (html/select page [:span.ProductPriceOrderBox])) :product
    (not-empty (html/select page [:div.no-products-section])) :not-found
    (not-empty (html/select page [:div.refineByHeader])) :search
    :else :unknown
    ))
;(platt-page-category q)

(defn has-platt-download? [search-str]
  (or
    (.exists (io/as-file (str "/Users/bmd/data/crawler/platt/product/" search-str ".html")))
    (.exists (io/as-file (str "/Users/bmd/data/crawler/platt/search/" search-str ".html")))
    (.exists (io/as-file (str "/Users/bmd/data/crawler/platt/not-found/" search-str ".html")))
    (.exists (io/as-file (str "/Users/bmd/data/crawler/platt/unknown/" search-str ".html")))
    ))

(defn platt-url [search-str]
  (str "https://www.platt.com/search.aspx?q=" search-str "&catNavPage=1&navPage=1_16_0")
  )

(defn html->enlive [html]
  (html/html-resource (java.io.StringReader. html)))

(defn htmlfile->enlive [html]
  (html/html-resource (java.io.FileReader. html)))



(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&" "&amp;")
      (replace "<" "&lt;")
      (replace ">" "&gt;")
      (replace "\"" "&quot;")))
(defn unescape-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&amp;" "&")
      (replace "&lt;" "<")
      (replace "&gt;" ">")
      (replace "&quot;" "\"")
      (replace "=>" ":")
      ))
#_(defn unescape-fat-arrow-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&amp;" "&")
      (replace "&lt;" "<")
      (replace "&gt;" ">")
      (replace "&quot;" "\"")
      (replace "=>" ":")
      ))


(defn save-platt-file [search-str html]
  (let [dir "/Users/bmd/data/crawler/platt/"
        content (html->enlive html)
        category (platt-page-category content)
        filename (str search-str ".html")
        full-filename (str dir (name category) "/" filename)
        ]
    (println full-filename)
    (spit full-filename html)
    ))

(defn force-download-platt [search-str]
  ;(let [url (str "https://www.platt.com/search.aspx?q=" search-str "&catNavPage=1&navPage=1_16_0")
  (let [url (platt-url search-str)
        html (slurp (java.net.URL. url))]
    ;(println url)
    (save-platt-file search-str html)
    html))

(defn download-platt [search-str]
  (if (not (has-platt-download? search-str))
    (force-download-platt search-str)))

(defn slow-download-platt [search-str]
  (if (not (has-platt-download? search-str))
    (do
      (force-download-platt search-str)
      (Thread/sleep 3000))))

;(has-platt-download? "045242309825")
;(.exists (io/as-file "/Users/bmd"))
;(spit "/Users/bmd/data/crawler/platt/045242309825" m)

;(def matnr045242309825 (download-platt "45242309825"))

;matnr045242309825
;(def m matnr045242309825)

;(def m (force-download-platt "781810464731"))
;(def l (force-download-platt "045242309719"))
;(platt-page-category l)
;l
;(def n (html/html-resource (java.net.URL. (platt-url "781810464731"))))
;(def l (html/html-resource (java.net.URL. (platt-url "045242309719"))))
;(def q (html/html-resource (java.net.URL. (platt-url "cutter"))))
;(save-platt-file "045242309719" (doseq l))
;(type l)
;045242309719
;(spit "/Users/bmd/data/crawler/platt/78181046473" n)
;n
;m
;(html/select (html/html-resource m) [:script])
;(html/select l [:span.ProductPriceOrderBox])
;(html/select n [:span])
;
;(-> (html/select l [:span.ProductPriceOrderBox]) first :content first)
;span.ProductPrice
; span.ProductPriceOrderBox
; span.ProductPricePerType







