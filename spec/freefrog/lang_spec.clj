;
; Copyright © 2015 Courage Labs
;
; This file is part of Freefrog.
;
; Freefrog is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; Freefrog is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

(ns freefrog.lang-spec
  (:require [freefrog.governance :as g]
            [freefrog.lang :as l]
            [speclj.core :refer :all]))

(def bad-governance (slurp "spec/bad-governance.txt"))

(def new-circle-governance
  (slurp "spec/new-circle-governance.txt"))

(def new-circle-without-crosslinks
  (slurp "spec/new-circle-no-crosslinks-governance.txt"))

(def post-anchor-circle-governance
  (slurp "spec/post-anchor-circle-governance.txt"))

(def circle-updates-governance
  (slurp "spec/circle-updates-governance.txt"))

(def sample-anchor-circle
  (-> (g/create-circle "Courage Labs")
      (g/add-role-to-circle "Environmental Impact")
      (g/add-role-to-circle "Benefit")
      (g/add-role-to-circle "Investor")
      (g/update-purpose "General public benefit")))

(def circle-with-created-structure
  (-> sample-anchor-circle
      (g/add-role-to-circle "Partner Matters"
                            "Bringing in and making Partners happy")
      (g/add-role-to-circle "Accounting"
                            "Spending money responsibly"
                            ["Checkbook", "Credit Cards"]
                            ["Telling people how much money there is to spend"
                             "Paying people"
                             "Reimbursing for expenses"
                             "Depositing income"])
      (g/add-role-to-circle "Products"
                            "Building cool products to sell"
                            ["Products"]
                            ["Communicating product direction"
                             "Gathering customer needs"])
      (g/convert-to-circle "Products")))

(def circle-with-updates
  (-> circle-with-created-structure
      (g/convert-to-circle "Partner Matters")
      (g/update-role-purpose "Partner Matters"
                             "Ensuring we have the right Partners")
      (g/add-role-domain "Partner Matters" "Addition/Removal of Partners")
      (g/add-role-accountability
        "Partner Matters"
        "Creating policies for addition and removal of partners")))

(defn assert-governance [msg circle expected governance-document]
  (it msg
    (should= expected
      (l/execute-governance circle governance-document))))

(describe "Parsing governance"
  (it "should ignore comments"
    (should= () (l/parse-governance "--this is fun\n")))

  (it "should allow double quotes in comments"
    #_(should= () (l/parse-governance "--this is fun \"stuff\"\n"))))

(describe "Executing governance documents"
  (it "should throw nice errors for bad parsing"
    (should-throw RuntimeException
      "Parse error at line 1, column 32:\nconvert role \"Partner Matters\" to a circle.\n                               ^\nExpected:\ninto a\n\n"
      (l/execute-governance bad-governance)))

  (it "should be able to create a new anchor circle"
    (should= sample-anchor-circle
      (l/execute-governance new-circle-governance)))

  (it "should be able to create a new anchor circle without crosslinks"
    (should= (g/create-circle "Courage Labs")
      (l/execute-governance new-circle-without-crosslinks)))

  (it "should not allow nonsense in role/circle conversions"
    ;Currently someone can say 'convert circle "blah" into a circle.' and it
    ;will convert it into a role, and vice versa, because it ignores the second
    ;part
    )

  (assert-governance
    "should be able to add roles and circles"
    sample-anchor-circle
    circle-with-created-structure
    post-anchor-circle-governance)

  (assert-governance
    "should be able to update various existing values"
    circle-with-created-structure
    circle-with-updates
    circle-updates-governance))