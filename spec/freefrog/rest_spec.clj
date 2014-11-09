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
            [clj-json.core :as json]
            [clj-http.client :as http-client]
            [freefrog.rest :as r])
  (:use [ring.util.codec :only [url-encode]]))

(defn http-post-request 
  ([uri body]
   (http-post-request uri body nil))
  ([uri body options]
   (http-client/post (str "http://localhost:3000/" uri) 
                     (merge {:throw-exceptions false
                             :content-type :json
                             :body body} options))))

(defn http-get-request 
  ([]
   (http-get-request "" nil))
  ([uri]
   (http-get-request uri nil))
  ([uri options]
   (http-client/get (str "http://localhost:3000/" uri) (merge {:throw-exceptions false} options))))

(defn get-location [response]
  (get (:headers response) "Location"))

(defn should-return-4xx [spec-context uri-fn request-content 
                         response-code response-content]
  (context spec-context
    (with response (http-post-request (uri-fn) request-content))
    (it "should return a bad request"
      (should= response-code (:status @response))
      (should-contain response-content (:body @response)))))

(describe "governance rest api"
  (before-all (r/start-test-server))
  (after-all (r/stop-test-server))
  (after (r/reset-database))

  (context "URIs"
    (it "should allow processing of the anchor circle"
      (should-not= 422 (:status (http-get-request "/"))))

    (it "should allow processing of the anchor circle's roles"
      (should-not= 422 (:status (http-get-request "/roles"))))

    (it "should allow processing of nested circles"
      (should-not= 422 (:status (http-get-request "/Circle1/SubCircle"))))

    (it "should allow processing of a circle's roles"
      (should-not= 422 (:status (http-get-request "/Circle1/roles"))))

    (it "should allow processing of a circle's particular roles"
      (should-not= 422 (:status (http-get-request "/Circle1/roles/MyRole"))))

    (it "should not allow processing of a circle named 'roles'"
      (should= 422 (:status (http-get-request "/Circle1/roles/SubCircle/MyRole"))))

    (it "should not allow processing of the roles of a circle named 'roles' "))

  (context "circles"
    (context "when no circles have been created"
      (context "requesting the root resource"
        (with response (http-get-request "/"))
        (it "should return an empty array"
          (should= 200 (:status @response))
          (should= "{}" (:body @response))))

      (context "requesting the circles resource"
        (with response (http-get-request "circles"))
        (it "should return an empty array"
          (should= 200 (:status @response))
          (should= "[]" (:body @response)))))

    (context "when creating a circle"
      (context "with valid parameters"
        (with response (http-post-request "circles" 
                                          (json/generate-string 
                                            {:name "Test Circle!"
                                             :lead-link-name "Bill"
                                             :lead-link-email "bfinn@example.com"})))
        (it "should return the location of the newly created resource"
          (should= 201 (:status @response))
          (should= (str "/circles/" (url-encode "Test Circle!")) (get-location @response))))

      (should-return-4xx "with invalid paramaters" 
                         (fn [] "circles")
                         (json/generate-string {:foo "Test Circle!"})
                         400
                         "IllegalArgumentException")
      (should-return-4xx "with missing paramaters" 
                         (fn [] "circles")
                         (json/generate-string {:name "Test Circle!"})
                         400
                         "IllegalArgumentException")
      (should-return-4xx "with malformed JSON" 
                         (fn [] "circles")
                         "{\"name\" :: \"Bill\""
                         400
                         "IOException"))

    (context "with a created circle"
      (with location (get-location (http-post-request
                                     "circles" 
                                     (json/generate-string 
                                       {:name "Test Circle!"
                                        :lead-link-name "Bill"
                                        :lead-link-email "bfinn@example.com"}))))
      (context "and its specified location"
        (with get-response (http-get-request @location))
        (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= {"lead-link" {"email" "bfinn@example.com"
                                 "name" "Bill" } "name" "Test Circle!"} 
                   (json/parse-string (:body @get-response)))))

      (context "and its implicit location"
        (with get-response (http-get-request (url-encode "Test Circle!")))
        (it "should return the content that was created" 
          (should-not= nil @location)
          (should= 200 (:status @get-response))
          (should= {"lead-link" {"email" "bfinn@example.com"
                                 "name" "Bill" } "name" "Test Circle!"} 
                   (json/parse-string (:body @get-response))))))

    (context "when creating multiple circles"
      (before (http-post-request "circles" 
                                 (json/generate-string 
                                   {:name "Test Circle!"
                                    :lead-link-name "Bill"
                                    :lead-link-email "bfinn@example.com"})
                                 {:throw-exceptions true}))

      (before (http-post-request "circles" 
                                 (json/generate-string 
                                   {:name "Test Circle 2"
                                    :lead-link-name "Bill"
                                    :lead-link-email "bfinn@example.com"})
                                 {:throw-exceptions true}))

      (with get-response (http-get-request "circles"))

      (it "should return an array of created circles"
        (should= 200 (:status @get-response))
        (should-contain "application/json" (get-in @get-response 
                                                   [:headers "Content-Type"]))
        (should= 2 (count (json/parse-string (:body @get-response)))))))

  (context "roles"
    (context "when requesting the list of roles for a non-existent circle"
      (with roles-response (http-get-request "circles/New%20Circle/roles"))
      (it "should 404"
        (should= 404 (:status @roles-response))))

    (context "when posting a new role to a non-existent circle"
      (with roles-response (http-post-request 
                             "circles/New%20Circle/roles"
                             (json/generate-string {:name "My Role!"})))
      (it "should 422 with a helpful error message"
        (should= 404 (:status @roles-response))
        (should-contain "Circle 'New Circle' does not exist." (:body @roles-response))))

    (context "with a circle"
      (with circle-location (get-location (http-post-request
                                            "circles" 
                                            (json/generate-string 
                                              {:name "Test Circle!"
                                               :lead-link-name "Bill"
                                               :lead-link-email "bfinn@example.com"}))))

      (context "when requesting the list of roles"
        (with roles-response (http-get-request (str @circle-location "/roles")))

        (it "should return an empty array"
          (should= 200 (:status @roles-response))
          (should= "[]" (:body @roles-response))))

      (context "when creating a role"
        (context "with valid parameters"
          (with roles-response (http-post-request
                                 (str @circle-location "/roles")
                                 (json/generate-string {:name "My Role!"})))

          (it "should return the location of the newly created resource"
            (should= 201 (:status @roles-response))
            (should= (str "/circles/" (url-encode "Test Circle!") "/roles/" 
                          (url-encode "My Role!"))
                     (get-location @roles-response))))

        (should-return-4xx "with invalid paramaters" 
                           #(str @circle-location "/roles")
                           (json/generate-string {:foo "Test Role"})
                           400
                           "IllegalArgumentException")
        (should-return-4xx "with missing paramaters" 
                           #(str @circle-location "/roles")
                           (json/generate-string {})
                           400
                           "IllegalArgumentException")
        (should-return-4xx "with malformed JSON" 
                           #(str @circle-location "/roles")
                           "{\"name\" :: \"Bill\""
                           400
                           "IOException"))

      (context "with a created role and its location"
        (with role-location (get-location (http-post-request
                                            (str @circle-location "/roles")
                                            (json/generate-string 
                                              {:name "Test Role"})
                                            {:throw-exceptions true} )))
        (with get-response (http-get-request @role-location))
        (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= "{}" (:body @get-response))))

      (context "with a created role with accountabilities and its location"
        (with role-location (get-location (http-post-request
                                            (str @circle-location "/roles")
                                            (json/generate-string 
                                              {:name "Test Role"
                                               :purpose "End world hunger"})
                                            {:throw-exceptions true} )))
        (with get-response (http-get-request @role-location))
        (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= (json/generate-string {:purpose "End world hunger"})
                   (:body @get-response)))))))
