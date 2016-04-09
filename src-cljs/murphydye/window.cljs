(ns murphydye.window
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]

                                        ;[timmus.sales-associate.core]
            [timmus.csr.core :refer [csr-page platt-prices]]
            [timmus.math :refer [math-page]]
                                        ;[timmus.sales-associate.core :refer [csr-page]]
            )
  (:import goog.History))

(def window-number (atom 0))

(defn new-window-url
  "map should have minimally :url"
  [m]
  (.window js/jQuery (clj->js m)))

(defn new-window [content-fn hash]
  (let [base
        {:showModal false
         :modalOpacity 0.5
         :icon "http://www.fstoke.me/favicon.ico"
         :title (str "Window #" (swap! window-number inc))
         :content "lots and lots of content"
         :footerContent "footer content"
         :width 200
         :height 160
         ;; :maxWidth 400
         ;; :maxHeight 300
         :x (+ 80 (rand-int 500))
         :y (+ 80 (rand-int 500))

         ;; :onOpen #(swap! num-opened inc)
         :onClose #(.log js/console "closed")
         }
        win (.window js/jQuery (clj->js (merge base hash)))
        win-id (.attr (.getContainer win) "id")
        $ele (.getElementById js/document win-id)
        $content (.item (.getElementsByClassName $ele "window_frame") 0)
        ;; $content (.item (.getFrame win) 0)
        ]
    (if (vector? content-fn)
      (r/render content-fn $content)
      (r/render [content-fn] $content)
      )
    win))

(defn windows-test [n]
  (when (> n 0)
    (let [win (new-window [:div "Window Test"] {:width 250})]
      (js/setTimeout
       #(.close win)
       (rand-int 2000)))
    (js/setTimeout #(windows-test (dec n)) (rand-int 1000))
    ))

(defn process-url [atom-val url url-options]
  (let [handler #(reset! atom-val %)
        error-handler #(println %)
        options (merge
                 {:headers {"Accept" "application/json"}
                  :timeout 240000    ; 2 minutes
                  :handler handler
                  :error-handler error-handler}
                 url-options)
        ]
    (println (str "processing " url))
    (GET url options)))



(defn make-window [content-fn hash]
  (new-window content-fn hash))
