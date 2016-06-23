(ns summit.step.jarred
  (:require
   [summit.utils.core :refer :all]
   [summit.step.restapi :as restapi]
   [summit.essence :refer :all]
   ))


(examples

 (new-unspsc {:step-id "IDW_UNSPSC_11101500"})
 (step-attrs (new-unspsc {:step-id "IDW_UNSPSC_11101500"}))
 (step-attrs (new-unspsc {:step-id (:parent-id (step-attrs (new-unspsc {:step-id "IDW_UNSPSC_11101500"})))}))
 (ppn (sort (step-attrs (new-unspsc {:step-id (:parent-id (step-attrs (new-unspsc {:step-id "IDW_UNSPSC_11101500"})))}))))
 (step-attrs! (new-unspsc {:step-id "IDW_UNSPSC_11101500"}))

 )
