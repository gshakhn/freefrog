;
; Copyright © 2014 Courage Labs
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

;;; # Circle Manipulation Spec #
;;; Defines how circles may be updated.
(ns freefrog.governance-circles-spec
  (:require [clojure.pprint :as pp]
            [freefrog.governance :as g]
            [freefrog.governance-spec-helpers :refer :all]
            [speclj.core :refer :all]))

(def sample-role-name "Test Thing")

(def sample-circle
  (->
    (g/anchor-circle "My Circle")
    (g/add-role sample-role-name nil)))

;; Section 2.1
(describe "Circles"
  (it "can tell you if a role is authorized to act as a circle"
    (should (g/is-circle? sample-circle))
    (should-not (g/is-circle? sample-circle sample-role-name)))

  (it "can convert a role into a circle"
    (should (g/is-circle? (g/convert-to-circle sample-circle sample-role-name)
                          sample-role-name)))

  (it "refuses to convert a role that is already a circle into a circle"
    (should-throw IllegalArgumentException
      (format "Role %s is already a circle!" sample-role-name)
      (-> sample-circle
          (g/convert-to-circle sample-role-name)
          (g/convert-to-circle sample-role-name))))

  (it "can convert an empty circle back into a role"
    (should= sample-circle (-> sample-circle
                               (g/convert-to-circle sample-role-name)
                               (g/convert-to-role sample-role-name))))

  (it "refuses to convert a non-empty circle into a role")

  (it "refuses to convert a role that isn't a circle into a role"
    (should-throw IllegalArgumentException
      (format "Role %s is not a circle!" sample-role-name)
      (g/convert-to-role sample-circle sample-role-name)))

  (should-not-update-missing-or-empty-roles g/convert-to-circle
                                            "convert to circle")
  (should-not-update-missing-or-empty-roles g/convert-to-role
                                            "convert to role"))

;; Section 2.2
#_(describe "Lead Link Role"
  (it "doesn't let you create the Lead Link role")
  (it "doesn't let you add domains to the Lead Link")
  (it "doesn't let you add accountabilities to the Lead Link"))

;; Section 2.4
#_(describe "Role Assignment"
  ;; Appendix A/Lead Link
  (it "can assign someone to a role")

  ;; Section 2.4.2
  (it "can assign multiple people to a role")
  (it "can assign multiple people to a role with focuses")

  ;; Section 2.4.3, Appendix A/Lead Link
  (it "can remove someone from a role"))

;; Section 2.5
#_(describe "Elected Roles"
  (it "doesn't let you create any of the elected roles")

  ;; Section 2.5.1
  (it "won't assign the person in the Lead Link role to the Facilitator
    or Rep Link role")

  ;; Section 2.5.3
  (it "doesn't let you change the purpose of the special roles")
  (it "allows you to add/remove domains to/from any of the elected roles")
  (it "doesn't let you update/remove any of the constitutional domains of
        the elected roles")
  (it "allows you to add/remove accountabilities to/from any of the
        elected roles")
  (it "doesn't let you update/remove any of the constitutional
        accountabilities of the elected roles"))

(def subcircle-name "Development")
(def subcircle-role-name "Programmer")
(def subcircle-role-purpose "Coding")
(def circle-with-subcircle
  (-> sample-anchor
      (g/add-role subcircle-name "Great software")
      (g/convert-to-circle subcircle-name)))

(describe "Subcircle manipulation"
  (it "can add a role to a subcircle"
    (let [expected (update-in circle-with-subcircle [:roles subcircle-name]
                              g/add-role subcircle-role-name
                              subcircle-role-purpose)
          actual (g/update-subcircle circle-with-subcircle [subcircle-name]
                                     g/add-role subcircle-role-name
                                     subcircle-role-purpose)]
      (should= expected actual)))

  (it "can remove a role from a subcircle"
    (let [circle-with-subrole
          (g/update-subcircle circle-with-subcircle [subcircle-name]
                              g/add-role role-name
                              subcircle-role-purpose)]
      (should= circle-with-subcircle (g/update-subcircle
                                       circle-with-subrole
                                       [subcircle-name] g/remove-role
                                       subcircle-role-name))))

  (it "can manipulate a deeply-nested structure")

  (it "refuses to add a role to a role that isn't a circle"))

(run-specs)