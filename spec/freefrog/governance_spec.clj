(ns freefrog.governance-spec
  (:require [speclj.core :refer :all]
            [freefrog.governance :as g]))

(def sample-anchor (g/anchor-circle "Sample"))
(def role-name "Programmer")

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

(let [purpose "Building awesome software"
      sample-anchor-with-role (g/add-role sample-anchor role-name purpose)
      domains ["Code" "Tests"]
      accountabilities ["Writing Code" "Testing their own stuff"]]
  (describe "Roles"
    (describe "adding"
      (it "can add a role to a circle with name and purpose"
        (should= (assoc sample-anchor :roles {role-name {:purpose purpose}})
                 sample-anchor-with-role))

      (it "can add a role to a circle that already has roles"
        (should= (update-in sample-anchor-with-role [:roles] assoc "Tester"
                            {:purpose "Making sure Programmers don't screw up"})
                 (g/add-role sample-anchor-with-role "Tester"
                             "Making sure Programmers don't screw up")))

      (it "can add a role to a circle with name and accountabilities"
        (should= (assoc sample-anchor :roles
                        {role-name {:accountabilities accountabilities}})
                 (g/add-role sample-anchor role-name
                             nil nil accountabilities)))

      (it "can add a role to a circle with name, purpose, and domains"
        (should= (assoc sample-anchor :roles {role-name {:domains domains}})
                 (g/add-role sample-anchor role-name
                             nil domains nil)))

      (it "can add a role to a circle with name, purpose, and accountabilities"
        (should= (assoc sample-anchor :roles {role-name {:domains domains}})
                 (g/add-role sample-anchor role-name
                             nil domains nil)))

      (it "can add a role to a circle with everything"
        (should= (assoc sample-anchor :roles
                        {role-name {:purpose          purpose
                                    :domains          domains
                                    :accountabilities accountabilities}})
                 (g/add-role sample-anchor role-name purpose domains
                             accountabilities)))

      (it "doesn't let you use empty names"
        (should-throw IllegalArgumentException "Name may not be empty"
                      (g/add-role sample-anchor nil nil nil nil))
        (should-throw IllegalArgumentException "Name may not be empty"
                      (g/add-role sample-anchor "" nil nil nil)))
      (it "doesn't let you overwrite an existing role"
        (should-throw IllegalArgumentException (str "Role already exists: "
                                                    role-name)
                      (g/add-role sample-anchor-with-role role-name nil nil
                                  nil))))

    (describe "removing"
      (it "can remove a role"
        (should= sample-anchor-with-role (-> sample-anchor-with-role
                                             (g/add-role "test" "test")
                                             (g/remove-role "test"))))

      (it "removes the roles array if deleting a role causes it to be empty"
        (should= sample-anchor (g/remove-role sample-anchor-with-role
                                              role-name))))))

(run-specs)
