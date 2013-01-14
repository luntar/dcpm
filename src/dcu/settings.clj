(ns dcu.settings
  (:require [clojure.string ])
  (:use [clj-time.local :only [local-now]])
  (:import [java.io FileOutputStream FileInputStream])
  (:use [clojure.pprint])
  (:use dcu.util))

;; This should be temporay...
(def storage (constantly :file))

;; Store interface:
(defmulti write-file-store storage)
(defmulti read-file-store  storage)

(def F-LOCK :lock)

;; Simple file-based storage
(defmethod write-file-store :file
  [path data]
  (locking F-LOCK
    (spit path (with-out-str (pprint data)))))

(defmethod read-file-store :file
  [path]
  (with-open [file (FileInputStream. path)]
    (read-string (slurp file))))

(def DCPM-DIRS
  (let [root   (str (System/getProperty "user.home") "/.dcpm")
        log    (str root "/log")
        backup (str root "/backup")]
      {:root root
       :log log
       :backup backup }))

(def DCPM-CONFIG-FILE (str (:root   DCPM-DIRS) "/config.clj"))
(def DCPM-BACKUP-FILE (str (:backup DCPM-DIRS) "/backup.clj"))
(def DCPM-LOG-FILE    (str (:log    DCPM-DIRS) "/dcpm.log"))

(def CONFIG-DEFAULTS
  {:os (get-os)
   :user-name (capitalize (system-user-name))
   :port "Port"})

(defn- ensure-dir-structure
  []
  (dorun
   (map #(mkdir! %) (vals DCPM-DIRS))))

(defn- ensure-config
  "Creates empty config file if one doesn't already exist"
  []
  (when-not (file-exists? DCPM-CONFIG-FILE)
    (write-file-store DCPM-CONFIG-FILE {})))

(defn live-file-store
  "Uses the file-store located at the given path. Restores the file-store if it
  already exists and persists any changes to disk as they occur.

  (def data (ref {}))
  (live-store data \"~/.app-data\")"
  [reference path & [initial-value]]
  (dosync
   (ref-set reference (or initial-value
                          (read-file-store path))))
  (add-watch reference ::live-file-store
             (fn [k r old-state new-state]
                (when-not (= old-state new-state)
                  (write-file-store path new-state)))))

(defonce config* (ref {}))
(defonce live-config (partial live-file-store config*))

(defn- load-config-defaults
  []
  (dosync
   (dorun
    (map (fn [[k v]]
           (when-not (contains? @config* k)
             (alter config* assoc k v)))
         CONFIG-DEFAULTS))))


(defn config-get
  "Get config value. Returns default if specified and the config does
  not contain key."
  ([key]
     (get @config* key))
  ([key not-found]
     (let [c @config*]
       )))

(defn config-set!
  "Set config key to val"
  [k v]
  (dosync
   (alter config* assoc k v)))

(defn config
  "Get the full config map"
  []
  @config*)

(defn load-config 
  "Load the config data from the file system"
  [path]
  (dosync
    (ref-set config* (read-file-store path))))

(defonce __ENSURE-DIRS___
  (ensure-dir-structure))

(defonce __ENSURE-CONFIG__
  (ensure-config))

(defonce __LOAD-CONFIG__
  (try
    (do
      (live-config DCPM-CONFIG-FILE)
      (load-config-defaults)
      )
    (catch Exception e
      (throw (Exception. (str "Unable to load config file - it doesn't appear to be valid clojure. Perhaps it has been modified externally? You may reset it by deleting " DCPM-CONFIG-FILE " and restarting the program. Error: " (.printStackTrace e)))))))

