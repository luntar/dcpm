(ns dcu.data-tools
	(:use [clojure.pprint])
  (:require clojure.string)
  (:import [java.io FileInputStream FileOutputStream File])
	)


(defn get-hi
	"Return the upper bits of the given value so it can be encoded in a 7-bit midi byte"
	 [val] 
	(bit-shift-right val 7))

(defn get-lo 
	"Return the lower bits of the given value so it can be encoded in a 7-bit midi byte"
	[val] 
	(bit-and val 0x7F))
	
(defn dmp-array 
	"A function to return byte-array as a seq"
	[#^bytes barr]
	(let [len (alength ^bytes barr)] 
		(map #(+ (aget ^bytes barr %) 0) (range 0 len))))

(defn dmp-sysex-array 
	"A function to print byte-array of midi sysex data by conj'ing a 0xF0 to the start of the list"
	[#^bytes barr]
	(let [len (alength ^bytes barr)] 
		(if (> len 0) 
		(conj (dmp-array barr) -16) 
		'())))
	
(def dex2hex (hash-map 
	0 "00" 1 "01" 2 "02" 3 "03" 4 "04" 5 "05" 6 "06" 7 "07" 8 "08" 9 "09" 10 "0A" 11 "0B" 12 "0C" 13 "0D" 14 "0E" 15 "0F" 
	16 "10" 17 "11" 18 "12" 19 "13" 20 "14" 21 "15" 22 "16" 23 "17" 24 "18" 25 "19" 26 "1A" 27 "1B" 28 "1C" 29 "1D" 30 "1E" 31 "1F" 
	32 "20" 33 "21" 34 "22" 35 "23" 36 "24" 37 "25" 38 "26" 39 "27" 40 "28" 41 "29" 42 "2A" 43 "2B" 44 "2C" 45 "2D" 46 "2E" 47 "2F" 
	48 "30" 49 "31" 50 "32" 51 "33" 52 "34" 53 "35" 54 "36" 55 "37" 56 "38" 57 "39" 58 "3A" 59 "3B" 60 "3C" 61 "3D" 62 "3E" 63 "3F" 
	64 "40" 65 "41" 66 "42" 67 "43" 68 "44" 69 "45" 70 "46" 71 "47" 72 "48" 73 "49" 74 "4A" 75 "4B" 76 "4C" 77 "4D" 78 "4E" 79 "4F" 
	80 "50" 81 "51" 82 "52" 83 "53" 84 "54" 85 "55" 86 "56" 87 "57" 88 "58" 89 "59" 90 "5A" 91 "5B" 92 "5C" 93 "5D" 94 "5E" 95 "5F" 
	96 "60" 97 "61" 98 "62" 99 "63" 100 "64" 101 "65" 102 "66" 103 "67" 104 "68" 105 "69" 106 "6A" 107 "6B" 108 "6C" 109 "6D" 110 "6E" 111 "6F" 
	112 "70" 113 "71" 114 "72" 115 "73" 116 "74" 117 "75" 118 "76" 119 "77" 120 "78" 121 "79" 122 "7A" 123 "7B" 124 "7C" 125 "7D" 126 "7E" 127 "7F" 
	-127 "80" -126 "81" -125 "82" -124 "83" -123 "84" -122 "85" -121 "86" -120 "87" -119 "88" -118 "89" -117 "8A" -116 "8B" -115 "8C" -114 "8D" -113 "8E" 
	-112 "90" -111 "9a" -110 "92" -109 "93" -108 "94" -107 "95" -106 "96" -105 "97" -104 "98" -103 "99" -102 "9A" -101 "9B" -100 "9C" -99 "9D" -98 "9E" -97 "9F" 
	-96 "A0" -95 "A1" -94 "A2" -93 "A3" -92 "A4" -91 "A5" -90 "A6" -89 "A7" -88 "A8" -87 "A9" -86 "AA" -85 "AB" -84 "AC" -83 "AD" -82 "AE" -81 "AF" 
	-80 "B0" -79 "B1" -78 "B2" -77 "B3" -76 "B4" -75 "B5" -74 "B6" -73 "B7" -72 "B8" -71 "B9" -70 "BA" -69 "BB" -68 "BC" -67 "BD" -66 "BE" -65 "BF" 
	-64 "C0" -63 "C1" -62 "C2" -61 "C3" -60 "C4" -59 "C5" -58 "C6" -57 "C7" -56 "C8" -55 "C9" -54 "CA" -53 "CB" -52 "CC" -51 "CD" -50 "CE" -49 "CF" 
	-48 "D0" -47 "D1" -46 "D2" -45 "D3" -44 "D4" -43 "D5" -42 "D6" -41 "D7" -40 "D8" -39 "D9" -38 "DA" -37 "DB" -36 "DC" -35 "DD" -34 "DE" -33 "DF" 
	-32 "E0" -31 "E1" -30 "E2" -29 "E3" -28 "E4" -27 "E5" -26 "E6" -25 "E7" -24 "E8" -23 "E9" -22 "EA" -21 "EB" -20 "EC" -19 "ED" -18 "EE" -17 "EF" 
	-16 "F0" -15 "F1" -14 "F2" -13 "F3" -12 "F4" -11 "F5" -10 "F6" -9 "F7" -8 "F8" -7 "F9" -6 "FA" -5 "FB" -4 "FC" -3 "FD" -2 "FE" -1 "FF"))
	
(defn hex 
  "Convert a twoscomp byte to an 8bit hex string -16->\"F0\""
  [val]
  (dex2hex val))
(defn dec2str
	"Convert a list of byte values to hex equivalent" 
	[list-of-dec]
	(clojure.string/join " " (map #(dex2hex (long %)) list-of-dec)))

(defn fetch-data
	"Read the contents at the given URL and save it to the filesystem in ofile"
	[url ofile]
  (let  [con    (-> url java.net.URL. .openConnection)
         fields (reduce (fn [h v] 
                          (assoc h (.getKey v) (into [] (.getValue v))))
                        {} (.getHeaderFields con))
         size   (first (fields "Content-Length"))
         in     (java.io.BufferedInputStream. (.getInputStream con))
         out    (java.io.BufferedOutputStream. 
                 (java.io.FileOutputStream. ofile))
         buffer (make-array Byte/TYPE 1024)]
    (loop [g (.read in buffer)
           r 0]
      (if-not (= g -1)
        (do
          (println r "/" size)
          (.write out buffer 0 g)
          (recur (.read in buffer) (+ r g)))))
    (.close in)
    (.close out)
    (.disconnect con)))

(defn printhex
	"Print the given seq of twos-complement 8bit data as a string of hex byte values"
	[dec-data] 
	(let [msg (dec2str dec-data)]
		(do (locking System/out (println msg)))))

(defn slurp-binary
  "Read in a file containing binary data into a byte-array."
  [f-name]
  (let [f   (File. f-name)
        fis (FileInputStream. f)
        len (.length f)
        ba  (byte-array len)]
    (loop [offset 0]
      (when (< offset len)
        (let [num-read (.read fis ba offset (- len offset))]
          (when (>= num-read 0)
            (recur (+ offset num-read))))))
    (.close fis)
    ba)) 

(defn spit-binary
  "Write a byte-array into a file with path f-name."
  [f-name bytes]
  (let [f   (File. f-name)
        fos (FileOutputStream. f)]
    (.write fos bytes)
    (.close fos)))
 
; This is like a global var, but it's thread safe!  Dave, the '*' is 
; part of the name, it's not a pointer, etc...  It tells us that 'presets*'
; is a mutabel var.
(def presets*  (atom  [])) 

(defn load-presets 
   "Load the preset file into the preset persitent buffer"
   [file-name]
   (let [p-data (dcu.data-tools/slurp-binary file-name)
         valid? true ] 
     (reset! presets* p-data)))

(defn dump-presets
  "Use at the repl, use this function to hex dump all the presets loaded in the presets* atom."
  []
  (printhex @presets*))

(defn aset-seq
  "Sets the byte value at idx of the array and return a seq"
  [a idx val]
  (let [new-a (aclone a)
        new-v (byte val)]
        (aset-byte new-a idx new-v)
        (dmp-array new-a) ))

(defn aset16-byte
  "Returns an array by cloning the byte array a, set the value to 2 bytes starting at idx with the hi and lo part of val16"
  [a idx val16]
  (let [new-a (aclone a)
        new-hi (get-hi val16)
        new-lo (get-lo val16)]
        (aset-byte new-a idx new-hi)
        (aset-byte new-a (+ idx 1) new-lo)
        new-a))
(defn mk-preset-vector
  "Returns a vector of vectors by subdividing the vector n by a count of p-sz.
   This requires that each preset in n is exactly the size p-sz.
   TODO: fix - Note, there's note error checking in this function (count n) better
   divide evanly by p-sz, otherwise something is wrong "
   [n p-sz]
    (loop [result [] preset-seq n]
       (if (zero? (count preset-seq))
         result
          (recur (conj result (vec (take p-sz preset-seq))) (drop p-sz preset-seq))
          )))
;; Test Notes - How to convert a preset binary file into a seq of individual preset vectors
; Load 200 650 byte preset sysex messages from a binary file
(load-presets "/Users/john/proj/dc/fp105.syx")
; Make a vector of individual preset vectors.  Each vector contains the twos complement
; MIDI byte data.
(def pv (mk-preset-vector (vec (dmp-array @presets*)) 650))
; This function will return the nth preset vector
(defn take-preset [n] (first (take n pv)))
; This hex-dumps the preset 4
;(printhex (take-preset 4))
;; For the repl 
(defn hhp 
  "Hex Head Preset - Return the head of the preset array has a hex string"
  [n] 
  (dec2str (take n (dmp-sysex-array @presets*))))

(defn hp
  "Head Preset - return the head of the preset array has a seq of twos-comp values"
  [n] 
  (take n (dmp-sysex-array @presets*)))

(defn htp 
  "Hex Tail Preset - Return the last n bytes of the preset array has a hex string"
  [n] 
  (dec2str (take-last n (dmp-sysex-array @presets*))))
