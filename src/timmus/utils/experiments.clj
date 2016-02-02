(ns timmus.utils.experiments
  (:require
    ;[clj-http.client :as client]
    ;[cheshire.core :refer :all]
    [clojure.pprint :refer [pprint]]
    ;[config.core :refer [env]]

    [cats.core :as m]
    [cats.builtin]
    [cats.monad.maybe :as maybe]
    [cats.context :as ctx]
    [cats.applicative.validation :as v]
    )
  )



; semigroup
(m/mappend (maybe/just [1 2 3])
           (maybe/just [4 5 6]))


; monad
(m/mappend (maybe/just [1 2 3])
           (maybe/nothing)
           (maybe/just [4 5 6])
           (maybe/nothing))

; functor
(maybe/just 2)
(m/fmap inc (maybe/just 1))
(m/fmap inc (maybe/nothing))

(m/fmap inc [1 2 3])

; this doesn't work
(m/fmap inc [(maybe/just 1) (maybe/just 2) (maybe/just 3)])
; but this does
(map (m/fmap inc) [(maybe/just 1) (maybe/just 2) (maybe/just 3)])

; applicative
(defn make-greeter
  [^String lang]
  (condp = lang
    "es" (fn [name] (str "Hola " name))
    "en" (fn [name] (str "Hello " name))
    nil))

(defn make-greeter
  [^String lang]
  (condp = lang
    "es" (maybe/just (fn [name] (str "Hola " name)))
    "en" (maybe/just (fn [name] (str "Hello " name)))
    (maybe/nothing)))

(m/fapply (make-greeter "es") (maybe/just "Alex"))
;; => #<Just "Hola Alex">

(m/fapply (make-greeter "en") (maybe/just "Alex"))
;; => #<Just "Hello Alex">

(m/fapply (make-greeter "it") (maybe/just "Alex"))
;; => #<Nothing>


; pure
(m/pure maybe/maybe-monad 5)

; foldable
(m/foldl (fn [acc v] (+ acc v)) 0 [1 2 3 4 5])

; r(ight) can cause stack overflow
(m/foldr #(cons (inc %1) %2) '() (range 100000))

(m/foldl #(m/return (+ %1 %2)) 1 (maybe/just 1))
(m/foldl #(m/return (+ %1 %2)) 1 (maybe/nothing))

(defn m-div
  [x y]
  (if (zero? y)
    (maybe/nothing)
    (maybe/just (/ x y))))
(m/foldm m-div 1 [1 2 3])
;; => #<Just 1/6>
(m/foldm m-div 1 [1 0 3])
;; => #<Nothing>

; traversable
(defn just-if-even
  [n]
  (if (even? n)
    (maybe/just n)
    (maybe/nothing)))
(ctx/with-context maybe/context
  (m/traverse just-if-even []))
;; => #<Just []>
(ctx/with-context maybe/context
  (m/traverse just-if-even [2 4]))
;; => #<Just [2 4]>
(ctx/with-context maybe/context
  (m/traverse just-if-even [1 2]))
;; => #<Nothing>
(ctx/with-context maybe/context
  (m/traverse just-if-even [2 3]))
;; => #<Nothing>

(defn valid-if-even
  [n]
  (if (even? n)
    (v/ok n)
    (v/fail {n :not-even})))
(ctx/with-context v/context
  (m/traverse valid-if-even []))
;; => #<Ok []>
(ctx/with-context v/context
  (m/traverse valid-if-even [2 4]))
;; => #<Ok [2 4]>
(ctx/with-context v/context
  (m/traverse valid-if-even [1 2]))
;; => #<Fail {1 :not-even}>
(ctx/with-context v/context
  (m/traverse valid-if-even [2 3 4 5]))
;; => #<Fail {3 :not-even, 5 :not-even}>

; monad
(m/bind (maybe/just 1)
        (fn [v] (maybe/just (inc v))))
;; => #<Just 2>
; can use m/return instead of maybe/just
(m/bind (maybe/just 1)
        (fn [v] (m/return (inc v))))
;; => #<Just 2>

; compose monads
(m/bind (maybe/just 1)
        (fn [a]
          (m/bind (maybe/just (inc a))
                  (fn [b]
                    (m/return (* b 2))))))
;; => #<Just 4>
; same but using m/mlet to compose
(m/mlet [a (maybe/just 1)
         b (maybe/just (inc a))]
        (m/return (* b 2)))



