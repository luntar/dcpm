(ns dcu.util
 (:import [java.net URL]
           [java.io StringWriter])
 (:use [clojure.java.io]))

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
(defn chop-first-n
  "Removes the first n chars in str. Returns empty string if n is >= str length."
  [n str]
  (let [size (count str)]
    (if (< size n)
      ""
      (subs str n size))))

(defn system-user-name
  "returns the name of the current user"
  []
  (System/getProperty "user.name"))

(defn home-dir
  "Returns the user's home directory"
  []
  (System/getProperty "user.home"))
(defn file?
  "Returns true if f is of type java.io.File"
  [f]
  (= java.io.File (type f)))

(defn file-separator
  "Returns the system's file separator"
  []
  java.io.File/separator)

(declare mk-path)

;; System helper fns shamelessly yanked
;; from Overtone, thanks to Sam Aaron and Jeff Rose
(defn resolve-tilde-path
  "Returns a string which represents the resolution of paths starting with
   ~ to point to home directory."
  [path]
  (let [path (if (file? path)
               (.getCanonicalPath path)
               (str path))]
    (cond
     (= "~" path)
     (home-dir)

     (.startsWith path (str "~" (file-separator)))
     (mk-path (home-dir) (chop-first-n (inc (count (file-separator))) path))

     :default
     path)))

(defn mk-path
  "Takes a seq of strings and returns a string which is a concatanation of all
  the input strings separated by the system's default file separator."
  [& parts]
  (let [path (apply str (interpose (file-separator) parts))]
    (resolve-tilde-path path)))


(defn mkdir!
  "Makes a dir at path if it doesn't already exist."
  [path]
  (let [path (resolve-tilde-path path)
        f    (file path)]
    (when-not (.exists f)
      (.mkdir f))))

;; System helper fns shamelessly yanked
;; from Overtone, thanks to Sam Aaron and Jeff Rose
(defn file-exists?
  "Returns true if a file specified by path exists"
  [path]
  (let [path (resolve-tilde-path path)
        f (file path)]
    (and (.exists f) (.isFile f))))

(defn dir-exists?
  "Returns true if a directory specified by path exists"
  [path]
  (let [path (resolve-tilde-path path)
        f (file path)]
    (and (.exists f) (.isDirectory f))))


(defn get-os
  "Return the OS as a keyword. One of :windows :linux :max"
  []
  (let [os (System/getProperty "os.name")]
    (cond
      (re-find #"[Ww]indows" os) :windows
      (re-find #"[Ll]inux" os)   :linux
      (re-find #"[Mm]ac" os)     :mac)))

;; System helper fns shamelessly yanked
;; from Overtone, thanks to Sam Aaron and Jeff Rose
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

(defn capitalize
  "Make the first char of the text uppercase and leave the rest unmodified"
  [text]
  (let [first-char (.toUpperCase (str (first text)))
        rest-chars (apply str (rest text))]
    (str first-char rest-chars)))
