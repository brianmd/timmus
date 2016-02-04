(ns timmus.utils.experiments
  (:require
    [clj-http.client :as client]
    [cheshire.core :refer :all]
    [clojure.pprint :refer [pprint]]
    [config.core :refer [env]]

    ;[cats.core :as m]
    ;[cats.builtin]
    ;[cats.monad.maybe :as maybe]
    )
  )

(when false

  (def zzz 3)


(def ^:dynamic *row-num* 0)
*row-num*
;(sequence-counter *test*)

(defn sequence-counter [dynamic-name]
  (let [fn (sequence-counter dynamic-name)]
    ()))
(defmacro sequence-counter-aux [dynamic-name]
  `(defn abc-aux [sequenc rownum]
     (println '~dynamic-name ~dynamic-name)
     (lazy-seq (cons (first sequenc) (abc-aux (rest sequenc) (+ rownum 1))))
     ))


;(defn remember-row-num-aux [rownum sequenc]
;  (binding [*row-num* rownum]
;    (println "*row-num*" rownum)
;    (lazy-seq (cons (first sequenc) (remember-row-num-aux (+ rownum 1) (rest sequenc))))
;    ))
(defn remember-row-num-aux [rownum sequenc]
  (def ^:dynamic *row-num* rownum)
  (println "*row-num*" rownum)
  (lazy-seq (cons (first sequenc) (remember-row-num-aux (+ rownum 1) (rest sequenc)))))
(defn remember-row-num [sequenc]
  (binding [*row-num* 100]
    (remember-row-num-aux 100 sequenc)))


(def def-it (macro->fn def))
(defn remember-var-aux [varname rownum sequenc]
  (println "varname" varname)
  (println "sequence" sequenc)
  (apply def-it [varname rownum])
  (println "row-num" rownum)
  (Thread/sleep 500)
  (lazy-seq (cons (first sequenc) (remember-var-aux varname (+ rownum 1) (rest sequenc)))))
(defmacro remember-var [varname sequenc]
  `(remember-var-aux '~varname 100 ~sequenc))

(->>
  (remember-var zzz [1 2 3])
  (take 7)
  )
(remember-var zzz [1 2 3])

;(->>
;  (range 50)
;  (remember-var zzz)
;  (take 10)
;  (map #(vector % row-num))
  ;(map #(vector % zzz))
  ;logit
  ;)

;(filter #(< (second %) 1))
;(map #(%) [1 2 3])
;(map #(vector %) [1 2 3])
;(map #(+ 1 %) [1 2 3])
;
;(vector 3)
;[]
;(sap-material [192 2])
;(sap-material (range 17))
;(process-sap-file-with "STEP_MATERIAL.txt" process-sap-material)
;(def x (process-sap-file-with "STEP_MATERIAL.txt" process-sap-material))
;(map :matnr x)
;
;
; remember-item-num
;
;(def fib-seq
;  ((fn rfib [a b]
;     (lazy-seq (cons a (rfib b (+ a b)))))
;    (bigint 0) (bigint 1)))
;(integer 3)
;(take 20000 fib-seq)


;(defn add-one [se]
;  (lazy-seq (cons (inc (first se)) (add-one (rest se)))))
;(take 500000 (add-one (range)))

;
;(map (fn [x] [x (env x)])
;     (sort (keys env))
;     )

)



