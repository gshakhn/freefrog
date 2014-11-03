(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
            [clj-json.core :as json]
            [clj-http.client :as http-client]
            [freefrog.rest :as r]))

(describe "governance rest api"
  (before-all (r/start-test-server))
  (after-all (r/stop-test-server))
  (after (r/reset-database))

  (it "should return status code 404 at the root"
    (should= 404 (:status (http-client/get "http://localhost:3000" {:throw-exceptions false}))))

  (context "when no circles have been created"
    (with response (http-client/get "http://localhost:3000/circles" {:throw-exceptions false}))
    (it "should return an empty array"
        (should= 200 (:status @response))
        (should= "[]" (:body @response))))

  (context "upon creating a circle"
    (with response (http-client/post "http://localhost:3000/circles" 
                                    {:throw-exceptions false
                                     :content-type :json
                                     :body (json/generate-string 
                                             {:name "Test Circle!"
                                              :lead-link-name "Bill"
                                              :lead-link-email "bfinn@example.com"})}))
    (it "should redirect"
      (should= 303 (:status @response))
      (should-contain "application/json" (get-in @response [:headers "Content-Type"]))
      (should-contain #"^\/circles\/\d+" (get (json/parse-string (:body @response)) "location")))

    (context "and getting the created circle"
      (with location (get (json/parse-string 
                            (:body (http-client/post 
                                     "http://localhost:3000/circles" 
                                     {:throw-exceptions false
                                      :content-type :json
                                      :form-params {:name "Test Circle!"
                                                    :lead-link-name "Bill"
                                                    :lead-link-email "bfinn@example.com"}})))
                            "location"))
      (with get-response (http-client/get (str "http://localhost:3000" 
                                               @location)
                                      {:throw-exceptions false}))
      (it "should return the content that was created" 
          (should= 200 (:status @get-response))
          (should= {"lead-link" {"email" "bfinn@example.com"
                                 "name" "Bill" } "name" "Test Circle!"} 
                   (json/parse-string (:body @get-response))))))

  (context "upon creating a circle with missing parameters"
    (with response (http-client/post "http://localhost:3000/circles" 
                                    {:throw-exceptions false
                                     :content-type :json
                                     :body (json/generate-string 
                                             {:name "Test Circle!"})}))
    (it "should return a bad request"
      (should= 400 (:status @response))
      (should-contain "IllegalArgumentException" (:body @response))))

  (context "upon creating a circle with malformed json"
    (with response (http-client/post "http://localhost:3000/circles" 
                                    {:throw-exceptions false
                                     :content-type :json
                                     :body "{\"name\" :: \"Bill\"}"}))
    (it "should return a bad request"
      (should= 400 (:status @response))
      (should-contain "IOException" (:body @response)))))