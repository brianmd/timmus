(ns summit.step.core
  (:require [summit.utils.core :as utils]))

(defn unspsc-parent [x]
  (let [pairs  (map #(apply str %) (partition 2 (str x) ))
        paired (->>
                pairs
                (remove #(= "00" %))
                )
        parent (->>
                (take (dec (count paired)) paired)
                (apply str))
        ]
    (utils/->int (subs (str parent "00000000") 0 8))
    ))

(defn unspsc-parents
  ([x] (unspsc-parents [] x))
  ([parents x]
   (let [parent (unspsc-parent x)]
     (if (= 0 parent)
       parents
       (unspsc-parents (conj parents parent) parent))))
  )

(utils/examples
 (unspsc-parent "39112233")
 (unspsc-parent "39112200")
 (unspsc-parent "39110000")
 (unspsc-parent "39000000")

 (unspsc-parents "39112233")
 )

