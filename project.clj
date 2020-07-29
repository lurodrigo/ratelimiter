(defproject lurodrigo/ratelimiter "0.1.3"
  :description "Clojure wrapper for Resilience4j's Rate Limiter."
  :url "http:/github.com/lurodrigo/ratelimiter"
  :license {:name "MIT License"
            :url  "https://raw.githubusercontent.com/lurodrigo/ratelimiter/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [io.github.resilience4j/resilience4j-ratelimiter "1.5.0"]]
  :profiles {:test {:dependencies [[lambdaisland/kaocha "1.0.641"]]}
             :dev  {:dependencies [[com.fzakaria/slf4j-timbre "0.3.14"]
                                   [com.taoensso/timbre "4.10.0"]]}}
  :repl-options {:init-ns rate-limiter.core}
  :scm {:name "git"
        :url  "https://github.com/lurodrigo/ratelimiter"})
