
(ns dcu.util
  )

(defn delay-ms
  "delay thread by givin ms"
  [ms] 
  (Thread/sleep ms))

;; Thanks to Timothy Pratley and stackoverflow
;; http://stackoverflow.com/questions/1683680/clojure-with-timeout-macro
(defmacro time-limited [ms & body]
  `(let [f# (future ~@body)]
     (.get f# ~ms java.util.concurrent.TimeUnit/MILLISECONDS)))

;; System helper fns shamelessly yanked 
;; from Overtone, thanks to Sam Aaron and Jeff Rose
;; TODO: figure out a better way of sharing code 
(defn system-user-name
  "returns the name of the current user"
  []
  (System/getProperty "user.name"))
(defn get-os
  "Return the OS as a keyword. One of :windows :linux :max"
  []
  (let [os (System/getProperty "os.name")]
    (cond
      (re-find #"[Ww]indows" os) :windows
      (re-find #"[Ll]inux" os)   :linux
      (re-find #"[Mm]ac" os)     :mac)))
(defn classpath-seq
  "Return the the classpath as a seq"
  []
  (map (memfn getPath)
       (seq (.getURLs (.getClassLoader clojure.lang.RT)))))
(defn windows-os?
  "Returns true if the current os is windows based"
  []
  (= :windows (get-os)))
(defn linux-os?
  "Returns true if the current os is mac based"
  []
  (= :linux (get-os)))
(defn mac-os?
  "Returns true if the current os is bac based"
  []
  (= :mac (get-os)))
