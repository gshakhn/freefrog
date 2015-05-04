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

  :dependencies [[clj-http "1.1.1"]
                 [clj-json "0.5.3"]
                 [clj-time "0.9.0"]
                 [environ "1.0.0"]
                 [http-kit "2.1.19"]
                 [instaparse "1.3.6"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [metosin/ring-swagger "0.20.2"]
                 [metosin/compojure-api "0.20.1"]
                 [metosin/ring-http-response "0.6.1"]
                 [metosin/ring-swagger-ui "2.0.24"]
                 [speclj "3.2.0"]]

  :main freefrog.rest

  :profiles {:freefrog {:ring {:handler freefrog.rest/app
                              :reload-paths ["src"]}
                       :main freefrog.rest
                       :dependencies [[metosin/ring-swagger-ui "2.0.24"]]}
             :uberjar {:aot :all}
             :dev {:ring {:handler freefrog.rest/app}
                   :plugins [[lein-clojars "0.9.1"]
                             [lein-ring "0.9.3"]]
                   :dependencies [[javax.servlet/servlet-api "2.5"]
                                  [peridot "0.3.1"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0-alpha4"]]}
             :cli {:main freefrog.cli}}

  :plugins [[lein-ancient "0.6.7"]
            [lein-kibit "0.1.2"]
            [lein-marginalia "0.8.0"]
            [speclj "3.1.0"]]

  :test-paths ["spec"]

  :aliases {"autotest" ["spec" "-a"]
            "docs" ["marg" "src" "spec"]
            "freefrog" ["with-profile" "freefrog" "run"]
            "cli" ["with-profile" "cli" "run"]})
