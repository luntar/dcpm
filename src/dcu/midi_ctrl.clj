(ns dcu.midi-ctrl
	(:use [dcu.midi])
	(:use [dcu.data-tools])
	(:use [dcu.midi-protocol])	
	(:use [clojure.pprint]))

(def midi-log* (ref []))

(defn tdelay [ms] (Thread/sleep ms))

; Define a vector as the MIDI SYSEX Input Queue
(def sx-data* (ref []))

(def mo* (ref []))
(def mi* (ref []))

(defn clear-sx
	[]
	(dosync 
		(ref-set sx-data* [])))
		
(defn set-midi-out 
	[re] 
	(dosync 
		(ref-set mo* (midi-out re))))

(defn set-midi-in
	[re] 
	(dosync 
		(ref-set mi* (midi-in re))))

(defn init-midisysex-input 
	"Register a midi sysex hander for the given midi source"
	[m-in] 
	(midi-sx-handle-events m-in (fn [e ts] (dosync (alter sx-data* conj e )))))

(defn init-midi
	"Initalize MIDI I/O, use the given regex to match a device in the device list"
	[re]
	(do 
		(set-midi-out re)
		(set-midi-in re)
		(init-midisysex-input @mi*)))
		
(defn send-dev-query
	"Sends a MIDI Device Inquirty to the given output sink \"m-out\""
	([] (send-dev-query @mo*))
	([m-out] (midi-sysex m-out 
		[0xF0 0x7E 0x7F 0x06 0x01 0xF7])))

(defn ret-sx
	"Return the head of the sysex queue"
	[] 
	(dosync (if (empty? @sx-data*) 
		(byte-array 0) 
		((first @sx-data*) :data))))

(defn rm-head
	"Remove the head of the sysex queue"
	[] 
	(dosync 
		(ref-set sx-data* (vec (drop 1 @sx-data*)))))

(defn get-patch
	"Request the given patch from the connected device"
	([num] (get-patch @mo* num))
	([m-out num] 
	(midi-sysex m-out (mk-patch-msg num))))

(defn psx
	"Remove, then print the head of the Sysex In Queue"
	[] 
	(let [msg (dmp-sysex-array (ret-sx))
		 qdata (rm-head)] 
		pprint msg))

(defn psxs
	"print sysex message"
	[]
	(dec2str (psx)))
	
(defn e-hand [event ts]
   (dosync
	(alter midi-log* conj event))
   	(println event))

(defn get-range-of-patches
	"Return the patch data for each patch within the patch range: start to end"
	[st en] 
	(let [last-patch (+ en 1)]
		(map #(do (get-patch %) (psx) (tdelay 10)) (range st last-patch))))

(defn id-dev
	[]
	(do 
		(clear-sysex-hdr)
		(clear-sx)
		(tdelay 5)
		(send-dev-query)
		(tdelay 20)
		(init-sysex (psx))
		(if (sx-hdr?) (println "DEVICE FOUND: " (dev-name)) (println "NO DEVICE FOUND"))))
		