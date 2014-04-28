(defproject clj-xmemcached "0.2.4"
  :author "dennis zhuang[killme2008@gmail.com]"
  :description "An opensource memcached client for clojure wrapping xmemcached"
  :dependencies [[org.clojure/data.json "0.2.4"]
                 [org.slf4j/slf4j-log4j12 "1.5.6"]
                 [com.taoensso/nippy "2.5.2"]
				 [com.googlecode.xmemcached/xmemcached "2.0.0"]]
  :test-paths ["test" "example"]
  :profiles {:dev {:dependencies [[log4j/log4j "1.2.16"]
                                  [org.slf4j/slf4j-log4j12 "1.5.6"]]
                   :resource-paths ["dev"]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}}
  :plugins [[lein-exec "0.3.0"]]
  :warn-on-reflection true)
