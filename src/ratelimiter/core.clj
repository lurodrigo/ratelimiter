(ns ratelimiter.core
  (:import
    (io.github.resilience4j.ratelimiter RateLimiter
                                        RateLimiterConfig
                                        RateLimiterRegistry
                                        RateLimiterConfig$Builder)

    (io.github.resilience4j.ratelimiter.event RateLimiterEvent$Type
                                              RateLimiterEvent
                                              RateLimiterOnFailureEvent
                                              RateLimiterOnSuccessEvent)

    (io.github.resilience4j.core EventConsumer)

    (io.vavr.control Try)

    (java.time Duration)
    (io.github.resilience4j.ratelimiter.internal SemaphoreBasedRateLimiter)))

(defn ^:private get-failure-handler [{:keys [fallback]}]
  (if fallback
    (fn [& args] (apply fallback args))
    (fn [& args] (throw (-> args first :cause)))))

(defn ^:private config-data->rate-limiter-config
  ^RateLimiterConfig [{:keys [limit-refresh-period limit-for-period timeout-duration]}]
  (.build ^RateLimiterConfig$Builder
          (cond-> (RateLimiterConfig/custom)
                  limit-refresh-period (.limitRefreshPeriod (Duration/ofMillis limit-refresh-period))
                  limit-for-period (.limitForPeriod limit-for-period)
                  timeout-duration (.timeoutDuration (Duration/ofMillis timeout-duration)))))

(defn ^:private rate-limiter-config->config-data
  [^RateLimiterConfig rl-config]
  {:limit-refresh-period (.getLimitRefreshPeriod rl-config)
   :limit-for-period     (.getLimitForPeriod rl-config)
   :timeout-duration     (.getTimeoutDuration rl-config)})

(defmulti ^:private event->data
          (fn [^RateLimiterEvent e]
            (-> e .getEventType .toString keyword)))

(defn ^:private base-event->data
  [^RateLimiterEvent e]
  {:creation-time     (.getCreationTime e)
   :rate-limiter-name (.getRateLimiterName e)
   :event-type        (.getEventType e)})

(defmethod event->data :default
  [e]
  (base-event->data e))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public functions
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create
  ([n]
   (create n nil))
  ([^String n opts]
   (if opts
     (RateLimiter/of ^String n ^RateLimiterConfig (config-data->rate-limiter-config opts))
     (RateLimiter/ofDefaults n))))

(defn decorate
  ([f ^RateLimiter breaker]
   (decorate f breaker nil))
  ([f ^RateLimiter breaker {:keys [effect permits semaphore-based?] :as opts}]
   (fn [& args]
     (let [callable           (reify Callable (call [_] (apply f args)))
           decorated-callable (if permits
                                (RateLimiter/decorateCallable breaker permits callable)
                                (RateLimiter/decorateCallable breaker callable))
           failure-handler    (get-failure-handler opts)
           result             (Try/ofCallable decorated-callable)]
       (if (.isSuccess result)
         (let [out (.get result)]
           (if effect
             (future (apply effect (conj args out)))
             out))
         (let [args' (-> args (conj {:cause (.getCause result)}))]
           (apply failure-handler args')))))))

(defn metrics
  [^RateLimiter rate-limiter]
  (let [metrics (.getMetrics rate-limiter)]
    {;; estimate of the count of available permissions.
     :available-permissions
     (.getAvailablePermissions metrics)

     ;;  estimate of the number of threads waiting for permission.
     :number-of-waiting-threads
     (.getNumberOfWaitingThreads metrics)}))

(defn config
  [^RateLimiter r]
  (-> r
      .getRateLimiterConfig
      rate-limiter-config->config-data))

(defn change
  [^RateLimiter r {:keys [limit-for-period timeout-duration]}]
  (when limit-for-period
    (.changeLimitForPeriod r limit-for-period))
  (when timeout-duration
    (.changeTimeoutDuration r timeout-duration)))

(comment

  (def rate-limiter (create "my-ratelimiter" {:limit-for-period     3
                                              :limit-refresh-period 3000
                                              :timeout-duration     3000}))

  (defn my-print
    [msg]
    (println (str "At " (java.time.LocalDateTime/now) ": " msg "\n")))

  (def dprint (decorate my-print rate-limiter))

  (doseq [x ["A" "B" "C" "D" "E" "F" "G" "H" "I"]]
    (dprint x))

  ; parallel example
  (import io.github.resilience4j.ratelimiter.RequestNotPermitted)

  (doseq [x ["A" "B" "C" "D" "E" "F" "G" "H" "I"]]
    (future
      (try
        (dprint x)
        (catch RequestNotPermitted e
          (my-print (.getMessage e))))))

  (def last-price (atom 10000))

  ; fallback example

  (def rate-limiter (create "my-ratelimiter" {:limit-for-period     10
                                              :limit-refresh-period 1000
                                              :timeout-duration     0}))

  (def last-price (atom 10000))

  (defn call-external-service
    "Flutuates between 99.99% and 100.01% of the last price."
    []
    (* @last-price (+ 0.9999 (rand 0.0002))))

  (defn get-price []
    (let [price (call-external-service)]
      (reset! last-price price)
      price))

  (def get-price (decorate get-price rate-limiter {:fallback (fn [e]
                                                               @last-price)}))

  (dotimes [i 15]
    (printf "call %2d: %.2f\n" i (get-price)))

  ; TODO study the differences between the implementation of print and println

  (metrics rate-limiter)

  )