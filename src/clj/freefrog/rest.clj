(ns freefrog.rest
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]])
  (:use ring.adapter.jetty)
  (:import java.net.URL))

(defonce circles (ref {}))

(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

(defresource circle-resource [id]
  :allowed-methods [:get :put :delete]
  :exists? (fn [_]
             (let [e (get @circles id)]
                    (if-not (nil? e)
                      {::entry e})))
  :existed? (fn [_] (nil? (get @circles id ::sentinel)))
  :available-media-types ["application/json"]
  :handle-created ::entry
  :delete! (fn [_] (dosync (alter circles assoc id nil)))
  :can-put-to-missing? false
  :put! #(dosync (alter circles assoc id (::data %)))
  :new? (fn [_] (nil? (get @circles id ::sentinel))))

(defresource circles-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get :post]
  :post! #(let [id (str (inc (rand-int 100000)))]
            (dosync (alter circles assoc id (::data %)))
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
