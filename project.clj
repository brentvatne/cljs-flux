(defproject ca.brentvatne/flux "0.1.2"
  :description "A core.async-based implementation of a Flux-style dispatcher."
  :url "https://github.com/brentvatne/flux"
  :license {:name "Eclipse"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :scm {:name "git"
        :url "https://github.com/brentvatne/flux"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2727"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :hooks [leiningen.cljsbuild]

  :plugins [[com.cemerick/clojurescript.test "0.3.3"]
            [lein-cljsbuild "1.0.4"]]

  :aliases {"auto-test" ["do" "clean," "cljsbuild" "auto" "test"]}

  :profiles {:dev {:source-paths ["dev"]
                   :plugins [[com.cemerick/austin "0.1.6"]]}}

  :cljsbuild {:test-commands {"unit-tests" ["phantomjs" :runner "target/testable.js"]}
              :builds {:test {:source-paths ["src" "test"]
                              :notify-command ["phantomjs" :cljs.test/runner "target/testable.js"]
                              :compiler {:output-to "target/testable.js"
                                         :optimizations :whitespace}}
                       :dev {:source-paths ["src"]
                             :compiler {:output-to "target/flux.js"
                                        :optimizations :none}}}})
