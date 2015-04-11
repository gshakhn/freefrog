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

(def new-circle-governance (slurp "spec/new-circle-governance.txt"))
(describe "Using the governance DSL"
  (it "should be able to create a new anchor circle"
    (let [expected (-> (g/create-circle "Courage Labs")
                       (g/add-role-to-circle "Environmental Impact")
                       (g/add-role-to-circle "Benefit")
                       (g/add-role-to-circle "Investor"))]
      (should= [expected] (l/execute-governance new-circle-governance)))))