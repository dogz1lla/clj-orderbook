(ns orderbook.book
  (:require [orderbook.order :as order]))


(defn priority-queue
  "Wrapper around clojure.core.sorted-map.
  TODO: add pre check of having even number of items"
  ([] (sorted-map-by <))
  ([comparator & items]
   (if (seq items)
     (into (sorted-map-by comparator) (map vec (partition 2 items)))
     (sorted-map-by comparator))))

(defn pq-peek
  "Peek method for a priority queue."
  [pq]
  (first pq))

(defn pq-pop
  "Pop method for a priority queue."
  [pq]
  (dissoc pq (first (first pq))))

(defn pq-add
  "Specifies how to 'add' a new item to the priority queue. If there is an item
  with the same priority in the queue already then update the corresponding key
  with the sum of the value and the existing value.
  NOTE: could in principle add another arity that takes a reduction fn also."
  [pq [k v]]
  (if (get pq k)
    (update pq k + v)
    (assoc pq k v)))

(defn order-book
  "Empty order book.
  Bids are a prio q sorted in descending order, asks are sorted in ascending."
  []
  {:bid (priority-queue >) :ask (priority-queue <)})

(defn process-order
  "Process an incoming order according to the current order book state.
  An order can be a buy or a sell, and it is assumed that it is a limit order.
  Upon arrival it is either filled, or otherwise any unfilled part of the order
  is put into the book.
  Return an updated order book."
  [{:keys [side price quantity] :as order} {:keys [bid ask] :as order-book}]
  (let [comparison-op (if (= side :buy) <= >=)
        exchange (if (= side :buy) ask bid)
        order-queue (if (= side :buy) bid ask)
        exchange-key (if (= side :buy) :ask :bid)
        order-queue-key (if (= side :buy) :bid :ask)]
    (if (pos? quantity)
      (if (and (pq-peek exchange) (comparison-op (first (pq-peek exchange)) price))
        ; there is at least one in exchange
        (let [[best-ask best-ask-q] (pq-peek exchange)]
          (if (> best-ask-q quantity)
            ; fill, no overflow
            (recur
              (assoc order :quantity 0)
              (assoc order-book exchange-key (assoc exchange best-ask (- best-ask-q quantity))))
            (if (= best-ask-q quantity)
              ; fill, remove the top of exchange
              (recur
                (assoc order :quantity 0)
                (assoc order-book exchange-key (pq-pop exchange)))
              ; (partial) fill, overflow
              (recur
                (assoc order :quantity (- quantity best-ask-q))
                (assoc order-book exchange-key (pq-pop exchange))))))
        ; there is nothing left on the exchange
        (assoc order-book order-queue-key (pq-add order-queue [price quantity])))
      ; no unfilled quantity left -> return
      order-book)))

(defn order-book->data
  [{:keys [bid ask]}]
  (let [bid-xy (into [] bid)
        ask-xy (into [] ask)
        labels (sort (set (concat (keys bid) (keys ask))))
        f (fn [v [x y]] (conj v {:x x :y y}))]
    {:labels labels
     :bid (reduce f [] bid-xy)
     :ask (reduce f [] ask-xy)}))

(comment
  (priority-queue > 0 0 1 1)
  (priority-queue)
  (pq-pop (priority-queue > 0 0 1 1))
  (pq-peek (priority-queue < 0 0 1 1))
  (pq-peek (priority-queue > 0 0 1 1))
  (pq-peek (priority-queue))
  (dissoc {} (first (first {})))
  (pq-add (priority-queue > 0 0 1 1) [1 100])
  (pq-add (priority-queue > 0 0 1 1) [2 100])

  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue <)})
  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue < 100.0 50)})
  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue < 90.0 5)})
  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue < 90.0 5 91.0 40)})
  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue < 101.0 50)})
  (process-order (order/new-order :buy 100.0 50) {:bid (priority-queue >) :ask (priority-queue < 101.0 50 90.0 5 91.0 40)})

  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue >) :ask (priority-queue <)})
  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 100.0 50) :ask (priority-queue <)})
  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 90.0 5) :ask (priority-queue <)})
  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 101.0 45) :ask (priority-queue <)})
  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 101.0 40 102.0 5) :ask (priority-queue <)})
  (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 90.0 5 101.0 40 102.0 5) :ask (priority-queue <)})

  (Double/parseDouble (format "%.2f" 114.86080892630645))
  (format "%.3f" 2.0)
  (Double/parseDouble (format "%.2f" (uniform-random 80.0 120.0)))
  (ca/<!! (order-generator))
  (repeatedly 10 (fn [] (uniform-random 1 2)))
  (repeatedly 11 (fn [] (ca/<!! (order-generator 10))))
  ; DONE insert this code into ws :on-open callback (replace println with send!)
  (let [gen (order-generator 10)]
    (loop [i 0]
      (when (< i 11)
        (do
          (println (ca/<!! gen))
          (recur (inc i))))))
  (map #(< 0 %) (repeatedly 10 (fn [] (rand-int 2))))
  (rand-int 2)
  (str {:hi :bye :q 10.0})
  (keys {:hi :bye :q 10.0})
  (into [] (vals {:hi :bye :q 10.0}))

  (reduce (fn [v [x y]] (conj v {:x x :y y})) [] [[1 2] [3 4]])
  (order-book->data (process-order (order/new-order :sell 100.0 50) {:bid (priority-queue > 90.0 5 101.0 40 102.0 5) :ask (priority-queue <)}))
  )
