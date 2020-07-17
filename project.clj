(defproject lurodrigo/rate-limiter "0.1.0-SNAPSHOT"
  :description "Clojure wrapper for Resilience4j's Rate Limiter"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "https://github.com/lurodrigo"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [io.github.resilience4j/resilience4j-ratelimiter "1.5.0"]]
  :profiles {:test [lambdaisland/kaocha "1.0.641"]}
  :repl-options {:init-ns rate-limiter.core})
