(ns dcu.midi-protocol
	(:use [dcu.data-tools])
)

(defonce DEV-INQ-RESP '(-16 126 0 6 2)) ; per MIDI spec

(def sx-dc-hdr* (ref []))
(def sx-man* (ref []))
(def sx-fam* (ref []))
(def sx-pid* (ref []))
(def sx-ver* (ref []))

(defn sx-man? [] (not (empty? @sx-man*)))


(def dnames 
	(hash-map 
		'(2 0) "Mobius" 
		'(1 0) "Timeline"))

(defn dev-name
	"Return a name string for the current device"
	[]
	(dnames @sx-pid*))
	 
(defn clear-sysex-hdr
	[]
	(dosync 
		(ref-set sx-man* [])
		(ref-set sx-fam* [])
		(ref-set sx-pid* [])	
		(ref-set sx-ver* [])))

; todo: use this structure to manage the sysex device id data
(defstruct sx-id-data
           :id
           :family
           :product
           :version
           )

(defn init-sysex
  "Takes a MIDI ID device response and initializes mutable device info "
	[inq-resp]
  (if (> (count inq-resp) 15)
    (let [
          id0 (nth inq-resp 5) 
          id1 (nth inq-resp 6) 
          id2 (nth inq-resp 7)
          fam0 (nth inq-resp 8)
          fam1 (nth inq-resp 9)
          pid0 (nth inq-resp 10)
          pid1 (nth inq-resp 11)
          ver0 (nth inq-resp 12)
          ver1 (nth inq-resp 13)
          ver2 (nth inq-resp 14)
          ver3 (nth inq-resp 15)]
    (dosync
      (ref-set sx-dc-hdr* [-16 id0 id1 id2 fam0 pid0])
      (ref-set sx-man* [-16 id0 id1 id2])
      (ref-set sx-fam* [fam0 fam1])
      (ref-set sx-pid* [pid0 pid1])
      (ref-set sx-ver* [ver0 ver1 ver2 ver3]))
      )
    (clear-sysex-hdr)))

(defn init-sysex-manufact
	"From the given dev-inq response, or 3 sysex id bytes, initialize the global sysex header"
	([inq-resp]
		(if (= DEV-INQ-RESP (take 5 inq-resp))
			(init-sysex-manufact (nth inq-resp 5) (nth inq-resp 7) (nth inq-resp 6))))
	([id0 id1 id2]
	(dosync
		(ref-set sx-man* [-16 id0 id1 id2]))))

(defn init-sysex-ver
	"From the given dev-inq response, or 4 sysex id bytes, initialize the global sysex header"
	[inq-resp]
	(if (= DEV-INQ-RESP (take 5 inq-resp))
	(dosync
		(ref-set sx-ver* [(nth inq-resp 10) (nth inq-resp 11) (nth inq-resp 12) (nth inq-resp 13)]))))

(defn init-sysex-pid
	"From the given dev-inq response, set the product id"
	[inq-resp]
	(if (= DEV-INQ-RESP (take 5 inq-resp))
	(dosync
		(ref-set sx-pid* [(nth inq-resp 9) (nth inq-resp 8)]))))

(defn init-devid-data
	[inq-responce]
	(do
		(init-sysex-manufact inq-responce)
		(init-sysex-ver inq-responce)
		(init-sysex-pid inq-responce)))
		
(defn append-sysex-hdr
	"Append the sysex header to the given vector.  Call init-sysex-manufact before calling this function"
	[byte-vec]
	(into @sx-dc-hdr* byte-vec))

(def dc-cmds {:write-preset 0x62 
              :write-factory-preset 0x65 
              :read-preset 0x63 
              :reset-device 0x1B
              :boot-opt 0x1C 
              :write-esn 0x20 
              :read-esn 0x21 
              :reinit-presets 0x1F})
(defn mk-command-msg
	"Returns a MIDI byte sequences to command the current device.  A command is
 an 1 byte opcod and an optional 16 bit argument"
  ([cmd] (append-sysex-hdr [(dc-cmds cmd) 0xF7]) )
	([cmd arg] 
    (let [hi (get-hi arg)
          lo (get-lo arg)]
    (append-sysex-hdr [(dc-cmds cmd) hi lo 0xF7]))))

(defn mk-patch-msg
	"Returns a MIDI byte sequences to retrieve the given patch number"
	[patch-num] 
  (mk-command-msg :read-preset patch-num))


