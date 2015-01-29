(ns flux.dispatcher
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [cljs.core.async :as async :refer [chan put! pub sub unsub mult tap <!]]))

(def dispatch-chan (chan))
(def dispatch-chan-mult (mult dispatch-chan))
(def dispatch-pub (pub (tap dispatch-chan-mult (chan)) first))
(defn register [tag] (sub dispatch-pub tag (chan)))
(defn unregister [tag chan] (unsub dispatch-pub tag chan))

(defmulti stream* (fn [tag & args] tag))

(def wildcard-key :*)
(def dispatch-pub-wildcard (pub (tap dispatch-chan-mult (chan)) (constantly wildcard-key)))
(defn register-wildcard [] (sub dispatch-pub-wildcard wildcard-key (chan)))
(defmethod stream* wildcard-key [_ handler & args]
  (let [this-chan (register-wildcard)]
    (go-loop []
      (let [[tag data] (<! this-chan)]
        (handler [tag data]))
      (recur))))

(defmethod stream* :default [tag handler options]
  (let [this-chan (register tag)
        wait-for-ids (->> [(get options :wait-for [])] (flatten) (set))]
    (go-loop []
      (let [[_ data] (<! this-chan)
            complete-chan-tap (tap (:complete-chan-mult data) (chan))]
        ;; TODO: Potential for deadlock here if the id we are waiting for
        ;; never says it's complete, should account for this
        (go-loop [waiting-for wait-for-ids]
           (if (empty? waiting-for)
             (do (handler data)
                 (put! (:complete-chan data) (:dispatch-id options)))
             (recur (disj waiting-for (<! complete-chan-tap))))))
      (recur))))

(defn stream
  "Streams actions that are dispatched for the given tag to be handled
  by the given handler function.

  id - optional but should be provided in most cases, it identifies this handler
  so that consumers of the stream are able to wait for it to complete if necessary.

  tag - a keyword that describes the action, for example :country-update.

  handler - a function of 1 argument, a map of data passed in by the user
  merged with :complete-chan and :complete-chan-mult.

  options - a map where the following keys are allowed:

    :wait-for - A keyword or vector of keywords that specify what other consumers of
                the tag stream should run before this handler.

    :max-wait - Number of milliseconds that we should wait on the wait-for list before
                just executing the handler anyways. (NOT IMPLEMENTED)

  Example:

    ;; In a country store namespace
    (def stream (partial dispatcher/stream :country-store)
    (stream :country-update update-country-state)
    (stream :clear-address-form clear-country-state)

    ;; In a city store namespace
    (def stream (partial dispatcher/stream :city-store)
    (stream :country-update update-city-state {:wait-for :country-store})
    (stream :clear-address-form clear-city-state) "
  ([tag handler]
   (stream* tag handler {}))
  ([id tag handler]
   (stream id tag handler {}))
  ([id tag handler options]
    (stream* tag handler (merge {:dispatch-id id} options))))

(defn dispatch! [tag data]
  "Puts the given data on the dispatch channel to published to all those consumers
  that are streaming it.

  tag - a keyword that describes the action, for example :country-update.

  data - a map of data related to the action, can have any structure that you like,
  but cannot use the keys :complete-chan :complete-chan-mult or :dispatcher-id"
  (let [complete-chan (chan)
        complete-chan-mult (mult complete-chan)]
    (put! dispatch-chan [tag (merge data {:complete-chan complete-chan
                                          :complete-chan-mult complete-chan-mult})])))
