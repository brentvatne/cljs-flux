# Flux in ClojureScript

A naïve core.async-based implementation of a Flux-style dispatcher.
But I like it. Heavily inspired by [Justin Tulloss' blog post](https://justin.harmonize.fm/development/2014/08/05/om-and-flux.html), with additional features such as `wait-for` and wildcard matching.

## Use it

Add the following to your project dependencies:
```clojure
[ca.brentvatne/flux "0.1.0"]
```

Then require it where necessary:

```clojure
(ns app.core
  (:require [flux.dispatcher :as dispatcher]))
```

## Example

```clojure
;; You would probably use this as a response to user input,
;; but for simplicity let's dispatch an action right away
(dispatch! :country-update {:country "Canada"})

;; Partial application saves us specifying the id each time
(def stream (partial dispatcher/stream :country-store)

;; Subscribes handlers (last argument) to the given tags
(stream :select-continent update-available-options!)
(stream :country-update update-state!)
(stream :clear-address-form clear-state!)

;; Declaratively express order depdendencies
(def stream (partial dispatcher/stream :city-store)
(stream :country-update filter-by-country! {:wait-for :country-store})
(stream :clear-address-form clear-state! {:wait-for [:some-thing :other-thing]})

;; Decouple backend synchronization logic
(def stream (partial dispatcher/stream :sync))
(stream :submit-form api/save-state {:wait-for :validator})

;; The wildcard matcher is streamed all actions, as you would expect
(dispatcher/stream :* (fn [data] (.log js/console data)))

;; Apply transducers to the streams
(defn mouse-loc->vec [e] [(.-clientX e) (.-clientY e)])
(def max-height 900)
(def min-height 100)
(dispatcher/stream :mouse-move
  (comp (map mouse-loc->vec)
        (filter [_ y] (or (> y max-height) (< y min-height))))
  (fn [[x y]] (.log js/console (str "valid mouse location x: " x ", y: " y)))
  {:wait-for :other-process})

;; Waits for the handler to finish executing any go blocks, so async
;; calls can be performed
(def store (atom {}))

(stream :sync :add-user
  (fn [{:keys [id done!]}]
    (go (swap! store assoc :id (<! (http/post "/users/")))
        (done!)))
  {:async true})

;; Show the alert once the user id has been persisted
(stream :alerts :add-user
  (fn [] (js/alert (str "Added user with id: " (:id @store))))
  {:wait-for :sync})

(dispatch! :add-user {:id 1}))))
```

## Related Projects

[Kyle Gann's cljs-flux](https://github.com/kgann/cljs-flux) is more
similar to the original JavaScript implementation of `dispatcher.js`
than this project.
