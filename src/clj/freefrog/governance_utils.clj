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
