(ns dcu.gui
  (:require [seesaw.bind :as bind])
  (:use [seesaw.core])
  (:require [dcu.midi-ctrl :as mctrl])
  (:use [dcu.midi-protocol])
  (:require [dcu.midi :as midi])
  (:require [seesaw.rsyntax :as rsyntax])
  (:use [dcu.data-tools])
  (:require dcu.settings)
  (:require [seesaw.bind :as b])
  (:use seesaw.core
        seesaw.chooser
        seesaw.dev
        seesaw.swingx
        seesaw.mig
        [seesaw.widgets.log-window]
        [clojure.java.io :only [file]]))

(seesaw.dev/debug!)
(native!)

(def current-file (atom (file (System/getProperty "user.home") ".presetscratch")))
(when-not (.exists @current-file) (spit @current-file ""))
(def current-file-label (label :text @current-file :font "COURIER-PLAIN-8"))

(def log-win (log-window :id :log-window :limit nil))

(defn say [s] (log log-win (str s "\n")))
(defn say-later [s] (invoke-later (log log-win (str s "\n"))))


(def device-label (label :text "no device"))
(defn device-label! [& strings] (text! device-label (apply str strings)))
(def port-label (label :text "no port"))
(defn port-label! [& strings] (text! port-label (apply str strings)))

(def pnames* (atom []))
(defn push-name
  [n] (swap! pnames* #(cons n %)))

(def mports* (atom []))
(defn push-mports
  [n] (swap! mports* #(cons n %)))

(def cb (combobox :model @mports*))

(def lb (listbox :model @pnames*))

(defn lb-select-action [e]
          (when-let [s (selection e)]
              (log log-win (str s "\n"))))

;(defn c-action [e] (mctrl/connect))

(def bl (busy-label :text "Working ..." :visible? false :busy? true))

(def c-btn (button :id :c-btn :text "Connect"))
(def s-btn (button :id :s-btn :text "Device Scan"))
(def d-btn (button :id :d-btn :enabled? false :text "Disconnect"))
(def dmp-btn (button :text "Dump"))
(def sync-btn (button :text "Sync Patches"))

(defn c-action
  "Connection button action"
  [e]
  (future 
    (let [ _ (config! c-btn :enabled? false)
          _ (config! bl :visible? true)
          rst (try (mctrl/connect) (catch Exception e nil)) ]
      (invoke-later 
       (if rst
        (do 
          (port-label! (:name @dcu.midi-ctrl/mo*)) 
          (device-label! (dcu.midi-protocol/dev-name))
          (config! d-btn :enabled? true))
         (do 
          (device-label!  "error")
          (config! c-btn :enabled? true))
         )
        (config! bl :visible? false)))))
;(set-status (str "Device: " dev ))

(defn d-action 
  "Disconnect button action"
  [e] 
  (do
    (config! c-btn :enabled? true)
    (mctrl/disconnect)))

(defn sync-action
  [e]
  (config! sync-btn :enabled? false)
  (future 
    (do
    (mctrl/foreach-preset 0 199 #(push-name (dcu.midi-protocol/preset-get-name %)))
    (invoke-later (config! sync-btn :enabled? true)))))

(defn scan-action
  "Scan for supported devices TODO: for not just dump the porst in the log"
  [e]
  (future
    (reset! mports* [])
   (doseq [p (dcu.midi/midi-sinks)]
     (push-mports (:name p)))))

;(dcu.midi-ctrl/restore-presets file-name))
(def status-panel
  (border-panel
  :id :status-panel
  :west device-label
  :center port-label 
  :east bl 
  :vgap 0 :hgap 100 :border 5))

(def main-panel 
  (border-panel
  :id :main-panel
  :north (horizontal-panel :items (list s-btn c-btn d-btn dmp-btn sync-btn cb) )
  :center (left-right-split (scrollable lb) (scrollable log-win) :divider-location 1/3)
  :south status-panel 
  :vgap 5 :hgap 5 :border 5))
;(horizontal-panel :items (list device-label port-label))

(defn set-current-file [f] (swap! current-file (constantly f)))
(defn select-file [type] (choose-file main-panel :type type))

(defn a-new [e]
  (let [selected (select-file :save)] 
    (if (.exists @current-file)
      (alert "File already exists.")
      (do (set-current-file selected)
          (say @current-file)
          ))))

(defn a-open [e]
  (let [selected (select-file :open)] (set-current-file selected))
  (future
    (do
    (say @current-file)
    (load-presets @current-file)
    (say-later (dcu.data-tools/dump-presets)))))

(defn a-save [e]
  (spit @current-file (text log-win)))

(defn a-save-as [e]
  (when-let [selected (select-file :save)]
    (set-current-file selected)
    (spit @current-file (text log-win))
    ))

(defn a-exit  [e] (dispose! e))
(defn a-copy  [e] (.copy log-win))
(defn a-cut   [e] (.cut log-win))
(defn a-paste [e] (.paste log-win))

(def menus
     (let [a-new (action :handler a-new :name "New" :tip "Create a new file." :key "menu N")
           a-open (action :handler a-open :name "Open" :tip "Open a file" :key "menu O")
           a-save (action :handler a-save :name "Save" :tip "Save the current file." :key "menu S")
           a-exit (action :handler a-exit :name "Exit" :tip "Exit the log-win")
           a-copy (action :handler a-copy :name "Copy" :tip "Copy selected text to the clipboard." :key "menu C")
           a-paste (action :handler a-paste :name "Paste" :tip "Paste text from the clipboard." :key "menu V")
           a-cut (action :handler a-cut :name "Cut" :tip "Cut text to the cl(defn decorate " :key "menu X")
           a-save-as (action :handler a-save-as :name "Save As" :tip "Save the current file." :key "menu shift S")]
       (menubar
        :items [(menu :text "File" :items [a-new a-open a-save a-save-as a-exit])
                (menu :text "Edit" :items [a-copy a-cut a-paste])])))

(defn make-frame 
  "Create the main frame for the gui"
  []
  (frame
    :title "Damage Control Preset Librarean"
    :content main-panel
    :minimum-size [(* 1 640) :by (* 0.5 480)]
    :on-close   :dispose ;;:exit
    :menubar menus))

(defn show []
  (-> (make-frame) pack! show!))

(defn execute-gui
  "Setup gui application and run"
  [options]
  (let [ _ (show)]
    (listen c-btn :action c-action )
    (listen d-btn :action d-action )
    (listen s-btn :action scan-action)
    (listen dmp-btn :action (fn [e] (say-later (dec2str @presets*))))
    (listen sync-btn :action sync-action)
    (listen lb :selection lb-select-action)
    (listen cb :selection (fn [e] (dcu.settings/config-write! :port (selection e))))
    
    (bind/bind pnames* (bind/property lb :model))
    (bind/bind mports* (bind/property cb :model))
    (push-mports (dcu.settings/config-read :port))
    ))

(comment
  (-> f pack! show!)
  (display main- panel)
  (select f [:#connect])

  (property c-btn :enabled?)
  (config! c-btn :enabled? true)
  )

(comment

  (execute-gui 0))
