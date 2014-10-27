(ns freefrog.governance-spec
  (:require [clojure.set :as s]
            [freefrog.governance :as g]
            [speclj.core :refer :all]))

(def sample-anchor (g/anchor-circle "Sample"))
(def role-name "Programmer")

(def sample-purpose "Building awesome software")
(def sample-anchor-with-role (g/add-role sample-anchor role-name sample-purpose))
(def sample-domains ["Code" "Tests"])
(def sample-accountabilities ["Writing Code" "Testing their own stuff"])

(describe "Anchor Circle"
  (it "can create an anchor circle with a lead link"
    (should= {:name      "Courage Labs"
              :lead-link {:name  "Stephen Starkey"
                          :email "stephen@couragelabs.com"}}
      (g/anchor-circle "Courage Labs" "Stephen Starkey"
                       "stephen@couragelabs.com"))

    (should= {:name "Fear Labs" :lead-link {:name  "Bill O'Reilly"
                                            :email "billoreilly@foxnews.com"}}
      (g/anchor-circle "Fear Labs" "Bill O'Reilly"
                       "billoreilly@foxnews.com")))

  (it "can create an anchor circle without a lead link"
    (should= {:name "Courage Labs"} (g/anchor-circle "Courage Labs")))

  (it "doesn't let you leave any parameters empty"
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/anchor-circle nil))
    (should-throw IllegalArgumentException "Name may not be empty"
      (g/anchor-circle ""))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle nil nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "" nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "" "Joe" "joescmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" nil nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" nil
                       "joeschmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" ""
                       "joeschmoe@here.com"))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" "Joe Schmoe" nil))
    (should-throw IllegalArgumentException "No parameters may be empty"
      (g/anchor-circle "Not Enough Information" "Joe Schmoe" ""))))

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

    (it "doesn't let you create any of the special roles")

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
      (should-throw IllegalArgumentException "No role specified to remove"
        (g/remove-role sample-anchor-with-role nil))
      (should-throw IllegalArgumentException "No role specified to remove"
        (g/remove-role sample-anchor-with-role "")))

    (it "doesn't let you remove any of the special roles"))

  (describe "updating"
    (describe "name"
      (let [new-name "Code Monkey"]
        (it "can rename a role"
          (should= (update-in sample-anchor-with-role [:roles] s/rename-keys
                              {role-name new-name})
            (g/rename-role sample-anchor-with-role role-name new-name)))
        (it "should refuse to rename a role that doesn't exist"
          (should-throw IllegalArgumentException (str "Role not found: "
                                                      role-name)
            (g/rename-role sample-anchor role-name new-name)))

        (it "doesn't rename using an empty role name"
          (should-throw IllegalArgumentException "No role specified to update"
            (g/rename-role sample-anchor-with-role nil new-name))
          (should-throw IllegalArgumentException "No role specified to update"
            (g/rename-role sample-anchor-with-role "" new-name)))

        (it "doesn't let you rename any of the special roles")))

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

      (it "should refuse to change the purpose of a role that doesn't exist"
        (should-throw IllegalArgumentException (str "Role not found: "
                                                    role-name)
          (g/update-role-purpose sample-anchor role-name "Stuff")))

      (it "doesn't change purpose using an empty role name"
        (should-throw IllegalArgumentException "No role specified to update"
          (g/update-role-purpose sample-anchor-with-role nil "Stuff"))
        (should-throw IllegalArgumentException "No role specified to update"
          (g/update-role-purpose sample-anchor-with-role "" "Stuff")))

      (it "doesn't let you change the purpose of the special roles"))

    (describe "domains"
      (it "doesn't let you add domains to the Lead Link")
      (it "allows you to add/remove domains to/from any of the elected roles")
      (it "doesn't let you update/remove any of the constitutional domains of
        the elected roles"))

    (describe "accountabilities"
      (it "doesn't let you add accountabilities to the Lead Link")
      (it "allows you to add/remove accountabilities to/from any of the
        elected roles")
      (it "doesn't let you update/remove any of the constitutional
        accountabilities of the elected roles")))

  (describe "assigning"
    (it "can assign someone to a role")
    (it "can remove someone from a role")
    (it "can assign multiple people to a role")
    (it "can only assign one person each to the elected roles")
    (it "won't assign the person in the Lead Link role to the Facilitator
    role")))

(run-specs)
