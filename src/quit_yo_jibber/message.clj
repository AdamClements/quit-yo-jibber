(ns quit-yo-jibber.message
  (:import [org.jivesoftware.smack        PacketListener
                                          XMPPException]
           [org.jivesoftware.smack.packet Message
                                          Message$Type]
           [org.jivesoftware.smack.util   StringUtils]
           [org.jivesoftware.smack.filter MessageTypeFilter]))

(defn error-map [e]
  (when e {:code (.getCode e) :message (.getMessage e)}))

(defn message-map [#^Message m]
  {:body    (.getBody m)
   :subject (.getSubject m)
   :thread  (.getThread m)
   :jid     (.getFrom m)
   :from    (StringUtils/parseBareAddress (.getFrom m))
   :to      (.getTo m)
   :error   (error-map (.getError m))
   :type    (keyword (str (.getType m)))})

(defn create-message [to message-body]
  (doto (Message.)
    (.setTo to)
    (.setBody (str message-body))
    (.setType Message$Type/chat)))

(defn send-message [conn to message-body]
  (.sendPacket conn (create-message to message-body)))

(defn with-responder [handler]
  (fn [conn message]
    (let [resp (handler message)]
      (send-message conn (:from message) resp))))

(defn with-message-map [handler]
  (fn [conn packet]
    (let [message (message-map #^Message packet)]
      (handler conn message))))

(defn add-message-listener [conn f]
  (doto conn
    (.addPacketListener
     (proxy [PacketListener] []
       (processPacket [packet]
         ((with-message-map (with-responder f)) conn packet)))
     (MessageTypeFilter. Message$Type/chat))))
