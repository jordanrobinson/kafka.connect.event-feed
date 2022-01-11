(ns kafka.connect.event-feed.utils
  (:import
   [java.util
    ArrayList
    Collection
    HashMap
    HashSet
    LinkedList
    List
    Map
    Set]))

(defn clojure-data->java-data [x]
  (cond
    (keyword? x) (name x)
    (map? x) (reduce
               (fn [map [k v]]
                 (doto map
                   (.put
                     (clojure-data->java-data k)
                     (clojure-data->java-data v))))
               (HashMap.)
               (seq x))
    (list? x) (ArrayList. ^Collection (map clojure-data->java-data x))
    (set? x) (HashSet. ^Collection (map clojure-data->java-data x))
    (seq? x) (LinkedList. (map clojure-data->java-data x))
    (number? x) (Long/parseLong (str x))
    :else x))

(defn java-data->clojure-data [^Object o]
  (cond
    (string? o) (str o)
    (instance? Map o) (zipmap
                        (map keyword (.keySet ^Map o))
                        (map java-data->clojure-data (.values ^Map o)))
    (instance? List o) (vec (map java-data->clojure-data o))
    (instance? Set o) (set (map java-data->clojure-data o))
    :else o))
