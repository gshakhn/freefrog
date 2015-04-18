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
  (:require [clojure.java.io :as io]
            [freefrog.governance :as g]
            [instaparse.core :as insta]))

(defn create-anchor-circle [_ {:keys [name cross-links]}]
  (let [anchor-circle (g/create-circle name)]
    (reduce g/add-role-to-circle
            anchor-circle
            cross-links)))

(defn create-role [circle {:keys [name purpose domains accountabilities]}]
  (g/add-role-to-circle circle name purpose domains accountabilities))

(defn create-circle [circle {:keys [name purpose domains accountabilities]}]
  (-> (g/add-role-to-circle circle name purpose domains accountabilities)
      (g/convert-to-circle name)))

(defn convert-to-pair
  "Convert the given vector into a key/value pair. The first
   value of the vector is the first value, whereas the rest
   is the second value as a vector. Unless the key is :purpose,
   in which case the value will be just the second value."
  [v]
  (if (= :purpose (first v))
    [(first v) (second v)]
    [(first v) (rest v)]))

(defn process-command
  "Execute the given governance transformation on the given
   circle, returning the new circle."
  [circle record]
  (let [function-primary (first record)
        function-secondary (first (second record))
        function-name (str (name function-primary) "-"
                           (name function-secondary))
        fn (resolve (symbol "freefrog.lang" function-name))
        entity-name (nth record 2)
        params (->> record
                    (drop 3)
                    (map convert-to-pair)
                    (into {:name entity-name}))]
    (fn circle params)))

(def parse-governance
  "Parse a governance document and produce a tree from it."
  (insta/parser (io/resource "governance.ebnf")
                :string-ci true))

(defn execute-governance
  "Take a governance string and execute the transformations
   it represents."
  ([governance-string]
   (execute-governance nil governance-string))

  ([circle governance-string]
   (let [parsed-document (-> governance-string
                             parse-governance)]
     (reduce process-command circle parsed-document))))

