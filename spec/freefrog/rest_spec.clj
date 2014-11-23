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
            [freefrog.rest :as r]
            [freefrog.persistence :as p])
  (:use [ring.adapter.jetty] [ring.util.codec :only [url-encode]])
  (:import [javax.persistence EntityNotFoundException]))

(def test-server (ref nil))

(defn start-test-server []
  (when-not @test-server
    (dosync
      (ref-set test-server (run-jetty #'r/handler {:port 3000 :join? false}))))
  (.start @test-server))

(defn stop-test-server []
  (.stop @test-server))

(def host-url "http://localhost:3000")

(defn http-put-request 
  ([uri]
   (http-put-request uri nil))
  ([uri body]
   (http-put-request uri body nil))
  ([uri body options]
   (http-client/put (str host-url uri) 
                    (merge {:throw-exceptions false
                            :content-type "text/plain"
                            :body body} options))))
(defn http-post-request 
  ([uri]
   (http-post-request uri nil))
  ([uri body]
   (http-post-request uri body nil))
  ([uri body options]
   (http-client/post (str "http://localhost:3000/" uri) 
                     (merge {:throw-exceptions false
                             :content-type "text/plain"
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

(def sample-gov-log {:body "Add role\nRename accountability."})

(defn circle-not-found-thrower [& args]
  (throw (EntityNotFoundException. "Circle does not exist")))

(defn govt-meeting-not-found-thrower [& args]
  (throw (EntityNotFoundException. "Governance meeting does not exist")))

(describe "governance rest api"
  (before-all (start-test-server))
  (after-all (stop-test-server))

  (context "with a non-existent circle"
    (around [it]
      (with-redefs [p/get-all-governance-logs circle-not-found-thrower
                    p/new-governance-log circle-not-found-thrower
                    p/get-governance-log circle-not-found-thrower
                    p/put-governance-log circle-not-found-thrower]
        (it)))
    (context "requesting the governance endpoint"
      (with response (http-get-request "/circles/1234/governance"))
      (it "should return a 404"
        (should= 404 (:status @response))
        (should= "Circle does not exist" (:body @response))))

    (context "posting to the governance endpoint"
      (with response (http-post-request "/circles/1234/governance"))
      (it "should return a 400"
        (should= 400 (:status @response))
        (should-contain "Circle does not exist" (:body @response))))

    (context "putting to the agenda endpoint"
      (with response (http-put-request "/circles/1234/governance/5678/agenda"
                                       "New agenda"))
      (it "should return a 400"
        (should= 400 (:status @response))
        (should-contain "Circle does not exist" (:body @response)))))

  (context "with a circle"
    (around [it]
      (with-redefs [p/get-all-governance-logs (fn [id] sample-gov-log)
                    p/new-governance-log (fn [& args] 5678)]
        (it)))
    (context "requesting the governance endpoint"
      (with response (http-get-request "/circles/1234/governance"))
      (it "should return the entire governance document"
        (should= 200 (:status @response))
        (should= (json/generate-string sample-gov-log) (:body @response))))

    (context "posting to the governance endpoint"
      (with response (http-post-request "/circles/1234/governance"))
      (it "should return a 201"
        (should= 201 (:status @response))
        (should= (str host-url "/circles/1234/governance/5678") 
                 (get-location @response))))

    (context "with a non-existent governance endpoint"
      (around [it]
        (with-redefs [p/get-governance-log govt-meeting-not-found-thrower]
          (it)))
      (context "putting to the agenda endpoint"
        (with response (http-put-request "/circles/1234/governance/5678/agenda"
                                         "New agenda"))
        (it "should return a 400"
          (should= 400 (:status @response))
          (should-contain "Governance meeting does not exist" (:body @response)))))

    (context "with an existing governance endpoint"
      (context "with an empty open agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? true :agenda nil})]
            (it)))

        (context "putting to the agenda endpoint"
          (with response (http-put-request "/circles/1234/governance/5678/agenda"
                                           "New agenda"))

          (it "should return a 201"
            (should= 201 (:status @response))))

        (context "getting the agenda endpoint"
          (with response (http-get-request "/circles/1234/governance/5678/agenda"))
          (it "should return an empty agenda"
            (should= 200 (:status @response))
            (should-contain "text/plain" (get-in @response
                                                 [:headers "Content-Type"]))
            (should= "" (:body @response)))))

      (context "with an existing open agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? true :agenda "Current agenda"})]
            (it)))

        (context "getting the governance resource"
          (with response (http-get-request "/circles/1234/governance/5678"))
          (it "should return a 200"
            (should= 200 (:status @response)))
          (it "should return that an open meeting exists"
            (should-contain "true" (get-in @response
                                           [:headers "Open-Meeting"]))))

        (context "putting to the governance resource"
          (with response (http-put-request "/circles/1234/governance/5678"))
          ;(xit "should persist a closed governance log")
          (it "should return a 204"
            (should= 204 (:status @response))))

        (context "putting to the agenda endpoint"
          (with response (http-put-request "/circles/1234/governance/5678/agenda"
                                           "New agenda"))

          (it "should return a 204"
            (should= 204 (:status @response))))

        (context "getting the agenda endpoint"
          (with response (http-get-request "/circles/1234/governance/5678/agenda"))
          (it "should return an empty agenda"
            (should= 200 (:status @response))
            (should-contain "text/plain" (get-in @response
                                                 [:headers "Content-Type"]))
            (should= "Current agenda" (:body @response)))))

      (context "with an existing closed agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? false 
                                                           :agenda "Current closed agenda"})]
            (it)))

        (context "getting the governance resource"
          (with response (http-get-request "/circles/1234/governance/5678"))
          (it "should return a 200"
            (should= 200 (:status @response)))
          (it "should return the details of the governance resource"
            (should-contain "{\"agenda\":\"Current closed agenda\"" 
                            (:body @response))))

        (context "putting to the governance resource"
          (with response (http-put-request "/circles/1234/governance/5678"))
          (it "should return a 204"
            (should= 204 (:status @response))))

        (context "putting to the agenda endpoint"
          (with response (http-put-request "/circles/1234/governance/5678/agenda"
                                           "New agenda"))

          (it "should return a 400"
            (should= 400 (:status @response))
            (should-contain "Agenda is closed" (:body @response))))

        (context "getting the agenda endpoint"
          (with response (http-get-request "/circles/1234/governance/5678/agenda"))
          (it "should return 400"
            (should= 400 (:status @response))
            (should-contain "Agenda is closed" (:body @response))))))))

;      (context "putting to the agenda endpoint"
;        (with response (http-put-request "/circles/1234/governance/5678/agenda"
;                                         "New agenda"))
;
;        (it "should return a 201"
;          (should= 201 (:status @response)))))))
;          (should-invoke p/put-governance-log {:times 1}
;                         @response)
;          (should-contain "Governance meeting does not exist" (:body @response)))))))

;    (context "with nothing having been created"
;      (context "requesting the root resource"
;        (with response (http-get-request "/"))
;        (it "should return an empty object"
;          (should= 200 (:status @response))
;          (should= "{}" (:body @response))))
;
;      (should-return-4xx "with creating a role" 
;                         (fn [] "/")
;                         (json/generate-string {:command "addRole"
;                                                :params {:name "Test Role"}})
;                         400
;                         "IllegalArgumentException"))
;
;    (context "with creating the anchor circle"
;      (should-return-4xx "with no paramaters" 
;                         (fn [] "/")
;                         (json/generate-string
;                           {:command "anchorCircle"})
;                         400
;                         "IllegalArgumentException")
;
;      (should-return-4xx "with missing paramaters" 
;                         (fn [] "/")
;                         (json/generate-string
;                           {:command "anchorCircle"
;                            :params {:foo "Test Circle!"}})
;                         400
;                         "IllegalArgumentException")
;
;      (context "with valid parameters"
;        (with create-anchor-circle-response (http-post-request 
;                                              "/" 
;                                              (json/generate-string
;                                                {:command "anchorCircle",
;                                                 :params {:name "Test Circle!"}})))
;        (before @create-anchor-circle-response)
;
;        (it "should return the location of the newly created resource"
;          (should= 201 (:status @create-anchor-circle-response))
;          (should= "/" (get-location @create-anchor-circle-response)))
;
;        (context "with retrieving the circle"
;          (with get-response (http-get-request (get-location @create-anchor-circle-response)))
;          (it "should return the content that was created" 
;            (should= 200 (:status @get-response))
;            (should-contain "Test Circle!" (:body @get-response))))
;
;        (context "with deleting the anchor circle"
;          (with delete-response (http-client/delete "http://localhost:3000/"
;                                                    {:throw-exceptions false}))
;          (it "should return method-not-allowed."
;            (should= 405 (:status @delete-response))))
;
;        (context "with updating the anchor circle"
;          (context "with invalid parameters"
;            (with response (http-put-request
;                             "/" 
;                             (json/generate-string
;                               {:command "anchorCircle",
;                                :params {:foo "Test Circle!"}})))
;            (xit "should return an error response"))
;
;          (context "with valid parameters"
;            (with response (http-put-request
;                             "/" 
;                             (json/generate-string
;                               {:command "anchorCircle",
;                                :params {:name "Test Circle!"}})))
;            (xit "should return the location of the updated resource")))
;
;        (context "with getting a non-existent role"
;          (with get-role-response (http-get-request "/DummyRole"))
;          (it "should return 404"
;            (should= 404 (:status @get-role-response))))
;
;        (context "with creating a role"
;          (with create-role-response (http-post-request 
;                                       "/" 
;                                       (json/generate-string
;                                         {:command "addRole",
;                                          :params {:foo "Test Circle!"}})))
;          (context "with invalid parameters"
;            (it "should return a failure"
;              (should= 400 (:status @create-role-response))))
;
;          (context "with valid parameters"
;            (with create-role-response (http-post-request 
;                                         "/" 
;                                         (json/generate-string
;                                           {:command "addRole",
;                                            :params {:name "Test Circle!"}})))
;            (before @create-role-response)
;            (it "should return the location of the newly created resource"
;              (should= 201 (:status @create-role-response))
;              (should= (str "/" (url-encode "Test Circle!")) 
;                (get-location @create-role-response)))
;
;            (context "with retrieving the role"
;              (with get-role-response (http-get-request 
;                                        (str "/" (url-encode "Test Circle!"))))
;              (xit "should return contents of the newly created resource"
;                (should= 200 (:status @get-role-response))))
;
;            (context "with deleting the role"
;              (with delete-response 
;                    (http-client/delete (str "http://localhost:3000/" 
;                                             (url-encode "Test Circle!"))
;                                        {:throw-exceptions false}))
;              (before @delete-response)
;              (xit "should return a valid response code of 204"
;                (should= 204 (:status @delete-response)))
;
;              (context "with requesting the deleted role"
;                (with deleted-get-response 
;                      (http-get-request (str "/" (url-encode "Test Circle!"))))
;                (xit "should return a 404.")))
;
;            (context "with updating a role"
;              (with put-response 
;                (http-put-request (str "/" (url-encode "Test Circle!"))))
;              (xit "should update the role accordingly"))
;
;            (context "with converting a role to a circle"
;              (with convert-response 
;                    (http-post-request 
;                      (str "/" (url-encode "Test Circle!"))
;                      (json/generate-string
;                        {:command "convertRoleToCircle"})))
;              (xit "should convert the role to a circle"))))))))
;
;
;;(should-return-4xx "with invalid paramaters" 
;;                   (fn [] "circles")
;;                   (json/generate-string {:foo "Test Circle!"})
;;                   400
;;                   "IllegalArgumentException")
;;(should-return-4xx "with missing paramaters" 
;;                   (fn [] "circles")
;;                   (json/generate-string {:foo "Test Circle!"})
;;                   400
;;                   "IllegalArgumentException")
;;(should-return-4xx "with malformed JSON" 
;;                   (fn [] "circles")
;;                   "{\"name\" :: \"Bill\""
;;                   400
;;                   "IOException"))
;;
;;(context "with a created circle"
;;  (with location (get-location (http-post-request
;;                                 "circles" 
;;                                 (json/generate-string 
;;                                   {:name "Test Circle!"}))))
;;  (context "and its specified location"
;;    (with get-response (http-get-request @location))
;;    (it "should return the content that was created" 
;;      (should= 200 (:status @get-response))
;;      (should-contain "Test Circle!" (:body @get-response))))
;;
;;  (context "and its implicit location"
;;    (with get-response (http-get-request (url-encode "Test Circle!")))
;;    (it "should return the content that was created" 
;;      (should-not= nil @location)
;;      (should= 200 (:status @get-response))
;;      (should-contain "Test Circle!" (:body @get-response)))))
;;
;;(context "when creating multiple circles"
;;  (before (http-post-request "circles" 
;;                             (json/generate-string 
;;                               {:name "Test Circle!"})
;;                             {:throw-exceptions true}))
;;
;;  (before (http-post-request "circles" 
;;                             (json/generate-string 
;;                               {:name "Test Circle 2"})
;;                             {:throw-exceptions true}))
;;
;;  (with get-response (http-get-request "circles"))
;;
;;  (it "should return an array of created circles"
;;    (should= 200 (:status @get-response))
;;    (should-contain "application/json" (get-in @get-response 
;;                                               [:headers "Content-Type"]))
;;    (should= 2 (count (json/parse-string (:body @get-response)))))))
;;
;;(context "roles"
;;  (context "when requesting the list of roles for a non-existent circle"
;;    (with roles-response (http-get-request "circles/New%20Circle/roles"))
;;    (it "should 404"
;;      (should= 404 (:status @roles-response))))
;;
;;  (context "when posting a new role to a non-existent circle"
;;    (with roles-response (http-post-request 
;;                           "circles/New%20Circle/roles"
;;                           (json/generate-string {:name "My Role!"})))
;;    (it "should 422 with a helpful error message"
;;      (should= 404 (:status @roles-response))
;;      (should-contain "Circle 'New Circle' does not exist." (:body @roles-response))))
;;
;;  (context "with a circle"
;;    (with circle-location (get-location (http-post-request
;;                                          "circles" 
;;                                          (json/generate-string 
;;                                            {:name "Test Circle!"}))))
;;
;;    (context "when requesting the list of roles"
;;      (with roles-response (http-get-request (str @circle-location "/roles")))
;;
;;      (it "should return an empty array"
;;        (should= 200 (:status @roles-response))
;;        (should= "[]" (:body @roles-response))))
;;
;;    (context "when creating a role"
;;      (context "with valid parameters"
;;        (with roles-response (http-post-request
;;                               (str @circle-location "/roles")
;;                               (json/generate-string {:name "My Role!"})))
;;
;;        (it "should return the location of the newly created resource"
;;          (should= 201 (:status @roles-response))
;;          (should= (str "/circles/" (url-encode "Test Circle!") "/roles/" 
;;                        (url-encode "My Role!"))
;;                   (get-location @roles-response))))
;;
;;      (should-return-4xx "with invalid paramaters" 
;;                         #(str @circle-location "/roles")
;;                         (json/generate-string {:foo "Test Role"})
;;                         400
;;                         "IllegalArgumentException")
;;      (should-return-4xx "with missing paramaters" 
;;                         #(str @circle-location "/roles")
;;                         (json/generate-string {})
;;                         400
;;                         "IllegalArgumentException")
;;      (should-return-4xx "with malformed JSON" 
;;                         #(str @circle-location "/roles")
;;                         "{\"name\" :: \"Bill\""
;;                         400
;;                         "IOException"))
;;
;;    (context "with a created role and its location"
;;      (with role-location (get-location (http-post-request
;;                                          (str @circle-location "/roles")
;;                                          (json/generate-string 
;;                                            {:name "Test Role"})
;;                                          {:throw-exceptions true} )))
;;      (with get-response (http-get-request @role-location))
;;      (it "should return the content that was created" 
;;        (should= 200 (:status @get-response))
;;        (should-contain "Test Role" (:body @get-response))))
;;
;;    (context "with a created role with accountabilities and its location"
;;      (with role-location (get-location (http-post-request
;;                                          (str @circle-location "/roles")
;;                                          (json/generate-string 
;;                                            {:name "Test Role"
;;                                             :purpose "End world hunger"})
;;                                          {:throw-exceptions true} )))
;;      (with get-response (http-get-request @role-location))
;;      (it "should return the content that was created" 
;;        (should= 200 (:status @get-response))
;;        (should-contain "Test Role" (:body @get-response))
;;        (should-contain "End world hunger" (:body @get-response)))))))

(run-specs)