(ns timmus.utils.core
  )

(defn logit [& args]
  (apply println args)
  (last args))

(defn as-document-num [string]
  (let [s (str "0000000000" string)]
    (subs s (- (count s) 10))))

