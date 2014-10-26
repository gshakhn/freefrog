(defproject freefrog "0.0.1-SNAPSHOT"
  :description "freefrog"
  :url "http://www.couragelabs.com"
  :license {:name "GNU Affero General Public License"
            :url  "http://www.gnu.org/licenses/agpl-3.0.html"}

  :min-lein-version "2.3.4"

  :source-paths ["src/clj" "src/cljs"]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2234"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [om "0.5.0"]
                 [com.facebook/react "0.9.0"]
                 [com.velisco/tagged "0.3.4"]
                 [clj-jgit "0.8.0"]
                 [speclj "3.1.0"]]
  
  :plugins [[lein-cljsbuild "1.0.3"]
            [speclj "3.1.0"]]

  :test-paths ["spec"]

  :hooks [leiningen.cljsbuild]

  :aliases {"autotest" ["with-profile" "tdd" "spec" "-a"]
            "spec" ["with-profile" "tdd" "spec"]}

  :cljsbuild
  {:builds {:freefrog
            {:source-paths ["src/cljs"]
             :compiler
             {:output-to "dev-resources/public/js/freefrog.js"
              :optimizations :advanced
              :pretty-print false}}}})
