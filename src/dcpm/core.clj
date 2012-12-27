(ns dcpm.core
  (:gen-class)
  (:use [dcu.midi-protocol])
  (:require dcu.midi-ctrl)
  (:use [dcu.data-tools])
  (:use [clojure.tools.cli :only  [cli]])
  (:require  [clojure.string])
  (:use [clojure.pprint])) 

(defn version [] "0.0.1")

(comment 
  (defn t [] 
    (do
      (init-midi "Port")
      (id-dev)
      (println (psx)))))

(defn -main [ & args]

	(let [[options args banner] 

		(cli args
			["-h" "--help" "Displays help." :default false :flag true]
			["-v" "--version" "Prints version." :default false :flag true]
			["-l" "--list-midi-ports" "Prints MIDI dev descriptions" :default false :flag true]
			["-r" "--read-patch" "Reads the specific patch number"  :default 0 :parse-fn #(Integer. %)]
			["-t" "--run-test" "run specific test number"  :default 0 :parse-fn #(Integer. %)]
                        ["-e" "--restore" "Restore specified preset file"]
			["-p" "--midi-port" "Use the port with the given 'partial' description" :default "Port"]
			)]
      
	   (try
        (when (:help options)
              (println banner)
              (System/exit 0))
       
        (when (:version options)
              (println (version))
              (System/exit 0))
       
	      (when (:list-midi-ports options)
              (println (pprint 
              (map :description 
              (dcu.midi/midi-devices))))
              (System/exit 0))

       ; Start the MIDI system
       (dcu.midi-ctrl/init-midi (:midi-port options))
       (dcu.midi-ctrl/id-dev)

        ( when (not= 0  (:run-test options) )
            ( let [id (:run-test options)]
              (println "run-test")
              (dcu.midi-ctrl/prt-id)
              (System/exit 0)
              ))

       (when (:restore options)
         (let [file-name (:restore options)]
            (dcu.midi-ctrl/restore-presets file-name))
         (System/exit 0))

       ; Options that require the MIDI susbsystem
       (when (:read-patch options)
         (let [id (:read-patch options)]
         (dcu.midi-ctrl/print-patch id))
         (System/exit 0))

        (catch Exception e
          (println "Internal Error: " e )))
   ))
  
; "/Users/john/proj/dc/fp105.syx"
