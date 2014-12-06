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

;;; # Governance Encoding #
;;; Defines how governance can be manipulated, and is intended
;;; to comply to the
;;; [Holacracy Constitution v4.0](http://holacracy.org/sites/default/files/resources/holacracy_constitution_v4.0.pdf)
;;;
;;; Designed to provide all manipulation functions for Holacracy governance
;;; structures. It is not recommended that the structures
;;; generated/manipulated by this namespace be manually edited outside of
;;; this namespace for any purpose.
;;;
;;; This would cause difficulties tracking down the source of a structure
;;; should one need to do so. It also adds the benefit of having all logic in
;;; one place so one can more easily reason about it.

(ns freefrog.governance
  (:require [clojure.set :as s]
            [freefrog.governance-utils :as u]
            [freefrog.core-roles :as c]))

;; ## General purpose utility functions ##

(defn- entity-path
  [role-name entities]
  (concat [:roles role-name] entities))

(defn- role-path
  [role-name & entities]
  (entity-path role-name entities))

(defn- get-role [circle role-name]
  (get-in circle (role-path role-name)))

(defn- get-entity-raw
  [circle role-name entities]
  (get-in circle (entity-path role-name entities)))

(defn- get-entity
  "Same as get-entity-raw but nicer."
  [circle role-name & entities]
  (get-entity-raw circle role-name entities))

(defn is-circle?
  "Returns true if the given circle really is a circle. If you give it a
  role, it will tell you if the given role inside the given circle is a circle."
  ([circle] (:is-circle? circle))
  ([circle role-name] (is-circle? (get-role circle role-name))))

(defn- update-role-raw
  "Generalizes any role manipulation. The entity-path is the path to the entity
   inside the role you want to manipulate. The function given is what
   gets applied to the final entity, and the params are the arguments passed to
   that function."
  [circle role-name entities fn args]
  (apply update-in
         (concat [circle (entity-path role-name entities) fn] args)))

(defn- update-role
  "Sames as update-role-raw, but a bit easier to use. Doesn't include an
  entity."
  [circle role-name fn & args]
  (update-role-raw circle role-name nil fn args))

(defn- update-role-entity
  "Same as update-role-raw, but a bit easier to use. Includes an entity."
  [circle role-name entity-path fn & args]
  (update-role-raw circle role-name entity-path fn args))

(defn- remove-and-purge
  "Abstract function that removes a thing from a collection of things in a role
   in a circle. Doesn't do ANY validation. Removes the collection if it's
   empty. Uses the given rmfn because you could be operating on any kind of
   collection."
  [circle role-name type rmfn thing]
  (let [result (update-role-entity circle role-name [type] rmfn thing)]
    (if (empty? (get-entity result role-name type))
      (update-role result role-name dissoc type)
      result)))

(defn update-subcircle
  "Generalizes any circle manipulation to unlimited subcircles. The path given
   is a series of role names starting from (but not including) the anchor
   circle. The function is what gets applied to the final subcircle, and the
   params are the arguments passed to that function."
  [circle path fn & params]
  (let [update-args
        (concat [circle (interleave (repeat :roles) path) fn] params)]
    (apply update-in update-args)))

;; ## Validators ##

(defn- validate [valid? err-msg]
  (when-not valid? (throw (IllegalArgumentException. err-msg))))

(defn- validate-not [invalid? err-msg]
  (validate (not invalid?) err-msg))

(defn- validate-role-exists [circle role-name]
  (validate-not (empty? (get-role circle role-name))
                (str "Role not found: " role-name)))

(defn- validate-role-name [role-name]
  (validate-not (empty? role-name) "Name may not be empty"))

(defn- validate-constitutional [role-name]
  (validate-not (c/core-roles role-name)
                (format "'%s' role is defined in the Constitution."
                        role-name)))

(defn- validate-role-updates [circle role-name]
  "Checks that the role name is not empty and that it exists in the circle."
  (validate-constitutional role-name)
  (validate-role-name role-name)
  (validate-role-exists circle role-name))


;; ## Role manipulation ##

(defn convert-to-circle
  "Convert the given role into a circle. Also supports converting a role
   inside of a circle into a circle. If the role is already a circle, expect
   an exception."
  ([role]
    (validate-not (is-circle? role)
                  (format "Role '%s' is already a circle" (:name role)))
    (assoc role :is-circle? true))
  ([circle role-name]
    (validate-role-updates circle role-name)
    (update-role circle role-name convert-to-circle)))

(defn convert-to-role
  "Convert the given circle into a role. If it's already not a circle, expect
   an exception."
  [circle role-name]
  (validate-role-updates circle role-name)
  (validate (is-circle? circle role-name)
            (format "Role '%s' is not a circle" role-name))
  (validate (empty? (get-entity circle role-name :roles))
            (format "Circle %s still contains roles" role-name))
  (update-role circle role-name dissoc :is-circle?))

(defn create-circle
  "Create a new circle with no parent."
  [name]
  (validate-not (empty? name) "Name may not be empty")
  (convert-to-circle (u/make-role name)))

(defn add-role
  "Adds a role to a circle.  The role may not conflict with an existing role.
   new-role-name may not be empty."
  ([circle new-role-name]
    (add-role circle new-role-name nil nil nil))

  ([circle new-role-name purpose]
    (add-role circle new-role-name purpose nil nil))

  ([circle new-role-name purpose domains accountabilities]
    (validate-role-name new-role-name)
    (validate (is-circle? circle)
              (format "Role '%s' is not a circle." (:name circle)))
    (validate-constitutional new-role-name)
    (validate-not (get-in circle [:roles new-role-name])
                  (str "Role already exists: " new-role-name))
    (update-in circle [:roles] assoc new-role-name
               (u/make-role new-role-name purpose domains
                            accountabilities))))

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
    (update-role circle role-name dissoc :purpose)
    (update-role circle role-name assoc :purpose new-purpose)))

(def ^:private err-types {:domains          "Domain"
                          :accountabilities "Accountability"
                          :policies         "Policy"})

(defn- validate-things
  "Abstract function to validate collections of things in a circle. Checks that
   the role name is non-empty and present. Also calls the custom validation
   function.

   If the check-fn returns true, throws an error with the given
   err-msg-fmt. err-msg-fmt be in the form of ^.*%s.*%s.*%s.*$ where the
   first %s is to be replaced with a singular English representation of
   the given type (see err-types), the second %s is to be replaced with the
   thing itself and the third with the role-name.

   For example: %s '%s' already exists on role '%s'"
  [circle role-name type thing check-fn err-msg-fmt]
  (validate-role-updates circle role-name)
  (validate-not (check-fn (get-entity circle role-name type) thing)
                (format err-msg-fmt (err-types type) thing role-name)))

(defn- add-to
  "Abstract function that adds anything to a set of things in a role in a
   circle. Performs all validation and so forth. Creates the set if it doesn't
   exist."
  [circle role-name type thing]
  (validate-things circle role-name type thing contains?
                   "%s '%s' already exists on role '%s'")
  (let [things (get-entity circle role-name type)
        circle (if things
                 circle
                 (update-role circle role-name assoc type #{}))]
    (update-role-entity circle role-name [type] conj thing)))

(defn- remove-from
  "Abstract function that removes a thing from a set of things in a role in a
   circle. Performs all validation and so forth. Removes the set if it's empty."
  [circle role-name type thing]
  (validate-things circle role-name type thing (comp not contains?)
                   "%s '%s' doesn't exist on role '%s'")
  (remove-and-purge circle role-name type disj thing))

;; ## Role Collection Manipulation Functions ##
;; These functions are critical to maintaining namespace encapsulation. Simply
;; allowing an external actor to call directly into the "add-to" and
;; "remove-from" functions artificially constrains this namespace from
;; easily being able to cause these functions to differentiate themselves from
;; one another should they need to, adding a heavier burden on future
;; maintainers.

(defn add-role-domain
  "Add a domain to a role in the given circle."
  [circle role-name domain]
  (add-to circle role-name :domains domain))

(defn remove-role-domain
  "Remove a domain from a role in the given circle."
  [circle role-name domain]
  (remove-from circle role-name :domains domain))

(defn add-role-accountability
  "Add an accountability to a role in the given circle."
  [circle role-name accountability]
  (add-to circle role-name :accountabilities accountability))

(defn remove-role-accountability
  "Remove an accountability from a role in the given circle."
  [circle role-name accountability]
  (remove-from circle role-name :accountabilities accountability))

(defn add-role-policy
  "Publish a policy to grant/revoke access to a domain on the given role in
   the given circle. If you give a domain, that will be added, too."
  ([circle role-name policy-name policy-text]
    (validate-things circle role-name :policies policy-name contains?
                     "%s '%s' already exists on role '%s'")
    (update-role-entity circle role-name [:policies] assoc policy-name
                        {:name policy-name :text policy-text}))
  ([circle role-name policy-name policy-text domain]
    (validate (contains? (get-entity circle role-name :domains) domain)
              (format "Role '%s' doesn't control domain '%s'" role-name domain))
    (-> (add-role-policy circle role-name policy-name policy-text)
        (update-role-entity role-name [:policies policy-name] assoc
                            :domain domain))))

(defn remove-role-policy
  "Remove a policy from a role in the given circle."
  [circle role-name policy-name]
  (validate-things circle role-name :policies policy-name (comp not contains?)
                   "%s '%s' doesn't exist on role '%s'")
  (remove-and-purge circle role-name :policies dissoc policy-name))