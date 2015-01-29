# Flux in ClojureScript

A naïve core.async-based implementation of a Flux-style dispatcher.
But I like it.

## Example

```clojurescript
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
(stream :clear-address-form clear-state!)"

(ns app.sync
  (:require [app.api :as api]
            [flux.dispatcher :as dispatcher]))

(def stream (partial dispatcher/stream :sync))
(stream :submit-form api/save-state {:wait-for :validator})

(ns app.logger
  (:require [flux.dispatcher :as dispatcher]))

;; Log all data that flows through the dispatcher
(dispatcher/stream :* (fn [data] (.log js/console data)))
```
