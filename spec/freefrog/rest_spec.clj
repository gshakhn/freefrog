;
; Copyright © 2014 Courage Labs
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

(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
            [clj-http.client :as http-client]
            [freefrog.rest :as r]
            [freefrog.persistence :as p])
  (:import [freefrog MissingEntityException])
  (:use [ring.adapter.jetty]))

(def host-url (format "http://localhost:%d/api" r/port))

(def http-request-fns
  {:get  http-client/get
   :put  http-client/put
   :post http-client/post})

(defn http-request
  ([method uri]
    (http-request method uri nil))
  ([method uri options]
    (apply (get http-request-fns method)
           [(str host-url uri)
            (merge {:throw-exceptions false
                    :content-type     "text/plain"
                    :body             ""} options)])))

(defn throw-circle-not-found [& args]
  (throw (MissingEntityException. "Circle does not exist")))

(defmacro it-responds-with-content-type [expected-content-type response]
  `(it "should return the right content type"
     (should= ~expected-content-type
       (get-in ~response [:headers "Content-Type"]))))

(defmacro it-responds-with-status [expected-status response]
  `(it "should return the right response code"
     (should= ~expected-status (:status ~response))))

(defmacro it-responds-with-body [expected-body response]
  `(it "should contain the appropriate body"
     (should= ~expected-body (:body ~response))))

(defmacro it-responds-with-body-containing [expected-body response]
  `(it "should contain the appropriate body"
     (should-contain ~expected-body (:body ~response))))

(describe "governance rest api"
  (with-all stop-server-fn (r/start-server))
  (before-all @stop-server-fn)
  (after-all (@stop-server-fn))

  (with circle-response (http-request :get "/anchor/dev/qa"
                                      {:accept "application/json"}))
  (with gov-response (http-request :get "/anchor/dev/qa/_governance"
                                   {:accept "application/json"}))

  (context "circle/role"
    (context "json"
      (it-responds-with-status 200 @circle-response)
      (it-responds-with-body "\"You requested circle/role: anchor/dev/qa\""
        @circle-response)
      (it-responds-with-content-type "application/json; charset=utf-8"
        @circle-response)))

  (context "governance"
    (context "getting a non-existent circle/role"
      (around [it]
        (with-redefs [p/get-all-governance-logs throw-circle-not-found] (it)))
      (it-responds-with-status 404 @gov-response))

    (context "json"
      (it-responds-with-status 200 @gov-response)
      (it-responds-with-body
        "[]"
        @gov-response)
      (it-responds-with-content-type "application/json; charset=utf-8"
        @gov-response))))

(run-specs)
