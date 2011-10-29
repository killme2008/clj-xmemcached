(defproject clj-xmemcached "0.1.0"
  :author "dennis zhuang[killme2008@gmail.com]"
  :description "An opensource memcached client for clojure wrapping xmemcached"
  :dependencies [
				 [org.clojure/clojure "1.2.1"]
				 [com.googlecode.xmemcached/xmemcached "1.3.5"]]
  :dev-dependencies [
					 [log4j/log4j "1.2.16"]
					 [org.slf4j/slf4j-log4j12 "1.5.6"]
					 [lein-clojars "0.6.0"]])