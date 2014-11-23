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

(run-specs)