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
            [freefrog.auth :as auth]
            [freefrog.rest :as r]
            [freefrog.persistence :as p]
            [clj-json.core :as json]
            [clojure.walk :as walk]
            [clj-http.cookies :as cookies])
  (:import [freefrog MissingEntityException])
  (:use [ring.adapter.jetty]))

(def host-url (format "http://localhost:%d/api" r/port))

(defn circle-api [path] (format "/circles/%s" path))
(def session-api "/session")

(def http-request-fns
  {:get    http-client/get
   :put    http-client/put
   :post   http-client/post
   :delete http-client/delete})

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

(defmacro it-responds-with-json [expected-body response]
  `(it "should contain the appropriate json body"
     (should= ~expected-body (walk/keywordize-keys
                               (json/parse-string (:body ~response))))))

(def principal1 "steve@example.com" )
(def principal2 "bill@example.com")

(def principals-map {"good1" {:email principal1}
                     "good2" {:email principal2}})

(defn json-post-body [cookies body]
  {:body         (json/generate-string body)
   :cookie-store cookies
   :content-type "application/json"})

(describe "rest api"
  (with-all stop-server-fn (r/start-server))
  (before-all @stop-server-fn)
  (after-all (@stop-server-fn))

  (context "governance"
    (with circle-response (http-request :get (circle-api "anchor/dev/qa")
                                        {:accept "application/json"}))
    (with gov-response (http-request :get
                                     (circle-api "anchor/dev/qa/_governance")
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

  (context "session"
    (context "posting to the session"
      (with-all cookie-store (cookies/cookie-store))

      (with get-response (http-request :get session-api
                                       {:cookie-store @cookie-store}))

      (with post-response1 (http-request :post session-api
                                         (json-post-body @cookie-store
                                                         {:assertion "good1"})))

      (with post-response2 (http-request :post session-api
                                         (json-post-body @cookie-store
                                                         {:assertion "good2"})))

      (with post-response3 (http-request :post session-api
                                         (json-post-body @cookie-store
                                                         {:assertion "bad"})))

      (with delete-response (http-request :delete session-api
                                          {:cookie-store @cookie-store}))

      (around [it]
        (with-redefs [auth/authenticate (fn [token]
                                          (principals-map token))] (it)))

      (context "simple login"
        (it-responds-with-status 404 @get-response)

        (it-responds-with-status 200 @post-response1)
        (it-responds-with-json principal1
                               @post-response1)
        (it-responds-with-json principal1
                               @get-response)
        (it-responds-with-status 200 @get-response))

      (context "second login"
        (it-responds-with-status 200 @post-response2)
        (it-responds-with-json principal2
                               @post-response2)
        (it-responds-with-json principal2
                               @get-response)
        (it-responds-with-status 200 @get-response))

      (context "logout"
        (it-responds-with-status 200 @delete-response)
        (it-responds-with-status 404 @get-response))

      (context "bad login"
        (it-responds-with-status 403 @post-response3)))

    (context "deleting a session")))

(run-specs)
