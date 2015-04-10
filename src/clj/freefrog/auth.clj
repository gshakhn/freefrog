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

(ns freefrog.auth
  (:require [clj-http.client :as http-client]
            [clojure.tools.logging :as log]
            [clj-json.core :as json]
            [clojure.walk :as walk]
            [environ.core :as env]))

(defn make-audience-url []
  (let [url (env/env :auth-audience-url)]
    (if (empty? url) "http://localhost:3000" url)))

(defn authenticate [assertion]
  (try
    (let [audience-url (make-audience-url)
          result (-> (http-client/post
                       "https://verifier.login.persona.org/verify"
                       {:form-params
                        {:assertion assertion
                         :audience  audience-url}})
                     :body
                     json/parse-string
                     walk/keywordize-keys)]
      (if (= "okay" (:status result))
        result
        (log/error (format "Authentication failed: %s (using %s)"
                           result
                           audience-url))))
    (catch Throwable t
      (log/error t "Authentication service failure"))))
