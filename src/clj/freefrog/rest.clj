(ns freefrog.rest
  (:require [liberator.core :refer [resource defresource]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes ANY]])
  (:use ring.adapter.jetty)
  (:import java.net.URL))

(defonce entries (ref {}))
(defn build-entry-url [request id]
  (URL. (format "%s://%s:%s%s/%s"
                (name (:scheme request))
                (:server-name request)
                (:server-port request)
                (:uri request)
                (str id))))

(defresource circle-resource
  :available-media-types ["application/json"]
  :allowed-methods [:get]
  :handle-ok #(map (fn [id] (str (build-entry-url (get % :request) id)))
                   (keys @entries)))
(defroutes app
  (ANY "/circle" [] circle-resource))

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
