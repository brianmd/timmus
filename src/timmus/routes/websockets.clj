(ns timmus.routes.websockets
  (:require [compojure.core :refer [GET defroutes wrap-routes]]
            [clojure.tools.logging :as log]
            [immutant.web.async :as async]
            [cognitect.transit :as transit]
            )
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           [java.nio.charset StandardCharsets]
           )
  )

(log/info "in websockets.cljs")

;; (.getBytes "abc" "UTF_8")
;; (bytes "abc")
;; (byte-array (map byte "ascii"))


(defrecord person [id name])
(defrecord room [id name people owner])

(defonce channels (atom #{}))
(defonce rooms (atom #{}))
(defonce people (atom #{}))

(defonce channel-seq-num (atom 0))
(defonce rooms-seq-num (atom 0))
(defonce person-seq-num (atom 0))

;; make another namespace for rooms/persons
;; selectors: contact-outbound, speak (or send-message/submit-message)

(defn msg->transit [m]
  (let [out (ByteArrayOutputStream. 4096)
        writer (transit/writer out :json)]
    (transit/write writer m)
    (.toString out)))

(defn transit->msg [t]
  (let [in (ByteArrayInputStream. (byte-array (map byte t)))
        reader (transit/reader in :json)]
    (transit/read reader)))

(defn connect! [channel]
  (log/info "channel open")
  (swap! channels conj channel))

(defn disconnect! [channel {:keys [code reason]}]
  (log/info "close code:" code "reason:" reason)
  (swap! channels #(remove #{channel} %)))

(defn notify-clients! [channel msg]
  (try
    (do
      (log/info "notifing clients ..")
      (log/info (.getClass msg))
      (log/info (transit->msg msg))
      (log/info channel)
      (log/info msg)
      (log/info "adding a message")
      ;; (async/send! channel (msg->transit {:message "got it"}))
      (doseq [channel @channels]
        (async/send! channel msg))
      )
    (catch Exception e (log/error (str "caught exception: " (.getMessage e)))))
    )

(def websocket-callbacks
  "WebSocket callback functions"
  {:on-open connect!
   :on-close disconnect!
   :on-message notify-clients!})

(defn ws-handler [request]
  (log/info "ws-handler")
  (async/as-channel request websocket-callbacks))

(defroutes websocket-routes
  (GET "/ws" [] ws-handler))

