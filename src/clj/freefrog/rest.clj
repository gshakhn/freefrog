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
            [liberator.dev]
            [liberator.representation :refer [ring-response]]
            [ring.middleware.params :refer [wrap-params]]
            [freefrog.governance :as g]
            [clojure.walk :refer [keywordize-keys]]
            [clojure.string :refer [split]]
            [clj-json.core :as json]
            [clojure.java.io :as io]
            [compojure.route :as route]
            [compojure.core :refer [defroutes ANY GET]])
  (:use [ring.adapter.jetty]
        [ring.util.codec :only [url-encode url-decode]])
  (:import java.net.URL))

(defonce circles (ref {}))

;;;; http://clojure-liberator.github.io/liberator/tutorial/all-together.html
;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn malformed-json? [context]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (keywordize-keys (json/parse-string body))]
          [false {::json-data data}])
        [true {:message "No body"}])
      (catch Exception e
        ;(.printStackTrace e)
        [true {:message (format "IOException: %s" (.getMessage e))}]))))

(defn get-circle-name-from-uri [uri index]
  (url-decode (first (take-last index (split uri #"/")))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
      (some #{(get-in ctx [:request :headers "content-type"])}
            content-types)
      [false {:message "Unsupported Content-Type"}])
    true))

(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

(def valid-reserved-word-indices
  {"roles" [-1, -2]})

(defn valid-special-value? [pair]
  (let [[index value] pair]
    (some #{index} (get valid-reserved-word-indices value))))

(defn valid-uri? [uri]
  (let [tree (split uri #"/")
        special-values (keep-indexed #(when (get #{"roles"} %2)
                                        [(- %1 (count tree)) %2]) tree)]
    (every? valid-special-value? special-values)))

(defn circle-exists? [context index]
  (let [uri (get-in context [:request :uri])
        uri-list (split uri #"/")
        circle-name (url-decode (first (take-last index uri-list)))
        circle (get @circles circle-name)]
    (if circle
      [true {::circle-name circle-name ::circle circle}]
      [false {:message (format "Circle '%s' does not exist." circle-name)}])))

(defn create-circle [context]
  (let [{:keys [name lead-link-name lead-link-email]} (::json-data context)]
    (try
      (dosync
        (alter circles assoc name 
               (g/anchor-circle name lead-link-name lead-link-email)))
      {::circle-data (get @circles name) ::url-encoded-id (url-encode name)}
      (catch IllegalArgumentException e
        ;(.printStackTrace e)
        {::create-failed (format "IllegalArgumentException: %s" (.getMessage e))}))))

(defn create-role [context]
  (let [{:keys [name purpose domains accountabilities]} (get context ::json-data)
        circle-name (::circle-name context)]
    (try
      (dosync (alter circles assoc circle-name 
                     (g/add-role (::circle context) 
                                 name purpose domains accountabilities)))
      {::role-data (:roles (get @circles circle-name)) 
       ::url-encoded-role-id (url-encode name)}
      (catch IllegalArgumentException e
        ;(.printStackTrace e)
        {::create-failed (format "IllegalArgumentException: %s" (.getMessage e))}))))

(defn role-exists? [context role-id]
  (let [[circle-exists? ret-val] (circle-exists? context 3)]
    (if circle-exists?
      (let [circle (::circle ret-val)
            curr-role (get-in circle [:roles (url-decode role-id)])]
        (if curr-role
          [true (merge ret-val {::role curr-role})]
          [false ret-val]))
      ret-val)))

(defresource anchor-circle-resource
  :handle-ok (fn[_] (json/generate-string @circles))
  :allowed-methods [:get])

(defresource implicit-circle-resource
  :processable? #(valid-uri? (get-in % [:request :uri]))
  :allowed-methods [:get]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? #(circle-exists? % 1)
  :available-media-types ["application/json"]
  :handle-ok ::circle
  :malformed? (fn [ctx] (malformed-json? ctx))
  :can-put-to-missing? false)

(defresource circle-resource [id]
  :processable? #(valid-uri? (get-in % [:request :uri]))
  :allowed-methods [:get]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [_]
             (let [e (get @circles (url-decode id))]
               (if-not (nil? e)
                 {::circle e})))
  :available-media-types ["application/json"]
  :handle-ok ::circle
  :malformed? (fn [ctx] (malformed-json? ctx))
  :can-put-to-missing? false)

(defresource collective-circles-resource
  :processable? #(valid-uri? (get-in % [:request :uri]))
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? #(malformed-json? %)
  :post! #(create-circle %)
  :new? true
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys @circles))
  :handle-created #(when (::create-failed %) 
                     (ring-response {:status 400 
                                     :body (::create-failed %)}))
  :location (fn [ctx] (str (:uri (:request ctx)) "/" (::url-encoded-id ctx))))

(defresource role-resource [role-id]
  :processable? #(valid-uri? (get-in % [:request :uri]))
  :allowed-methods [:get]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? #(role-exists? % role-id)
  :available-media-types ["application/json"]
  :handle-ok ::role
  :malformed? #(malformed-json? %)
  :can-put-to-missing? false)

(defresource collective-roles-resource
  :processable? #(valid-uri? (get-in % [:request :uri]))
  :exists? #(circle-exists? % 2)
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? (fn [ctx] (malformed-json? ctx))
  :new? true
  :post! #(create-role %)
  :can-post-to-missing? false
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys (get-in % [::circle :roles])))
  :handle-created #(when (::create-failed %) 
                     (ring-response {:status 400 
                                     :body (::create-failed %)}))
  :location (fn [ctx] (str (:uri (:request ctx)) "/" (::url-encoded-role-id ctx))))

(defresource accountabilities-resource [role-id]
  :allowed-methods [:get])

(defresource domains-resource [role-id]
  :allowed-methods [:get])

(defroutes app
  (ANY "/" [] anchor-circle-resource)
  (ANY "*/circles" [] collective-circles-resource)
  (ANY "*/circles/:id" [id] (circle-resource id))
  (ANY "*/roles" [] collective-roles-resource)
  (ANY "*/roles/:id" [id] (role-resource id))
  (ANY "*/roles/:id/accountabilities" [id] (accountabilities-resource id))
  (ANY "*/roles/:id/domains" [id] (domains-resource id))
  (ANY "*" [] implicit-circle-resource)
  (route/not-found "<h1>:-(</hi>"))

(def handler 
  (-> app 
    (liberator.dev/wrap-trace :header :ui)))

(def test-server (ref nil))

(defn start-test-server []
  (when-not @test-server
    (dosync
      (ref-set test-server (run-jetty #'handler {:port 3000 :join? false}))))
  (.start @test-server))

(defn stop-test-server []
  (.stop @test-server))

(defn reset-database []
  (dosync
    (ref-set circles {})))
