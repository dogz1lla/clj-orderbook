(ns orderbook.main
  (:require [clojure.core.async :as ca]
            [orderbook.server :as ws]))


(defn -main
  "Start the websocket server."
  [& args]
  (let [port (Integer/parseInt (or (System/getenv "CLJ_WEBSERVER_PORT") "3000"))]
    (ws/start-server port)))

(comment
  (ws/start-server 3000)
  (ws/stop-server))
