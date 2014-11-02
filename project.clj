(defproject freefrog "0.0.1-SNAPSHOT"
  :description "freefrog"
  :url "http://www.couragelabs.com"
  :license {:name "GNU Affero General Public License"
            :url  "http://www.gnu.org/licenses/agpl-3.0.html"}

  :min-lein-version "2.3.4"

  :source-paths ["src/clj"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [liberator "0.12.2"]
                 [compojure "1.2.1"]
                 [ring/ring-core "1.3.1"]
                 [org.clojure/core.typed "0.2.72"]
                 [com.velisco/tagged "0.3.4"]
                 [clj-jgit "0.8.0"]
                 [speclj "3.1.0"]]

  :profiles {:dev {:dependencies [[clj-http "1.0.1"]] }}

  :ring {:handler freefrog.rest/handler :reload-paths ["src"]}

  :plugins [[speclj "3.1.0"]
            [lein-ring "0.8.13"]
            [lein-marginalia "0.8.0"]
            [lein-ancient "0.5.5"]
            [lein-kibit "0.0.8"]]

  :test-paths ["spec"]

  :aliases {"autotest" ["spec" "-a"]
            "docs" ["marg" "src" "spec"]})
