# Description
Simple model of how order book looks like and operates on an exchange.

# Motivation
This project is part of the series of small projects that i use to learn clojure.
Each project aims to teach me one (or maybe more but not many) features/patterns
that could be found in a real-life clojure codebase.

In this one i wanted to tackle `clojure.core.async` (see also this [chapter](https://www.braveclojure.com/core-async/) in CftBaT)
 tools and in particular clojure channels.

# Model assumptions
- only limit orders;
- limit order prices are uniformly distributed in [80.0, 120.0];
- limit order quantities are uniformly distributed in [10, 110);
- new orders arrive one after another with a random delay (drawn from [1, 10] ms);
- each order has 50% chance of being either buy or sell;

# Visualization
To see a real time simulation visualization run `clj -M -m orderbook.main` from
the root of the project and then navigate to `localhost:3000/bidask` in your browser.
This will trigger a simulation of 10k orders and you will see a bar chart of 
bids and asks.

# Implementation details
- order book is a map with two keys: `:bid` and `:ask`; each key holds a sorted
map as its value; sorted maps contain prices for keys and volumes for values. 
Sorted maps use `>` as the comparator for bids and `<` for asks. This way best
bid and best ask are the first element in their respective maps at any time.
- a limit order is a map with `:side` (buy or sell), `:price` (limit price) and 
`:quantity` as keys;
- order book is updated with a new order depending on order's side, price and 
quantity; if there is a (partial) fill possible then it will be reflected in the
updated value of the order book;
- visualization is achieved using [chart.js](https://www.chartjs.org/); a simple 
websocket server is running on the backend and once a client connects to it it 
starts sending JSONs of order book state back to the client which is then shown
on the bar chart;
- orders are generated in an async `go` block and delivered to the websocket 
client through an async `chan`.
