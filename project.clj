(defproject clj-xmemcached "0.2.2"
  :author "dennis zhuang[killme2008@gmail.com]"
  :description "An opensource memcached client for clojure wrapping xmemcached"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.2"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]
				 [com.googlecode.xmemcached/xmemcached "1.4.1"]]
  :test-paths ["test" "example"]
  :profiles {:dev {:dependencies [[log4j/log4j "1.2.16"]
                                  [org.slf4j/slf4j-log4j12 "1.5.6"]]
                   :resource-paths ["dev"]}}
  :plugins [[lein-exec "0.3.0"]]
  :warn-on-reflection true)