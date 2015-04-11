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

(def verbs {"Create Anchor Circle" create-anchor-circle})

(defn process-command [[command args]]
  (let [fn-to-call (get verbs command)]
    (fn-to-call (walk/keywordize-keys args))))

(defn execute-governance [governance-string]
  (let [parsed-document (-> governance-string
                   (yaml/parse-string)
                   (walk/stringify-keys))]
    (map process-command parsed-document)))