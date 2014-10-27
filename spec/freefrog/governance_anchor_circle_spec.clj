(ns freefrog.governance-anchor-circle-spec
  (:require [freefrog.governance :as g]
            [speclj.core :refer :all]))

;; Section 5.2
(describe "Anchor Circle"
  ;; Section 5.2.2.a
  (it "can create an anchor circle with a lead link"
    (should= {:name      "Courage Labs"
              :lead-link {:name  "Stephen Starkey"
                          :email "stephen@couragelabs.com"}}
      (g/anchor-circle "Courage Labs" "Stephen Starkey"
                       "stephen@couragelabs.com"))

    (should= {:name "Fear Labs" :lead-link {:name  "Bill O'Reilly"
                                            :email "billoreilly@foxnews.com"}}
      (g/anchor-circle "Fear Labs" "Bill O'Reilly"
                       "billoreilly@foxnews.com")))

  ;; Section 5.2.2.b
  (it "can create an anchor circle without a lead link")

  (it "doesn't let you leave any parameters empty"
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/anchor-circle nil))
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/anchor-circle ""))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle nil nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "" nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "" "Joe" "joescmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" nil
                       "joeschmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" ""
                       "joeschmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" "Joe Schmoe" nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" "Joe Schmoe" ""))))

(run-specs)