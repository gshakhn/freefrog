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

(ns freefrog.lang
  (:require [clj-yaml.core :as yaml]
            [clojure.pprint :as pp]
            [clojure.walk :as walk]
            [freefrog.governance :as g]))

(defn create-anchor-circle [{:keys [name cross-links]}]
  (let [anchor-circle (g/create-circle name)]
    (reduce g/add-role-to-circle
            anchor-circle
            cross-links)))

(defn create-role [circle {:keys [name purpose domains accountabilities]}]
  (g/add-role-to-circle circle name purpose domains accountabilities))

(defn create-circle [circle {:keys [name purpose domains accountabilities]}]
  (-> (g/add-role-to-circle circle name purpose domains accountabilities)
      (g/convert-to-circle name)))

(def verbs {"Create Anchor Circle" create-anchor-circle
            "Create Role"          create-role
            "Create Circle"        create-circle})

(defn process-command
  ([[command args]]
   (let [fn-to-call (get verbs command)]
     (fn-to-call (walk/keywordize-keys args))))
  ([circle [command args]]
   (try
     (let [fn-to-call (get verbs command)]
       (fn-to-call circle (walk/keywordize-keys args)))
     (catch Exception e
       (printf "Unable to process command %s / %s%n" command args)
       (throw e)))))

(defn execute-governance
  ([governance-string]
   (let [parsed-document (-> governance-string
                             (yaml/parse-string)
                             (walk/stringify-keys))]
     (process-command (first parsed-document))))
  ([circle governance-string]
   (let [parsed-document (-> governance-string
                             (yaml/parse-string)
                             (walk/stringify-keys))]
     (reduce process-command circle parsed-document))))