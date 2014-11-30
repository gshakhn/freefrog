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
(def sample-domains [sample-domain-1 sample-domain-2])
(def sample-anchor-with-domain
  (g/add-role-domain sample-anchor-with-role role-name sample-domain-1))
(def sample-anchor-with-domains
  (g/add-role-domain sample-anchor-with-domain role-name sample-domain-2))
(def sample-acc-1 "Writing Code")
(def sample-acc-2 "Testing their own stuff")
(def sample-accountabilities [sample-acc-1 sample-acc-2])
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
      (should= (assoc sample-anchor :roles {role-name
                                            {:name    role-name
                                             :purpose sample-purpose}})
        sample-anchor-with-role))

    (it "can add a role to a circle that already has roles"
      (let [second-role-name "Tester"
            second-role-purpose "Making sure Programmers don't screw up"]
        (should= (update-in sample-anchor-with-role [:roles] assoc
                            second-role-name
                            {:name    second-role-name
                             :purpose second-role-purpose})
          (g/add-role sample-anchor-with-role second-role-name
                      second-role-purpose))))

    (it "can add a role to a circle with name and accountabilities"
      (should= (assoc sample-anchor :roles
                      {role-name {:name             role-name
                                  :accountabilities sample-accountabilities}})
        (g/add-role sample-anchor role-name
                    nil nil sample-accountabilities)))

    (it "can add a role to a circle with name, purpose, and domains"
      (should= (assoc sample-anchor :roles {role-name {:name    role-name
                                                       :domains sample-domains}})
        (g/add-role sample-anchor role-name nil sample-domains nil)))

    (it "can add a role to a circle with name, purpose, and accountabilities"
      (should= (assoc sample-anchor :roles {role-name {:name    role-name
                                                       :domains sample-domains}})
        (g/add-role sample-anchor role-name nil sample-domains nil)))

    (it "can add a role to a circle with everything"
      (should= (assoc sample-anchor :roles
                      {role-name {:name             role-name
                                  :purpose          sample-purpose
                                  :domains          sample-domains
                                  :accountabilities sample-accountabilities}})
        (g/add-role sample-anchor role-name sample-purpose sample-domains
                    sample-accountabilities)))

    (it "doesn't let you use empty names"
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/add-role sample-anchor nil nil nil nil))
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/add-role sample-anchor "" nil nil nil)))

    (it "doesn't let you overwrite an existing role"
      (should-throw IllegalArgumentException (str "Role already exists: "
                                                  role-name)
        (g/add-role sample-anchor-with-role role-name nil nil nil))))

  (describe "removing"
    (it "can remove a role"
      (should= sample-anchor-with-role (-> sample-anchor-with-role
                                           (g/add-role "test" "test")
                                           (g/remove-role "test"))))

    (it "removes the roles array if deleting a role causes it to be empty"
      (should= sample-anchor (g/remove-role sample-anchor-with-role role-name)))

    (should-not-update-missing-or-empty-roles g/remove-role "role itself"))

  (describe "updating"
    (describe "name"
      (let [new-name "Code Monkey"]
        (it "can rename a role"
          (should= (update-in sample-anchor-with-role [:roles] s/rename-keys
                              {role-name new-name})
            (g/rename-role sample-anchor-with-role role-name new-name)))
        (should-not-update-missing-or-empty-roles g/rename-role "renaming role"
          new-name)))

    ;; Section 1.1.a
    (describe "purpose"
      (it "can change a role's purpose"
        (let [new-purpose "Building software that's grrreat!"]
          (should= (update-in sample-anchor-with-role [:roles role-name]
                              assoc :purpose new-purpose)
            (g/update-role-purpose sample-anchor-with-role role-name
                                   new-purpose))))

      (it "can clear a role's purpose"
        (should= (update-in sample-anchor-with-role [:roles role-name] dissoc
                            :purpose)
          (g/update-role-purpose sample-anchor-with-role role-name nil))
        (should= (update-in sample-anchor-with-role [:roles role-name] dissoc
                            :purpose)
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
(run-specs)
