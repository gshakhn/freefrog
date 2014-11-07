;
; Copyright © 2014 Courage Labs
;
; This file is part of Freefrog.
;
; Harthe is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; Harthe is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

;;; # Anchor Circle Manipulation Spec #
;;; Defines how the Anchor Circle can be updated, and helps us differentiate
;;; this special circle from what a "regular circle" can do.
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