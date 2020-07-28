(defproject lurodrigo/ratelimiter "0.1.3-SNAPSHOT"
  :description "Clojure wrapper for Resilience4j's Rate Limiter."
  :url "http:/github.com/lurodrigo/ratelimiter"
  :license {:name "MIT License"
            :url  "https://raw.githubusercontent.com/lurodrigo/ratelimiter/master/LICENSE"}
  :dependencies [[io.github.resilience4j/resilience4j-ratelimiter "1.5.0"]]
  :profiles {:test {:repositories [[lambdaisland/kaocha "1.0.641"]]}
             :dev  {:repositories [[org.clojure/clojure "1.10.1"]]}}
  :repl-options {:init-ns rate-limiter.core}
  :scm {:name "git"
        :url  "https://github.com/lurodrigo/ratelimiter"})
