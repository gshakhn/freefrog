(ns freefrog.governance)

(defn anchor-circle
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