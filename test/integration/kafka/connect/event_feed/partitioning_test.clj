(ns kafka.connect.event-feed.partitioning-test
  (:require
   [clojure.test :refer :all]

   [clj-wiremock.fixtures :as wmf]
   [clj-wiremock.core :as wmc]
   [clj-wiremock.utils :as wmu]

   [kafka.testing.combined :as ktc]

   [kafka.connect.event-feed.test.logging]
   [kafka.connect.event-feed.test.resources :as tr]
   [kafka.connect.event-feed.test.consumer :as tc]
   [kafka.connect.event-feed.test.stubs :as ts]
   [kafka.connect.event-feed.test.data :as td]
   [kafka.connect.event-feed.test.connector :as tcn]))

(def kafka-atom (atom nil))
(def wiremock-atom (atom nil))

(use-fixtures :each
  (ktc/with-kafka kafka-atom
    :kafka {:num.partitions 2})
  (wmf/with-wiremock wiremock-atom))

(deftest partitions-by-stream-id
  (let [kafka (ktc/kafka @kafka-atom)
        kafka-connect (ktc/kafka-connect @kafka-atom)
        wiremock-server @wiremock-atom
        wiremock-url (wmu/base-url wiremock-server)

        topic-name :events

        stream-id-a "1"
        stream-id-b "A"

        event-resource-1 (tr/event-resource wiremock-url
                           {:id        (td/random-event-id)
                            :stream-id stream-id-a
                            :type      :event-type-1})
        event-resource-2 (tr/event-resource wiremock-url
                           {:id        (td/random-event-id)
                            :stream-id stream-id-a
                            :type      :event-type-2})
        event-resource-3 (tr/event-resource wiremock-url
                           {:id        (td/random-event-id)
                            :stream-id stream-id-b
                            :type      :event-type-3})
        event-resource-4 (tr/event-resource wiremock-url
                           {:id        (td/random-event-id)
                            :stream-id stream-id-b
                            :type      :event-type-3})]
    (wmc/with-stubs
      [(ts/discovery-resource wiremock-server)
       (ts/events-resource wiremock-server
         :events-link-parameters {:pick 5}
         :event-resources [event-resource-1
                           event-resource-2
                           event-resource-3
                           event-resource-4])]
      (tcn/with-connector kafka-connect
        {:name   :event-feed-source
         :config {:connector.class         tcn/connector-class
                  :batch.size              0
                  :topic.name              topic-name
                  :eventfeed.discovery.url (tr/discovery-href
                                             wiremock-url)
                  :eventfeed.pick          5}}
        (let [messages (tc/consume-n kafka topic-name 4
                         :max-attempts 20)
              partition-a-messages (take 2 messages)
              partition-b-messages (drop 2 messages)]
          (is (every? #(= (:partition %) 0) partition-a-messages))
          (is (every? #(= (:partition %) 1) partition-b-messages)))))))

(comment
  (compile 'kafka.connect.event-feed.task)
  (compile 'kafka.connect.event-feed.connector))
