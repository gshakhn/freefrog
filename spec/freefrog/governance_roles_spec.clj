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
    (describe "domains"
      (it "can add a domain to a role with no domains"
        (should= (update-in sample-anchor-with-role [:roles role-name]
                            assoc :domains #{sample-domain-1})
          (g/add-domain sample-anchor-with-role role-name sample-domain-1)))

      (it "can add a domain to a role with existing domains"
        (let [expected
              (update-in sample-anchor-with-domain [:roles role-name :domains]
                         conj sample-domain-2)]
          (should= expected
            (g/add-domain sample-anchor-with-domain role-name sample-domain-2))))

      (should-not-update-missing-or-empty-roles g/add-domain "adding a domain"
        sample-domain-1)

      (it "refuses to add the same domain twice"
        (should-throw IllegalArgumentException
          (format "Domain '%s' already exists on role '%s'" sample-domain-1
                  role-name)
          (g/add-domain sample-anchor-with-domain role-name sample-domain-1)))

      (it "can remove a domain from a role"
        (should= sample-anchor-with-domain
          (g/remove-domain sample-anchor-with-domains role-name
                           sample-domain-2)))

      (it "removes the domains array when there are no domains"
        (should= sample-anchor-with-role
          (g/remove-domain sample-anchor-with-domain role-name
                           sample-domain-1)))

      (it "refuses to remove a domain that doesn't exist"
        (should-throw IllegalArgumentException
          (format "Domain '%s' doesn't exist on role '%s'" sample-domain-2
                  role-name)
          (g/remove-domain sample-anchor-with-domain role-name
                           sample-domain-2)))

      (should-not-update-missing-or-empty-roles g/remove-domain
        "removing domain" sample-domain-1))

    ;; Section 1.1.c
    (describe "accountabilities"
      (it "adds an accountability to a role that has no accountabilities"
        (should= (update-in sample-anchor-with-role [:roles role-name]
                            assoc :accountabilities #{sample-acc-1})
          (g/add-accountability sample-anchor-with-role role-name
                                sample-acc-1)))

      (it "adds an accountability to a role that already has one"
        (should= (update-in sample-anchor-with-acc [:roles role-name
                                                    :accountabilities]
                            conj sample-acc-2)
          (g/add-accountability sample-anchor-with-acc role-name
                                sample-acc-2)))

      (it "refuses to add the same accountability twice"
        (should-throw IllegalArgumentException
          (format "Accountability '%s' already exists on role '%s'" sample-acc-1
                  role-name)
          (g/add-accountability sample-anchor-with-acc role-name sample-acc-1)))

      (should-not-update-missing-or-empty-roles g/add-accountability
        "adding accountability" sample-acc-1)

      (it "can remove an accountability"
        (should= sample-anchor-with-acc
          (g/remove-accountability sample-anchor-with-accs role-name sample-acc-2))
        (should= sample-anchor-with-role
          (g/remove-accountability sample-anchor-with-acc role-name sample-acc-1)))

      (it "refuses to remove an accountability that doesn't exist"
        (should-throw IllegalArgumentException
          (format "Accountability '%s' doesn't exist on role '%s'"
                  sample-acc-2 role-name)
          (g/remove-accountability sample-anchor-with-acc role-name
                                   sample-acc-2)))

      (it "should not remove an accountability from a missing or empty role"
        (should-not-update-missing-or-empty-roles g/remove-accountability
          "removing accountability" sample-acc-1)))))

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
