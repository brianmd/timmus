(ns murphydye.websockets
  (:require [cognitect.transit :as t]
            [murphydye.window :as win]
            )
  )

(defonce ws-chan (atom nil))
(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn receive-transit-msg!
  [update-fn]
  (fn [msg]
    (let [data (->> msg .-data (t/read json-reader))]
      (win/qgrowl (str "received websocket:" data))
      (.log js/console (str "received websocket-----------" data))
      (update-fn
       (->> msg .-data (t/read json-reader))))))

(defn send-transit-msg!
  [msg]
  (if @ws-chan
    (.send @ws-chan (t/write json-writer msg))
    (throw (js/Error. "Websocket is not available!"))))

(defn make-websocket! [url receive-handler]
  (println "attempting to connect websocket")
  (if-let [chan (js/WebSocket. url)]
    (do
      (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
      (reset! ws-chan chan)
      (win/qgrowl "websocket connected")
      (println "Websocket connection established with: " url))
    (let [msg "Websocket connection failed"]
      (win/qgrowl msg)
      (throw (js/Error. msg))
      )))

