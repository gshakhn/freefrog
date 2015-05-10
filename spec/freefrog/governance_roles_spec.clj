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

;;; # Role Manipulation Spec #
;;; Defines how all roles can be manipulated, both through governance
;;; (maintenance and elections) and through the normal business of
;;; appointment/removal.
(ns freefrog.governance-roles-spec
  (:require [clojure.set :as s]
            [freefrog.governance :as g]
            [freefrog.governance-spec-helpers :refer :all]
            [speclj.core :refer :all]))

(def sample-domain-1 "Code")
(def sample-domain-2 "Tests")
(def sample-domains #{sample-domain-1 sample-domain-2})
(def sample-anchor-with-domain
  (g/add-role-domain sample-anchor-with-role role-name sample-domain-1))
(def sample-anchor-with-domains
  (g/add-role-domain sample-anchor-with-domain role-name sample-domain-2))
(def sample-acc-1 "Writing Code")
(def sample-acc-2 "Testing their own stuff")
(def sample-accountabilities #{sample-acc-1 sample-acc-2})
(def sample-anchor-with-acc
  (g/add-role-accountability sample-anchor-with-role role-name sample-acc-1))
(def sample-anchor-with-accs
  (-> sample-anchor-with-role (g/add-role-accountability role-name sample-acc-1)
      (g/add-role-accountability role-name sample-acc-2)))

(defn- should-handle-collection-properly [add-fn remove-fn type type-str coll1
                                          coll2 val1 val2]
  (describe type-str
    (it (str "can add a " type-str " to a role with no " type-str "s")
      (should= (update-in sample-anchor-with-role [:roles role-name]
                          assoc type #{val1})
        (add-fn sample-anchor-with-role role-name val1)))

    (it (str "can add a " type-str " to a role with existing " type-str "s")
      (let [expected
            (update-in coll1 [:roles role-name type] conj val2)]
        (should= expected
          (add-fn coll1 role-name val2))))

    (should-not-update-missing-or-empty-roles add-fn
      (str "adding a " type-str) val1)

    (it (str "won't add the same " type-str " twice")
      (should-throw IllegalArgumentException
        (format "%s '%s' already exists on role '%s'" type-str val1 role-name)
        (add-fn coll1 role-name val1)))

    (it (str "can remove a " type-str " from a role")
      (should= coll1 (remove-fn coll2 role-name val2)))

    (it (str "removes the " type-str "s array when there are none left")
      (should= sample-anchor-with-role (remove-fn coll1 role-name val1)))

    (it (str "won't remove a " type-str " that doesn't exist")
      (should-throw IllegalArgumentException
        (format "%s '%s' doesn't exist on role '%s'" type-str val2 role-name)
        (remove-fn coll1 role-name val2)))

    (should-not-update-missing-or-empty-roles remove-fn
      (str "removing a " type-str) val1)))

;; Section 3.1.a
(describe "Role Manipulation"
  (describe "adding"
    (it "can add a role to a circle with name and purpose"
      (should (.equals (assoc sample-anchor :roles
                              {role-name
                               (g/map->Role {:name    role-name
                                             :purpose sample-purpose})})
                       sample-anchor-with-role)))

    (it "can add a role to a circle that already has roles"
      (let [second-role-name "Tester"
            second-role-purpose "Making sure Programmers don't screw up"]
        (should
          (.equals (update-in sample-anchor-with-role [:roles] assoc
                              second-role-name
                              (g/map->Role {:name    second-role-name
                                            :purpose second-role-purpose}))
                   (g/add-role-to-circle sample-anchor-with-role
                                         second-role-name
                                         second-role-purpose)))))

    (it "can add a role to a circle with name and accountabilities"
      (should
        (.equals (assoc sample-anchor :roles
                        {role-name
                         (g/map->Role
                           {:name             role-name
                            :accountabilities sample-accountabilities})})
                 (g/add-role-to-circle sample-anchor role-name
                                       nil nil sample-accountabilities))))

    (it "can add a role to a circle with name, purpose, and domains"
      (should
        (.equals (assoc sample-anchor :roles
                        {role-name
                         (g/map->Role {:name    role-name
                                       :domains sample-domains})})
                 (g/add-role-to-circle sample-anchor role-name nil
                                       sample-domains nil))))

    (it "can add a role to a circle with name, purpose, and accountabilities"
      (should
        (.equals (assoc sample-anchor :roles
                        {role-name
                         (g/map->Role {:name    role-name
                                       :domains sample-domains})})
                 (g/add-role-to-circle sample-anchor role-name nil
                                       sample-domains nil))))

    (it "can add a role to a circle with everything"
      (should (.equals (assoc sample-anchor
                              :roles
                              {role-name
                               (g/map->Role
                                 {:name             role-name
                                  :purpose          sample-purpose
                                  :domains          sample-domains
                                  :accountabilities sample-accountabilities})})
                       (g/add-role-to-circle sample-anchor role-name
                                             sample-purpose sample-domains
                                             sample-accountabilities))))

    (it "doesn't let you use empty names"
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/add-role-to-circle sample-anchor nil nil nil nil))
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/add-role-to-circle sample-anchor "" nil nil nil)))

    (it "doesn't let you overwrite an existing role"
      (should-throw IllegalArgumentException (str "Role already exists: "
                                                  role-name)
        (g/add-role-to-circle sample-anchor-with-role role-name nil nil nil))))

  (describe "removing"
    (it "can remove a role"
      (should= sample-anchor-with-role (-> sample-anchor-with-role
                                           (g/add-role-to-circle "test" "test")
                                           (g/remove-role "test"))))

    (it "removes the roles array if deleting a role causes it to be empty"
      (should= sample-anchor (g/remove-role sample-anchor-with-role role-name)))

    (should-not-update-missing-or-empty-roles g/remove-role "role itself"))

  (def new-name "Code Monkey")
  (describe "updating"
    (describe "name"
      (it "can rename a role"
        (should= (-> sample-anchor-with-role
                     (update-in [:roles] s/rename-keys {role-name new-name})
                     (update-in [:roles new-name] assoc :name new-name))

          (g/rename-role sample-anchor-with-role role-name new-name)))
      (should-not-update-missing-or-empty-roles g/rename-role "renaming role"
        new-name))

    ;; Section 1.1.a
    (describe "purpose"
      (it "can change a role's purpose"
        (let [new-purpose "Building software that's grrreat!"]
          (should= (update-in sample-anchor-with-role [:roles role-name]
                              assoc :purpose new-purpose)
            (g/update-role-purpose sample-anchor-with-role role-name
                                   new-purpose))))

      (it "can clear a role's purpose"
        (should= (update-in sample-anchor-with-role [:roles role-name] assoc
                            :purpose nil)
          (g/update-role-purpose sample-anchor-with-role role-name nil))
        (should= (update-in sample-anchor-with-role [:roles role-name] assoc
                            :purpose nil)
          (g/update-role-purpose sample-anchor-with-role role-name "")))

      (should-not-update-missing-or-empty-roles g/update-role-purpose
        "updating purpose" "Stuff"))

    ;; Section 1.1.b
    (should-handle-collection-properly g/add-role-domain
                                       g/remove-role-domain
                                       :domains "Domain"
                                       sample-anchor-with-domain
                                       sample-anchor-with-domains
                                       sample-domain-1
                                       sample-domain-2)

    ;; Section 1.1.c
    (should-handle-collection-properly g/add-role-accountability
                                       g/remove-role-accountability
                                       :accountabilities "Accountability"
                                       sample-anchor-with-acc
                                       sample-anchor-with-accs
                                       sample-acc-1
                                       sample-acc-2)))

(def sample-policy-name "Pull requests")
(def sample-policy-text "You gotta use pull requests to contribute any code.")
(def sample-policy2-name "Straight to Master")
(def sample-policy2-text "Just tell someone what the commit has was.")
(def sample-anchor-with-policy (g/add-role-policy sample-anchor-with-domain
                                                  role-name sample-policy-name
                                                  sample-policy-text))
(def sample-anchor-with-policies (g/add-role-policy sample-anchor-with-policy
                                                    role-name
                                                    sample-policy2-name
                                                    sample-policy2-text))

;; Section 1.3
(describe "policies"
  (should-not-update-missing-or-empty-roles g/add-role-policy "policy"
    sample-policy-name sample-policy-text)
  (should-not-update-missing-or-empty-roles g/add-role-policy "policy"
    sample-policy-name
    sample-policy-text sample-domain-1)
  (it "can add a policy granting access to all domains"
    (should= (my-add-policy sample-anchor-with-domain role-name
                            sample-policy-name sample-policy-text)
      sample-anchor-with-policy)
    (should= (my-add-policy sample-anchor-with-policy role-name
                            sample-policy2-name sample-policy2-text)
      (g/add-role-policy sample-anchor-with-policy role-name
                         sample-policy2-name sample-policy2-text)))

  (it "can add a policy granting access to a domain"
    (should= (my-add-policy sample-anchor-with-domain role-name
                            sample-policy-name sample-policy-text
                            sample-domain-1)
      (g/add-role-policy sample-anchor-with-domain role-name
                         sample-policy-name sample-policy-text
                         sample-domain-1)))
  (it (str "won't add a policy granting access to a domain that the role"
           "doesn't control")
    (should-throw IllegalArgumentException
      (format "Role '%s' doesn't control domain '%s'" role-name sample-domain-2)
      (g/add-role-policy sample-anchor-with-domain role-name
                         "Don't test my stuff!" "Only I can test stuff"
                         sample-domain-2)))
  (it "won't add a policy with the same name as one that already exists"
    (should-throw IllegalArgumentException
      (format "Policy '%s' already exists on role '%s'" sample-policy-name
              role-name)
      (g/add-role-policy sample-anchor-with-policy role-name sample-policy-name
                         "More coding stuff!")))

  (should-not-update-missing-or-empty-roles g/remove-role-policy "policy"
    sample-policy-name)

  (it "can remove a policy"
    (should= sample-anchor-with-policy
      (g/remove-role-policy sample-anchor-with-policies role-name
                            sample-policy2-name))
    (should= sample-anchor-with-domain
      (g/remove-role-policy sample-anchor-with-policy role-name
                            sample-policy-name)))
  (it "won't remove a policy that doesn't exist"
    (should-throw IllegalArgumentException
      (format "Policy '%s' doesn't exist on role '%s'" sample-policy-name
              role-name)
      (g/remove-role-policy sample-anchor-with-domain role-name
                            sample-policy-name))))
(run-specs)
