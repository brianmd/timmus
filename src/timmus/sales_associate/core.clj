(ns timmus.sales_associate.core
  (:require [clojure.string :as str]
            [config.core :refer [env]]
            [clojure.java.io :refer [as-url]]
            [korma.db :refer :all]
            [korma.core :refer :all]
            [postal.core :refer [send-message]]
            ;[cemerick.url :refer (url url-encode)]
            [cheshire.core :refer :all]
            ;[clj-http.client :as client]
            ;[net.cgrand.enlive-html :as html]
            ;[coloure-tools.clojure.tools.html-utils :as hutils]
            [ring.util.codec :as c]
            [clojure.java [io :as io] [shell :as shell]]
            [clojure.data.xml :as xml]

            [timmus.utils.core :refer :all]
            ;[utils.config :as :utils]
            ;[db.core :refer :all]
            ))

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
      (replace "&quot;" "\"")))
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

;(order-query-params "sdff")
;(get-sap-order "0002991654")
;(aaa
;  (get-sap-order "0002991654")
;  )

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
  (clj-http.client/get
    (create-order-query-url order-num)
    {:basic-auth (-> env :papichulo vals)}
    ;{:basic-auth (vals (:papichulo config-env))}
    ))

;(get-sap-order "0002991654")
;(get-sap-order "0003674882")

;(def y
;  (str
;    "http://localhost:4000/bapi/show?function_name="
;    "z_o_orders_query"
;    "&args="
;    (url-encode
;      (generate-string
;        x
;        )
;      )
;    )
;  )
;y
;(slurp y)

;(clj-http.client/get "http://google.com")

;(def z
;  (clj-http.client/get y
;                       {:basic-auth (vals (:papichulo config-env))})
;  )
;z

;(html/deftemplate ugh {:parser html/xml-parser}
;                  (java.io.StringReader. (slurp "/Users/bmd/Downloads/manufacturer.xml"))
;                  [])
;ugh
;(html/deftemplate ugh {:parser html/xml-parser}
;             (java.io.StringReader. (:body z))
;             [])
;(def body (:body z))

;(defn parse [s]
;  (clojure.xml/parse
;    (java.io.ByteArrayInputStream. (.getBytes s))))
;(xml/parse-str "<top>\nBaby, I'm the top\n  <mid>\n    <bot foo=\"bar\">\n      I'm the bottom!\n    </bot>\n  </mid>\n</top>")




;(def manufacturers
;  (->
;    (slurp "/Users/bmd/Downloads/manufacturer.xml")
;    (xml/parse-str)
;    ))
;  OR
;(def manufacturers (xml/parse (java.io.FileReader. "/Users/bmd/Downloads/manufacturer.xml")))
;manufacturers






;(defn- xml-str
;  "Like clojure.core/str but escapes < > and &."
;  [x]
;  (-> x str (.replace "&" "&amp;") (.replace "<" "&lt;") (.replace ">" "&gt;")))

;(re-find #"\{(.*DETAIL.*)}" body)
;(def detail
;  (->
;    (re-find #"\{(.*DETAIL.*)}" body)
;    second
;    )
;  )
;detail
;(c/url-decode detail)
;(def a
;  (->
;    (re-find #"(\{.*DETAIL.*})" body)
;    second
;    unescape-fat-arrow-html
;    parse-string
;    )
;  )

(defn get-sap-line-items [sap-order]
  (second
    (
      (->>
        sap-order
        :body
        (re-find #"(\{.*ORDERS_DETAIL.*})")
        second
        unescape-fat-arrow-html
        parse-string
        )
      "table")))
;(aaa order-info)


;order-info
;(get-sap-line-items order-info)
;((aaa order-info) "table")
;(second (re-find #"(\{.*DETAIL.*})" (:body order-info)))
;(type order-info)
;(def line-items
;  (second (a "table"))
;  )
;z

(defn extract-sap-mail-data [items]
  (map
    (fn [item] [(item "ITEM") (item "MATERIAL") (item "MATERIAL_DESC")])
    items
    )
  )

;(def order-info (get-sap-order "0003674882"))
;(def order-info (get-sap-order "0002991654"))
;(extract-nick-data (second (order-info "table")))
;(order-info "table")
;order-info

;(def line-info
;  (map extract-nick-data line-items)
;  )


;(def body (order-info :body))
;body
;(->
;  (re-find #"(\{.*DETAIL.*})" body)
;  second
;  unescape-fat-arrow-html
;  parse-string
;  c/url-decode
;  escape-html
;  url-decode
  ;)

;(c/url-encode "\"<a>")
;(c/url-decode
;  (c/url-encode "\"<a>")
;  )
;(c/url-encode "<a>")
;
;(re-find #"\{abc}" "{abc}")

; item material material_desc



;(html/select (:body z))
;
;(as-url y)
;(slurp
;  (as-url y)
;  )

;2c ,
;22 "
;0d carriage return
;0a linefeed
;3a :


;http://localhost:4000/bapi/show?utf8=%E2%9C%93&function_name=z_o_orders_query&args=

;%7B
;%22i_order%22%3A%220002991654%22%2C

;%7B
;%22i_order%22
;  %3A
;  %220002991654%22
;%2C

;%22if_orders%22%3A%22X%22%2C%22if_details%22%3A+%22X%22%2C%0D%0A++%22if_texts%22%3A%22X%22%2C%0D%0A++%22if_addresses%22%3A+%22X%22%0D%0A%7D&returns=&options=&commit=Run

;http://localhost:4000/bapi/show?utf8=%E2%9C%93&function_name=z_o_orders_query&args=
;%7B%0D%0A%22i_order%22%3A%220002991654%22%2C%0D%0A%22if_orders%22%3A%22X%22%2C%0D%0A%22if_details%22%3A%22X%22%2C%0D%0A%22if_texts%22%3A%22X%22%2C%0D%0A%22if_addresses%22%3A%22X%22%0D%0A%7D&returns=&options=&commit=Run


;http://localhost:4000/bapi/show?utf8=%E2%9C%93&function_name=z_o_orders_query&args=%7B%22i_order%22%3A%220002991654%22%2C%22if_orders%22%3A%22X%22%2C%22if_details%22%3A+%22X%22%2C%0D%0A++%22if_texts%22%3A%22X%22%2C%0D%0A++%22if_addresses%22%3A+%22X%22%0D%0A%7D&returns=&options=&commit=Run
;http://localhost:4000/bapi/show?utf8=%E2%9C%93&function_name=z_o_orders_query&args=%7B%22i_order%22%20%220002991654%22%2C%20%22if_orders%22%20%22X%22%2C%20%22if_details%22%20%22X%22%2C%20%22if_texts%22%20%22X%22%2C%20%22if_addresses%22%20%22X%22%7D&returns=&options=&commit=Run

;http://localhost:4000/bapi/show?utf8=%E2%9C%93&function_name=z_o_orders_query&args=

;%7B%0D%0A++

;%22i_order%22%3A+
;%220002991654%22%2C

;%0D%0A++%22if_orders%22%3A+%22X%22
;%2C%0D%0A++%22if_details%22%3A+%22X%22%2C%0D%0A++%22if_texts%22%3A+%22X%22%2C%0D%0A++%22if_addresses%22%3A+%22X%22%0D%0A%7D&returns=&options=&commit=Run
;(url-encode
;(generate-string
;  x
;  )
;)
;(slurp
;  (url-encode
    ;(as-url y)
    ;)
  ;)
;
;(str
;  "http://localhost:4000/bapi/show?function_name="
;  "z_o_orders_query"
;  "&args="
;  (url-encode
;    x
;    )
;  )
;
;(url-encode
;  (order-query "2991654")
;  )
;(order-query "2991654")
;(generate-string
;  {
;   "i_order" "0002991654",
;"if_orders" "X",
;"if_details" "X",
;"if_texts" "X",
;"if_addresses" "X"
;}
;  )
;(def order (str ))
;(slurp (as-url order))
;(select customer (with cart) (limit 1))


; 3*4 + 7
;(+ 7 (* 3 4))
;
;(slurp
;  (as-url
;    (url-encode
;      "http://localhost:4000/bapi/show?utf8=✓&function_name=z_o_orders_query&args={\"i_order\":\"0002991654\",\n++\"if_orders\":+\"X\",\n++\"if_details\":+\"X\",\n++\"if_texts\":+\"X\",\n++\"if_addresses\":+\"X\"\n}&returns=&options=&commit=Run"
;      )
;    )
;  )

;line-info
;(first line-info)
;(select product (where {:matnr (second (first line-info))}) (with external-file))
;(select product (where {:matnr (second (first line-info))}) (with external-file (where {:type "Attachment"})))



(defn merge-product-info [line-item]
  (let [product
        (select product (where {:matnr (second line-item)}) (with external-file (where {:type "Attachment"})))
        ]
    (conj line-item (first product))
    ))
;(map merge-product-info x)

;line-info
;(merge-product-info (first line-info))
;(def line-product-info (map merge-product-info line-info))

;(select product (where {:matnr (second (second line-info))}) (with external-file (where {:type "Attachment"})))

;(defn get-db-data [item]
;  (first
;    (select product (where {:matnr (second item)}) (with external-file (where {:type "Attachment"})))
;    )
;  )

;(get-db-data (first line-info))

(defn get-url [line-product]
  (let [files (:external_files (nth line-product 3))]
    (if files
      (map :url files))
    ))

;(get-url (second line-product-info))
;(second line-product-info)
;(first line-product-info)
;(map get-url line-product-info)

(defn get-urls [data]
  (set (flatten (map get-url data)))
  )

;(get-urls line-product-info)

(defn copy-as-stream [uri file]
  (with-open [in (io/input-stream uri)
              out (io/output-stream file)]
    (io/copy in out)))

(defn download-file-to [dirname url]
  (let [filename (last (str/split url #"/"))
        full-filename (str dirname "/" filename)]
    (copy-as-stream url full-filename)
    full-filename))





;(.mkdir (clojure.java.io/file "/Users/bmd/Downloads/test123"))
;clojure.java.io/create-directory
;(defmacro with-new-directory [dirname]
;  )




;(download-file (first (get-urls product-data)))

;(pmap download-file (get-urls line-product-info))

;(def l (first line-product-info))
;l

(defn extract-mail-line-item [item]
  [(first item) (nth item 2) (get-url item) (:id (nth item 3))])

(defn create-pdf-link [url]
  (let [filename (last (str/split url #"/"))]
    (str "<a href=\"" url "\">" filename "</a>")))
;(create-pdf-link "http://sss.com/pdf.pdf")

(defn create-mail-table-row [mail-line-item]
  (str "<tr><td>" (first mail-line-item)
       "</td><td><a href=\"https://www.summit.com/store/products/" (nth mail-line-item 3) "\">"
          (second mail-line-item)
       "</a></td><td>" (str/join "<br/>" (map create-pdf-link (nth mail-line-item 2))) "</td></tr>"))
;(create-mail-table-row (extract-mail-line-item l))

(defn create-mail-table [mail-line-items]
  (let [header "<table border=\"1\">\n<tr><th>Line #</th><th>Item</th><th>Pdf Name</th></tr>\n"
        footer "\n</table>"]
    (str
      header
      (str/join "\n" (map create-mail-table-row mail-line-items))
      footer)))

;(create-mail-table (map extract-mail-line-item (take 9 line-product-info)))

;(logit 3)
;(extract-mail-line-item l)
;(map extract-mail-line-item (take 2 line-product-info))

;(last
;  (str/split "a/bc/dd.pdf" #"/")
;  )
;(->>
;  product-data
;  (map get-url)
;  flatten
;  )
;
;(def product-data
;  (map get-db-data line-info)
;  )
;
;(defn get-urls [items]
;  (map))
;
;
;line-info
;product-data

(defn make-pdf-attachment [dirname filename]
  {:type :attachment
   :content-type "application/pdf"
   :content (str dirname "/" (last (str/split filename #"/")))})

;(map make-pdf-attachment (get-urls product-data))



;(defn flatten-urls [line-item-rows]
;  )

(defn send-spec-email [email order-num]
  (let [order-num (as-document-num order-num)
        dir-name (str "/Users/bmd/Downloads/" order-num)
        line-item-rows (->>
                         (get-sap-order order-num)
                         get-sap-line-items
                         extract-sap-mail-data
                         (map merge-product-info)
                         (map extract-mail-line-item)
                         )
        urls (flatten (map #(nth % 2) line-item-rows))
        ]
    (.mkdir (clojure.java.io/file dir-name))
    (doall (pmap (partial download-file-to dir-name) urls)
           ;(create-email email order-num line-item-rows)
           )
    (send-message
      (:bmd-mailer (:mail config-env))

      {:from "jarred.killgore@summit.com"
       :to email
       :subject (str "Order #" order-num)
       :body (into
               [{:type "text/html"
                 :content (create-mail-table line-item-rows)
                 ;:content (create-mail-table (map extract-mail-line-item line-item-rows))
                 }]
               (map (partial make-pdf-attachment dir-name) urls)
               )})
    (println "need to delete files and directory !!!")
    )
  )



;(send-spec-email "jarred.killgore@summit.com" "0002991654")
;(send-spec-email "jarred.killgore@summit.com" "0003674882")
;(send-spec-email "nick.robertson@summit.com" "0002991654")

  ;order-info
;(def order-num "0002991654")
;(let [dir-name (str "/Users/bmd/Downloads/" order-num)
;      line-item-rows (->>
;                       (get-sap-order order-num)
;                       get-sap-line-items
;                       extract-sap-mail-data
;                       (map merge-product-info)
;                       (map extract-mail-line-item)
;                       )
;      ]
;  (.mkdir (clojure.java.io/file dirname))
;  (doall (pmap (partial download-file-to dirname) line-item-rows)
;         (create-email email order-num line-item-rows)
;         )
;  (println "need to delete files and directory !!!")
;  )



