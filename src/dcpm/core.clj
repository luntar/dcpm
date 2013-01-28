(ns dcpm.core
  (:gen-class)
  (:use [dcu.midi-protocol])
  (:require dcu.midi-ctrl)
  (:use [dcu.data-tools])
  (:require [dcu.gui :as gui])
  (:use [dcu.settings])
  (:use [clojure.tools.cli :only  [cli]])
  (:require  [clojure.string])
  (:use [overtone.studio.midi])
  (:use [overtone.midi])
  (:require [overtone.config.log :as log])
  (:use [clojure.pprint]))

(defn version [] "0.0.2")

(def midi-running* ( atom 0) )

(defn start-midi
  "starts up the midi subsystem, or if it's running does nothing"
  []
  (do (if (zero? @midi-running*)
        (do (dcu.midi-ctrl/init-midi (dcu.settings/config-read :port)) 
                                    (dcu.midi-ctrl/id-dev) 
                                    (swap! midi-running* inc )))))



(defn -main [ & args]

	(let [[options args banner] 

		(cli args
			["-h" "--help" "Displays help." :default false :flag true]
			["-v" "--version" "Prints version." :default false :flag true]
			["-r" "--read-patch" "Reads the specific patch number" :parse-fn #(Integer. %)]
			["-s" "--save-preset" "Saves the specific preset" :parse-fn #(Integer. %)]
			["-w" "--write-preset" "Write the specific preset" :parse-fn #(String. %)]
      ["-d" "--destination" "Destination preset 0-199" :parse-fn #(Integer. %)]
			["-t" "--run-test" "run specific test number"  :default 0 :parse-fn #(Integer. %)]
      ["-e" "--restore" "Restore specified preset file"]
      ["-c" "--csv" "dump each patch to a csv file" :parse-fn #(String. %)]
			["-p" "--midi-port" "Sets and saves the MIDI port assigment"]
			["-l" "--list-midi-ports" "Prints MIDI dev descriptions" :default false :flag true]
			["-g" "--start-gui" "starts the gui" :default true :flag true]
			)]
      
	   (try
        (when (:start-gui options)
              (gui/execute-gui options))
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

	      (when (:midi-port options)
              (dcu.settings/config-write! :port (:midi-port options)))

        (println "Using MIDI Port: "  (dcu.settings/config-read :port)) 
       ; (println "Options: " (count options) " " options)

        ( when (not= 0  (:run-test options) )
            ( let [id (:run-test options)]
              (println "run-test")
              (start-midi)
              (dcu.midi-ctrl/prt-id)
              (System/exit 0)
              ))

       (when (:restore options)
         (let [file-name (:restore options)]
           (println "Restore presets from " file-name)
           (start-midi)
            (dcu.midi-ctrl/restore-presets file-name))
         (System/exit 0))

       (when (:csv options)
         (let [file-name (:csv options)]
           (println "Dump presets to csvn file " file-name)
           (start-midi)
            (dcu.midi-ctrl/dump-presets-csv file-name))
         (System/exit 0))

       ; Options that require the MIDI susbsystem
       (when (:read-patch options)
         (let [id (:read-patch options)]
           (println "Read Preset " id)
           (start-midi)
         (dcu.midi-ctrl/print-patch id))
         (System/exit 0))

       (when (:save-preset options)
         (let [id (:save-preset options)]
           (println "Save Preset " id )
           (start-midi)
         (dcu.midi-ctrl/preset-save id))
         (System/exit 0))

       (when (and (:destination options) (:write-preset options))
         (let [src-fname (:write-preset options)
               dst-num (:destination options) ]
           (do
             (println "Write preset " src-fname " to preset " dst-num  )
             (start-midi)
             (load-presets src-fname)
             (dcu.midi-ctrl/set-preset dst-num)
             (System/exit 0))))

        (catch Exception e
          (println "Internal Error: " e )))))
  
; "/Users/john/proj/dc/fp105.syx"
