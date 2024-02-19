(ns orderbook.order
  (:require [clojure.core.async :as async]
            [orderbook.utils :as utils]))


(defn new-order
  [side p q]
  {:side side :price p :quantity q})

(defn order-generator
  "Generate a random order n times; put it on a channel, return channel."
  [n]
  {:pre [(pos? n)]}
  ; use a sliding buffer to emulate a continious stream of incoming orders
  (let [out (async/chan (async/sliding-buffer 1))]
  ;(let [out (async/chan)]
    (async/go
      (loop [counter 0]
        (if (< counter n)
          (let [sleep (+ 1 (rand-int 10))
                side (if (< 0 (rand-int 2)) :buy :sell)
                price (Double/parseDouble (format "%.2f" (utils/uniform-random 80.0 120.0)) ) 
                quantity (+ 10 (rand-int 100))]
            (do
              (Thread/sleep sleep)
              (async/>! out (new-order side price quantity))
              (recur (inc counter))))
        (async/close! out))))
    out))
