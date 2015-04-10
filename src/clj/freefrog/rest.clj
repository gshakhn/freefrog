;
; Copyright © 2015 Courage Labs
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

(ns freefrog.rest
  (:require [org.httpkit.server :as httpkit]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.core :as c]
            [compojure.route :as route]
            [ring.middleware.session :as session]
            [schema.core :as s]
            [freefrog.persistence :as p]
            [freefrog.auth :as auth]
            [clojure.tools.logging :as log])
  (:import (freefrog MissingEntityException))
  (:gen-class))

;;todo Make this configurable
(def port 3000)

(defn wrap-dir-index [handler]
  (fn [req]
    (handler (update-in req [:uri] #(if (= "/" %) "/index.html" %)))))

(defn wrap-missing-entity [handler]
  (fn [req]
    (try
      (handler req)
      (catch MissingEntityException e
        (let [{:keys [_ message]} (meta e)]
          (not-found message))))))

(defapi api
  (swagger-ui "/api")
  (swagger-docs
    :title "Freefrog API")
  (swaggered "freefrog"
    :description "The Freefrog API"
    (middlewares [wrap-missing-entity session/wrap-session]
      (context "/api" []
        (context "/circles" []
          (GET* "/*/_governance" {{path :*} :route-params}
                :return [s/Str]
                :summary "Retrieve the governance for a circle"
                (ok (p/get-all-governance-logs path)))

          (GET* "/*" {{path :*} :route-params}
                :return String
                :summary "Retrieve a circle or role"
                (format "You requested circle/role: %s" path)))

        (context "/session" []
          (GET* "/" {session :session}
                :return String
                :summary "Get a session"
                (if-let [principal (:principal session)]
                  (ok principal)
                  nil))

          (POST* "/" []
                 :return String
                 :summary "Establish a session"
                 :body-params [assertion :- String]
                 (let [result (auth/authenticate assertion)
                       principal (when result (:email result))
                       session (if principal
                                 {:principal principal}
                                 {})

                       response
                       (if principal
                         (ok principal)
                         (forbidden "Please submit a valid assertion"))]
                   (log/info (format "Login attempt: %s" result))
                   (assoc response :session session)))

          (DELETE* "/" {session :session}
                   :summary "Remove the current session; effectively a logout"
                   (log/info (format "Logged out: %s" (:principal session)))
                   (assoc (ok "Logged out") :session {})))))))

(def app (-> (c/routes api (route/resources "/"))
             wrap-dir-index))

(defn start-server []
  (httpkit/run-server app {:port port}))

(defn -main []
  (start-server)
  (log/info (format "server started on port %d" port)))
