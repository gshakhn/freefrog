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

(ns freefrog.cli
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [freefrog.lang :as l])
  (:gen-class))

(defn -main [& args]
  (if (= 1 (count args))
    (pp/pprint (->> args
                    first
                    io/file
                    file-seq
                    (filter #(.isFile %))
                    (map slurp)
                    (reduce l/execute-governance {})))
    (println "Please specify a directory full of governance files.")))