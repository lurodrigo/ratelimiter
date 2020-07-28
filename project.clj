(defproject lurodrigo/ratelimiter "0.1.2-SNAPSHOT"
  :description "Clojure wrapper for Resilience4j's Rate Limiter."
  :url "http:/github.com/lurodrigo/ratelimiter"
  :license {:name "MIT License"
            :url  "https://raw.githubusercontent.com/lurodrigo/ratelimiter/master/LICENSE"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [io.github.resilience4j/resilience4j-ratelimiter "1.5.0"]]
  :profiles {:test [lambdaisland/kaocha "1.0.641"]}
  :repl-options {:init-ns rate-limiter.core}
  :scm {:name "git"
        :url  "https://github.com/lurodrigo/ratelimiter"})
