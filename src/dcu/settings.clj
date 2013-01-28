(ns dcu.settings
  (:use [clj-time.local :only [local-now]])
  (:import [java.io FileOutputStream FileInputStream])
  (:use [clojure.pprint])
  (:use dcu.util))

;; This should be temporay...
(def dcu_storage (constantly :file))

;; Store interface:
(defmulti write-file-store dcu_storage)
(defmulti read-file-store  dcu_storage)

(def DCU_F-LOCK :lock)

;; Simple file-based dcu_storage
(defmethod write-file-store :file
  [path data]
  (locking DCU_F-LOCK
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

(def DCU_CONFIG-DEFAULTS
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

(defonce dcu_config* (ref {}))
(defonce dcu_live-config (partial live-file-store dcu_config*))

(defn- load-config-defaults
  []
  (dosync
   (dorun
    (map (fn [[k v]]
           (when-not (contains? @dcu_config* k)
             (alter dcu_config* assoc k v)))
         DCU_CONFIG-DEFAULTS))))


(defn config-read
  "Get config value. Returns default if specified and the config does
  not contain key."
  ([key]
     (get @dcu_config* key))
  ([key not-found]
     (let [c @dcu_config*]
       )))

(defn config-write!
  "Set config key to val"
  [k v]
  (dosync
   (alter dcu_config* assoc k v)))

(defn dcu_config
  "Get the full config map"
  []
  @dcu_config*)

(defn dcu_load-config 
  "Load the config data from the file system"
  [path]
  (dosync
    (ref-set dcu_config* (read-file-store path))))

(defonce __DCU_ENSURE-DIRS___
  (ensure-dir-structure))

(defonce __DCU_ENSURE-CONFIG__
  (ensure-config))

(defonce __DCU_LOAD-CONFIG__
  (try
    (do
      (dcu_live-config DCPM-CONFIG-FILE)
      (load-config-defaults)
      )
    (catch Exception e
      (throw (Exception. (str "Unable to load config file - it doesn't appear to be valid clojure. Perhaps it has been modified externally? You may reset it by deleting " DCPM-CONFIG-FILE " and restarting the program. Error: " (.printStackTrace e)))))))

