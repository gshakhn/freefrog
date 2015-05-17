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

(ns freefrog.lang-gen-spec
  (:require [clj-yaml.core :as yaml]
            [clj-time.core :as t]
            [freefrog.governance :as g]
            [freefrog.lang :as l]
            [freefrog.lang-gen :as lg]
            [speclj.core :refer :all]))

;; Monkey patch clj-yaml to do nice encoding of dates and defrecords
;(ns clj-yaml.core
;  (:require [clj-time.format :as f]))

;(defn encode-without-nils [data]
;  (encode (into {} (remove (comp nil? second) data))))

;(extend-protocol YAMLCodec
;  freefrog.governance.Role
;  (encode [data] (encode-without-nils data))
;
;  freefrog.governance.Circle
;  (encode [data] (encode-without-nils data))
;
;  org.joda.time.DateTime
;  (encode [data]
;    (f/unparse (f/formatters :date) data)))

(ns freefrog.lang-gen-spec)

(defn run-test [test-name]
  (let [circle
        (l/execute-directory
          (str "spec/freefrog/lang_gen/" test-name "/source"))
        generated
        (lg/generate-lang circle)]
    (should= (slurp (str "spec/freefrog/lang_gen/" test-name "/expected_result.txt"))
             (lg/generate-lang circle))
    (should= circle
             (l/execute-governance generated))))

(describe "Generating governance documents"
  (it "should be able to generate for an anchor circle"
    (run-test "base_circle"))

  (it "should be able to generate for an anchor circle with a purpose"
    (run-test "base_circle_with_purpose")))
