;  Copyright (c) Dave Ray, 2011. All rights reserved.

;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this 
;   distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "Functions to aid in debugging."
      :author "Dave Ray"} 
  seesaw.debug
  (:require [seesaw.core :as core]))


(defonce ^{:private true} error-frame 
  (delay
    (core/frame
      :title "Seesaw - Unhandled Exception"
      :size [800 :by 600]
      :on-close :dispose
      :content
        (core/border-panel
          :hgap 5 :vgap 5
          :border 5
          :north (core/label :id :header)
          :center 
            (core/top-bottom-split
              (core/scrollable (core/text :id :event
                                          :editable? false 
                                          :wrap-lines? true
                                          :multi-line? true)
                               :border "Current Event")
              (core/scrollable (core/text :id :throwable
                                          :editable? false 
                                          :multi-line? true)
                               :border "Stack Trace")
              :divider-location 1/3)))))

(defn- set-error-frame-content [frame event ^java.lang.Throwable throwable]
  (let [h (core/select frame [:#header])
        e (core/select frame [:#event])
        t (core/select frame [:#throwable])]
    (-> h
      (core/text! (format "Unhandled exception at %s" (java.util.Date.))))
    (->
      (core/text! e (str event))
      (core/scroll! :to :top))
    (->
      (core/text! t (let [sw (java.io.StringWriter.)
                        pw (java.io.PrintWriter. sw)
                        _  (.printStackTrace throwable pw)
                        _  (.flush pw)] 
                    (.toString sw)))
      (core/scroll! :to :top)))
  frame)

(defn- show-error-frame [event throwable]
  (->
    (force error-frame) 
    (set-error-frame-content event throwable)
    core/show!
    (core/move! :to-front)))

(defonce ^{:private true :tag seesaw.ExceptionHandler} handler (seesaw.ExceptionHandler.))

(defn debug! 
  "Install a custom exception handler which displays a window with event and
  stack trace info whenever an unhandled exception occurs in the UI thread.
  This is usually more friendly than the console, especially in a repl.
  
  Calling with no args, enables default debugging. Otherwise, pass a two arg
  function that takes a java.awt.AWTEvent and a java.lang.Throwable. Passing
  nil disables debugging.
  "
  ([] (debug! show-error-frame))
  ([f] (core/invoke-now
    (let [q (.. (java.awt.Toolkit/getDefaultToolkit) getSystemEventQueue)
          installed? (identical? q handler)] 
      (cond
        (and f installed?)       (do (.setHandler handler f) true)
        (and f (not installed?)) (do (.push q (.setHandler handler f)) true)
        (and (not f) installed?) (do (.setHandler handler nil) false)
        :else                    false)))))

