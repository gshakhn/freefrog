(ns freefrog.rest
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [freefrog.governance :as g]
            [clojure.walk :refer [keywordize-keys]]
            [clj-json.core :as json]
            [clojure.java.io :as io]
            [compojure.core :refer [defroutes ANY]])
  (:use ring.adapter.jetty)
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
          [false data])
        [true {:message "No body"}])
      (catch Exception e
        (.printStackTrace e)
        [true {:message (format "IOException: %s" (.getMessage e))}]))))

(defn get-circle [circle-id]
  (let [c (get @circles circle-id)]
    (if-not (nil? c)
      {::circle c})))

(defn create-circle [context key]
  (when (#{:post} (get-in context [:request :request-method]))
    (let [[malformed? ret-data] (malformed-json? context)]
      (if malformed?
        ret-data
        (let [{:keys [name lead-link-name lead-link-email]} ret-data]
          (try
            [false {key (g/anchor-circle name lead-link-name lead-link-email)}]
            (catch IllegalArgumentException e
              (.printStackTrace e)
              [true {:message 
                     (format "IllegalArgumentException: %s" (.getMessage e))}])))))))

(defn create-role [circle-id context key]
  (when (#{:post} (get-in context [:request :request-method]))
    (let [[malformed? ret-data] (malformed-json? context)]
      (if malformed?
        ret-data
        (if-let [circle (::circle (get-circle circle-id))]
          (let [{:keys [name purpose domains accountabilities]} ret-data]
            (try
              [false {key (g/add-role circle name purpose domains accountabilities)}]
              (catch IllegalArgumentException e
                (.printStackTrace e)
                [true {:message 
                       (format "IllegalArgumentException: %s" (.getMessage e))}])))
          [true {:message (format "IllegalArgumentException: Circle %s does not exist."
                                  circle-id)}])))))

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

(defresource circle-resource [id]
  :allowed-methods [:get :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [_]
             (let [e (get @circles id)]
               (if-not (nil? e)
                 {::circle e})))
  :existed? (fn [_] (nil? (get @circles id ::sentinel)))
  :available-media-types ["application/json"]
  :handle-ok ::circle
  ;:delete! (fn [_] (dosync (alter circles assoc id nil)))
  :malformed? #(malformed-json? %)
  :can-put-to-missing? false
  :put! #(dosync (alter circles assoc id (::data %)))
  :new? (fn [_] (nil? (get @circles id ::sentinel))))

(defresource collective-circles-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? #(create-circle % ::data)
  :post! #(let [id (str (inc (rand-int 100000)))]
            (dosync (alter circles assoc id (::data %)))
            {::id id})
  :post-redirect? true
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys @circles))
  :location (fn [ctx] {:location (format "/circles/%s" (::id ctx))}))

(defresource role-resource [circle-id role-id]
  :allowed-methods [:get :put :delete]
  :known-content-type? #(check-content-type % ["application/json"])
  :exists? (fn [_]
             (if-let [e (get-in @circles [circle-id role-id])]
               {::circle e}))
  :existed? (fn [_] (nil? (get @circles circle-id ::sentinel)))
  :available-media-types ["application/json"]
  :handle-ok ::circle
  ;:delete! (fn [_] (dosync (alter circles assoc id nil)))
  :malformed? #(malformed-json? %)
  :can-put-to-missing? false
  ;:put! #(dosync (alter circles assoc id (::data %)))
  :new? (fn [_] (nil? (get @circles circle-id ::sentinel))))

(defresource collective-roles-resource [circle-id]
  :exists? (fn [_] (get-circle circle-id))
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? #(create-role circle-id % ::data)
  :post! #(let [id (str (inc (rand-int 100000)))]
            (println %)
            (println @circles)
            (dosync (alter circles assoc circle-id (::data %)))
            (println @circles)
            {::id id})
  :post-redirect? true
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys (get-in % [::circle :roles])))
  :location (fn [ctx] {:location (format "/circles/%s/roles/%s" circle-id (::id ctx))}))

(defroutes app
  (ANY "/circles" [] collective-circles-resource)
  (ANY "/circles/:id" [id] (circle-resource id))
  (ANY "/circles/:circle-id/roles" [circle-id] 
       (collective-roles-resource circle-id))
  (ANY "/circles/:circle-id/roles/:role-id" [circle-id role-id] 
       (role-resource circle-id role-id)))

(def handler 
  (-> app 
    wrap-params))

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
