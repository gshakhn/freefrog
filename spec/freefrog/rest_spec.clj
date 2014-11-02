(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
            [clj-http.client :as http-client]
            [freefrog.rest :as r]))

(describe "governance rest api"
  (before-all (r/start-test-server))
  (after-all (r/stop-test-server))
  (it "should return status code 200 at the root"
    (should= 200 (:status (http-client/get "http://localhost:3000")))))