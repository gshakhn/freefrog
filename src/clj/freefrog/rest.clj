(ns freefrog.rest
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
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
;; ;; under the given key.
(defn parse-json [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (json/parse-string body)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

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
  :delete! (fn [_] (dosync (alter circles assoc id nil)))
  :malformed? #(parse-json % ::data)
  :can-put-to-missing? false
  :put! #(dosync (alter circles assoc id (::data %)))
  :new? (fn [_] (nil? (get @circles id ::sentinel))))

(defresource circles-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :malformed? #(parse-json % ::data)
  :post! #(let [id (str (inc (rand-int 100000)))]
            (println @circles)
            (println %)
            (println (:body (:request %)))
            (println (::data %))
            (dosync (alter circles assoc id (::data %)))
            (println @circles)
                 {::id id})
  :post-redirect? true
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys @circles))
  :location (fn [ctx] {:location (format "/circles/%s" (::id ctx))}))

(defroutes app
  (ANY "/circles/:id" [id] (circle-resource id))
  (ANY "/circles" [] circles-resource))

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
