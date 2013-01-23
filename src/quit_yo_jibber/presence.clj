(ns quit-yo-jibber.presence
  (:import [org.jivesoftware.smack RosterListener]
           [org.jivesoftware.smack.packet Presence
                                          Presence$Type]))


(defn mapify-presence [#^Presence m]
  (try
    {:jid     (first (clojure.string/split (.getFrom m) #"/"))
     :status  (.getStatus m)
     :type    (str (.getType m))
     :mode    (str (.getMode m))
     :online? (.isAvailable m)
     :away?   (.isAway m)}
    (catch Exception e (println e) {})))

(defn with-presence-map [f]
  (fn [presence] (mapify-presence (f presence))))

(def presence-types {:available   Presence$Type/available
                     :unavailable Presence$Type/unavailable})

(defn set-availability!
  [conn type & [status addr]]
  (let [packet (Presence. (type presence-types))]
    (when status
      (doto packet (.setStatus status)))
    (when addr
      (doto packet (.setTo addr)))
    (doto conn (.sendPacket packet))))

(defn add-presence-listener [conn f]
  (let [roster (.getRoster conn)]
    (doto roster
      (.addRosterListener
       (proxy [RosterListener] []
         (entriesAdded [_])
         (entriesDeleted [_])
         (entriesUpdated [_])
         (presenceChanged [presence]
                          (f (mapify-presence presence))))))))

(defn subscribe-presence [conn addr]
  (let [presence (Presence. Presence$Type/subscribe)]
    (doto presence (.setTo addr))
    (doto conn (.sendPacket presence))))
