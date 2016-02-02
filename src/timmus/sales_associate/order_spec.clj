(ns timmus.sales-associate.order-spec
  (:require [clojure.string :as str]
            [clojure.java.io :as io :refer [as-url]]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [postal.core :refer [send-message]]
            [cemerick.url :refer [url-encode url]]
            [cheshire.core :refer :all]
            [clj-http.client :as client]
            ;[net.cgrand.enlive-html :as html]
            ;[coloure-tools.clojure.tools.html-utils :as hutils]
            ;[ring.util.codec :as c]
            ;[clojure.java [io :as io] [shell :as shell]]
            ;[clojure.data.xml :as xml]
            [config.core :refer [env]]

            [timmus.utils.core :refer :all]
            ;[utils.config :as :utils]
            ;[db.core :refer :all]
            [timmus.db.relationships :refer :all]
            ))

;(str "http://stackoverflow.com/search?" (client/generate-query-string {"q=7" "clojure url"}))

;(defn escape-html
;  "Change special characters into HTML character entities."
;  [text]
;  (.. #^String (str text)
;      (replace "&" "&amp;")
;      (replace "<" "&lt;")
;      (replace ">" "&gt;")
;      (replace "\"" "&quot;")))
;(defn unescape-html
;  "Change special characters into HTML character entities."
;  [text]
;  (.. #^String (str text)
;      (replace "&amp;" "&")
;      (replace "&lt;" "<")
;      (replace "&gt;" ">")
;      (replace "&quot;" "\"")))
(defn unescape-fat-arrow-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String (str text)
      (replace "&amp;" "&")
      (replace "&lt;" "<")
      (replace "&gt;" ">")
      (replace "&quot;" "\"")
      (replace "=>" ":")
      ))

(defn order-query-params [order-num]
  {
   "i_order" order-num,
   "if_orders" "X",
   "if_details" "X",
   "if_texts" "X",
   "if_addresses" "X"
   })

(defn create-order-query-url [order-num]
  (str
    "http://localhost:4000/bapi/show?function_name="
    "z_o_orders_query"
    "&args="
    (url-encode
      (generate-string
        (order-query-params order-num)))))
;(create-order-query-url "3333")

(defn get-sap-order [order-num]
  (let [creds (-> env :papichulo vals)]
    ;(println "papichulo credentials" creds)
    (client/get
      (create-order-query-url (as-document-num order-num))
      {:basic-auth creds}
      ))
    )
;(get-sap-order "2991654")

(defn get-sap-line-items [sap-order]
  (->>
    sap-order
    :body
    (re-find #"(\{.*ORDERS_DETAIL.*})")
    second
    unescape-fat-arrow-html
    parse-string
    (#(% "table"))
    second
    ))

(defn extract-sap-mail-data [items]
  (map
    (fn [item] [(item "ITEM") (item "MATERIAL") (item "MATERIAL_DESC")])
    items
    )
  )

(defn merge-product-info [line-item]
  (let [product
        (select product (where {:matnr (second line-item)}) (with external-file (where {:type "Attachment"})))
        ]
    (conj line-item (first product))
    ))

(defn get-url [line-product]
  (let [files (:external_files (nth line-product 3))]
    (if files
      (map :url files))
    ))

;(defn get-urls [data]
;  (set (flatten (map get-url data)))
;  )

(defn copy-as-stream [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn download-file-to [dirname url]
  (let [filename (last (str/split url #"/"))
        full-filename (str dirname "/" filename)]
    (println full-filename)
    (copy-as-stream url full-filename)
    full-filename))

(defn extract-mail-line-item [item]
  [(first item) (nth item 2) (get-url item) (:id (nth item 3))])

(defn create-pdf-link [url]
  (let [filename (last (str/split url #"/"))]
    (str "<a href=\"" url "\">" filename "</a>")))

(defn create-mail-table-row [mail-line-item]
  (str "<tr><td>" (first mail-line-item)
       "</td><td><a href=\"https://www.summit.com/store/products/" (nth mail-line-item 3) "\">"
          (second mail-line-item)
       "</a></td><td>" (str/join "<br/>" (map create-pdf-link (nth mail-line-item 2))) "</td></tr>"))

(defn create-mail-table [mail-line-items]
  (let [header "<table border=\"1\">\n<tr><th>Line #</th><th>Item</th><th>Pdf Name</th></tr>\n"
        footer "\n</table>"]
    (str
      header
      (str/join "\n" (map create-mail-table-row mail-line-items))
      footer)))

(defn make-pdf-attachment [dirname filename]
  {:type :attachment
   :content-type "application/pdf"
   :content (str dirname "/" (last (str/split filename #"/")))})

(defn last-not-nil? [x] (last x))
;(->>
;  '([10 " TYPE B" nil nil] [20 " TYPE N" nil nil])
;  (filter last-not-nil?)
  ;(filter #(last %))
  ;)

(defn send-spec-email [email order-num]
  (println)
  (println (:papichulo env))
  (println (-> env :db :blue-harvest :local))
  (println (-> env :paths :local))
  (println (-> env :paths :local :download-path))
  (println)
  (let [dir-name (str (-> env :paths :local :download-path) order-num)
        line-item-rows (->>
                         (get-sap-order order-num)
                         get-sap-line-items
                         extract-sap-mail-data
                         (map merge-product-info)
                         logit
                         (filter last-not-nil?)
                         logit
                         (map extract-mail-line-item)
                         )
        xyz (println "line-item-rows" line-item-rows)
        urls (flatten (map #(nth % 2) line-item-rows))
        ]
    (if (= 0 (count line-item-rows))
      (throw (Exception. (str "No line items for order #" order-num))))
    (.mkdir (clojure.java.io/file dir-name))
    (println "downloading" urls)
    (doall (pmap (partial download-file-to dir-name) urls)
           ;(create-email email order-num line-item-rows)
           )
    (println "sending email to" email "for order #" order-num)
    (send-message
      (-> env :smtp :bmd-mailer)
      ;(:bmd-mailer (:mail config-env))

      {:from "marketingtech@summit.com"
       :reply-to "marketingtech@summit.com"
       :to email
       :subject (str "Order #" order-num)
       :body (into
               [{:type "text/html"
                 :content (create-mail-table line-item-rows)
                 ;:content (create-mail-table (map extract-mail-line-item line-item-rows))
                 }]
               (map (partial make-pdf-attachment dir-name) urls)
               )})
    (println "finished sending email to" email "for order #" order-num)
    (println "need to delete files and directory !!!")
    )
  )


;send-spec-email
;(send-spec-email "jarred.killgore@summit.com" "0002991654")
;(send-spec-email "jarred.killgore@summit.com" "0003674882")
;(send-spec-email "nick.robertson@summit.com" "0002991654")
;(send-spec-email "bmurphydye@summit.com" "2991654")

