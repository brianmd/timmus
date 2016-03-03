;; this file is experimenting with test.check
;;
;; see https://github.com/clojure/test.check/blob/master/doc/intro.md
;;     note: has a page two at https://github.com/clojure/test.check/blob/master/doc/generator-examples.md

(ns test-check
  (:require  [clojure.test :as t]
             [clojure.test.check :as tc]
             [clojure.test.check.generators :as gen]
             [clojure.test.check.properties :as prop]
             [clojure.test.check.clojure-test :refer [defspec]]
             ))

;; this passes
(def sort-idempotent-prop
  (prop/for-all [v (gen/vector gen/int)]
    (= (sort v) (sort (sort v)))))

(tc/quick-check 100 sort-idempotent-prop)



;; this fails. first entry is not less than the last on a one element list
(def prop-sorted-first-less-than-last
  (prop/for-all [v (gen/not-empty (gen/vector gen/int))]
    (let [s (sort v)]
      (< (first s) (last s)))))

(tc/quick-check 100 prop-sorted-first-less-than-last)



;; fails when 42 is in the vector
(def prop-no-42
  (prop/for-all [v (gen/vector gen/int)]
    (not (some #{42} v))))

(tc/quick-check 100 prop-no-42)


(gen/sample gen/int)
(gen/sample gen/nat) ; natural numbers
(gen/sample gen/string)
(take 5 (gen/sample-seq gen/int))

;; this macro runs under the test runner.
(defspec first-element-is-min-after-sorting ;; the name of the test
  100 ;; the number of iterations for test.check to test
  (prop/for-all [v (gen/not-empty (gen/vector gen/int))]
    (= (apply min v)
       (first (sort v)))))



;; create email address.
(defrecord User [user-name user-id email active?])
(def domain (gen/elements ["gmail.com" "hotmail.com" "computer.org"]))
(def email-gen
  (gen/fmap (fn [[name domain-name]]
              (str name "@" domain-name))
            (gen/tuple (gen/not-empty gen/string-alphanumeric) domain)))
(last (gen/sample email-gen))

;; create record
(def user-gen
  (gen/fmap (partial apply ->User)
            (gen/tuple (gen/not-empty gen/string-alphanumeric)
                       gen/nat
                       email-gen
                       gen/boolean)))
(last (gen/sample user-gen))


;; create nested critters
(def compound (fn [inner-gen]
                (gen/one-of [(gen/list inner-gen)
                             (gen/map inner-gen inner-gen)])))
(def scalars (gen/one-of [gen/int gen/boolean]))
(def my-json-like-thing (gen/recursive-gen compound scalars))
(last (gen/sample my-json-like-thing 20))


