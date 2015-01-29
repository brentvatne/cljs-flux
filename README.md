# Flux in ClojureScript

A naïve core.async-based implementation of a Flux-style dispatcher.
But I like it.

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
(ns app.core
  (:require [flux.dispatcher :refer [dispatch!]))

;; You would probably use this as a response to user input,
;; but for simplicity let's dispatch an action right away
(dispatch! :country-update {:country "Canada"})

(ns app.country-store
  (:require [flux.dispatcher :as dispatcher]))

(def stream (partial dispatcher/stream :country-store)
(stream :select-continent update-available-options!)
(stream :country-update update-state!)
(stream :clear-address-form clear-state!)

(ns app.city-store
  (:require [flux.dispatcher :as dispatcher]))

(defn update-state! [attrs] .... )
(defn clear-state! [attrs] .... )

(def stream (partial dispatcher/stream :city-store)
(stream :country-update update-state! {:wait-for :country-store})
(stream :clear-address-form clear-state!)

(ns app.sync
  (:require [app.api :as api]
            [flux.dispatcher :as dispatcher]))

(def stream (partial dispatcher/stream :sync))
(stream :submit-form api/save-state {:wait-for :validator})

(ns app.logger
  (:require [flux.dispatcher :as dispatcher]))

;; The wildcard matcher is streamed all actions, as you would expect
(dispatcher/stream :* (fn [data] (.log js/console data)))
```
