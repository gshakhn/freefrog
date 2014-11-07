;;; # Role Manipulation Spec #
;;; Defines how all roles can be manipulated, both through governance
;;; (maintenance and elections) and through the normal business of
;;; appointment/removal.
(ns freefrog.governance-roles-spec
  (:require [clojure.set :as s]
            [freefrog.governance :as g]
            [speclj.core :refer :all]))

(def sample-anchor (g/anchor-circle "Sample"))
(def role-name "Programmer")

(def sample-purpose "Building awesome software")
(def sample-anchor-with-role (g/add-role sample-anchor role-name sample-purpose))

(def sample-domain-1 "Code")
(def sample-domain-2 "Tests")
(def sample-domains [sample-domain-1 sample-domain-2])
(def sample-anchor-with-domain
  (g/add-domain sample-anchor-with-role role-name sample-domain-1))
(def sample-anchor-with-domains
  (g/add-domain sample-anchor-with-domain role-name sample-domain-2))
(def sample-acc-1 "Writing Code")
(def sample-acc-2 "Testing their own stuff")
(def sample-accountabilities [sample-acc-1 sample-acc-2])
(def sample-anchor-with-acc
  (g/add-accountability sample-anchor-with-role role-name sample-acc-1))
(def sample-anchor-with-accs
  (-> sample-anchor-with-role (g/add-accountability role-name sample-acc-1)
      (g/add-accountability role-name sample-acc-2)))

(defn should-not-update-missing-or-empty-roles [fn type-str val]
  (describe (format "%s problems" type-str)
    (it "doesn't work with a role that doesn't exist"
      (should-throw IllegalArgumentException (str "Role not found: "
                                                  role-name)
        (fn sample-anchor role-name val)))

    (it "doesn't work with an empty role name"
      (should-throw IllegalArgumentException "Name may not be empty"
        (fn sample-anchor-with-role nil val))
      (should-throw IllegalArgumentException "Name may not be empty"
        (fn sample-anchor-with-role "" val)))))

(defn should-handle-collection-properly [add-fn remove-fn type type-str coll1
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

    (it (str "refuses to add the same " type-str " twice")
      (should-throw IllegalArgumentException
        (format "%s '%s' already exists on role '%s'" type-str val1 role-name)
        (add-fn coll1 role-name val1)))

    (it (str "can remove a " type-str " from a role")
      (should= coll1 (remove-fn coll2 role-name val2)))

    (it (str "removes the " type-str "s array when there are none left")
      (should= sample-anchor-with-role (remove-fn coll1 role-name val1)))

    (it (str "refuses to remove a " type-str " that doesn't exist")
      (should-throw IllegalArgumentException
        (format "%s '%s' doesn't exist on role '%s'" type-str val2 role-name)
        (remove-fn coll1 role-name val2)))

    (should-not-update-missing-or-empty-roles remove-fn
      (str "removing a " type-str) val1)))

;; Section 3.1.a
(describe "Role Manipulation"
  (describe "adding"
    (it "can add a role to a circle with name and purpose"
      (should= (assoc sample-anchor :roles {role-name {:purpose sample-purpose}})
        sample-anchor-with-role))

    (it "can add a role to a circle that already has roles"
      (should= (update-in sample-anchor-with-role [:roles] assoc "Tester"
                          {:purpose "Making sure Programmers don't screw up"})
        (g/add-role sample-anchor-with-role "Tester"
                    "Making sure Programmers don't screw up")))

    (it "can add a role to a circle with name and accountabilities"
      (should= (assoc sample-anchor :roles
                      {role-name {:accountabilities sample-accountabilities}})
        (g/add-role sample-anchor role-name
                    nil nil sample-accountabilities)))

    (it "can add a role to a circle with name, purpose, and domains"
      (should= (assoc sample-anchor :roles {role-name {:domains sample-domains}})
        (g/add-role sample-anchor role-name
                    nil sample-domains nil)))

    (it "can add a role to a circle with name, purpose, and accountabilities"
      (should= (assoc sample-anchor :roles {role-name {:domains sample-domains}})
        (g/add-role sample-anchor role-name
                    nil sample-domains nil)))

    (it "can add a role to a circle with everything"
      (should= (assoc sample-anchor :roles
                      {role-name {:purpose          sample-purpose
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

    (it "doesn't let you remove a role that doesn't exist"
      (should-throw IllegalArgumentException (str "Role not found: " role-name)
        (g/remove-role sample-anchor role-name)))

    (it "doesn't let you remove using an empty role name"
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/remove-role sample-anchor-with-role nil))
      (should-throw IllegalArgumentException "Name may not be empty"
        (g/remove-role sample-anchor-with-role ""))))

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
        "updating purpose"
        "Stuff"))

    ;; Section 1.1.b
    (should-handle-collection-properly g/add-domain g/remove-domain
                                       :domains "Domain"
                                       sample-anchor-with-domain
                                       sample-anchor-with-domains
                                       sample-domain-1
                                       sample-domain-2)

    ;; Section 1.1.c
    (should-handle-collection-properly g/add-accountability g/remove-accountability
                                       :accountabilities "Accountability"
                                       sample-anchor-with-acc
                                       sample-anchor-with-accs
                                       sample-acc-1
                                       sample-acc-2)))

;; Section 2.2
(describe "Lead Link Role"
  (it "doesn't let you create the Lead Link role")
  (it "doesn't let you add domains to the Lead Link")
  (it "doesn't let you add accountabilities to the Lead Link"))

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

(run-specs)
