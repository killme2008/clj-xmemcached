# clj-xmemcached

An opensource memcached client for clojure wrapping [xmemcached](http://code.google.com/p/xmemcached/). [Xmemcached](http://code.google.com/p/xmemcached/) is an opensource high performance memcached client for java.

##Leiningen Usage

To include clj-xmemcached,add:

   		 [clj-xmemcached "0.1.1"]

to your project.clj.

## Usage

### Create a client

    (use [clj-xmemcached.core])
	(def client (xmemcached "host:port"))
	(def client (xmemcached "host1:port1 host2:port2" :protocol "binary"))

Then we create a memcached client using binary protocol to talk with memcached servers host1:port1 and host2:port2.
Valid options including:

	  :name  Client's name
	  :protocol  Protocol to talk with memcached,a string value in text,binary or kestrel,default is text protocol.
	  :hash  Hash algorithm,a string value in consistent or standard,default is standard hash.
	  :timeout Operation timeout in milliseconds,default is five seconds.
	  :pool  Connection pool size,default is one.

### Store items

	(xset client "key" "dennis")
	(xset client "key" "dennis" 100)
	(xappend client "key" " zhuang")
	(xprepend client "key" "hello,")

The value 100 is the expire time for the item in seconds.Store functions include xset,xadd,xreplace,xappend and xprepend.Please use doc to print documentation for these functions.

### Get items

	(xget client "key")
	(xget client "key1" "key2" "key3")
	(xgets client "key")

xgets returns a value including a cas value,for example:

	  {:value "hello,dennis zhuang", :class net.rubyeye.xmemcached.GetsResponse, :cas 396}

And bulk get returns a HashMap contains existent items.

### Increase/Decrease numbers

	(xincr client "num" 1)
	(xdecr client "num" 1)
	(xincr client "num" 1 0)

Above codes try to increase/decrease a number in memcached with key "num",and if the item is not exists,then set it to zero.

### Delete items

	(xdelete client "num")

### Compare and set

	(xcas client "key" inc)

We use inc function to increase the current value in memcached and try to compare and set it at most Integer.MAX_VALUE times.
xcas can be called as:

	 (xcas client key cas-fn max-times)

The cas-fn is a function to return a new value,set the new value to 

	(cas-fn current-value)

### Shutdown

	(xshutdown client)

### Flush

	(xflush client)
	(xflush client (InetSocketAddress. host port))

### Statistics

	(xstats client)

### Example

Please see the example code in [example/demo.clj](https://github.com/killme2008/clj-xmemcached/blob/master/example/demo.clj)

## License

Copyright (C) 2011-2014 dennis zhuang[killme2008@gmail.com]

Distributed under the Eclipse Public License, the same as Clojure.
