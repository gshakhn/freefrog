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

(ns freefrog.rest
  (:require [org.httpkit.server :as httpkit]
            [ring.util.http-response :refer :all]
            [compojure.api.sweet :refer :all]
            [compojure.core :as c]
            [compojure.route :as route]
            [schema.core :as s]
            [freefrog.persistence :as p])
  (:import (freefrog MissingEntityException)))

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
    (middlewares [wrap-missing-entity]
      (context "/api" []
        (GET* "/*/_governance" {{path :*} :route-params}
              :return [s/Str]
              :summary "Retrieve the governance for a circle"
              (ok (p/get-all-governance-logs path)))

        (GET* "/*" {{path :*} :route-params}
              :return String
              :summary "Retrieve a circle or role"
              (format "You requested circle/role: %s" path))))))

(def app (-> (c/routes api (route/resources "/"))
             wrap-dir-index))

(defn start-server []
  (httpkit/run-server app {:port port}))

(defn -main []
  (start-server)
  (println "server started"))

