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
  (:require [clj-time.format :as f]
            [clojure.java.io :as io]
            [freefrog.governance :as g]
            [instaparse.core :as insta]
            [clojure.tools.logging :as log]))

;;Note: this function may show up as unused in IntelliJ but it is because
;;      metaprogramming!
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

;;Note: this function may show up as unused in IntelliJ but it is because
;;      metaprogramming!
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
                         :domains          "domain"})

(defn apply-collection-update [circle name update-type op]
  (let [component (first op)
        function-name (format "%s-role-%s"
                              update-type (update-conversions component))
        fn (resolve (symbol "freefrog.governance" function-name))]
    (reduce #(fn %1 name %2) circle (rest op))))

(defn apply-collection-updates [circle name update-type ops]
  (reduce #(apply-collection-update %1 name update-type %2) circle ops))

(defn update-role [circle
                   {:keys [name rename change-purpose add remove]}]
  (-> circle
      (apply-collection-updates name "add" add)
      (apply-collection-updates name "remove" remove)
      (update-purpose-conditionally name change-purpose)
      (rename-conditionally name rename)))

(def update-circle update-role)

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

(defn execute-governance-function [record fn circle params]
  (try (fn circle params)
       (catch Exception e
         (throw
           (RuntimeException.
             (format "Unable to execute governance record: %s " record) e)))))

(defn modify-entity [circle record function-primary]
  (let [function-secondary (first (second record))
        function-name (format "%s-%s"
                              (name function-primary) (name function-secondary))
        fn (resolve (symbol "freefrog.lang" function-name))
        entity-name (nth record 2)
        params (->> record
                    (drop 3)
                    (map convert-to-pair)
                    (map array-to-map)
                    merge-array-values
                    (merge {:name entity-name}))]
    (execute-governance-function record fn circle params)))

(defn define-policy [circle record _]
  (g/add-policy circle (second record) (nth record 2)))

(defn strike-policy [circle record _]
  (g/remove-policy circle (second record)))

(def elected-role-mapping {"facilitator" g/facilitator-name
                           "secretary"   g/secretary-name})

(def formatter (f/formatter "yyyy-MM-dd"))

(defn elect [circle record _]
  (g/elect-to-role circle (elected-role-mapping (nth record 2))
                   (second record) (f/parse formatter (nth record 3))))

(def commands {:create  modify-entity
               :delete  modify-entity
               :update  modify-entity
               :convert modify-entity
               :define  define-policy
               :strike  strike-policy
               :elect   elect})

(defn process-command
  "Execute the given governance transformation on the given
   circle, returning the new circle."
  [circle record]
  (let [function-primary (first record)
        command (function-primary commands)]
    (if command
      (command circle record function-primary)
      (do
        (log/warnf "Can't handle this record yet: %s" record)
        circle))))

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
       (throw (RuntimeException. (pr-str parsed-document)))
       (reduce process-command circle parsed-document)))))

(defn execute-directory
  "Run a directory full of files as if they were governance of
   a brand new organization"
  [directory]
  (log/infof "Executing governance in directory: %s" directory)
  (->> directory
       io/file
       file-seq
       (filter #(.isFile %))
       (filter #(not (.startsWith (.getName %) ".")))
       (sort-by #(.getName %))
       (reduce (fn [circle governance-file]
                 (log/infof "Executing: %s" (.getName governance-file))
                 (execute-governance circle (slurp governance-file))) {})))

