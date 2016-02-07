(ns timmus.macros)

(defmacro sleep [ms & body]
  `(js/setTimeout (fn [] ~@body) ~ms))
