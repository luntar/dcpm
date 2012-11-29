(ns dcpm.core
  (:gen-class)
  (:use [dcu.midi-ctrl])
  (:use [clojure.tools.cli :only [cli]])
  (:use [clojure.string :only [upper-case]])
  (:use [dcu.midi-protocol])
  (:use [dcu.data-tools])
  (:use [clojure.pprint])
)

(defn version [] "0.0.1")

(comment 
  (defn t [] 
    (do
      (init-midi "MIDI")
      (id-dev)
      (println (psx))))
  
)

(defn -main [ & args]

	(let [[options args banner] 

		(cli args
			["-h" "--help" "Displays help." :default false :flag true]
			["-v" "--version" "Prints version." :default false :flag true]
			["-l" "--list-midi-ports" "Prints MIDI dev descriptions" :default false :flag true]
			["-r" "--read-patch" "Reads the specific patch number"  :default 0 :parse-fn #(Integer. %)]
			["-t" "--run-test" "run specific test number"  :default 0 :parse-fn #(Integer. %)]
			["-p" "--midi-port" "Use the port with the given 'partial' description" :default "MIDI"]
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
       (init-midi (:midi-port options))
       (id-dev)
       (prt-id)

        ( when (:run-test options)
            ( let [id (:run-test options)]
              (prt-id)
              (System/exit 0)
              ))
       ; Options that require the MIDI susbsystem
       (when (:read-patch options)
         (let [id (:read-patch options)]
           ( println "test: " options )
           (println (:read-patch options) 
             (dcu.midi-ctrl/print-patch id) 
             ;(send-dev-query)
             ;(tdelay 150)
             ;(println (psx))
            (save-patch-txt id)
           ))
         (System/exit 0))

        (catch Exception e
          (println "err")))
   ))
  
