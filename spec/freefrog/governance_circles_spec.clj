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


(def sample-policy-name "Do whatever")
(def sample-policy-text "Anybody can join/leave roles whenever")
(def sample-policies {sample-policy-name {:name   sample-policy-name
                                          :domain g/role-assignments-domain
                                          :text   sample-policy-text}})

(def sample-policy-name2 "Fire everybody")
(def sample-policy-text2 "Anybody can remove anybody else")

(def sample-anchor-with-lead-link-policy
  (g/add-role-policy sample-anchor g/lead-link-name sample-policy-name
                     sample-policy-text g/role-assignments-domain))

(def sample-anchor-with-lead-link-policies
  (g/add-role-policy sample-anchor-with-lead-link-policy g/lead-link-name
                     sample-policy-name2 sample-policy-text2))

;; Section 2.2.3
(describe "Lead Link Role"
  (it "won't add domains to the Lead Link"
    (should-throw IllegalArgumentException
      (format "May not add Domain to '%s'" g/lead-link-name)
      (g/add-role-domain sample-anchor g/lead-link-name "test"))
    (should-throw IllegalArgumentException
      (format "May not add Domain to '%s'" g/lead-link-name)
      (g/add-role-domain sample-anchor-with-lead-link-policy
                         g/lead-link-name "test")))

  (it "won't add accountabilities to the Lead Link"
    (should-throw IllegalArgumentException
      (format "May not add Accountability to '%s'" g/lead-link-name)
      (g/add-role-accountability sample-anchor g/lead-link-name "test"))
    (should-throw IllegalArgumentException
      (format "May not add Accountability to '%s'" g/lead-link-name)
      (g/add-role-accountability sample-anchor-with-lead-link-policy
                         g/lead-link-name "test")))

  (describe "adding policies"
    (it "can delegate a predefined domain from Lead Link"
      (should=
        (update-in sample-anchor [:roles] assoc g/lead-link-name
                   (g/map->Role {:name     g/lead-link-name
                                 :policies sample-policies}))
        sample-anchor-with-lead-link-policy))

    (it "won't create policies for domains Lead Link doesn't have"
      (should-throw IllegalArgumentException
        "Role 'Lead Link' doesn't control domain 'domain it doesn't have'"
        (g/add-role-policy sample-anchor g/lead-link-name sample-policy-name
                           sample-policy-text "domain it doesn't have")))

    (it "can create multiple policies"
      (should=
        (update-in sample-anchor [:roles] assoc g/lead-link-name
                   (g/map->Role {:name g/lead-link-name
                                 :policies
                                       (assoc sample-policies
                                              sample-policy-name2
                                              {:name sample-policy-name2
                                               :text sample-policy-text2})}))
        sample-anchor-with-lead-link-policies)))

  (describe "removing policies"
    (it "removes Lead Link when it is empty"
      (should= sample-anchor
        (g/remove-role-policy sample-anchor-with-lead-link-policy
                              g/lead-link-name sample-policy-name)))

    (it "doesn't remove Lead Link when it isn't empty"
      (should= sample-anchor-with-lead-link-policy
        (g/remove-role-policy sample-anchor-with-lead-link-policies
                              g/lead-link-name sample-policy-name2)))))

;; Section 2.4
(describe "Role Assignment"
  ;; Appendix A/Lead Link
  (it "can assign someone to a role")
  (it "can assign someone to Lead Link role")
  (it "can assign someone to a role with a term expiration date")

  ;; Section 2.4.2
  (it "can assign multiple people to a role")
  (it "can assign multiple people to a role with focuses")
  (it "won't assign multiple people to core roles")

  ;; Section 2.4.3, Appendix A/Lead Link
  (it "can remove someone from a role"))

;; Section 2.5
(describe "Elected Roles"
  ;; Section 2.5.1
  (it "won't assign the person in the Lead Link role to the Facilitator
    or Rep Link role")

  ;; Section 2.5.2
  (it "will only assign someone to an elected role with a term expiration date")

  ;; Section 2.5.3
  (it "can add domains")
  (it "can remove domains")
  (it "can add accountabilities")
  (it "can remove accountabilities")
  (it "can create policies")
  (it "can create policies with predefined domains")
  (it "can remove policies")
  (it "removes elected roles when they have no additions"))

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

(run-specs)