(ns freefrog.governance-spec
  (:require [clojure.core.typed :as t]
            [speclj.core :refer :all]))

(describe "governance namespace"
  (it "should be fully type-safe"
    (should (t/check-ns 'freefrog.governance))))