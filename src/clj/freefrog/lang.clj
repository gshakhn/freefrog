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

(defn create-anchor-circle [_ {:keys [name cross-links purpose]}]
  (let [anchor-circle (-> name
                          g/create-circle
                          (g/update-purpose purpose))]
    (reduce g/add-role-to-circle
            anchor-circle
            cross-links)))

(defn create-role [circle {:keys [name purpose domains accountabilities]}]
  (g/add-role-to-circle circle name purpose domains accountabilities))

(defn create-circle [circle {:keys [name purpose domains accountabilities]}]
  (-> (g/add-role-to-circle circle name purpose domains accountabilities)
      (g/convert-to-circle name)))

(defn convert-role [anchor-circle {:keys [name]}]
  (g/convert-to-circle anchor-circle name))

(defn update-purpose-conditionally [anchor-circle name purpose]
  (if purpose
    (g/update-role-purpose anchor-circle name (first purpose))
    anchor-circle))

(defn rename-conditionally [anchor-circle name new-name]
  (if new-name
    (g/rename-role anchor-circle name (first new-name))
    anchor-circle))

(def update-conversions {:accountabilities "accountability"
                         :domains "domain"})

(defn apply-collection-update [circle name update-type op]
  (let [component (first op)
        function-name (str update-type "-role-" (update-conversions component))
        fn (resolve (symbol "freefrog.governance" function-name))]
    (reduce #(fn %1 name %2) circle (rest op))))

(defn apply-collection-updates [circle name update-type ops]
  (reduce #(apply-collection-update %1 name update-type %2) circle ops))

(defn update-circle [circle
                     {:keys [name rename change-purpose add remove]}]
  (-> circle
      (apply-collection-updates name "add" add)
      (apply-collection-updates name "remove" remove)
      (update-purpose-conditionally name change-purpose)
      (rename-conditionally name rename)))

(defn convert-to-pair
  "Convert the given vector into a key/value pair. The first
   value of the vector is the first value, whereas the rest
   is the second value as a vector. Unless the key is :purpose,
   in which case the value will be just the second value."
  [v]
  (if (= :purpose (first v))
    [(first v) (second v)]
    [(first v) (rest v)]))

(defn array-to-map [v] {(first v) (second v)})

(defn merge-array-values [v] (apply merge-with (concat [concat] v)))

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
                    (map array-to-map)
                    merge-array-values
                    (merge {:name entity-name}))]
    (try (fn circle params)
         (catch Exception e
           (throw (RuntimeException.
                    (str "Unable to execute governance record " record) e))))))

(def parse-governance
  "Parse a governance document and produce a tree from it."
  (insta/parser (io/resource "governance.ebnf") :string-ci true))

(defn execute-governance
  "Take a governance string and execute the transformations
   it represents."
  ([governance-string]
   (execute-governance nil governance-string))

  ([circle governance-string]
   (let [parsed-document (-> governance-string
                             parse-governance)]
     (if (insta/failure? parsed-document)
       (throw (RuntimeException.
                (with-out-str (println parsed-document))))
       (reduce process-command circle parsed-document)))))

