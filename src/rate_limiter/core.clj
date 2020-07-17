(ns rate-limiter.core
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

    (java.time Duration)))

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
  ([f ^RateLimiter breaker {:keys [effect permits] :as opts}]
   (fn [& args]
     (let [callable           (reify Callable (call [_] (apply f args)))
           decorated-callable (if permits
                                (RateLimiter/decorateCallable breaker permits callable)
                                (RateLimiter/decorateCallable breaker 1 callable))
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
  (defn f [] (do
               (println "oi")
               1))
  (def rl (create "rl1" {:limit-for-period 10
                         :limit-refresh-period 10000}))
  (def df (decorate f rl))
  )