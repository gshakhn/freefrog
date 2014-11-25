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
  (:use [ring.adapter.jetty])
  (:import [javax.persistence EntityNotFoundException]
           [org.apache.http HttpStatus]))

(def test-server (ref nil))

(defn start-test-server []
  (when-not @test-server
    (dosync
      (ref-set test-server (run-jetty #'r/handler {:port 3000 :join? false}))))
  (.start @test-server))

(defn stop-test-server []
  (.stop @test-server))

(def host-url "http://localhost:3000")

(def http-request-fns
  {:get http-client/get 
   :put http-client/put 
   :post http-client/post})

(defn http-request 
  ([method uri]
   (http-request method uri nil))
  ([method uri options]
   (apply (get http-request-fns method) 
          [(str host-url uri) 
           (merge {:throw-exceptions false
                   :content-type "text/plain"
                   :body ""} options)])))

(defn get-location [response]
  (get (:headers response) "Location"))

(def sample-gov-log {:body "Add role\nRename accountability."})

(defn circle-not-found-thrower [& args]
  (throw (EntityNotFoundException. "Circle does not exist")))

(defn govt-meeting-not-found-thrower [& args]
  (throw (EntityNotFoundException. "Governance meeting does not exist")))

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
      (with response (http-request :get "/circles/1234/governance"))

      (it-responds-with-status HttpStatus/SC_NOT_FOUND @response)
      (it-responds-with-body "Circle does not exist" @response))

    (context "posting to the governance endpoint"
      (with response (http-request :post "/circles/1234/governance"))

      (it-responds-with-status HttpStatus/SC_BAD_REQUEST @response)
      (it-responds-with-body "Circle does not exist" @response))

    (context "putting to the agenda endpoint"
      (with response (http-request :put 
                                   "/circles/1234/governance/5678/agenda"
                                   {:body "New agenda"}))
      (it-responds-with-status HttpStatus/SC_BAD_REQUEST @response)
      (it-responds-with-body "Circle does not exist" @response)))

  (context "with a circle"
    (around [it]
      (with-redefs [p/get-all-governance-logs (fn [id] sample-gov-log)
                    p/new-governance-log (fn [& args] 5678)]
        (it)))

    (context "requesting the governance endpoint"
      (with response (http-request :get "/circles/1234/governance"))

      (it-responds-with-status HttpStatus/SC_OK @response)
      (it-responds-with-body (json/generate-string sample-gov-log)
                                        @response))

    (context "posting to the governance endpoint with application/json"
      (with response (http-request :post 
                                   "/circles/1234/governance" 
                                   {:content-type "application/json"}))
      (it "should not respond to application/json"
        (should= HttpStatus/SC_UNSUPPORTED_MEDIA_TYPE (:status @response))))

    (context "posting to the governance endpoint"
      (with response (http-request :post "/circles/1234/governance"))

      (it-responds-with-status HttpStatus/SC_CREATED @response)
      (it "should return the location of the newly created governance log"
        (should= (str host-url "/circles/1234/governance/5678") 
                 (get-location @response))))

    (context "with a non-existent governance endpoint"
      (around [it]
        (with-redefs [p/get-governance-log govt-meeting-not-found-thrower]
          (it)))

      (context "putting to the agenda endpoint"
        (with response (http-request :put 
                                     "/circles/1234/governance/5678/agenda"
                                     {:body "New agenda"}))
        (it-responds-with-status HttpStatus/SC_BAD_REQUEST @response)
        (it-responds-with-body "Governance meeting does not exist" 
                                          @response)))

    (context "with an existing governance endpoint"
      (context "with an empty open agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? true :agenda nil})]
            (it)))

        (context "putting to the agenda endpoint with an unsupported media type"
          (with response (http-request :put
                                       "/circles/1234/governance/5678/agenda"
                                       {:body "New agenda"
                                        :content-type "application/json"}))
          (it-responds-with-status 415 @response))

        (context "putting to the agenda endpoint"
          (with response (http-request :put
                                       "/circles/1234/governance/5678/agenda"
                                       {:body "New agenda"}))

          (it-responds-with-status HttpStatus/SC_CREATED @response)
          (it "should return the location of the newly created governance log"
            (should= (str host-url "/circles/1234/governance/5678/agenda") 
                     (get-location @response))))

        (context "getting the agenda endpoint"
          (with response (http-request :get "/circles/1234/governance/5678/agenda"))
          (it-responds-with-status HttpStatus/SC_OK @response)
          (it-responds-with-body "" @response)
          (it "should return an empty agenda"
            (should-contain "text/plain" (get-in @response
                                                 [:headers "Content-Type"])))))

      (context "with an existing open agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? true :agenda "Current agenda"})]
            (it)))

        (context "getting the governance resource"
          (with response (http-request :get "/circles/1234/governance/5678"))
          (it-responds-with-status HttpStatus/SC_OK @response)
          (it "should return that an open meeting exists"
            (should-contain "true" (get-in @response
                                           [:headers "Open-Meeting"]))))

        (context "putting to the governance resource"
          (with response (http-request :put "/circles/1234/governance/5678"))
          ;(xit "should persist a closed governance log")
          (it-responds-with-status HttpStatus/SC_NO_CONTENT @response))

        (context "putting to the agenda endpoint"
          (with response (http-request :put 
                                       "/circles/1234/governance/5678/agenda"
                                       {:body "New agenda"}))

          (it-responds-with-status HttpStatus/SC_NO_CONTENT @response))

        (context "getting the agenda endpoint"
          (with response (http-request :get "/circles/1234/governance/5678/agenda"))
          (it-responds-with-status HttpStatus/SC_OK @response)
          (it-responds-with-body "Current agenda" @response)
          (it "should return the contents of the existing, open agenda"
            (should-contain "text/plain" (get-in @response
                                                 [:headers "Content-Type"])))))

      (context "with an existing closed agenda"
        (around [it]
          (with-redefs [p/get-governance-log (fn [& args] {:is-open? false 
                                                           :agenda "Current closed agenda"})]
            (it)))

        (context "getting the governance resource"
          (with response (http-request :get "/circles/1234/governance/5678"))
          (it-responds-with-status HttpStatus/SC_OK @response)
          (it-responds-with-body-containing "{\"agenda\":\"Current closed agenda\"" 
                                            @response))

        (context "putting to the governance resource"
          (with response (http-request :put "/circles/1234/governance/5678"))
          (it-responds-with-status HttpStatus/SC_NO_CONTENT @response))

        (context "putting to the agenda endpoint"
          (with response (http-request :put
                                       "/circles/1234/governance/5678/agenda"
                                       {:body "New agenda"}))

          (it-responds-with-status HttpStatus/SC_BAD_REQUEST @response)
          (it-responds-with-body "Agenda is closed." @response))

        (context "getting the agenda endpoint"
          (with response (http-request :get "/circles/1234/governance/5678/agenda"))
          (it-responds-with-status HttpStatus/SC_BAD_REQUEST @response)
          (it-responds-with-body "Agenda is closed." @response))))))

(run-specs)
