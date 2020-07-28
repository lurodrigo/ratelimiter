
[clojars-badge]: https://img.shields.io/clojars/v/lurodrigo/ratelimiter.svg
[clojars]: http://clojars.org/lurodrigo/ratelimiter
[github-issues]: https://github.com/lurodrigo/ratelimiter/issues
[license-badge]: https://img.shields.io/badge/license-MIT-blue.svg
[license]: ./LICENSE
[resilience4clj]: https://github.com/resilience4clj

# Rate Limiter

This library allows you to decorate a function with a rate limiter. This project's structure borrows heavily from
[Resilicience4clj][resilience4clj]. Hopefully it will be merged there soon :)

[![Clojars][clojars-badge]][clojars]

## Getting Started

Add `lurodrigo/ratelimiter` as a dependency to your
`deps.edn` file:

``` clojure
lurodrigo/ratelimiter {:mvn/version "0.1.0"}
```

If you are using `lein` instead, add it as a dependency to your
`project.clj` file:

``` clojure
[lurodrigo/ratelimiter "0.1.0"]
```

Require the library:

``` clojure
(require '[ratelimiter.core :as r])
```

Then create a rate limiter calling the function `create`:

``` clojure
(def rate-limiter (r/create "my-ratelimiter" {:limit-for-period     10
                                              :limit-refresh-period 10000}))
```

Now you can decorate any function you have with the rate limiter you just
defined.

TODO write example

## Rate Limiter Settings

When creating a rate limiter, you can fine tune three of its settings:

1. `:limit-for-period` - The number of permissions available during one limit refresh period. Default is 50.
2. `:limit-refresh-period` - The period of a limit refresh. After each period the rate limiter sets its permissions
 count back to the limit-for-period value. Default is 0.0005 ms (500 ns).
3. `:timeout-duration` - The default wait time a thread waits for a permission. Default value is 5000 (ms). 

These three options can be sent to `create` as a map. In the following
example, any function decorated with `rate-limiter` will be attempted for 10
times with in 300ms intervals.

``` clojure
(def rate-limiter (r/create "my-rate-limiter" {:limit-for-period 10
                                               :limit-refresh-period 10000}))
```

The function `config` returns the configuration of a rate-limiter in case
you need to inspect it. Example:

``` clojure
(r/config rate-limiter)
=> {:limit-refresh-period #object[java.time.Duration 0x5fbf7654 "PT10S"],
    :limit-for-period 10,
    :timeout-duration #object[java.time.Duration 0x6e0113dc "PT5S"]}
```

Using `change`, it's possible to change some of the rate limiter's configs after its
creation. Currently, it's possible to change `limit-for-period` and `timeout-duration`.

```clojure
(r/change rate-limiter {:limit-for-period 20})
```

## Fallback Strategies

TODO

## Effects

TODO

## Metrics

The function `metrics` returns a map with the rate limiter's metrics:

``` clojure
(r/metrics rate-limiter)

=> {:available-permissions 10, 
    :number-of-waiting-threads 0}
```

The nodes should be self-explanatory.

## Events

TODO

## Exception Handling

When a rate limiter exhausts all permits, calls will wait the time specified in `:timeout-duration`. 
If no sufficient permits become available in that period, a `io.github.resilience4j.ratelimiter.RequestNotPermitted/RequestNotPermitted`
exception will be thrown.

## Bugs

If you find a bug, submit a [Github issue][github-issues].

## License

Copyright © 2020 Luiz Rodrigo de Souza

Distributed under the MIT License.
