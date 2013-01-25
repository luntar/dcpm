 (ns dcu.midi-ctrl
      (:use [dcu.midi])
      (:use [dcu.data-tools])
      (:use [dcu.midi-protocol])
      (:use [dcu.sx-data]) 
      (:use [dcu.settings])
      (:require [clojure.string :as str])
      (:use [clj-time.local :only [local-now]])
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

        (set-midi-out re)
        (set-midi-in re)
        (init-midisysex-input @mi*))

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

(defn calculate-checksum
  [pseq]
  (reduce  (fn  [a b]  (bit-and 0x7F  (+ a b)))  (drop-last 2 (drop 9 pseq))))

(defn midi-sx-send-fix-get-bytes
  "Issue the given command, wait for then return the responce and return a byte-array"
  ([msg] (midi-sx-send-get-bytes @mo* msg))
  ([out-port msg]
   (let [pdata (append-sysex-hdr (drop 6 msg))
         chksum (calculate-checksum pdata)
         pdatar (into (vec (drop 2 pdata)) (vector chksum -9)) ]
   (midi-sysex out-port pdatar)
   (sx-wait 10000)
   (sx-pop-bytes))))

(defn send-dev-query
        "Sends a MIDI Device Inquirty to the given output sink \"m-out\""
        ([] (send-dev-query @mo*))
        ([m-out] (midi-sx-send m-out
                [0xF0 0x7E 0x7F 0x06 0x01 0xF7])))

(defn restore-presets
  "Write the presets vec to the device"
  [file-name]
  (let [ adata (dcu.data-tools/load-presets file-name)
        pv (mk-preset-vector adata 650)]
      (doall (map #(do (flush) (println (dec2str (midi-sx-send-get-bytes (nth pv %))))) (range 0 5)) )))

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
   (dec2str  (dmp-array  (midi-sx-send-get-bytes  (dmp-array  (aset16-byte @presets* 7 num)))))))

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
                []
                (println (dec2str (sx-pop))))

(defn print-patch-range
        "print a range of patchs"
        [st en]
        (let [last-patch (+ en 1)]
                (map #(do (println "Patch " % ":") (dcu.data-tools/printhex  (get-patch %)) ) (range st last-patch))))
(defn tocsv
        "return csv data for the given seq"
        [msg]
        (str/replace (dec2str (msg)) #" " ",") )

(defn patch2csv
        "Append preset data 'num' to the given file"
        [num file-name]
        (do
                (let [pdata (get-patch num)
          hdata (dec2str (get-patch num))
          hdata-newline (str/join (vector  hdata "\n"))
          preset-data (str/replace hdata-newline #" " ",")]
                (spit file-name preset-data :append true))))

(defn mk-file-hdr
  "Create the file 'file-name' and write information about the time, device, and version"
  [file-name title]
  (let [t  (str/join (vector "Title," title "\n"))
        ct (str/join (vector "Creation Time," (.toString (local-now)) "\n"))
        n  (str/join (vector "Device Name," (dev-name) "\n"))
        v  (str/join (vector "Firmware Version," (dev-ver) "\n"))]
    (spit file-name t)
    (spit file-name ct :append true)
    (spit file-name n :append true)
    (spit file-name v :append true)
    (spit file-name "\n" :append true)))

(defn lprt [s]
    (locking System/out (print s))
  true
  )

(defn lprtln [s]
    (locking System/out (println s))
  true
  )


(defn dump-presets-csv
  "Save all presets as a csv in the given file"
  [file-name]
   (do
    (println "Saving presets to " file-name)
    (sx-clear)
    (mk-file-hdr file-name "Preset Dump")
    (lprt "Writing: ")
   (time
      (doall (map  (fn [num] (do (print (+ 1 num) " ") (flush) (patch2csv num file-name))) (range 0 200))))
    (lprtln "\ndone")
     ))

(defn preset-save-data
  "Write the preset seq to a file of the name stored in the patch"
  [pdata]
  (do (let [fname (str/replace
                     (str (str/trim
                            (dcu.midi-protocol/preset-get-name pdata)) ".syx") " " "_")
            lname (str/lower-case fname)
            pnum (dcu.midi-protocol/preset-get-num pdata) ]
        (do
          (println "Saving preset " pnum " to file " lname )
          (spit-binary lname (into-array Byte/TYPE pdata))))))

(defn preset-save
  "Save the specified preset"
  [num]
  (do (let [pdata (get-patch num) ]
        (do
          (preset-save-data pdata)))))

(defn save-patch-txt
        " Save the patch number to the txt file patch-#.txt"
        [num]
        (do
                (let [ pdata (get-patch num)]
                (spit (str/join (vector "patch-" num ".txt")) (dec2str pdata)))))

(defn save-patch-html
  [num]
  (do
    (get-patch num)
         (spit (str/join (vector "../server/public/patch-" num ".html")) (psxs))))

(defn get-range-of-patches
        "Return the patch data for each patch within the patch range: start to end"
        [st en]
        (let [last-patch (+ en 1)]
       ))

(defn foreach-preset [st end f] (doseq [e (range st end)] (f (dcu.midi-ctrl/get-patch e))))

(defn save-patch-range
        "Return the patch data for each patch within the patch range: start to end"
        [st en]
        (let [last-patch (+ en 1)
                    pdata (map #(get-patch %) (range st last-patch))]
       (seq pdata)))

(defn prt-id []
  (do
    (send-dev-query)
    (psxs)))

(defn id-dev
        []
        (do
                (clear-sysex-hdr)
                (sx-clear)
                (init-sysex (send-dev-query) )
                (if (sx-man?) (dev-name) nil)))

(def connection-status* ( atom 0) )

(defn connect 
  "starts up the midi subsystem, or if it's running does nothing"
  []
  (do (if (zero? @connection-status*)
        (do (init-midi (dcu.settings/config-get :port)) 
                                    (id-dev) 
                                    (swap! connection-status* inc )))))
(defn disconnect 
  "shuts down the  midi subsystem"
  []
  (reset! connection-status* 0))

(defn idbg [] (init-midi "Port") (id-dev))

(defn gp
  "get preset n, and return the first 8 bytes as a hext string"
  [n]
  (do (get-preset n) (hhp 15)))
