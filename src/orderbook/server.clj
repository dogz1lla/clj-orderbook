(ns orderbook.server
  (:require [clojure.core.async :as ca]
            [org.httpkit.server :as server]
            [compojure.core :refer [defroutes GET POST routes]]
            [hiccup.core :as hiccup]
            [ring.middleware.params :as rmp]
            [ring.middleware.keyword-params :as rmkp]
            [ring.middleware.resource :as rmr]
            [cheshire.core :as cheshire]
            [orderbook.views :as views]
            [orderbook.utils :as utils]
            [orderbook.order :as order]
            [orderbook.book :as book]))


(defn root-handler
  [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "<h1>Please visit '/bidask' page</h1>"})

(defn chart-handler
  [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (-> (views/chart-view)
             (hiccup/html)
             (str))})

(defn ws-connect-handler
  "Handle a websocket connection request."
  [ring-req]
  (if-not (:websocket? ring-req)
    {:status 200 :body "Welcome to the chatroom! JS client connecting..."}
    (server/as-channel ring-req
      (let [uid (utils/uuid)] ; websocket connection uid
        {:on-receive (fn [ch message])
         :on-close   (fn [ch status] 
                       (println (str "client " uid " disconnected with status " status)))
         :on-open    (fn [ch]
                       (println (str "client " uid " connected"))
                       (let [n 10000
                             gen (order/order-generator n)]
                         (loop [i 0
                                order-book (book/order-book)]
                           (when (< i (dec n))
                             ; HACK: if channel returns nil for whatever reason
                             ; where we still expect it to return a valid order
                             ; we replace nil with the placeholder order that
                             ; has quantity = 0 which would make process-order
                             ; return the order-book without changes
                             (let [new-order (or (ca/<!! gen) (order/new-order :buy 0.0 0))
                                   new-order-book (book/process-order new-order order-book)
                                   new-order-book-data (book/order-book->data new-order-book)]
                               (server/send! ch (cheshire/generate-string new-order-book-data))
                               (recur (inc i) new-order-book))))))}))))

(defroutes app-routes
  ;; rest api
  (GET "/" request (root-handler request))
  (GET "/bidask" request (chart-handler request))
  (GET "/ws/connect" request (ws-connect-handler request)))

;; server 
(defonce server (atom nil))

(def app
  (-> app-routes
      (rmr/wrap-resource "public")
      rmkp/wrap-keyword-params
      rmp/wrap-params))

(defn start-server [port]
  (println "Starting server")
  (reset! server (server/run-server #'app {:port port :join? false})))

(defn stop-server []
  (when-not (nil? @server)
    (println "Stopping server")
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))
