(ns orderbook.utils)


(defn uuid
  "Random uuid as a string."
  []
  (str (java.util.UUID/randomUUID)))

(defn uniform-random
  " Return a number uniformly drawn in [a, b)."
  [a b]
  {:pre [(< a b)]}
  (let [l (- b a)]
    (+ a (* l (rand)))))
