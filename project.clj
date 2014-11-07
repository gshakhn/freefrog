;
; Copyright © 2014 Courage Labs
;
; This file is part of Freefrog.
;
; Harthe is free software: you can redistribute it and/or modify
; it under the terms of the GNU Affero General Public License as published by
; the Free Software Foundation, either version 3 of the License, or
; (at your option) any later version.
;
; Harthe is distributed in the hope that it will be useful,
; but WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
; GNU Affero General Public License for more details.
;
; You should have received a copy of the GNU Affero General Public License
; along with this program.  If not, see <http://www.gnu.org/licenses/>.
;

(defproject freefrog "0.0.1-SNAPSHOT"
  :description "freefrog"
  :url "http://www.couragelabs.com"
  :license {:name "GNU Affero General Public License"
            :url  "http://www.gnu.org/licenses/agpl-3.0.html"}

  :min-lein-version "2.3.4"

  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.velisco/tagged "0.3.4"]
                 [clj-jgit "0.8.0"]
                 [speclj "3.1.0"]]

  :plugins [[speclj "3.1.0"]
            [lein-marginalia "0.8.0"]
            [lein-ancient "0.5.5"]
            [lein-kibit "0.0.8"]]

  :test-paths ["spec"]

  :aliases {"autotest" ["spec" "-a"]
            "docs" ["marg" "src" "spec"]})
