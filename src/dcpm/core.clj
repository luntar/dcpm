(ns dcpm.core
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]])
  (:use [clojure.string :only [upper-case]])
  (:use [dcu.midi-protocol])
  (:use [dcu.data-tools])
  (:use [dcu.midi-ctrl])
  (:use [clojure.pprint])
)

(defn version [] "0.0.1")



(defn -main [ & args]

	(let [[options args banner] 
		(cli args
			["-h" "--help" "Displays help." :default false :flag true]
			["-v" "--version" "Prints version." :default false :flag true]
			["-l" "--list-midi-ports" "Prints MIDI dev descriptions" :default false :flag true]
			["-r" "--read-patch" "Reads the specific patch number"  :default 0 :parse-fn #(Integer. %)]
			["-p" "--midi-port" "Use the port with the given 'partial' description" :default "MIDI"]
			)]
      
        (when (:help options)
              (println banner)
              (System/exit 0))

        (when (:version options)
              (println (version))
              (System/exit 0))

	    (when (:list-midi-ports options)
	          (println (pprint (map :description (dcu.midi/midi-devices))))
	          (System/exit 0))
		
		(init-midi (:midi-port options))

        (id-dev)

		(when (:read-patch options)
			(let [id (:read-patch options)]
			(println "Read patch " id)
			(save-patch-txt id))
		    (System/exit 0))
))
