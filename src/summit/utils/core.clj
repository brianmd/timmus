(println "loading summit.utils.core")

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
    [taoensso.carmine :as car :refer (wcar)]
    ;[cats.core :as m]
    ;[cats.builtin]
    ;[cats.monad.maybe :as maybe]

    [net.cgrand.enlive-html :as html]
    ))

(def datomic-url "datomic:sql://datomic?jdbc:mysql://localhost:3306/datomic?user=summit&password=qw23er")

(defn exec-sql [sql]
  (korma.core/exec-raw sql :results))

(def step-input-path (-> env :paths :local :step-input-path))
(def step-output-path (-> env :paths :local :step-output-path))


(defn default-env-setting [key]
  (let [default (-> env :defaults key)]
    (-> env key default)))
;; (default-env-setting :redis)
;; (default-env-setting :db)
;; ((default-env-setting :db) :local)


(def redis-conn {:pool {} :spec (default-env-setting :redis)})
(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

;; (wcar* (car/ping))



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

(defn save-to-x
  "may be used in a threading macro"
  [obj]
  (def x obj)
  x)

(defn logit [& args]
  (binding [*out* *err*]
    (map pprint args))
  (last args))

(defn logit-plain [& args]
  (apply println args)
  (last args))

(defn first-element-equals? [key coll]
  (and (sequential? coll) (= key (first coll))))

(defn floored [x]
  (java.lang.Math/floor (double x)))

(defn select-ranges [rows & ranges]
  (let [r (vec rows)]
    (mapcat #(subvec r (first %) (second %)) ranges)))
;; (select-ranges [0 1 2 3 4 5 6 7 8 9 10] [0 2] [4 5])

(defn convert-row-num [row-num num-rows]
  (floored (* num-rows row-num (double 0.01))))

(defn convert-range [a-range num-rows]
  [(convert-row-num (first a-range) num-rows) (convert-row-num (second a-range) num-rows)])

(defn select-percentage-ranges [num-rows rows & ranges]
  (mapcat #(apply subvec (vec rows) (convert-range % num-rows)) ranges))
;; (select-ranges 5 prods [0 20] [30 44])

(defn select-keyword [nested-arr keyword]
  (let [i (atom [])]
    (prewalk #(if (first-element-equals? keyword %) (do (swap! i conj %) %) %) nested-arr)
    @i))
;; (select-keyword [:a [:b 3 4] [:c]] :b)

(defn as-matnr [string]
  (if string
    (let [s (str "000000000000000000" string)]
      (subs s (- (count s) 18)))))
(defn as-document-num [string]
  (if string
    (let [s (str "0000000000" string)]
      (subs s (- (count s) 10)))))
;; (as-document-num "asdf")
(defn as-short-document-num [string]
  "remove leading zeros"
  (if string (str/replace string #"^0*" "")))
;(as-short-document-num (as-document-num "1"))

(defn as-integer [string]
  (read-string (as-short-document-num string)))

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
; => {:status 201, :headers {"Server" "nginx/1.4.6 (Ubuntu)", "Content-Type" "application/json; charset=utf-8", "X-Content-Type-Options" "nosniff", "X-Runtime" "0.107563", "X-Frame-Options" "SAMEORIGIN", "Connection" "close", "X-CSRF-Token" "2yucxbJnvTNt2oHJBzvXEcrKbsDkZzU7xMIpo5J4zdA=", "Location" "/", "Transfer-Encoding" "chunked", "ETag" "\"3e601d8f4e7c7ce73f0e89041750abe2\"", "Date" "Mon, 29 Feb 2016 16:18:35 GMT", "X-CSRF-Param" "authenticity_token", "X-Request-Id" "2f903ed7-8458-4945-a4f5-f774ac01abbb", "X-XSS-Protection" "1; mode=block", "Cache-Control" "max-age=0, private, must-revalidate"}, :body "{\"customers\":[{\"id\":28,\"first_name\":\"Brian\",\"last_name\":\"\",\"email\":\"brian@murphydye.com\",\"office_phone\":\"\",\"cell_phone\":\"\",\"address1\":\"\",\"address2\":\"\",\"city\":\"Seattle\",\"state\":\"WA\",\"zip\":\"\",\"sign_in_count\":151,\"active_account_number\":\"1000736\",\"active_job_account_number\":null,\"jobAccount\":null,\"links\":{\"accounts\":\"/store/accounts\"}}]}", :request-time 176, :trace-redirects ["https://www.summit.com/store/customers/sign_in.json"], :orig-content-encoding nil, :cookies {"customer_id" {:discard true, :path "/", :secure false, :value "28", :version 0}, "_blueharvest_session" {:discard true, :path "/", :secure false, :value "SlJEM0ZkOTdNSWJaWVFlQ0xmWGtTWFZVSWJtMC8vTk05aExES1gzT3FUMGgvRXBUYW5qUU5nZjBjNno4Yk00Ym8rWXhVQkpNOWpPVkkvS2dma2pUclR0eHJOUnNKeEwrTU14MS91Y2U4OW42eGlmdTJ3N0tUdmRDdVB4ODJ4aWd0cGlPaCtiaHZMejMvcGVRbEcrWVpONmNjaHFIdGlBd3JuOWM4cGpaUVF3ay9aUmdXNDZIUUQ1eElodEtLZUdueHJhM1JzVTJUZDk0Ni93ZjFwdHZSSEtOVUpPeHd3bXc1YzhzNXVJdjZ3bnJoWWpuVmRaUEtDTzVYSTdpTzNDbTJ2RlJUSkpoUmFXenBGMmI3MnRDcVd1MWlvaUJWSTJTcXhjNlQySFhyNjg9LS1CdzM3M3JFK1lEcWZlb0FEWUxpVWtRPT0%3D--76d2f3c88be29bc3d4c79b87b711dbe2b7f2d7f5", :version 0}}}


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

(def db-time-format (clj-time.format/formatter "yyyy-MM-dd HH:mm:ss"))
(defn db-timenow []
  (clj-time.format/unparse db-time-format (clj-time.core/now))
  ;; (.format (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm:ss Z") (new java.util.Date))
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






(println "done loading summit.utils.core")

