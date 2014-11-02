(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
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
                                     :form-params {:name "Test Circle!"}}))
    (it "should redirect"
      (should= 303 (:status @response))
      (should-contain "application/json" (get-in @response [:headers "Content-Type"]))
      (should-contain "location" (:body @response)))
    (context "and getting the created circle"
      (it "should return the content that was created" ))))