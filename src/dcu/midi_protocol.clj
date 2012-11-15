(ns dcu.midi-protocol
	(:use [dcu.data-tools])
)

(defonce DEV-INQ-RESP '(-16 126 0 6 2))

(def sx-hdr* (ref []))
(def sx-ver* (ref []))
(def sx-pid* (ref []))

(defn sx-hdr? [] (not (empty? @sx-hdr*)))

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
		(ref-set sx-hdr* [])
		(ref-set sx-pid* [])
		(ref-set sx-ver* [])))

(defn init-sysex
	[inq-resp]
	(let [id0 (nth inq-resp 5) 
	      id1 (nth inq-resp 7) 
	      id2 (nth inq-resp 6)
	      pid0 (nth inq-resp 10)
	      pid1 (nth inq-resp 11)
	      ver0 (nth inq-resp 12)
	      ver1 (nth inq-resp 13)
	      ver2 (nth inq-resp 14)
	      ver3 (nth inq-resp 15)]
	(dosync
		(ref-set sx-hdr* [-16 id0 id1 id2])
		(ref-set sx-pid* [pid0 pid1])
		(ref-set sx-ver* [ver0 ver1 ver2 ver3]))))

(defn init-sysex-manufact
	"From the given dev-inq responce, or 3 sysex id bytes, initalize the global sysex header"
	([inq-resp]
		(if (= DEV-INQ-RESP (take 5 inq-resp))
			(init-sysex-manufact (nth inq-resp 5) (nth inq-resp 7) (nth inq-resp 6))))
	([id0 id1 id2]
	(dosync
		(ref-set sx-hdr* [-16 id0 id1 id2]))))

(defn init-sysex-ver
	"From the given dev-inq responce, or 4 sysex id bytes, initalize the global sysex header"
	[inq-resp]
	(if (= DEV-INQ-RESP (take 5 inq-resp))
	(dosync
		(ref-set sx-ver* [(nth inq-resp 10) (nth inq-resp 11) (nth inq-resp 12) (nth inq-resp 13)]))))

(defn init-sysex-pid
	"From the given dev-inq responce, set the product id"
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
	(into @sx-hdr* byte-vec))

(defn mk-patch-msg
	"Returns a MIDI byte sequences to retrive the given patch number"
	[patch-num] 
	(let [cmd 0x63
		  hi (get-hi patch-num)
		  lo (get-lo patch-num)]
		(append-sysex-hdr [0x12 0x02 cmd hi lo 0xF7])))
