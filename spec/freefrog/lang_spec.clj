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
  (:require [clj-yaml.core :as yaml]
            [clj-time.core :as t]
            [freefrog.governance :as g]
            [freefrog.lang :as l]
            [speclj.core :refer :all])
  (:import (freefrog GovernanceParseException)))

;; Monkey patch clj-yaml to do nice encoding of dates and defrecords
(ns clj-yaml.core
  (:require [clj-time.format :as f]))

(defn encode-without-nils [data]
  (encode (into {} (remove (comp nil? second) data))))

(extend-protocol YAMLCodec
  freefrog.governance.Role
  (encode [data] (encode-without-nils data))

  freefrog.governance.Circle
  (encode [data] (encode-without-nils data))

  org.joda.time.DateTime
  (encode [data]
    (f/unparse (f/formatters :date) data)))

(ns freefrog.lang-spec)

(defn governance [name]
  (slurp (str "spec/freefrog/lang/" name "-governance.txt")))

(def sample-anchor-circle (g/create-circle "Courage Labs"))

(def expiration-date (t/date-time 2014 01 01))

(def very-governed-circle
  (-> sample-anchor-circle
      (g/add-role-to-circle "Benefit Context Link"
                            "The Organization as a provider of General Public Benefit"
                            nil
                            #{"Representing the Benefit Context within the Organization"})
      (g/add-role-to-circle "Investor Context Link"
                            "The Organization as an effective investment vehicle for its investors"
                            nil
                            #{(str "Representing the Investor Context within "
                                   "the Organization")})
      (g/add-role-to-circle "Environmental Impact Context Link"
                            "The Organization as a good steward of the Environment"
                            nil
                            #{(str "Representing the Environmental Impact "
                                   "Context within the Organization")})
      (g/update-purpose "General public benefit")
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
      (g/convert-to-circle "Products")
      (g/convert-to-circle "Partner Matters")
      (g/update-role-purpose "Partner Matters"
                             "Ensuring we have the right Partners")
      (g/add-role-domain "Partner Matters" "Addition/Removal of Partners")
      (g/add-role-accountability
        "Partner Matters"
        "Creating policies for addition and removal of partners")
      (g/elect-to-role g/facilitator-name "bill" expiration-date)
      (g/elect-to-role g/secretary-name "jill" expiration-date)
      (g/update-subcircle ["Products"] g/add-role
                          (g/map->Role {:name "Product Analyzer"}))))

(describe "Parsing governance"
  (it "should ignore comments"
    (should= () (l/parse-governance "--this is fun\n")))

  (it "should allow double quotes in comments"
    (should= () (l/parse-governance "--this is fun \"stuff\"\n"))))

(describe "Executing governance documents"
  (it "should throw nice errors for bad parsing"
    (should-throw GovernanceParseException
      "Parse error at line 1, column 32:\nconvert role \"Partner Matters\" to a circle.\n                               ^\nExpected:\n\"into a\"\n"
      (l/execute-governance "convert role \"Partner Matters\" to a circle.")))

  (it "should be able to create a new anchor circle without crosslinks"
    (should= (g/create-circle "Courage Labs")
      (l/execute-governance "CREATE ANCHOR CIRCLE \"Courage Labs\".")))

  (it "should not allow nonsense in role/circle conversions"
    ;Currently someone can say 'convert circle "blah" into a circle.' and it
    ;will convert it into a role, and vice versa, because it ignores the second
    ;part
    )

  (it "should do all the governance"
    (should= very-governed-circle
      (l/execute-directory "spec/freefrog/lang/basic")))

  (describe "governing subcircles"
    (it "won't allow multiple subcircles to be governed in one document"
      (should-throw GovernanceParseException
        #"Parse error at line 3, column 1:\ngovern circle \"Partner Matters\".*"
        (l/execute-governance
          sample-anchor-circle
          (governance "multiple-subcircle"))))
    (it "won't allow subcircle governance except as the first statement"
      (should-throw GovernanceParseException
        #"Parse error at line 2, column 1:\ngovern circle \"stuff\".*"
        (l/execute-governance
          sample-anchor-circle
          "create role \"things\".\ngovern circle \"stuff\".\n"))))

  (describe "Courage Labs Governance smoke test"
    (with-all result (l/execute-directory "spec/freefrog/lang/courage_labs"))
    (it "should be able to execute all Courage Labs Governance"
      (println (yaml/generate-string @result)))))
