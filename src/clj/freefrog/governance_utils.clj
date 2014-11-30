(ns freefrog.governance-utils)

(defn- assoc-if [map key value]
  "Associate a value with a key only if the value is non-nil."
  (if value (assoc map key value) map))

(defn make-role
  "Make a role with the given name, purpose, domains and accountabilities.
   Any of these items can be nil or empty, and they won't be added to the role.
   This particular function doesn't validate anything, so be careful to
   validate before using it!"
  ([role-name]
    {:name role-name})
  ([role-name purpose domains accountabilities]
    (-> (make-role role-name)
        (assoc-if :purpose purpose)
        (assoc-if :domains domains)
        (assoc-if :accountabilities accountabilities))))
