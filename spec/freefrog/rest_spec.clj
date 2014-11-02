(ns freefrog.rest-spec
  (:require [speclj.core :refer :all]
            [clj-http.client :as http-client]
            [freefrog.rest :as r]))

(describe "governance rest api"
  (it "should return status code 200 at the root"
    (should= 200 (:status (http-client/get "http://localhost:3000")))))