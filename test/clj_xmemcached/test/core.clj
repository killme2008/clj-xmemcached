(ns clj-xmemcached.test.core
  (:refer-clojure :exclude [get set replace])
  (:use [clj-xmemcached.core])
  (:use [clojure.test])
  (:import (net.rubyeye.xmemcached MemcachedClient MemcachedClientBuilder XMemcachedClient CASOperation XMemcachedClientBuilder)
		   (net.rubyeye.xmemcached.impl KetamaMemcachedSessionLocator ArrayMemcachedSessionLocator)
		   (net.rubyeye.xmemcached.utils AddrUtil)
		   (net.rubyeye.xmemcached.command BinaryCommandFactory KestrelCommandFactory TextCommandFactory)
		   (java.net InetSocketAddress)))


(defmacro with-private-fns [[ns fns] & tests]
  "Refers private fns from ns and runs tests in context."
  `(let ~(reduce #(conj %1 %2 `(ns-resolve '~ns '~%2)) [] fns)
	 ~@tests))
;;Test memcached servers,plz configure it by yourself on your machine.
(def test-servers (System/getProperty "memcached.servers" "localhost:11211"))

(with-private-fns [clj-xmemcached.core [make-command-factory make-session-locator]]
  (deftest test-make-command-factory
	(is (instance? TextCommandFactory (make-command-factory :text)))
	(is (instance? BinaryCommandFactory (make-command-factory :binary)))
	(is (instance? KestrelCommandFactory (make-command-factory :kestrel))))
  (deftest test-make-session-locator
	(is (instance? ArrayMemcachedSessionLocator (make-session-locator :standard)))
	(is (instance? KetamaMemcachedSessionLocator (make-session-locator :ketama)))
	(is (instance? KetamaMemcachedSessionLocator (make-session-locator :consistent)))))


(deftest test-xmemcached1
  (let [cli (deref (memcached test-servers
                              :protocol :kestrel
                              :name "test"
                              :hash :ketama
                              :pool 10))]
	(try
	  (is (= 10 (.. cli getConnector getSessionSet size)))
	  (is (= "Kestrel" (.. cli getProtocol name)))
	  (is (= "test" (.getName cli)))
	  (finally
        (.shutdown cli)))))

(deftest test-xmemcached2
  (let [cli (deref (memcached test-servers
                              :protocol  :binary
                              :name "test"
                              :hash :standard
                              :timeout 1000
                              :pool 2))]
	(try
	  (is (= 2 (.. cli getConnector getSessionSet size)))
	  (is (= "Binary" (.. cli getProtocol name)))
	  (is (= "test" (.getName cli)))
	  (finally
        (.shutdown cli)))))

(deftest test-store-get-delete
  (let [cli (memcached test-servers
                       :transcoder clj-json-transcoder)]
    (with-client cli
      (try
        (is (add "a" 1))
        (is (not (add "a" 2)))
        (is (= 1 (get "a")))
        (is (replace "a" 2))
        (is (= 2 (get "a")))
        (is (set "a" 3))
        (is (= 3 (get "a")))
        (is (delete "a"))
        (is (nil? (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-nippy-transcoder
  (let [cli (memcached test-servers
                       :transcoder nippy-transcoder)]
    (with-client cli
      (try
        (is (add "a" 1))
        (is (not (add "a" 2)))
        (is (= 1 (get "a")))
        (is (replace "a" 2))
        (is (= 2 (get "a")))
        (is (set "a" 3))
        (is (= 3 (get "a")))
        (is (delete "a"))
        (is (nil? (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-set-client!
  (let [cli (memcached test-servers)]
    (set-client! cli)
    (try
      (is (add "a" 1))
      (is (not (add "a" 2)))
      (is (= 1 (get "a")))
      (is (replace "a" 2))
      (is (= 2 (get "a")))
      (is (set "a" 3))
      (is (= 3 (get "a")))
      (is (delete "a"))
      (is (nil? (get "a")))
      (finally
        (flush-all @cli)
        (shutdown @cli)
        (set-client! nil)))))

(deftest test-gets-delete
  (let [cli (memcached test-servers :protocol :binary)]
    (with-client cli
      (try
        (is (add "a" 1))
        (is (:value (gets "a")))
        (is (> (:cas (gets "a")) 0))
        (is  (not (delete "a" 1999)))
        (is (delete "a" (:cas (gets "a"))))
        (is (nil? (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-try-lock
  (let [cli (memcached test-servers)
        counter (atom 0)]
    (with-client cli
      (try
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
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-through
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (through "a" 99)
        (dorun (pmap #(through "a" %) [0 1 2 3 4]))
        (is (= 99 (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-bulk-get
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is (set "key1" 1))
        (is (add "key2" 2))
        (is (set "key3" 3))
        (let [rt (get "key1" "key2" "key3")]
          (is (= 3 (count rt)))
          (is (contains? rt "key1"))
          (is (contains? rt "key2"))
          (is (contains? rt "key3"))
          (is (= 1 (clojure.core/get rt "key1")))
          (is (= 2 (clojure.core/get rt "key2")))
          (is (= 3 (clojure.core/get rt "key3"))))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-expire
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is (add "a" 1 1))
        (is (= 1 (get "a")))
        (Thread/sleep 2000)
        (is (nil? (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-incr-decr
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is (= 0 (incr "a" 1)))
        (is (= 2 (incr "a" 2)))
        (is (= 1 (decr "a" 1)))
        (is (= "1" (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-incr-decr-with-expire
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is (= 0 (incr "a" 1 0 1)))
        (is (= 2 (incr "a" 2)))
        (Thread/sleep 2000)
        (is (nil? (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-append-prepend
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is (not (append "a" "hello")))
        (is (set "a" "dennis"))
        (is (append "a" " zhuang"))
        (is (= "dennis zhuang" (get "a")))
        (is (prepend "a" "hello,"))
        (is (= "hello,dennis zhuang" (get "a")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-cas
  (let [cli (memcached test-servers)]
    (with-client cli
      (try
        (is  (set "key" "hello"))
        (is (cas "key" #(str % " world")))
        (is (= "hello world"  (get "key")))
        (is (cas "key" #(.hashCode %) 1))
        (is (= (.hashCode "hello world") (get "key")))
        (finally
          (flush-all)
          (shutdown))))))

(deftest test-compressor
  (let [x (clojure.string/join (repeatedly (* 32 1024) #(rand-int 10)))]
    (is (= (* 32 1024) (count x)))
    (let [compressed (compress default-compressor (.getBytes x))]
      (is (not= (* 32 1024) (count compressed)))
      (is (not= x (String. compressed)))
      (is (= x (String. (decompress default-compressor compressed)))))
    (let [cli (memcached test-servers)]
      (with-client cli
        (try
          (is (set "x" x))
          (is (= x (get "x")))
          (finally
            (flush-all)
            (shutdown)))))))
