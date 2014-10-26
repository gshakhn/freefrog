(ns freefrog.governance-spec
  (:require [speclj.core :refer :all]
            [freefrog.governance :as g]))

(describe "Anchor Circle"
  (it "can create an anchor circle with a lead link"
    (should= {:name      "Courage Labs"
              :lead-link {:name  "Stephen Starkey"
                          :email "stephen@couragelabs.com"}}
             (g/anchor-circle "Courage Labs" "Stephen Starkey"
                              "stephen@couragelabs.com"))

    (should= {:name "Fear Labs" :lead-link {:name "Bill O'Reilly"
                                            :email "billoreilly@foxnews.com"}}
             (g/anchor-circle "Fear Labs" "Bill O'Reilly"
                              "billoreilly@foxnews.com")))

  (it "can create an anchor circle without a lead link"
    (should= {:name "Courage Labs"} (g/anchor-circle "Courage Labs")))

  (it "throws errors when things aren't filled in correctly"
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
                  (g/anchor-circle "Not Enough Information" "Joe Schmoe" ""))

    (should-throw IllegalArgumentException "Name may not be empty"
                  (g/anchor-circle nil))
    (should-throw IllegalArgumentException "Name may not be empty"
                  (g/anchor-circle ""))))

(run-specs)
