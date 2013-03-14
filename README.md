# clj-xmemcached

An opensource memcached client for clojure wrapping [xmemcached](http://code.google.com/p/xmemcached/). [Xmemcached](http://code.google.com/p/xmemcached/) is an opensource high performance memcached client for java.

##Leiningen Usage

To use clj-xmemcached,add:
```clj
	[clj-xmemcached "0.2.1"]
```
to your project.clj.

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
     :sanitize-keys  Whether to sanitize keys before operation,default is false.
     :reconnect  Whether to reconnect when connections are disconnected,default is true.
     :heartbeat  Whether to do heartbeating when connections are idle,default is true.
     :timeout  Operation timeout in milliseconds,default is five seconds.
     :transcoder Transcoder to encode/decode data.
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

### Dereference raw XmemcachedClient
Because `memcached` function returns a Delay object,so if you want the raw `XmemcachedClient` instance,you have to deref it:
```clj
	@client
	(xm/shutdown @client)
```

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
