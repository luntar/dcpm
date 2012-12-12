(ns dcu.midi-ctrl

	(:use [dcu.midi])
	(:use [dcu.data-tools])
	(:use [dcu.midi-protocol])	
  (:use dcu.sx-data)
  (:require [clojure.string ])
	(:use [clojure.pprint]))

(def midi-log* (ref []))


(def mo* (ref []))
(def mi* (ref []))

(defn set-midi-out 
  "assign port used for midi out"
	[re] 
	(dosync 
		(ref-set mo* (midi-out re))))

(defn set-midi-in
  "assign port used for midi in" 
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
		
(defn psx
	"Print the data in the sysex queue"
	[] 
	(let [msg (sx-pop)]
		(do (locking System/out (println "psx: " msg)))))


(defn midi-sx-send
  "Issue the given command, wait for then return the responce"
  ([msg] (midi-sx-send @mo* msg))
  ([out-port msg]
   (sx-clear)
   (midi-sysex out-port msg)
   (sx-wait 10000)
   (sx-pop)))

(defn midi-sx-send-get-bytes
  "Issue the given command, wait for then return the responce and return a byte-array"
  ([msg] (midi-sx-send-get-bytes @mo* msg))
  ([out-port msg]
   (sx-clear)
   (midi-sysex out-port msg)
   (sx-wait 10000)
   (sx-pop-bytes)))

(defn send-dev-query
	"Sends a MIDI Device Inquirty to the given output sink \"m-out\""
	([] (send-dev-query @mo*))
	([m-out] (midi-sx-send m-out 
		[0xF0 0x7E 0x7F 0x06 0x01 0xF7])))

(defn restore-presets
  "Write the presets vec to the device"
  [file-name]
  (let [ adata (dcu.data-tools/load-presets file-name)        
        vdata (vec (dmp-array adata))
        pv (mk-preset-vector vdata 650)]
    ))
(defn send-dc-cmd
  ([cmd]
    (midi-sx-send (mk-command-msg cmd)))
  ([cmd arg]
    (midi-sx-send (mk-command-msg cmd arg))))
(defn get-patch
	"Request the given patch from the connected device"
	([num] 
	(midi-sx-send (mk-patch-msg num))))

(defn set-preset
	"Write the preset, return hex string responce"
	([num] 
   (let [preset (aset16-byte @presets* 6 num)
         responce (dec2str (dmp-sysex-array (midi-sx-send-get-bytes (dmp-sysex-array preset))))]
     responce
    )))
(defn get-preset
	"Request the given preset, return data as byte-array"
	([num] 
	(let [p-data (midi-sx-send-get-bytes (mk-patch-msg num))]
    (reset! presets* p-data)
    p-data)))

(defn print-patch
	"Request the given patch from the connected device"
	([num] 
	(let [pdata  (dec2str (get-patch num))]
       (locking System/out (println "Patch # " num ":" pdata)) )))

(defn psxs
	"print sysex message"
	[]
	(dec2str (sx-pop)))

(defn prn-psxs
		"print sysex message"
		[]
		(println (dec2str (sx-pop))))
		
(defn psxcsv
	"print CSV sysex message"
	[]
	(clojure.string/replace (dec2str (sx-pop)) #" " ",") )

(defn print-patch-range
	"print a range of patchs"
	[st en] 
	(let [last-patch (+ en 1)]
		(map #(do (println "Patch " % ":") (dcu.data-tools/printhex  (get-patch %)) ) (range st last-patch))))


(defn tocsv 
	"return csv data for the given seq"
	[msg]
	(clojure.string/replace (dec2str (msg)) #" " ",") )

(defn save-patch-csv
	"Save the patch number to the csv file patch-#.csv"
	[num]
	(get-patch num)
	(spit (clojure.string/join (vector "patch-" num ".csv")) (psxcsv)))

(defn save-patch-txt
	"Save the patch number to the txt file patch-#.txt"
	[num]
	(do
		(let [ pdata (get-patch num)] 
		(spit (clojure.string/join (vector "patch-" num ".txt")) (dec2str pdata)))))
			
(defn save-patch-html
  [num]
  (do
    (get-patch num)
	 (spit (clojure.string/join (vector "../server/public/patch-" num ".html")) (psxs))))
(defn get-range-of-patches
	"Return the patch data for each patch within the patch range: start to end"
	[st en] 
	(let [last-patch (+ en 1)]
		(map #( get-patch % ) (range st last-patch))))

(defn save-patch-range
	"Return the patch data for each patch within the patch range: start to end"
	[st en] 
	(let [last-patch (+ en 1)
		    pdata (map #(get-patch %) (range st last-patch))] 
       (seq pdata)))

(defn prt-id []
  (do
    (send-dev-query)
    (psxs)
  )
  )
(defn id-dev
	[]
	(do 
		(clear-sysex-hdr)
		(sx-clear)
		(init-sysex (send-dev-query) )
		(if (sx-man?) 
          (println "DEVICE FOUND: " (dev-name)) 
          (println "NO DEVICE FOUND"))))
		
(defn idbg [] (init-midi "Port") (id-dev))
(defn gp 
  "get preset n, and return the first 8 bytes as a hext string"
  [n] 
  (do (get-preset n) (hhp 15)))