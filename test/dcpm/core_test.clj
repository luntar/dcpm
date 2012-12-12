(ns dcpm.core-test
  (:use clojure.test
        dcpm.core)
  (:require dcu.midi-ctrl)
  (:require dcu.util)
  (:use dcu.sx-data)
  )
(defonce PORT_NAME "Port")


(deftest data-queue
         (testing "data queue must be empty after clear"
                  (dcu.sx-data/sx-clear)
                  (is (= (sx-len) 0))))

(deftest a-test
  (testing "MIDI init"
    (is 
      (dcu.midi/midi-ports))))

(deftest id-test 
  (testing "id-dev test"
    (is (dcu.midi-ctrl/init-midi PORT_NAME))
    (sx-clear)
    (dcu.midi-ctrl/id-dev)
    (is (= () (sx-pop)))))

(deftest get-patch-test 
  (testing "get-patch"
           (is (dcu.midi-ctrl/init-midi PORT_NAME))
           (dcu.midi-ctrl/id-dev)
           (is  (dcu.midi-ctrl/get-patch 1))))

(deftest check-timeout-code
         (testing "tim-limited will throw an exception"
            (is (thrown? java.lang.RuntimeException  
                         (dcu.util/time-limited 1000 (Thread/sleep 1200))))))
