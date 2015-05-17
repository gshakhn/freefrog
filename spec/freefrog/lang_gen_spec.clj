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

(describe "Generating governance documents"
  (it "should be able to generate a document for a new anchor circle without crosslinks"
      (let [anchor-circle  (g/create-circle "Courage Labs")]
        (should= (l/execute-governance (lg/generate-lang anchor-circle) )
                 anchor-circle)))

  (it "should be able to generate a document for another anchor circle without crosslinks"
      (let [anchor-circle  (g/create-circle "Courage Labs 2")]
        (should= (l/execute-governance (lg/generate-lang anchor-circle) )
                 anchor-circle))))
