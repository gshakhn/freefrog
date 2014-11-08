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

  (it "should return status code 404 at the root"
    (should= 404 (:status (http-get-request))))

  (context "circles"
    (context "when no circles have been created"
      (with response (http-get-request "c"))
      (it "should return an empty array"
        (should= 200 (:status @response))
        (should= "[]" (:body @response))))

    (context "when creating a circle"
      (context "with valid parameters"
        (with response (http-post-request "c" 
                                          (json/generate-string 
                                            {:name "Test Circle!"
                                             :lead-link-name "Bill"
                                             :lead-link-email "bfinn@example.com"})))
        (it "should redirect to the location of the newly created resource"
          (should= 303 (:status @response))
          (should-contain "application/json" (get-in @response [:headers "Content-Type"]))
          (should= (str "/c/" (url-encode "Test Circle!")) (get (json/parse-string (:body @response)) 
                                                  "location"))))

      (should-return-4xx "with invalid paramaters" 
                         (fn [] "c")
                         (json/generate-string {:foo "Test Circle!"})
                         422
                         "IllegalArgumentException")
      (should-return-4xx "with missing paramaters" 
                         (fn [] "c")
                         (json/generate-string {:name "Test Circle!"})
                         422
                         "IllegalArgumentException")
      (should-return-4xx "with malformed JSON" 
                         (fn [] "c")
                         "{\"name\" :: \"Bill\""
                         400
                         "IOException"))

    (context "with a created circle and its location"
      (with location (get (json/parse-string 
                            (:body (http-post-request
                                     "c" 
                                     (json/generate-string 
                                       {:name "Test Circle!"
                                        :lead-link-name "Bill"
                                        :lead-link-email "bfinn@example.com"}))))
                          "location"))
      (with get-response (http-get-request @location))
      (it "should return the content that was created" 
        (should= 200 (:status @get-response))
        (should= {"lead-link" {"email" "bfinn@example.com"
                               "name" "Bill" } "name" "Test Circle!"} 
                 (json/parse-string (:body @get-response)))))

    (context "when creating multiple circles"
      (before (http-post-request "c" 
                                 (json/generate-string 
                                   {:name "Test Circle!"
                                    :lead-link-name "Bill"
                                    :lead-link-email "bfinn@example.com"})
                                 {:throw-exceptions true}))

      (before (http-post-request "c" 
                                 (json/generate-string 
                                   {:name "Test Circle 2"
                                    :lead-link-name "Bill"
                                    :lead-link-email "bfinn@example.com"})
                                 {:throw-exceptions true}))

      (with get-response (http-get-request "c"))

      (it "should return an array of created circles"
        (should= 200 (:status @get-response))
        (should-contain "application/json" (get-in @get-response 
                                                   [:headers "Content-Type"]))
        (should= 2 (count (json/parse-string (:body @get-response)))))))

  (context "roles"
    (context "when requesting the list of roles for a non-existent circle"
      (with roles-response (http-get-request "c/New%20Circle/r"))
      (it "should 422"
        (should= 422 (:status @roles-response))))

    (context "when posting a new role to a non-existent circle"
      (with roles-response (http-post-request 
                             "c/New%20Circle/r"
                             (json/generate-string {:name "My Role!"})))
      (it "should 422 with a helpful error message"
        (should= 422 (:status @roles-response))
        (should-contain "Circle 'New Circle' does not exist." (:body @roles-response))))

    (context "with a circle"
      (with circle-location (get (json/parse-string 
                                   (:body (http-post-request
                                            "c" 
                                            (json/generate-string 
                                              {:name "Test Circle!"
                                               :lead-link-name "Bill"
                                               :lead-link-email "bfinn@example.com"}))))
                                 "location"))

      (context "when requesting the list of roles"
        (with roles-response (http-get-request (str @circle-location "/r")))

        (it "should return an empty array"
          (should= 200 (:status @roles-response))
          (should= "[]" (:body @roles-response))))

      (context "when creating a role"
        (context "with valid parameters"
          (with roles-response (http-post-request
                                 (str @circle-location "/r")
                                 (json/generate-string {:name "My Role!"})))

          (it "should redirect to the location of the newly created resource"
            (should= 303 (:status @roles-response))
            (should-contain "application/json" 
                            (get-in @roles-response [:headers "Content-Type"]))
            (should= (str "/c/" (url-encode "Test Circle!") "/r/" 
                          (url-encode "My Role!"))
                     (get (json/parse-string (:body @roles-response)) "location"))))

        (should-return-4xx "with invalid paramaters" 
                           #(str @circle-location "/r")
                           (json/generate-string {:foo "Test Role"})
                           422
                           "IllegalArgumentException")
        (should-return-4xx "with missing paramaters" 
                           #(str @circle-location "/r")
                           (json/generate-string {})
                           422
                           "IllegalArgumentException")
        (should-return-4xx "with malformed JSON" 
                           #(str @circle-location "/r")
                           "{\"name\" :: \"Bill\""
                           400
                           "IOException"))

      (context "with a created role and its location"
        (with role-location (get (json/parse-string 
                              (:body (http-post-request
                                       (str @circle-location "/r")
                                       (json/generate-string 
                                         {:name "Test Role"})
                                       {:throw-exceptions true} )))
                            "location"))
        (with get-response (http-get-request @role-location))
        (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= "{}" (:body @get-response))))

      (context "with a created role with accountabilities and its location"
        (with role-location (get (json/parse-string 
                              (:body (http-post-request
                                       (str @circle-location "/r")
                                       (json/generate-string 
                                         {:name "Test Role"
                                          :purpose "End world hunger"})
                                       {:throw-exceptions true} )))
                            "location"))
        (with get-response (http-get-request @role-location))
        (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= (json/generate-string {:purpose "End world hunger"})
                   (:body @get-response)))))))
