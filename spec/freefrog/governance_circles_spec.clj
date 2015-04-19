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

;;; # Circle Manipulation Spec #
;;; Defines how circles may be updated.
(ns freefrog.governance-circles-spec
  (:require [freefrog.governance :as g]
            [freefrog.governance-spec-helpers :refer :all]
            [speclj.core :refer :all]))

(def sample-anchor-with-sample-policy
  (my-add-policy sample-anchor-with-role "test" "stuff"))

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
    (should (g/is-circle? sample-anchor-with-role))
    (should-not (g/is-subrole-circle? sample-anchor-with-role role-name)))

  (it "can convert a role into a circle"
    (should (g/is-subrole-circle?
              (g/convert-to-circle sample-anchor-with-role role-name)
              role-name)))

  (it "refuses to convert a role that is already a circle into a circle"
    (should-throw IllegalArgumentException
      (format "Role '%s' is already a circle" role-name)
      (-> sample-anchor-with-role
          (g/convert-to-circle role-name)
          (g/convert-to-circle role-name))))

  (it "can convert an empty circle back into a role"
    (should (.equals sample-anchor-with-role
                     (-> sample-anchor-with-role
                         (g/convert-to-circle role-name)
                         (g/convert-to-role role-name)))))

  (it "refuses to convert a non-empty circle into a role"
    (should-throw IllegalArgumentException
      (format "Circle %s still contains roles" role-name)
      (let [circle-with-full-subcircle
            (-> sample-anchor-with-role
                (g/convert-to-circle role-name)
                (g/update-subcircle [role-name] g/add-role-to-circle
                                    "Fun"))]
        (g/convert-to-role circle-with-full-subcircle role-name))))

  (it "refuses to convert a role that isn't a circle into a role"
    (should-throw IllegalArgumentException
      (format "Role '%s' is not a circle" role-name)
      (g/convert-to-role sample-anchor-with-role role-name)))

  (should-not-update-missing-or-empty-roles g/convert-to-circle
    "convert to circle")
  (should-not-update-missing-or-empty-roles g/convert-to-role
    "convert to role")

  ;; TODO unify this with the role-specific code because there is a TON
  ;; of validation and behavior in there
  (describe "policies"
    (describe "adding"
      (it "can add to a circle with no policies"
        (should= sample-anchor-with-sample-policy
          (g/add-policy sample-anchor-with-role "test" "stuff"))))))


(def sample-policy-name "Do whatever")
(def sample-policy-text "Anybody can do anything whenever")

(def sample-policies-lead-link {sample-policy-name
                                {:name   sample-policy-name
                                 :domain g/role-assignments-domain
                                 :text   sample-policy-text}})

(def sample-policies-secretary {sample-policy-name
                                {:name   sample-policy-name
                                 :domain g/governance-records-domain
                                 :text   sample-policy-text}})
(def sample-policy-name2 "Do things to other people")
(def sample-policy-text2 "Anybody do things to anyone else")

(def sample-anchor-with-lead-link-policy
  (g/add-role-policy sample-anchor g/lead-link-name sample-policy-name
                     sample-policy-text g/role-assignments-domain))

(def sample-anchor-with-lead-link-policies
  (g/add-role-policy sample-anchor-with-lead-link-policy g/lead-link-name
                     sample-policy-name2 sample-policy-text2))

(def sample-anchor-with-secretary-policy
  (g/add-role-policy sample-anchor g/secretary-name sample-policy-name
                     sample-policy-text g/governance-records-domain))

(def sample-anchor-with-secretary-policies
  (g/add-role-policy sample-anchor-with-secretary-policy g/secretary-name
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
                                 :policies sample-policies-lead-link}))
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
                                       (assoc sample-policies-lead-link
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

(def sample-domain1 "stuff")
(def sample-domain2 "bits")

(def sample-anchor-with-secretary-with-domain
  (g/add-role-domain sample-anchor g/secretary-name sample-domain1))

(def sample-anchor-with-secretary-with-domains
  (g/add-role-domain sample-anchor-with-secretary-with-domain
                     g/secretary-name sample-domain2))

(def sample-anchor-with-facilitator-with-domain
  (g/add-role-domain sample-anchor g/facilitator-name sample-domain1))

(def sample-anchor-with-facilitator-with-domains
  (g/add-role-domain sample-anchor-with-facilitator-with-domain
                     g/facilitator-name sample-domain2))

(def sample-anchor-with-rep-link-with-domain
  (g/add-role-domain sample-anchor g/rep-link-name sample-domain1))

(def sample-anchor-with-rep-link-with-domains
  (g/add-role-domain sample-anchor-with-rep-link-with-domain
                     g/rep-link-name sample-domain2))

(def sample-acc1 "doing stuff")
(def sample-acc2 "doing bits")

(def sample-anchor-with-secretary-with-acc
  (g/add-role-accountability sample-anchor g/secretary-name sample-acc1))

(def sample-anchor-with-secretary-with-accs
  (g/add-role-accountability sample-anchor-with-secretary-with-acc
                             g/secretary-name sample-acc2))

(def sample-anchor-with-facilitator-with-acc
  (g/add-role-accountability sample-anchor g/facilitator-name sample-acc1))

(def sample-anchor-with-facilitator-with-accs
  (g/add-role-accountability sample-anchor-with-facilitator-with-acc
                             g/facilitator-name sample-acc2))

(def sample-anchor-with-rep-link-with-acc
  (g/add-role-accountability sample-anchor g/rep-link-name sample-acc1))

(def sample-anchor-with-rep-link-with-accs
  (g/add-role-accountability sample-anchor-with-rep-link-with-acc
                             g/rep-link-name sample-acc2))

(def sample-anchor-with-rep-link-with-acc-and-domain
  (g/add-role-domain sample-anchor-with-rep-link-with-acc
                     g/rep-link-name sample-domain1))

(defn- should-manipulate-things-in-core-role
  [role-name description which-things sample-with-one sample-with-two first
   second removal-fn]
  (describe (format "%s %s" role-name description)
    (it "can add one"
      (should= (update-in sample-anchor [:roles] assoc role-name
                          (g/map->Role {:name        role-name
                                        which-things #{first}}))
        sample-with-one))

    (it "can add second one"
      (should= (update-in sample-with-one
                          [:roles role-name which-things] conj
                          second)
        sample-with-two))

    (it "removes role when last one is removed"
      (should= sample-anchor
        (removal-fn sample-with-one role-name first)))

    (it "doesn't remove role when second-to-last one is removed"
      (should= sample-with-one
        (removal-fn sample-with-two role-name second)))))

;; Section 2.5
(describe "Elected Roles"
  ;; Section 2.5.2
  (it "can specify that an elected role has had someone elected to it
       and when their term expires")

  ;; Section 2.5.3
  (should-manipulate-things-in-core-role
    g/secretary-name "domains"
    :domains sample-anchor-with-secretary-with-domain
    sample-anchor-with-secretary-with-domains
    sample-domain1 sample-domain2 g/remove-role-domain)

  (should-manipulate-things-in-core-role
    g/secretary-name "accountabilities"
    :accountabilities sample-anchor-with-secretary-with-acc
    sample-anchor-with-secretary-with-accs
    sample-acc1 sample-acc2 g/remove-role-accountability)

  (should-manipulate-things-in-core-role
    g/facilitator-name "domains"
    :domains sample-anchor-with-facilitator-with-domain
    sample-anchor-with-facilitator-with-domains
    sample-domain1 sample-domain2 g/remove-role-domain)

  (should-manipulate-things-in-core-role
    g/facilitator-name "accountabilities"
    :accountabilities sample-anchor-with-facilitator-with-acc
    sample-anchor-with-facilitator-with-accs
    sample-acc1 sample-acc2 g/remove-role-accountability)

  (should-manipulate-things-in-core-role
    g/rep-link-name "domains"
    :domains sample-anchor-with-rep-link-with-domain
    sample-anchor-with-rep-link-with-domains
    sample-domain1 sample-domain2 g/remove-role-domain)

  (should-manipulate-things-in-core-role
    g/rep-link-name "accountabilities"
    :accountabilities sample-anchor-with-rep-link-with-acc
    sample-anchor-with-rep-link-with-accs
    sample-acc1 sample-acc2 g/remove-role-accountability)

  (it "doesn't remove core role when manipulating one collection among many"
    (should= sample-anchor-with-rep-link-with-acc
      (g/remove-role-domain sample-anchor-with-rep-link-with-acc-and-domain
                            g/rep-link-name sample-domain1)))

  (describe "Adding policies"
    (it "can delegate a predefined domain from Secretary"
      (should=
        (update-in sample-anchor [:roles] assoc g/secretary-name
                   (g/map->Role {:name     g/secretary-name
                                 :policies sample-policies-secretary}))
        sample-anchor-with-secretary-policy))

    (it "won't create policies for domains Lead Link doesn't have"
      (should-throw IllegalArgumentException
        "Role 'Secretary' doesn't control domain 'domain it doesn't have'"
        (g/add-role-policy sample-anchor g/secretary-name sample-policy-name
                           sample-policy-text "domain it doesn't have")))

    (it "can create multiple policies"
      (should=
        (update-in sample-anchor [:roles] assoc g/secretary-name
                   (g/map->Role {:name g/secretary-name
                                 :policies
                                       (assoc sample-policies-secretary
                                              sample-policy-name2
                                              {:name sample-policy-name2
                                               :text sample-policy-text2})}))
        sample-anchor-with-secretary-policies)))

  (describe "removing policies"
    (it "removes Secretary when it is empty"
      (should= sample-anchor
        (g/remove-role-policy sample-anchor-with-secretary-policy
                              g/secretary-name sample-policy-name)))

    (it "doesn't remove Secretary when it isn't empty"
      (should= sample-anchor-with-secretary-policy
        (g/remove-role-policy sample-anchor-with-secretary-policies
                              g/secretary-name sample-policy-name2))))

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