(ns dcu.sx-data
	(:use [dcu.data-tools])
  (:require [dcu.util :as util]))

(def sx-data* (ref []))

(defn sx-clear
  "clear the sysex input buffer"
	[]
	(dosync 
		(ref-set sx-data* [])))
		
(defn sx-len 
  "Return the number of MIDI messages in the queue"
  []
  (count @sx-data*))

(defn sx-ret
	"Return the head of the sysex queue"
	[] 
	(dosync (if (empty? @sx-data*) 
		(byte-array 0) 
		((first @sx-data*) :data))))

(defn sx-drop-head
	"Remove the head of the sysex queue"
	[] 
	(dosync 
		(ref-set sx-data* (vec (drop 1 @sx-data*)))))

(defn sx-add 
 "Add msg to end of queue"
 [msg] 
  (dosync (alter sx-data* conj msg ))) 

(defn sx-pop
  "Removes and returns the head of the sysex queue"
  []
	(let [msg (dmp-sysex-array (sx-ret))] 
	(do 
    (sx-drop-head)
    (identity msg))))

(defn sx-pop-bytes
  "Removes and returns the head of the sysex queue as a byte-array"
  []
	(let [msg (sx-ret)] 
	(do 
    (sx-drop-head)
    (identity msg))))


(defn sx-wait
  "Wait for a sysex message or timeout after \"ms\" milliseconds"
  [ms]
  (util/time-limited ms  (while (= (sx-len) 0)
         (Thread/sleep 2)))) 

