(defproject dcpm "0.1.0-SNAPSHOT"
  :description "Damage Control Patch Manager - load and save patch data to and from Damage Control effects pedels"
  :url "http://github.com/luntar/dcpm.git"
  :license {:name "Eclipse Public License"
  :url "http://www.eclipse.org/legal/epl-v10.html"}
 ; :offline? no 
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.cli "0.2.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [hiccup "1.0.2"]]
  :uberjar-name "../dcpm.jar"
  :main dcpm.core
)

