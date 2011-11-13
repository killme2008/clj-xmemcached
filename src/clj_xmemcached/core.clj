(ns
	^{:doc "The core clj-xmemcached"
	  :author "Dennis zhuang"}
  clj-xmemcached.core
  (:import (net.rubyeye.xmemcached MemcachedClient MemcachedClientBuilder XMemcachedClient CASOperation XMemcachedClientBuilder)
		   (net.rubyeye.xmemcached.impl KetamaMemcachedSessionLocator ArrayMemcachedSessionLocator PHPMemcacheSessionLocator)
		   (net.rubyeye.xmemcached.utils AddrUtil)
		   (net.rubyeye.xmemcached.command BinaryCommandFactory KestrelCommandFactory TextCommandFactory)
		   (java.net InetSocketAddress))
  (:use 
   [clojure.walk :only [walk]]))

(defn- unquote-options [args]
  (walk (fn [item]
		  (cond (and (seq? item) (= `unquote (first item))) (second item)
				(or (seq? item) (symbol? item)) (list 'quote item)
				:else (unquote-options item)))
		identity
		args))

(defn- make-command-factory [protocol]
  (cond (= protocol "binary") (BinaryCommandFactory.)
		(= protocol "kestrel") (KestrelCommandFactory.)
		:else (TextCommandFactory.)))

(defn- make-session-locator [hash]
  (cond (or (= hash "consistent")
            (= hash "ketama")) (KetamaMemcachedSessionLocator.)
        (= hash "phpmemcache") (PHPMemcacheSessionLocator.)
        :else (ArrayMemcachedSessionLocator.)))

(defn xmemcached
  "Create a memcached client with zero or more options(any order):
    :protocol  Protocol to talk with memcached,a string in \"text\" \"binary\" or \"kestrel\",default is text.

    :hash  Hash algorithm,a string in  \"consistent\", \"standard\" or \"phpmemcache\", default is standard hash.

    :pool  Connection pool size,default is 1

    :timeout  Operation timeout in milliseconds,default is five seconds.

    :name  A name to define a memcached client instance"
  [servers & opts]
  (let [m (apply hash-map (unquote-options opts))
		name (:name m)
		protocol (:protocol m)
		hash (:hash m)
		pool (or (:pool m) 1)
		timeout (or (:timeout m) 5000)
		builder (XMemcachedClientBuilder.  (AddrUtil/getAddresses servers))]
	(.setName builder name)
	(.setSessionLocator builder (make-session-locator hash))
	(.setConnectionPoolSize builder pool)
	(.setCommandFactory builder (make-command-factory protocol))
	(let [rt (.build builder)]
	  (.setOpTimeout rt timeout)
	  rt)))

;;Macro to store item to memcached
(defmacro store
  [method]
  (let [m (symbol method)]
	(if (or (= m (symbol "append")) (= m (symbol "prepend")))
	  ` (fn [^MemcachedClient cli# ^String key# value#] (. cli# ~m key# value#))
		` (fn ([^MemcachedClient cli# ^String key# value#] (. cli# ~m key# 0 value#))
			([^MemcachedClient cli# ^String key# value# ^Integer exp#] (. cli# ~m key# exp# value#))))))


(def 
 ^{:arglists '([client key value] [client key value expire])
   :doc "Set an item with key and value."}
 xset
 (store  "set"))

(def 
 ^{:arglists '([client key value] [client key value expire])
   :doc "Add an item with key and value,success only when item is not exists."}
 xadd
 (store "add"))

(def
 ^{:arglists '([client key value] [client key value expire])
   :doc "Replace an existing item's value by new value"}
 xreplace
 (store "replace"))

(def
 ^{:arglists '([client key value])
   :doc "Append a string to an existing item's value by key"}
 xappend
 (store "append"))

(def 
 ^{:arglists '([client key value])
   :doc "Prepend a string to an existing item's value by key"}
 xprepend
 (store "prepend"))

(defn xget
  "Get items by keys"
  [^MemcachedClient cli key & keys]
  (if (empty? keys)
	(.get cli key)
	(.get cli (cons key keys))
	))

(defn xgets
  "Gets an item's value by key,return value has a cas value"
  [^MemcachedClient cli key]
  (bean  (.gets cli key)))

(defn xcas
  "Compare and set an item's value by key
  set the new value to:
       (cas-fn current-value)"
  ([^MemcachedClient cli ^String key cas-fn]
	 (xcas cli key cas-fn Integer/MAX_VALUE))
  ([^MemcachedClient cli ^String key cas-fn ^Integer max-times]
	 (xcas cli key cas-fn max-times 0))
  ([^MemcachedClient cli ^String key cas-fn ^Integer max-times ^Integer expire]
	 (.cas cli key expire (reify CASOperation 
							(getMaxTries [this] max-times)
							(getNewValue [this _ value] (cas-fn  value))))))
;;Macro to increase/decrease item's value
(defmacro incr-decr
  [method]
  (let [m (symbol method)]
	`(fn ([^MemcachedClient cli# ^String key# ^Long delta#] (. cli# ~m key# delta#))
	   ([^MemcachedClient cli# ^String key# ^Long delta# ^Long init#] (. cli# ~m key# delta# init#)))))

(def
 ^{:arglist '([client key delta] [client key delta initValue])
   :doc "Increase an item's value by key"}
 xincr
 (incr-decr "incr"))

(def
 ^{:arglist '([client key delta] [client key delta initValue])
   :doc "Decrease an item's value by key"}
 xdecr
 (incr-decr "decr"))

(defn xdelete
  "Delete an item by key"
  [^MemcachedClient cli key]
  (.delete cli key))

(defn xflush
  "Flush all values in memcached"
  ([cli] (.flushAll cli))
  ([cli ^InetSocketAddress addr] (.flushAll cli addr)))

(defn xstats
  "Get statistics info from all memcached servers"
  [cli]
  (.getStats cli))

(defn xshutdown
  "Shutdown the memcached client"
  [cli]
  (.shutdown cli))
