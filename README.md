# clj-xmemcached

An opensource memcached client for clojure wraps [xmemcached](http://code.google.com/p/xmemcached/). [Xmemcached](http://code.google.com/p/xmemcached/) is an opensource high performance memcached client for java.

##Leiningen Usage

To use clj-xmemcached,add:

```
	[clj-xmemcached "0.2.4"]
```
to your project.clj.

##API docs

[clj-xmemcached APIs](http://fnil.net/docs/clj-xmemcached/)

## Usage

### Client
```clj
    (require '[clj-xmemcached.core :as xm])
	(def client (xm/memcached "host:port"))
	(def client (xm/memcached "host1:port1 host2:port2" :protocol :binary))
```
We create a memcached client using text protocol by default,but we can create a client using binary protocol.

Also,we can create a client using consistent hash and binary protocol:
```clj
	(def client (xm/memcached "host1:port1 host2:port2" :protocol :binary :hash :consistent))
```
All valid options:

	 :protocol  Protocol to talk with memcached,a keyword in :text,:binary or :kestrel,default is text.
     :hash  Hash algorithm,a keyword in  :consistent, :standard or :php, default is standard hash.
     :pool  Connection pool size,default is 1,it's a recommended value.
     session-locator memcached connection locator,default is created based on :hash algorithm value.
     :sanitize-keys  Whether to sanitize keys before operation,default is false.
     :reconnect  Whether to reconnect when connections are disconnected,default is true.
     :heartbeat  Whether to do heartbeating when connections are idle,default is true.
     :timeout  Operation timeout in milliseconds,default is five seconds.
     :transcoder Transcoder to encode/decode data. For example, clj-json-transcoder.
     :name  A name to define a memcached client instance"

### Store items
```clj
	(xm/with-client client
	    (xm/set "key1" "dennis")
        (xm/set "key2" "dennis" 1)
        (xm/add "key3" "dennis")
		;;touch the key3 expire timeout to 3 seconds.
		(xm/touch "key3" 2))
```
Use `with-client` macro to bind the client for following operations.
The value `100` in `set` is the expire timeout for the item in seconds.Storing item functions include `set`,`add`,`replace`,`touch`,`append` and `prepend`.

Unless you need the added flexibility of specifying the client for each request,you can save some typing with a little macro:

```clj
	(defmacro wxm
	    [& body]
	    `(xm/with-client client ~@body))
```

If you have only one client in your application, you can set the global client by:
```clj
	(xm/set-client! client)
```
Then all the following requests will use the global client by default,except you bind another client using `with-cliet` macro.

### Get items
```clj
    ;;get k1 k2 k3...
    (wxm
		(xm/get "key1")
		(xm/get "key1" "key2" "key3")
		(xm/gets "key1"))
```
Using `get` to retrieve items from memcached.You can retrieve many items at one time by bulk getting,but it's result is a `java.util.HashMap`.
`gets` returns a clojure map contains cas and value,for example:
```clj
	  {:value "hello,dennis zhuang", :cas 396}
```
### Increase/Decrease numbers
```clj
	;;incr/decr key delta init-value
	(wxm
		(xm/incr "num" 1)
		(xm/decr "num" 1)
		(xm/incr "num" 1 0)
```
Above codes try to increase/decrease a number in memcached with key "num",and if the item is not exists,then set it to zero.

### Delete items
```clj
	(xm/delete "num")
	;;delete with CAS in binary protocol.
	(xm/delete "num" (:cas (gets "num")))
```
### Compare and set
```clj
	(xm/cas "key" inc)
```
We use `inc` function to increase the current value in memcached and try to compare and set it at most Integer.MAX_VALUE times.
`cas` can be called in:
```clj
	 (xm/cas key cas-fn max-times)
```
The cas-fn is a function to return a new value,set item's new value to:
```clj
	(cas-fn current-value)
```

### Distribution Lock

Use memcached as a lightweight distribution lock:

```clj
	(def counter (atom 0))
	(future (try-lock "lock" 5 (do (Thread/sleep 3000)
	                               (swap! counter inc))
							    (println "else1")))
	(future (try-lock "lock" 5 (do (Thread/sleep 3000)
	                               (swap! counter inc))
							    (println "else2")))
	(future (try-lock "lock" 5 (do (Thread/sleep 3000)
	                               (swap! counter inc))
							    (println "else3")))

	(Thread/sleep 4000)
    (is (nil? (get "lock")))
    (is (= 1 @counter))
```

### through macro


```clj
	(through uid (get-user-from-databse uid))
```

Equals to:

```clj
	(if-let [rt (get uid)]
		rt
	    (let [rt (get-user-from-database uid)]
			(when rt
				(add uid rt 0))
			rt))
```


### Shutdown
```clj
	(xm/shutdown)
```
### Flush
```clj
	(xm/flush-all)
```
### Statistics
```clj
	(xm/stats)
```

### Get the raw XmemcachedClient instance
Because `memcached` function returns a delayed object,so if you want to get the raw `XmemcachedClient` instance,you have to deref it:
```clj
	@client
	(xm/shutdown @client)
```

### Transcoders

We use [SerializationTranscoder](http://xmemcached.googlecode.com/svn/trunk/apidocs/net/rubyeye/xmemcached/transcoders/SerializingTranscoder.html) by default,it will encode/decode values using java serialization.
But since `0.2.2`, we provide a new transcoder `clj-json-transcoder` to encode/decode values using [clojure.data.json](https://github.com/clojure/data.json).It is suitable to integrate with other systems written in other languages.

And we add `nippy-transcoder` in 0.2.3, it use [nippy](https://github.com/ptaoussanis/nippy) for serialization.

### Example

Please see the example code in [example/demo.clj](https://github.com/killme2008/clj-xmemcached/blob/master/example/demo.clj)

## Performance:
Benchmark on my machine:

	CPU 2.3 GHz Intel Core i5
	Memory 8G 1333 MHz DDR3
	JVM options: default

Start memcached by:

	memcached -m 4096 -d

Benchmark result using text protocol and 1 NIO connection:

	Benchmark set: threads=50,repeats=10000,total=500000
	Elapsed time: 10990.256 msecs
	Benchmark get: threads=50,repeats=10000,total=500000
	Elapsed time: 7768.998 msecs
	Benchmark set & get: threads=50,repeats=10000,total=500000
	Elapsed time: 18445.409 msecs

That it is:

    45495 sets/secs
    64350 gets/secs
	27114 tps (both set and get an item in one round)


## License

Copyright (C) 2011-2014 dennis zhuang[killme2008@gmail.com]

Distributed under the Eclipse Public License, the same as Clojure.
