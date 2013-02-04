(ns
	^{:doc "The clj-xmemcached core"
	  :author "Dennis zhuang<killme2008@gmail.com>"}
  clj-xmemcached.core
  (:import (net.rubyeye.xmemcached MemcachedClient MemcachedClientBuilder XMemcachedClient CASOperation XMemcachedClientBuilder GetsResponse)
		   (net.rubyeye.xmemcached.impl KetamaMemcachedSessionLocator ArrayMemcachedSessionLocator PHPMemcacheSessionLocator)
		   (net.rubyeye.xmemcached.utils AddrUtil)
		   (net.rubyeye.xmemcached.command BinaryCommandFactory KestrelCommandFactory TextCommandFactory)
		   (java.net InetSocketAddress))
  (:refer-clojure :exclude [get set replace])
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
  (case protocol
    :binary (BinaryCommandFactory.)
    :kestrel (KestrelCommandFactory.)
    (TextCommandFactory.)))

(defn- make-session-locator [hash]
  (case hash
    (:consistent :ketama :consist) (KetamaMemcachedSessionLocator.)
    (:php :phpmemcache) (PHPMemcacheSessionLocator.)
    (ArrayMemcachedSessionLocator.)))

(def ^:dynamic *memcached-client* nil)
(def ^:private global-memcached-client (atom nil))

(defmacro with-client
  "Evalutes body in the context of a thread-bound client to a memcached server."
  [client & body]
  `(binding [*memcached-client* ~client]
     ~@body))

(defn set-client!
  "Set a global  memcached client for all thread contexts,prefer binding a client by `with-client` macro"
  [^MemcachedClient client]
  (reset! global-memcached-client client))

(def no-client-error
  (Exception. (str "Memcached methods must be called within the context of a"
                   " client to Memcached server. See `with-client`.")))

(defn get-memcached-client []
  {:doc "Returns current thread-bound memcached client,if it is not bound,try to get the global client,otherwise throw an exception."
   :tag MemcachedClient}
  (or *memcached-client* @global-memcached-client (throw no-client-error)))

(defn memcached
  "Create a memcached client with zero or more options(any order):
    :protocol  Protocol to talk with memcached,a keyword in :text,:binary or :kestrel,default is text.
    :hash  Hash algorithm,a keyword in  :consistent, :standard or :php, default is standard hash.
    :pool  Connection pool size,default is 1,it's a recommended value.
    :sanitize-keys  Whether to sanitize keys before operation,default is false.
    :reconnect  Whether to reconnect when connections are disconnected,default is true.
    :heartbeat  Whether to do heartbeating when connections are idle,default is true.
    :timeout  Operation timeout in milliseconds,default is five seconds.
    :name  A name to define a memcached client instance"
  [servers & opts]
  (let [m (apply hash-map (unquote-options opts))
		name (:name m)
		protocol (:protocol m)
		hash (:hash m)
		pool (or (:pool m) 1)
		timeout (or (:timeout m) 5000)
        reconnect (or (:reconnect m) true)
        sanitize-keys (or (:sanitize-keys m) false)
        heartbeat (or (:heartbeat m) true)]
	(let [builder (doto (XMemcachedClientBuilder.  (AddrUtil/getAddresses servers))
                    (.setName name)
                    (.setSessionLocator (make-session-locator hash))
                    (.setConnectionPoolSize pool)
                    (.setCommandFactory  (make-command-factory protocol)))
          rt (.build builder)]
      (doto rt
        (.setOpTimeout timeout)
        (.setEnableHealSession reconnect)
        (.setEnableHeartBeat heartbeat)
        (.setSanitizeKeys sanitize-keys)))))

;;define store functions:  set,add,replace,append,prepend
(defmacro define-store-fn [meta name]
  (case name
    (append prepend)
    `(defn ~name ~meta [^String key# value#]
       (. (get-memcached-client) ~name key# value#))
    `(defn ~name ~meta
       ([^String key# value#] (. (get-memcached-client) ~name key# 0 value#))
       ([^String key# value# ^Integer exp#] (. (get-memcached-client) ~name key# exp# value#)))))

(define-store-fn
  {:arglists '([key value] [key value expire])
   :doc "Set an item with key and value."}
  set)

(define-store-fn
  {:arglists '([key value] [key value expire])
   :doc "Add an item with key and value,success only when item is not exists."}
  add)

(define-store-fn
  {:arglists '([key value] [key value expire])
   :doc "Replace an existing item's value by new value"}
  replace )

(define-store-fn
  {:arglists '([key value])
   :doc "Append a string to an existing item's value by key"}
  append)

(define-store-fn
  {:arglists '([key value])
   :doc "Prepend a string to an existing item's value by key"}
  prepend)

(defn touch "Touch a item with new expire time."
  [key expire]
  (.touch (get-memcached-client) key expire))

(defn get "Get items by a key or many keys,when bulk get items,the result is java.util.HashMap"
  ([^String k]
     (.get (get-memcached-client) k))
  ([k1 k2 ]
     (.get (get-memcached-client) ^java.util.Collection (list k1 k2)))
  ([k1 k2 & ks]
     (.get (get-memcached-client) ^java.util.Collection (list* k1 k2 ks))))

(defn gets
  "Gets an item's value by key,return value has a cas value"
  [^String key]
  (when-let [^GetsResponse resp (.gets (get-memcached-client) key)]
    {:cas (.getCas resp)
     :value (.getValue resp)}))

(defn cas
  "Compare and set an item's value by key
  set the new value to:
       (cas-fn current-value)"
  ([^String key cas-fn]
	 (cas key cas-fn Integer/MAX_VALUE))
  ([^String key cas-fn ^Integer max-times]
	 (cas key cas-fn max-times 0))
  ([^String key cas-fn ^Integer max-times ^Integer expire]
	 (.cas (get-memcached-client) key expire (reify CASOperation 
                                               (getMaxTries [this] max-times)
                                               (getNewValue [this _ value] (cas-fn  value))))))

;;define incr/decr functions
(defmacro define-incr-decr-fn
  [meta name]
  `(defn ~name ~meta ([^String key# ^Long delta#] (. (get-memcached-client) ~name key# delta#))
     ([^String key# ^Long delta# ^Long init#] (. (get-memcached-client) ~name key# delta# init#))))

(define-incr-decr-fn
  {:arglist '([key delta] [key delta init-value])
   :doc "Increase an item's value by key"}
  incr)

(define-incr-decr-fn
  {:arglist '([key delta] [key delta init-value])
   :doc "Decrease an item's value by key"}
  decr)

(defn delete
  "Delete an item by key"
  [key]
  (.delete (get-memcached-client) key))

(defn flush-all
  "Flush all values in memcached.WARNNING:this method will remove all items in memcached."
  ([] (flush-all (get-memcached-client)))
  ([^MemcachedClient cli] (.flushAll cli)))

(defn stats
  "Get statistics info from all memcached servers"
  ([]
     (stats (get-memcached-client)))
  ([^MemcachedClient cli]
     (.getStats cli)))

(defn shutdown
  "Shutdown the memcached client"
  ([]
     (shutdown (get-memcached-client)))
  ([^MemcachedClient cli]
     (.shutdown cli)))
