(ns dcu.data-tools
	(:use [clojure.pprint])
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
	"A function to print byte-array data"
	[#^bytes barr]
	(let [len (alength ^bytes barr)] 
		(map #(+ (aget ^bytes barr %) 0) (range 0 len))))

(defn dmp-sysex-array 
	"A function to print byte-array of midi sysex data by conj'ing a 0xF0 to the start of the list"
	[#^bytes barr]
	(let [len (alength ^bytes barr)] 
		(if (> len 0) (conj (dmp-array barr) -16) '() )))
	
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
	
(defn dec2str
	"Convert a list of byte values to hex equivalent" 
	[list-of-dec]
	(clojure.string/join " " (map #(dex2hex (long %)) list-of-dec)))
	