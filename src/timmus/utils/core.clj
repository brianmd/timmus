(ns timmus.utils.core
  )

(defn logit [& args]
  (apply println args)
  (last args))

