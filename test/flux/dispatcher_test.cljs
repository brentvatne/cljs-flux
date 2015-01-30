(ns flux.dispatcher-test
  (:require-macros [cemerick.cljs.test :refer [is deftest testing done]]
                   [cljs.core.async.macros :refer [go go-loop]])
  (:require [cemerick.cljs.test :as t]
            [cljs.core.async :as async :refer [<! timeout]]
            [flux.dispatcher :as dispatcher :refer [dispatch! stream]]))

;; TODO: Unregister all in the teardown

(deftest ^:async streaming-and-wait-for
  (let [store (atom {})
        validate (fn [] (is (= {:foo "FOO" :bar "BAR"} @store)))]
    (testing "invoking all registered callbacks"
      (stream :p1 :foobar (fn [_] (swap! store assoc :foo "FOO")))
      (stream :p2 :foobar (fn [_] (swap! store assoc :bar "BAR")))
      (stream :p3 :foobar (fn [_] (validate) (done)) {:wait-for [:p1 :p2]})
      (dispatch! :foobar {}))))

(deftest ^:async wait-for-executes-in-order
  (let [store (atom "")
        validate (fn [] (is (= "abcd" @store)))]
    (testing "invoking all registered callbacks"
      (stream :d    :abcd (fn [_] (reset! store (str @store "d"))) {:wait-for [:c]})
      (stream :c    :abcd (fn [_] (reset! store (str @store "c"))) {:wait-for [:b]})
      (stream :a    :abcd (fn [_] (reset! store (str @store "a"))))
      (stream :b    :abcd (fn [_] (reset! store (str @store "b"))) {:wait-for [:a]})
      (stream :test :abcd (fn [_] (validate) (done)) {:wait-for [:d]})
      (dispatch! :abcd {}))))

(deftest ^:async wait-for-async
  (let [store (atom {})
        start-time (.now js/Date)]
    (testing "invoking all registered callbacks"
      (stream :sync :add-user
        (fn [{:keys [id done!]}] (go (<! (timeout 1000))
                                    (swap! store assoc :id 1)
                                    (done!))) {:async true})
      (stream :alerts :add-user
        (fn [] (is (= ({:id 1} @store)))
              (> (- start-time (.now js/Date)) 1000)
              (done))
        {:wait-for :sync})
      (dispatch! :add-user {:id 1}))))

(deftest ^:async applies-transducers
  (let [store (atom [])
        ;; Should not have to keep the tag alive! Should only operate on the
        ;; data that is passed in
        pos->vec (fn [[tag {:keys [x y] :as m}]] [tag (assoc m :v [x y])])
        mouse-pos {:x 100 :y 100}]
    (testing "invoking all registered callbacks"
      (stream :log-mouse :mouse-move (map pos->vec)
              (fn [{:keys [v]}] (swap! store conj v)))
      (stream :verify :mouse-move
              (fn [_]
                (is (= @store [[100 100]]))
                (done)) {:wait-for :log-mouse})
      (dispatch! :mouse-move mouse-pos))))
