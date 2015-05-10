;
; Copyright © 2015 Courage Labs
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
;;; [Holacracy Constitution v4.0](http://bit.ly/holacracy-constitution-v4)
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
  (:require [clojure.set :as s]))

(def lead-link-name "Lead Link")
(def rep-link-name "Rep Link")
(def secretary-name "Secretary")
(def facilitator-name "Facilitator")

(def elected-role-mapping {facilitator-name :facilitator
                           secretary-name   :secretary})

(defn is-lead-link? [role-name]
  (= lead-link-name role-name))

(def elected-roles
  #{facilitator-name secretary-name})

(def core-roles
  (into elected-roles #{lead-link-name rep-link-name}))

(def role-assignments-domain "Role assignments within the Circle")

(def governance-records-domain
  (str "All records required of a Circle under this Constitution, and any "
       "record-keeping processes and systems required to create and "
       "maintain such records for the Circle"))

(def core-role-domains {lead-link-name role-assignments-domain
                        secretary-name governance-records-domain})

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
  ([circle entities]
   (get-in circle entities))
  ([circle role-name entities]
   (get-entity-raw circle (entity-path role-name entities))))

(defn- get-entity
  "Same as get-entity-raw but nicer."
  [circle role-name & entities]
  (get-entity-raw circle role-name entities))

(defn- role-missing? [circle role-name]
  (empty? (get-role circle role-name)))

;; ## Validators ##

(defn- validate [valid? err-msg]
  (when-not valid? (throw (IllegalArgumentException. err-msg))))

(defn- validate-not [invalid? err-msg]
  (validate (not invalid?) err-msg))

(defn- validate-role-exists [circle role-name]
  (validate-not (role-missing? circle role-name)
                (str "Role not found: " role-name)))

(defn- validate-role-name [role-name]
  (validate-not (empty? role-name) "Name may not be empty"))

(defn- validate-role-updates [circle role-name]
  "Checks that the role name is not empty and that it exists in the circle."
  (validate-role-name role-name)
  (validate-role-exists circle role-name))

;; ## Types ##

(defprotocol GovernanceRecord
  (is-circle? [record]))

(defprotocol RoleContainer
  (add-role [container role])
  (remove-role [container role-name])
  (rename-role [container role-name new-name]))

(defrecord Role [name purpose domains accountabilities policies]
  GovernanceRecord
  (is-circle? [_] false))

;;todo This re-definition of fields in Circle is ridiculous.
(defrecord Circle [name purpose domains accountabilities policies roles
                   facilitator secretary]
  GovernanceRecord
  (is-circle? [_] true)

  RoleContainer
  (add-role [circle role]
    (let [new-role-name (:name role)]
      (validate-role-name new-role-name)
      (validate-not (get-in circle [:roles new-role-name])
                    (str "Role already exists: " new-role-name))
      (update-in circle [:roles] assoc new-role-name role)))

  (remove-role [circle role-name]
    (validate-role-updates circle role-name)
    (let [result (update-in circle [:roles] dissoc role-name)]
      (if (empty? (:roles result))
        (assoc result :roles nil)
        result)))

  (rename-role [circle role-name new-name]
    (validate-role-updates circle role-name)
    (-> circle
        (update-in [:roles] s/rename-keys {role-name new-name})
        (update-in [:roles new-name] assoc :name new-name))))

(defn create-circle
  "Create a new circle with no parent."
  [circle-name]
  (validate-not (empty? circle-name) "Name may not be empty")
  (map->Circle {:name circle-name}))

(defn is-subrole-circle?
  "Returns true if the given circle really is a circle. If you give it a
  role, it will tell you if the given role inside the given circle is a circle."
  [circle role-name]
  (is-circle? (get-role circle role-name)))

(def ^:private sets-of-things {:domains          "Domain"
                               :accountabilities "Accountability"
                               :policies         "Policy"})

(defn- validate-things
  "Abstract function to validate collections of things in a circle. The
   variant that takes a role-name checks that the role name is non-empty and
   present. Also calls the custom validation function.

   If the check-fn returns true, throws an error with the given
   err-msg-fmt. err-msg-fmt be in the form of ^.*%s.*%s.*%s.*$ where the
   first %s is to be replaced with a singular English representation of
   the given component (see sets-of-things), the second %s is to be replaced
   with the thing itself and the third with the role-name.

   For example: %s '%s' already exists on role '%s'"
  ([entity component thing check-fn err-msg-fmt]
   (validate-not (check-fn (get-entity-raw entity [component]) thing)
                 (format err-msg-fmt (sets-of-things component) thing)))
  ([circle role-name component thing check-fn err-msg-fmt]
   (validate-role-updates circle role-name)
   (validate-not (check-fn (get-entity circle role-name component) thing)
                 (format err-msg-fmt (sets-of-things component) thing
                         role-name))))

;; ## Entity Generalization Functions ##

(defn- add-to-raw
  "Adds a component to a collection without any validation."
  [entity component empty-collection collection-op thing args]
  (let [things (component entity)
        entity (if things
                 entity
                 (assoc entity component empty-collection))
        add-args (into [thing] args)]
    (apply update-in entity [component] collection-op add-args)))

(defn- add-to
  "Generalizes an addition to any collection of things within an entity,
   ensuring that the given thing doesn't already exist."
  [entity component empty-collection collection-op thing & args]
  (validate-things entity component thing contains? "%s '%s' already exists")
  (add-to-raw entity component empty-collection collection-op thing args))

(defn add-policy
  "Add a policy to any entity."
  [entity policy-name policy-text]
  (add-to entity :policies {} assoc policy-name {:text policy-text}))

;; todo this looks strikingly like remove-and-purge-from-role, right? BAD.
(defn remove-policy
  "Remove a policy from an entity."
  [entity policy-name]
  (let [result (update-in entity [:policies] dissoc policy-name)]
    (if (empty? (:policies result))
      (assoc result :policies nil)
      result)))

;; ## Role Generalization Functions ##

(defn- update-role-raw
  "Generalizes any role manipulation. The entities vector is the path to the
   entity inside the role you want to manipulate. The function given is what
   gets applied to the final entity, and the params are the arguments passed to
   that function."
  [circle role-name entities function args]
  (apply update-in
         (concat [circle (entity-path role-name entities) function] args)))

(defn- update-role
  "Sames as update-role-raw, but a bit easier to use. Doesn't include an
   entity path."
  [circle role-name function & args]
  (update-role-raw circle role-name nil function args))

(defn- update-role-entity
  "Same as update-role-raw, but a bit easier to use. Includes an entity path."
  [circle role-name entity-path function & args]
  (update-role-raw circle role-name entity-path function args))

(defn- remove-and-purge-from-role
  "Abstract function that removes a thing from a collection of things in a role
   in a circle. Doesn't do ANY validation. Removes the collection if it's
   empty. Uses the given rmfn because you could be operating on any kind of
   collection."
  [circle role-name component rmfn thing]
  (let [result (update-role-entity circle role-name [component] rmfn thing)]
    (if (empty? (get-entity result role-name component))
      (update-role result role-name assoc component nil)
      result)))

(defn update-subcircle
  "Generalizes any circle manipulation to unlimited subcircles. The path given
   is a series of role names starting from (but not including) the anchor
   circle. The function is what gets applied to the final subcircle, and the
   params are the arguments passed to that function."
  [circle path function & params]
  (let [update-args
        (concat [circle (interleave (repeat :roles) path) function] params)]
    (apply update-in update-args)))

;; ## Role manipulation ##

(defn convert-to-circle
  "Convert the given role into a circle. Also supports converting a role
   inside of a circle into a circle. If the role is already a circle, expect
   an exception."
  ([role]
   (validate-not (is-circle? role)
                 (format "Role '%s' is already a circle" (:name role)))
   (map->Circle (into {} role)))
  ([circle role-name]
   (validate-role-updates circle role-name)
   (update-role circle role-name convert-to-circle)))

(defn convert-to-role
  "Convert the given circle into a role. If it's already not a circle, expect
   an exception."
  [circle role-name]
  (validate-role-updates circle role-name)
  (validate (is-subrole-circle? circle role-name)
            (format "Role '%s' is not a circle" role-name))
  (validate (empty? (get-entity circle role-name :roles))
            (format "Circle %s still contains roles" role-name))
  (-> circle
      (update-role role-name map->Role)
      (update-role role-name dissoc :roles :facilitator :secretary)))

(defn add-role-to-circle
  "Adds a role to a circle.  The role may not conflict with an existing role.
   new-role-name may not be empty."
  ([circle new-role-name]
   (add-role-to-circle circle new-role-name nil nil nil))

  ([circle new-role-name purpose]
   (add-role-to-circle circle new-role-name purpose nil nil))

  ([circle new-role-name purpose domains accountabilities]
   (add-role circle (Role. new-role-name purpose
                           (when (seq domains) (into (hash-set) domains))
                           (when (seq accountabilities)
                             (into (hash-set) accountabilities))
                           nil))))

(defn update-purpose [entity new-purpose]
  (if (empty? new-purpose)
    (assoc entity :purpose nil)
    (assoc entity :purpose new-purpose)))

(defn update-role-purpose
  "Update the purpose of a role in the given circle."
  [circle role-name new-purpose]
  (validate-role-updates circle role-name)
  (update-role circle role-name update-purpose new-purpose))

(defn- add-role-if-missing [circle role-name]
  (if (and (core-roles role-name)
           (role-missing? circle role-name))
    (add-role circle (map->Role {:name role-name}))
    circle))

(defn- add-to-role
  "Very abstract function that adds things to things in a role, making sure
   that if a core role is being manipulated, it is made to be present.
   Performs collection-op on the collection, or the given empty-collection
   if it doesn't exist."
  [circle role-name component empty-collection collection-op thing & args]
  (let [circle (add-role-if-missing circle role-name)]
    (validate-things circle role-name component thing contains?
                     "%s '%s' already exists on role '%s'")
    (update-in circle (role-path role-name) add-to-raw
               component empty-collection collection-op thing args)))

(defn- remove-from-role
  "Removes a thing from a collection of things in a role, making sure that
   if a core role is being manipulated, and becomes empty, it gets removed
   altogether. Performs collection-op on the collection."
  [circle role-name component collection-op thing]
  (validate-things circle role-name component thing (comp not contains?)
                   "%s '%s' doesn't exist on role '%s'")
  (let [result
        (remove-and-purge-from-role circle role-name component collection-op
                                    thing)]
    (if (and (core-roles role-name)
             (every? empty? (map (partial get-entity result role-name)
                                 [:domains :accountabilities :policies])))
      (remove-role result role-name)
      result)))

(defn- add-to-set-in-role
  "Abstract function that adds anything to which-set-of-things in a role in a
   circle. Performs all validation and so forth. Creates the set if it doesn't
   exist. The sets that can be manipulated are defined in sets-of-things."
  [circle role-name which-set-of-things thing]
  (validate-not (is-lead-link? role-name)
                (format "May not add %s to '%s'"
                        (sets-of-things which-set-of-things) role-name))
  (add-to-role circle role-name which-set-of-things #{} conj thing))

(defn- remove-from-set-in-role
  "Abstract function that removes a thing from a set-of-things in a role in a
   circle. Performs all validation and so forth. Removes the set if it's empty."
  [circle role-name set-of-things thing]
  (remove-from-role circle role-name set-of-things disj thing))

(defn elect-to-role [circle role-name person-name expiration-date]
  (validate (core-roles role-name)
            (format "'%s' is not an elected role." role-name))
  (assoc circle (elected-role-mapping role-name)
         {:name            person-name
          :expiration-date expiration-date}))

;; ## Role Collection Manipulation Functions ##
;; These functions are critical to maintaining namespace encapsulation. Simply
;; allowing an external actor to call directly into the "add-to-set" and
;; "remove-from-set" functions artificially constrains this namespace from
;; easily being able to cause these functions to differentiate themselves from
;; one another should they need to, adding a heavier burden on future
;; maintainers.

(defn add-role-domain
  "Add a domain to a role in the given circle."
  [circle role-name domain]
  (add-to-set-in-role circle role-name :domains domain))

(defn remove-role-domain
  "Remove a domain from a role in the given circle."
  [circle role-name domain]
  (remove-from-set-in-role circle role-name :domains domain))

(defn add-role-accountability
  "Add an accountability to a role in the given circle."
  [circle role-name accountability]
  (add-to-set-in-role circle role-name :accountabilities accountability))

(defn remove-role-accountability
  "Remove an accountability from a role in the given circle."
  [circle role-name accountability]
  (remove-from-set-in-role circle role-name :accountabilities accountability))

(defn add-role-policy
  "Publish a policy to grant/revoke access to a domain on the given role in
   the given circle. If you give a domain, that will be added, too."
  ([circle role-name policy-name policy-text]
   (add-to-role circle role-name :policies {} assoc policy-name
                {:text policy-text}))
  ([circle role-name policy-name policy-text domain]
   (let [with-added-policy (add-role-policy circle role-name policy-name
                                            policy-text)]
     (validate (or (= (get core-role-domains role-name) domain)
                   (contains? (get-entity circle role-name :domains)
                              domain))
               (format "Role '%s' doesn't control domain '%s'" role-name
                       domain))
     (update-role-entity with-added-policy role-name [:policies policy-name]
                         assoc :domain domain))))

(defn remove-role-policy
  "Remove a policy from a role in the given circle."
  [circle role-name policy-name]
  (remove-from-role circle role-name :policies dissoc policy-name))