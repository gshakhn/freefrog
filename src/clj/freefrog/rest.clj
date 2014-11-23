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
  (:require [liberator.core :refer [resource defresource]]
            [liberator.representation :refer [ring-response]]
            [ring.middleware.params :refer [wrap-params]]
            [freefrog.governance :as g]
            [freefrog.governance-logs :as gl]
            [freefrog.persistence :as p]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :refer [split]]
            [clj-json.core :as json]
            [clojure.java.io :as io]
            [compojure.route :as route]
            [compojure.core :refer [defroutes ANY GET]])
  (:use [ring.util.codec :only [url-encode url-decode]])
  (:import java.net.URL
           [javax.persistence EntityNotFoundException]))

(defn put-or-post? [ctx]
  (#{:put :post} (get-in ctx [:request :request-method])))

(defn check-content-type [ctx content-types]
  (if (put-or-post? ctx)
    (or (some #{(get-in ctx [:request :headers "content-type"])} content-types)
        [false {:message "Unsupported Content-Type"}])
    true))

(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

(defn new-governance-log [circle-id]
  (try 
    {::new-governance-log-id (p/new-governance-log circle-id (gl/create-governance-log))}
    (catch EntityNotFoundException e
      {::create-failed (.getMessage e)})))

(defn get-governance-log [circle-id log-id]
  (try
    [true {::governance-log (p/get-governance-log circle-id log-id)}]
    (catch EntityNotFoundException e
      [false {:message (.getMessage e)}])))

(defn put-governance-log [circle-id gov-id context]
  (try
    (dosync
      (let [gov-log (p/get-governance-log circle-id gov-id)]
        (when (:is-open? gov-log)
          (p/put-governance-log circle-id gov-id 
                                (assoc gov-log
                                       :is-open? false)))))
    (catch EntityNotFoundException e
      {::create-failed (.getMessage e)})))

(defn get-governance-logs [circle-id]
  (try
    [true {::governance-logs (p/get-all-governance-logs circle-id)}]
    (catch EntityNotFoundException e
      [false {:message (.getMessage e)}])))

(defn put-governance-log-agenda [circle-id gov-id context]
  (try
    (dosync
      (let [gov-log (p/get-governance-log circle-id gov-id)]
        (if (:is-open? gov-log)
          (p/put-governance-log circle-id gov-id 
                                (assoc gov-log
                                       :agenda (:body context)))
          {::create-failed "Agenda is closed."})))
    (catch EntityNotFoundException e
      {::create-failed (.getMessage e)})))

(defn validate-context [ctx]
  (when (::create-failed ctx)
    (ring-response {:status 400 :body (::create-failed ctx)})))

(defresource governance-agenda-resource [circle-id log-id]
  :allowed-methods [:get :put]
  :known-content-type? #(check-content-type % ["text/plain"])
  :exists? (fn [_] (get-governance-log circle-id log-id))
  :new? #(nil? (:agenda (::governance-log %)))
  :put! #(put-governance-log-agenda circle-id log-id %)
  :handle-ok #(if (:is-open? (::governance-log %))
                (ring-response {:status 200 
                                :headers {"Content-Type" "text/plain"}
                                :body (str (:agenda (::governance-log %)))})
                (ring-response {:status 400 :body "Agenda is closed."}))
  :handle-created #(validate-context %)
  :handle-no-content #(validate-context %))

(defresource specific-governance-resource [circle-id log-id]
  :allowed-methods [:put :get]
  :known-content-type? #(check-content-type % ["text/plain"])
  :exists? (fn [_] (get-governance-log circle-id log-id))
  :new? #(nil? (::governance-log %))
  :put! #(put-governance-log circle-id log-id %)
  :handle-ok #(if (:is-open? (::governance-log %))
                (ring-response {:status 200 :headers {"Open-Meeting" "true"}})
                (json/generate-string (::governance-log %)))
  :handle-no-content #(validate-context %))

(defresource general-governance-resource [circle-id]
  :allowed-methods [:get :post]
  :known-content-type? #(check-content-type % ["text/plain"])
  :post! (fn [_] (new-governance-log circle-id))
  :exists? (fn [_] (get-governance-logs circle-id))
  :handle-ok #(json/generate-string (::governance-logs %))
  :handle-created #(when (::create-failed %) 
                     (ring-response {:status 400 
                                     :body (::create-failed %)}))
  :location #(build-entry-url (:request %) (::new-governance-log-id %)))

(defroutes app
  (ANY "/circles/:circle-id/governance" [circle-id] 
       (general-governance-resource circle-id))
  (ANY "/circles/:circle-id/governance/:log-id" [circle-id log-id] 
       (specific-governance-resource circle-id log-id))
  (ANY "/circles/:circle-id/governance/:log-id/agenda" [circle-id log-id]
       (governance-agenda-resource circle-id log-id))
  (route/not-found "<h1>:-(</hi>"))

(def handler 
  (-> app 
    (liberator.dev/wrap-trace :header :ui)))

