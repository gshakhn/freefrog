(ns freefrog.governance)

(defn anchor-circle
  "Create a new anchor circle.  If given lead-link-* parameters, will
   assign a lead link."
  ([name]
   (if (empty? name)
     (throw (IllegalArgumentException. "Name may not be empty")))
   {:name name})

  ([name lead-link-name lead-link-email]
   (if (or (empty? name) (empty? lead-link-name) (empty? lead-link-email))
     (throw (IllegalArgumentException. "No parameters may be empty")))
   (let [anchor-circle (anchor-circle name)]
     (assoc anchor-circle :lead-link
            {:name lead-link-name :email lead-link-email}))))

(defn- add-if [map key value]
  (if value
    (assoc map key value)
    map))

(defn- make-role [purpose domains accountabilities]
  (-> {}
      (add-if :purpose purpose)
      (add-if :domains domains)
      (add-if :accountabilities accountabilities)))

(defn add-role
  "Adds a role to a circle.  The role may not conflict with an existing role.
   role-name may not be empty."
  ([circle role-name purpose]
   (add-role circle role-name purpose nil nil))
  ([circle role-name purpose domains accountabilities]
   (if (empty? role-name)
     (throw (IllegalArgumentException. "Name may not be empty")))
   (if (get-in circle [:roles role-name])
     (throw (IllegalArgumentException. (str "Role already exists: " role-name))))
   (let [circle (if (contains? circle :roles)
                  circle
                  (assoc circle :roles {}))]
     (update-in circle [:roles] assoc role-name
                (make-role purpose domains accountabilities)))))