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
