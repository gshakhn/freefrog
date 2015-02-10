(ns freefrog.auth
  (:require [clj-http.client :as http-client]
            [clojure.tools.logging :as log]
            [clj-json.core :as json]
            [clojure.walk :as walk]))


(defn authenticate [assertion]
  (try
    (let [result (-> (http-client/post
                       "https://verifier.login.persona.org/verify"
                       {:form-params
                        {:assertion assertion

                         ;;todo Make this configurable
                         :audience  "http://localhost:3000"}})
                     :body
                     json/parse-string
                     walk/keywordize-keys)]
      (if (= "okay" (:status result))
        result
        (log/error (format "Authentication failed: %s" result))))
    (catch Throwable t
      (log/error t "Authentication service failure"))))
