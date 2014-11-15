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
  (:use [ring.adapter.jetty] [ring.util.codec :only [url-encode]]))

(def test-server (ref nil))

(defn start-test-server []
  (when-not @test-server
    (dosync
      (ref-set test-server (run-jetty #'r/handler {:port 3000 :join? false}))))
  (.start @test-server))

(defn stop-test-server []
  (.stop @test-server))

(defn http-put-request 
  ([uri body]
   (http-put-request uri body nil))
  ([uri body options]
   (http-client/put (str "http://localhost:3000" uri) 
                    (merge {:throw-exceptions false
                            :content-type :json
                            :body body} options))))
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
  (before-all (start-test-server))
  (after-all (stop-test-server))
  (after (r/reset-database))

  (context "anchor circle"
    (should-return-4xx "with malformed JSON" 
                       (fn [] "/")
                       "{\"command\" :: \"addRole\""
                       400
                       "IOException")

    (should-return-4xx "with no paramaters" 
                       (fn [] "/")
                       ""
                       422
                       "No command specified for request")

    (should-return-4xx "with an invalid command" 
                       (fn [] "/")
                       (json/generate-string {:command "nonsense"})
                       422
                       "Invalid command 'nonsense' received.")

    (context "with nothing having been created"
      (context "requesting the root resource"
        (with response (http-get-request "/"))
        (it "should return an empty object"
          (should= 200 (:status @response))
          (should= "{}" (:body @response))))

      (should-return-4xx "with creating a role" 
                         (fn [] "/")
                         (json/generate-string {:command "addRole"
                                                :params {:name "Test Role"}})
                         400
                         "IllegalArgumentException"))

    (context "with creating the anchor circle"
      (should-return-4xx "with no paramaters" 
                         (fn [] "/")
                         (json/generate-string
                           {:command "anchorCircle"})
                         400
                         "IllegalArgumentException")

      (should-return-4xx "with missing paramaters" 
                         (fn [] "/")
                         (json/generate-string
                           {:command "anchorCircle"
                            :params {:foo "Test Circle!"}})
                         400
                         "IllegalArgumentException")

      (context "with valid parameters"
        (with create-anchor-circle-response (http-post-request 
                                              "/" 
                                              (json/generate-string
                                                {:command "anchorCircle",
                                                 :params {:name "Test Circle!"}})))
        (before @create-anchor-circle-response)

        (it "should return the location of the newly created resource"
          (should= 201 (:status @create-anchor-circle-response))
          (should= "/" (get-location @create-anchor-circle-response)))

        (context "with retrieving the circle"
          (with get-response (http-get-request (get-location @create-anchor-circle-response)))
          (it "should return the content that was created" 
            (should= 200 (:status @get-response))
            (should-contain "Test Circle!" (:body @get-response))))

        (context "with deleting the anchor circle"
          (with delete-response (http-client/delete "http://localhost:3000/"
                                                    {:throw-exceptions false}))
          (it "should return method-not-allowed."
            (should= 405 (:status @delete-response))))

        (context "with updating the anchor circle"
          (context "with invalid parameters"
            (with response (http-put-request
                             "/" 
                             (json/generate-string
                               {:command "anchorCircle",
                                :params {:foo "Test Circle!"}})))
            (xit "should return an error response"))

          (context "with valid parameters"
            (with response (http-put-request
                             "/" 
                             (json/generate-string
                               {:command "anchorCircle",
                                :params {:name "Test Circle!"}})))
            (xit "should return the location of the updated resource")))

        (context "with getting a non-existent role"
          (with get-role-response (http-get-request "/DummyRole"))
          (it "should return 404"
            (should= 404 (:status @get-role-response))))

        (context "with creating a role"
          (with create-role-response (http-post-request 
                                       "/" 
                                       (json/generate-string
                                         {:command "addRole",
                                          :params {:foo "Test Circle!"}})))
          (context "with invalid parameters"
            (it "should return a failure"
              (should= 400 (:status @create-role-response))))

          (context "with valid parameters"
            (with create-role-response (http-post-request 
                                         "/" 
                                         (json/generate-string
                                           {:command "addRole",
                                            :params {:name "Test Circle!"}})))
            (before @create-role-response)
            (it "should return the location of the newly created resource"
              (should= 201 (:status @create-role-response))
              (should= (str "/" (url-encode "Test Circle!")) 
                (get-location @create-role-response)))

            (context "with retrieving the role"
              (with get-role-response (http-get-request 
                                        (str "/" (url-encode "Test Circle!"))))
              (xit "should return contents of the newly created resource"
                (should= 200 (:status @get-role-response))))

            (context "with deleting the role"
              (with delete-response 
                    (http-client/delete (str "http://localhost:3000/" 
                                             (url-encode "Test Circle!"))
                                        {:throw-exceptions false}))
              (before @delete-response)
              (xit "should return a valid response code of 204"
                (should= 204 (:status @delete-response)))

              (context "with requesting the deleted role"
                (with deleted-get-response 
                      (http-get-request (str "/" (url-encode "Test Circle!"))))
                (xit "should return a 404.")))

            (context "with updating a role"
              (with put-response 
                (http-put-request (str "/" (url-encode "Test Circle!"))))
              (xit "should update the role accordingly"))

            (context "with converting a role to a circle"
              (with convert-response 
                    (http-post-request 
                      (str "/" (url-encode "Test Circle!"))
                      (json/generate-string
                        {:command "convertRoleToCircle"})))
              (xit "should convert the role to a circle")))))


(should-return-4xx "with invalid paramaters" 
                   (fn [] "circles")
                   (json/generate-string {:foo "Test Circle!"})
                   400
                   "IllegalArgumentException")
(should-return-4xx "with missing paramaters" 
                   (fn [] "circles")
                   (json/generate-string {:foo "Test Circle!"})
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
                                   {:name "Test Circle!"}))))
  (context "and its specified location"
    (with get-response (http-get-request @location))
    (it "should return the content that was created" 
      (should= 200 (:status @get-response))
      (should-contain "Test Circle!" (:body @get-response))))

  (context "and its implicit location"
    (with get-response (http-get-request (url-encode "Test Circle!")))
    (it "should return the content that was created" 
      (should-not= nil @location)
      (should= 200 (:status @get-response))
      (should-contain "Test Circle!" (:body @get-response)))))

(context "when creating multiple circles"
  (before (http-post-request "circles" 
                             (json/generate-string 
                               {:name "Test Circle!"})
                             {:throw-exceptions true}))

  (before (http-post-request "circles" 
                             (json/generate-string 
                               {:name "Test Circle 2"})
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
                                            {:name "Test Circle!"}))))

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
        (should-contain "Test Role" (:body @get-response))))

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
        (should-contain "Test Role" (:body @get-response))
        (should-contain "End world hunger" (:body @get-response)))))))

(run-specs)