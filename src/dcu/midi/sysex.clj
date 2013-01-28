(ns dcu.midi.sysex

  (:use [dcu.midi-protocol])
  (:require dcu.midi-ctrl)
  (:require [dcu.data-tools :as dcu.data])
  (:require [dcu.gui :as gui])
  (:use [dcu.settings])
  (:use [clojure.tools.cli :only  [cli]])
  (:require  [clojure.string])
  (:require [overtone.studio.midi :as ot.st.midi])
  (:require [overtone.midi :as ot.midi])
  (:require [overtone.libs.event :as ot.events])
  (:require [overtone.config.log :as log])
  (:use [clojure.pprint]))

;; A NOTE ABOUT NAMES AND ABSTRACTIONS
;;
;; The overtone project provides the MIDI interface abstraction and event system.
;; It uses the term "device" to describe what what this code considers a MIDI port.
;; 
;; We shall use the term "device" to mean "the thing that which you are communicating."
;; The "interface" is what implements/exposes MIDI ports, and a MIDI "port," along with a sometimes
;; optional channel number, is what is expressed when selecting/targeting a device for communication.
;;
;; Data Direction
;; This code uses traditional definitions for data direction.
;;   out - data that flows from the host into the device
;;   in  - data that flows into the host 

;; DEVICE DEFINITION
;; A device has identity and is present on a particular port.

(defstruct device-identity
           :channel
           :manufacturer
           :family
           :family-member
           :version)

(defstruct device-info
           :identity
           :port)

;; MIDI SPECIFICATION DEPENDENCIES
(defonce ANY_CH_RESPOND 0x7F)

(defn get-port-list 
   "Return the \"full-device-key\" list.

   The low-level MIDI system keeps a list of known good interface ports.
   The list contains a hash-map of important interface details. Included in the
   this hash is the \"full-device-key,\" a value used to uniquely identify all
   interfaces, even if multiple devices of the same type are present. "
   [] 
   (map :overtone.studio.midi/full-device-key 
        (midi-find-connected-devices "")))

(defn out-ports-get 
  "Returns a list of the useable MIDI-out ports."
  [] (overtone.studio.midi/connected-midi-receivers))

;; Keep track of registered handlers so they can be removed
(def 
  ^{:doc "A collection of event handler \"ids.\" The ids refer to 
         handlers that have been registered with the event system.
         Ids are useful when removing handlers from the event system."}
  handler-ids* (atom []))

;; IDENTITY REQUESTS

;; Getting General System Information
(defn identity-req 
  "For the given port and optional ch, send a 'Identify Request'
  If the channel param is nil, a reciving device should respond regardless
  of what MIDI channel it is on."
  ([port] (identity-req port ANY_CH_RESPOND))
  ([port cc] 
   (midi-sysex port [0xF0 0x7E cc 0x06 0x01 0xF7])))

(defn identity-req-all 
  "Send identity request on all ports"
  [] 
  (map #(identity-req (midi-out %)) (out-ports-get)))

;; SYSEX "HANDLER FUNCTIONS and HANDLER FACTORIES"

;; This one prints events 
(defn eh-p [pred] (fn [e] (if (pred e) (println "yes") (println "no"))))
(defn ptr-evts [e] 
  (when-let [dev (:dev-key (:sysex e))]
                  (println dev (dec2str (dmp-array (:data (:sysex e)))))))


(defn make-data-pred-handler
  "Return event handler that applies the given predicate to the 
  data information, if truthy, the event is passed to the given handler"
  [pred true-fn] 
  (fn [e] (when-let [data (dcu.data/bytes2sysex (:data (:sysex e)))]
                  (if (pred data) 
                    (do (true-fn e) (println "yes"))
                    (println "no")))))


(defn make-dev-pred-handler
  "Return event handler that applies the predicate to the 
  port information, if truthy, the event is passed to the given handler"
  [pred true-fn] 
  (fn [e] (when-let [port (:dev-key (:sysex e))]
                  (if (pred port) 
                    (do (true-fn e) (println "yes"))
                    (println "no")))))

;; SYSEX EVENT HANDLER REGISTRATION

(defn sysex-register! 
  "Registers an event hander on the given port"
  [port eh] 
  (let [id (gensym "midi.sx_")
        result (ot.events/on-event port eh id)]
    (if result {:port port :id id} nil)))

(defn sysex-register-all!
  "Registers an event handler on all ports. group-id can be used 
  to remove the handlers"
  [handler-fn group-id] 
  (let [eh-ids (into '[] (assoc (map #(sysex-register! % handler-fn) 
                                     (get-port-list))) :group-id group-id)]
      (reset! handler-ids* eh-ids)))

(defn sysex-unregister-all!
   "Removes the currently registed handlers"
   []
  (doseq [ elm  @handler-ids*] 
    (ot.events/remove-handler (:id elm))))

(defn sysex-unregister-group!
   "Removes the registed handlers for the given group-id"
   [group-id]
  (doseq [ elm  @handler-ids*] 
    (if (= group-id (:group-id elm))
      (ot.events/remove-handler (:id elm)))))

  
(comment
(get-port-list)
(identity-req-all)
(register-for-sysex)
(doseq [ elm  @handler-ids*] (ot.events/remove-handler (:id elm)))
(swap! sysex-input-mode* sx-yep)
(pprint  @sysex-handlers*)
(def tdev (second (get-port-list)))
(ot.events/on-event tdev (fn[e] (println (:dev-key (:sysex e)) (dec2str (dmp-array (:data (:sysex e)))))) (gensym tdev))
)
