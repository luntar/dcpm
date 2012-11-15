(ns dcpm.core
  (:gen-class)
  (:use [clojure.tools.cli :only [cli]])
  (:use [clojure.string :only [upper-case]])
  (:use [dcu.midi-protocol])
  (:use [dcu.data-tools])
  (:use [dcu.midi-ctrl])
)

(defn version [] "0.0.1")


(defn -main [ & args]

	(let [[options a banner] (cli args
                                   ["-h" "--help" "Displays help.", :default false, :flag true]
                                   ["-v" "--version" "Prints version.", :default false, :flag true]
                                     )]
      
        (when (:help options)
              (println banner)
              (System/exit 0))

        (when (:version options)
              (println (version))
              (System/exit 0))

		(init-midi "MIDI")
              
))