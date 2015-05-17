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

(ns freefrog.lang-gen
  (:require [clj-time.format :as f]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [freefrog.governance :as g]
            [instaparse.core :as insta]))

(defn components [circle]
  (if (empty? (:purpose circle))
    "."
   (str " with purpose \"" (:purpose circle) "\".")))

(defn generate-lang [anchor-circle]
  "Generates language for a circle"
  (str "CREATE ANCHOR CIRCLE \""
       (:rname anchor-circle)
       "\""
       (components anchor-circle)))