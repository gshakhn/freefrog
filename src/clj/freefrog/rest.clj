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
            [compojure.core :refer [defroutes ANY]])
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

(defn role-processable? [context]
  (if (#{:post :get} (get-in context [:request :request-method]))
    (let [circle-name (get-circle-name-from-uri 
                        (get-in context [:request :uri]) 2)]
      (if-let [curr-circle (get @circles circle-name)]
        (if (#{:post} (get-in context [:request :request-method]))
          (let [{:keys [name purpose domains accountabilities]} (get context ::json-data)]
            (try
              [true {::circle-data (g/add-role curr-circle name purpose domains accountabilities)
                     ::roles (:roles curr-circle) 
                     ::role-id (url-encode name) 
                     ::circle circle-name}]
               (catch IllegalArgumentException e
                 ;(.printStackTrace e)
                 [false {:message 
                         (format "IllegalArgumentException: %s" (.getMessage e))}])))
          {::roles (:roles curr-circle) ::circle circle-name})
        [false {:message (format "Circle '%s' does not exist." circle-name)}]))
    [true]))

(defn role-exists? [context role-id]
  (let [circle-name (get-circle-name-from-uri 
                      (get-in context [:request :uri]) 3)]
    (if-let [curr-role 
             (get-in @circles 
                     [circle-name :roles (url-decode role-id)])]
      [true {::role curr-role}])))

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

(defn create-circle [context]
  (prn "Create circle!")
  (let [{:keys [name lead-link-name lead-link-email]} (::json-data context)]
    (try
      (dosync
        (alter circles assoc name 
               (g/anchor-circle name lead-link-name lead-link-email)))
      {::circle-data (get @circles name) ::url-encoded-id (url-encode name)}
      (catch IllegalArgumentException e
        ;(.printStackTrace e)
        {::create-failed (format "IllegalArgumentException: %s" (.getMessage e))}))))

(defresource circle-resource [id]
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
  :allowed-methods [:get]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? #(role-exists? % role-id)
  :available-media-types ["application/json"]
  :handle-ok ::role
  :malformed? #(malformed-json? %)
  :can-put-to-missing? false)

(defresource collective-roles-resource
  :exists? (fn [_] true)
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? (fn [ctx] (malformed-json? ctx))
  :processable? (fn [ctx] (role-processable? ctx))
  :post! #(dosync (alter circles assoc (::circle %) (::circle-data %)))
  :post-redirect? true
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys (get-in % [::circle :roles])))
  :location (fn [ctx] {:location (str (:uri (:request ctx)) "/" (::role-id ctx))}))

(defroutes app
  (ANY "*/c" [] collective-circles-resource)
  (ANY "*/c/:id" [id] (circle-resource id))
  (ANY "*/r" [] collective-roles-resource)
  (ANY "*/r/:id" [id] (role-resource id))
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
