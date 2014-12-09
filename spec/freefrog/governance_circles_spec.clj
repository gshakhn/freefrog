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
            [clojure.set :as s]
            [freefrog.governance :as g]
            [freefrog.governance-spec-helpers :refer :all]
            [speclj.core :refer :all]))

(def sample-role-name "Test Thing")

(def sample-circle (g/add-role-to-circle sample-anchor sample-role-name nil))

;; Section 2.1
(describe "Circles"
  (it "can create a circle"
    (should= (g/map->Circle {:name "Courage Labs"})
      (g/create-circle "Courage Labs")))

  (it "doesn't work with an empty name"
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/create-circle nil))
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/create-circle "")))

  (it "can tell you if a role is authorized to act as a circle"
    (should (g/is-circle? sample-circle))
    (should-not (g/is-subrole-circle? sample-circle sample-role-name)))

  (it "can convert a role into a circle"
    (should (g/is-subrole-circle? (g/convert-to-circle sample-circle sample-role-name)
                          sample-role-name)))

  (it "refuses to convert a role that is already a circle into a circle"
    (should-throw IllegalArgumentException
      (format "Role '%s' is already a circle" sample-role-name)
      (-> sample-circle
          (g/convert-to-circle sample-role-name)
          (g/convert-to-circle sample-role-name))))

  (it "can convert an empty circle back into a role"
    (should= sample-circle (-> sample-circle
                               (g/convert-to-circle sample-role-name)
                               (g/convert-to-role sample-role-name))))

  (it "refuses to convert a non-empty circle into a role"
    (should-throw IllegalArgumentException
      (format "Circle %s still contains roles" sample-role-name)
      (let [circle-with-full-subcircle
            (-> sample-circle
                (g/convert-to-circle sample-role-name)
                (g/update-subcircle [sample-role-name] g/add-role-to-circle
                                    "Fun"))]
        (println (g/convert-to-role circle-with-full-subcircle sample-role-name)))))

  (it "refuses to convert a role that isn't a circle into a role"
    (should-throw IllegalArgumentException
      (format "Role '%s' is not a circle" sample-role-name)
      (g/convert-to-role sample-circle sample-role-name)))

  (should-not-update-missing-or-empty-roles g/convert-to-circle
                                            "convert to circle")
  (should-not-update-missing-or-empty-roles g/convert-to-role
                                            "convert to role"))

(describe "Constitutional Roles"
  )

;; Section 2.2.3
(describe "Lead Link Role"
  (it "won't add domains to the Lead Link")
  (it "won't add accountabilities to the Lead Link")
  (it "won't remove domains from Lead Link")
  (it "won't remove accountabilities from Lead Link")
  (it "can delegate a predefined domain from Lead Link to a role")
  (it "can delegate a predefined domain from Lead Link to a policy")
  (it "can delegate a predefined accountability from Lead Link to a role")
  (it "can delegate a predefined accountability from Lead Link to a policy")
  (it "can create policies"))

;; Section 2.4
(describe "Role Assignment"
  ;; Appendix A/Lead Link
  (it "can assign someone to a role")

  ;; Section 2.4.2
  (it "can assign multiple people to a role")
  (it "can assign multiple people to a role with focuses")

  ;; Section 2.4.3, Appendix A/Lead Link
  (it "can remove someone from a role"))

;; Section 2.5
(describe "Elected Roles"
  ;; Section 2.5.1
  (it "won't assign the person in the Lead Link role to the Facilitator
    or Rep Link role")

  ;; Section 2.5.3
  (it "can add domains")
  (it "can remove domains")
  (it "can add accountabilities")
  (it "won't add the same accountability twice to the same core role")
  (it "can remove accountabilities")
  (it "won't remove an accountability that doesn't exist")
  (it "won't manipulate role domains on non-circles")
  (it "won't remove any of the constitutional accountabilities")
  (it "can create policies"))

(def subcircle-name "Development")
(def subcircle-role-name "Programmer")
(def subcircle-role-purpose "Coding")
(def circle-with-subcircle
  (-> sample-anchor
      (g/add-role-to-circle subcircle-name "Great software")
      (g/convert-to-circle subcircle-name)))
(def circle-with-subrole
  (g/update-subcircle circle-with-subcircle [subcircle-name]
                      g/add-role-to-circle role-name
                      subcircle-role-purpose))

(describe "Subcircle manipulation"
  (it "can add a role to a subcircle"
    (let [expected (update-in circle-with-subcircle [:roles subcircle-name]
                              g/add-role-to-circle subcircle-role-name
                              subcircle-role-purpose)
          actual (g/update-subcircle circle-with-subcircle [subcircle-name]
                                     g/add-role-to-circle subcircle-role-name
                                     subcircle-role-purpose)]
      (should= expected actual)))

  (it "can remove a role from a subcircle"
    (should= circle-with-subcircle (g/update-subcircle
                                     circle-with-subrole
                                     [subcircle-name] g/remove-role
                                     subcircle-role-name)))

  (it "can manipulate a deeply-nested structure"
    (let [expected
          (update-in circle-with-subrole [:roles subcircle-name :roles
                                          subcircle-role-name]
                     g/convert-to-circle)

          actual
          (g/update-subcircle circle-with-subrole [subcircle-name
                                                   subcircle-role-name]
                              g/convert-to-circle)]
      (should= expected actual))))

(def domain "domain")
(def accountability "acc")
(def alternate-circle-name "something else")
(def policy "Policy1")
(def policy-text "Policy1 Text")

(def circle-with-subcircle-with-domain
  (g/add-role-domain circle-with-subcircle subcircle-name domain))

(def circle-with-subcircle-with-acc
  (g/add-role-accountability circle-with-subcircle subcircle-name
                             accountability))

(def circle-with-subcircle-with-policy
  (g/add-role-policy circle-with-subcircle subcircle-name policy policy-text))

(describe "using role operations on a circle is OK"
  (it "can change the purpose of a circle"
    (should= (update-in circle-with-subcircle [:roles subcircle-name]
                        assoc :purpose "stuff")
      (g/update-role-purpose circle-with-subcircle subcircle-name "stuff")))

  (it "can add a domain to a circle"
    (should= (update-in circle-with-subcircle [:roles subcircle-name]
                        assoc :domains #{domain})
      circle-with-subcircle-with-domain))

  (it "can remove a domain from a circle"
    (should= circle-with-subcircle
      (g/remove-role-domain circle-with-subcircle-with-domain subcircle-name
                            domain)))

  (it "can add an accountability to a circle"
    (should= (update-in circle-with-subcircle [:roles subcircle-name]
                        assoc :accountabilities #{accountability})
      circle-with-subcircle-with-acc))

  (it "can add a policy to a circle"
    (should= (update-in circle-with-subcircle [:roles subcircle-name]
                        assoc :policies {policy {:name policy
                                                 :text policy-text}})
      circle-with-subcircle-with-policy)
    (should= (update-in circle-with-subcircle-with-domain
                        [:roles subcircle-name]
                        assoc :policies {policy {:name   policy
                                                 :text   policy-text
                                                 :domain domain}})
      (g/add-role-policy circle-with-subcircle-with-domain subcircle-name policy
                         policy-text domain)))

  (it "can remove a policy from a circle"
    (should= circle-with-subcircle
      (g/remove-role-policy circle-with-subcircle-with-policy subcircle-name
                            policy)))

  (it "can remove an accountability from a circle"
    (should= circle-with-subcircle
      (g/remove-role-accountability circle-with-subcircle-with-acc
                                    subcircle-name accountability)))

  (it "can rename a circle"
    (should= (update-in circle-with-subcircle [:roles]
                        s/rename-keys {subcircle-name alternate-circle-name})
      (g/rename-role circle-with-subcircle subcircle-name alternate-circle-name)))

  (it "can remove a circle"
    (should= sample-anchor
      (g/remove-role circle-with-subcircle subcircle-name))))

(run-specs)