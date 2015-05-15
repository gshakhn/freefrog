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

(defproject freefrog "0.0.1-SNAPSHOT"
  :description "freefrog"
  :url "http://www.couragelabs.com"
  :license {:name "GNU Affero General Public License"
            :url  "http://www.gnu.org/licenses/agpl-3.0.html"}

  :min-lein-version "2.3.4"

  :source-paths ["src/clj" "src/java"]

  :java-source-paths ["src/java"]

  :dependencies [[clj-http "1.1.2"]
                 [clj-json "0.5.3"]
                 [clj-time "0.9.0"]
                 [instaparse "1.4.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [speclj "3.2.0"]]

  :profiles {:uberjar {:aot :all}
             :dev {
                   :plugins [[lein-clojars "0.9.1"]]
                   :dependencies [[clj-yaml "0.4.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}
             :cli {:main freefrog.cli}}

  :plugins [[lein-ancient "0.6.7"]
            [lein-bikeshed "0.2.0"]
            [lein-kibit "0.1.2"]
            [lein-marginalia "0.8.0"]
            [speclj "3.2.0"]]

  :test-paths ["spec"]

  :aliases {"autotest" ["spec" "-a"]
            "docs" ["marg" "src" "spec"]
            "cli" ["with-profile" "cli" "run"]})
