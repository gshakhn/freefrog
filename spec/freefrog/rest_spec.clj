(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
            [clj-json.core :as json]
            [clj-http.client :as http-client]
            [freefrog.rest :as r]))

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

(defn should-return-400 [spec-context circle-content response-content]
  (context spec-context
    (with response (http-post-request "circles" circle-content))
    (it "should return a bad request"
      (should= 400 (:status @response))
      (should-contain response-content (:body @response)))))

(describe "governance rest api"
  (before-all (r/start-test-server))
  (after-all (r/stop-test-server))
  (after (r/reset-database))

  (it "should return status code 404 at the root"
    (should= 404 (:status (http-get-request))))

  (context "circles"
    (context "when no circles have been created"
      (with response (http-get-request "circles"))
      (it "should return an empty array"
          (should= 200 (:status @response))
          (should= "[]" (:body @response))))
  
    (context "when creating a circle"
      (with response (http-post-request "circles" 
                                        (json/generate-string 
                                          {:name "Test Circle!"
                                           :lead-link-name "Bill"
                                           :lead-link-email "bfinn@example.com"})))
      (it "should redirect to the location of the newly created resource"
        (should= 303 (:status @response))
        (should-contain "application/json" (get-in @response [:headers "Content-Type"]))
        (should-contain #"^\/circles\/\d+" (get (json/parse-string (:body @response)) "location")))
  
      (should-return-400 "with invalid paramaters" 
                         (json/generate-string {:foo "Test Circle!"})
                         "IllegalArgumentException")
      (should-return-400 "with missing paramaters" 
                         (json/generate-string {:name "Test Circle!"})
                         "IllegalArgumentException")
      (should-return-400 "with malformed JSON" 
                         "{\"name\" :: \"Bill\""
                         "IOException"))
  
    (context "when creating and retrieving the created circle"
      (with location (get (json/parse-string 
                            (:body (http-post-request
                                     "circles" 
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
      (with roles-response (http-get-request "circles/1234/roles"))
        (it "should 404"
          (should= 404 (:status @roles-response))))

    (context "when posting a new role to a non-existent circle"
      (with roles-response (http-post-request 
                             "circles/1234/roles"
                             (json/generate-string {:name "My Role!"})))
        (it "should 400 with a helpful error message"
            (should= 400 (:status @roles-response))
            (should-contain "Circle 1234 does not exist." (:body @roles-response))))

    (context "with a new circle"
      (with circle-location (get (json/parse-string 
                                   (:body (http-post-request
                                            "circles" 
                                             (json/generate-string 
                                                     {:name "Test Circle!"
                                                      :lead-link-name "Bill"
                                                      :lead-link-email "bfinn@example.com"}))))
                                 "location"))

        (context "when requesting the list of roles"
          (with roles-response (http-get-request (str @circle-location "/roles")))
          (it "should return an empty array"
              (should= 200 (:status @roles-response))
              (should= "[]" (:body @roles-response))))

        (context "creating a role"
          (with roles-response (http-post-request
                                 (str @circle-location "/roles")
                                 (json/generate-string 
                                          {:name "My Role!"})))
          (it "should redirect to the location of the newly created resource"
              (should= 303 (:status @roles-response))
              (should-contain "application/json" 
                              (get-in @roles-response [:headers "Content-Type"]))
              (should-contain #"^\/circles\/\d+\/roles\/\d+" (get 
                                                   (json/parse-string 
                                                     (:body @roles-response)) "location")))))))
