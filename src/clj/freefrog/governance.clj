;;; # Governance Encoding #
;;; This namespace defines how governance can be manipulated, and is intended
;;; to comply to the
;;; [Holacracy Constitution v4.0](http://holacracy.org/sites/default/files/resources/holacracy_constitution_v4.0.pdf)
(ns freefrog.governance
  (:require [clojure.set :as s]))

(defn- validate-not
  "Throws exception with given err-msg if invalid? is true."
  [invalid? err-msg]
  (when invalid? (throw (IllegalArgumentException. err-msg))))

(defn anchor-circle
  "Create a new anchor circle.  If given lead-link-* parameters, will
   assign a lead link."
  ([name]
   (validate-not (empty? name) "Name may not be empty")
   {:name name})

  ([name lead-link-name lead-link-email]
   (validate-not (or (empty? name) (empty? lead-link-name)
                      (empty? lead-link-email)) "No parameters may be empty")
   (let [anchor-circle (anchor-circle name)]
     (assoc anchor-circle :lead-link
            {:name lead-link-name :email lead-link-email}))))

(defn- assoc-if [map key value]
  "Associate a value with a key only if the value is non-nil."
  (if value (assoc map key value) map))

(defn- make-role [purpose domains accountabilities]
  (-> {}
      (assoc-if :purpose purpose)
      (assoc-if :domains domains)
      (assoc-if :accountabilities accountabilities)))

(defn- validate-role-name [role-name]
  (validate-not (empty? role-name) "Name may not be empty"))

(defn add-role
  "Adds a role to a circle.  The role may not conflict with an existing role.
   role-name may not be empty."
  ([circle role-name purpose]
   (add-role circle role-name purpose nil nil))
  ([circle role-name purpose domains accountabilities]
   (validate-role-name role-name)
   (validate-not (get-in circle [:roles role-name])
                  (str "Role already exists: " role-name))
   (let [circle (if (contains? circle :roles)
                  circle
                  (assoc circle :roles {}))]
     (update-in circle [:roles] assoc role-name
                (make-role purpose domains accountabilities)))))

(defn- validate-role-exists [circle role-name]
  (validate-not (empty? (get-in circle [:roles role-name]))
                 (str "Role not found: " role-name)))

(defn- validate-role-updates [circle role-name]
  "Checks that the role name is not empty and that it exists in the circle."
  (validate-role-name role-name)
  (validate-role-exists circle role-name))

(defn remove-role
  "Remove a role from a circle."
  [circle role-name]
  (validate-role-updates circle role-name)
  (let [result (update-in circle [:roles] dissoc role-name)]
    (if (empty? (:roles result))
      (dissoc result :roles)
      result)))

(defn rename-role
  "Rename a role in the given circle."
  [circle role-name new-name]
  (validate-role-updates circle role-name)
  (update-in circle [:roles]
             s/rename-keys {role-name new-name}))

(defn update-role-purpose
  "Update the purpose of a role in the given circle."
  [circle role-name new-purpose]
  (validate-role-updates circle role-name)
  (if (empty? new-purpose)
    (update-in circle [:roles role-name]
               dissoc :purpose)
    (update-in circle [:roles role-name]
               assoc :purpose new-purpose)))

(defn- get-domains [circle role-name]
  (get-in circle [:roles role-name :domains]))

(defn- validate-domain [circle role-name domain check-fn err-msg-fmt]
  "Validate everything about domain manipulation. Checks that the role name
   is non-empty and present. Also calls the custom validation function.

   If the check-fn returns true, throws an error with the given
   err-msg-fmt. err-msg-fmt be in the form of ^.*?%s.*?%s.*$ where the
   first %s is to be replaced with the domain and the second with the
   role-name."
  (validate-role-updates circle role-name)
  (validate-not (check-fn (get-domains circle role-name) domain)
                 (format err-msg-fmt domain role-name)))

(defn add-domain
  "Add a domain to a role in the given circle."
  [circle role-name domain]
  (validate-domain circle role-name domain contains?
                   "Domain '%s' already exists on role '%s'")
  (let [domains (get-domains circle role-name)
        circle (if domains
                 circle
                 (update-in circle [:roles role-name] assoc :domains #{}))]
    (update-in circle [:roles role-name :domains] conj domain)))

(defn remove-domain
  "Remove a domain from a role in the given circle."
  [circle role-name domain]
  (validate-domain circle role-name domain (comp not contains?)
                   "Domain '%s' doesn't exist on role '%s'")
  (let [result (update-in circle [:roles role-name :domains] disj domain)]
    (if (empty? (get-domains result role-name))
      (update-in result [:roles role-name] dissoc :domains) result)))

(defn add-accountability
  "Add an accountability to a role in the given circle."
  [circle role-name accountability]
  (validate-role-updates circle role-name)
  (let [accountabilities (get-in circle [:roles role-name :accountabilities])
        circle (if accountabilities
                 circle
                 (update-in circle [:roles role-name] assoc
                            :accountabilities []))]
    (update-in circle [:roles role-name :accountabilities]
               conj accountability)))