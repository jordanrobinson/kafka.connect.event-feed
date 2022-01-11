(ns kafka.connect.event-feed.utils-test
  (:require
    [clojure.test :refer [deftest is]]
    [kafka.connect.event-feed.test.logging]
    [kafka.connect.event-feed.utils :refer [clojure-data->java-data]]))

(deftest utils-parses-numbers-as-longs
  (is (= 100 (clojure-data->java-data 100)))
  (is (= (+ 1 Integer/MAX_VALUE) (clojure-data->java-data (+ 1 Integer/MAX_VALUE)))))
