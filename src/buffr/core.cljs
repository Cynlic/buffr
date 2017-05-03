(ns buffr.core
  (:require ))

(enable-console-print!)

;; ***************************************
;; * Application Code for /buffr/ by A&H *
;; ***************************************

;; A first pass at the  app which uses the default Figwheel template and the WebAudio API.
;; Roughly, it creates the various state objects required to costruct WebAudio's
;; graph representation, generates a vector of white noise (random values between 0 and 1),
;; sets the buffer of an AudioSourceNode to said vector, connects the varios components
;; and plays the noise.

;; Known Bugs: When editing the file, the defonce function doesn't seem to work for the
;; white noise generation -- each time the file loads a sonically "weaker" version of the
;; noise is generated. This may be due to some kind of frequency cancelation where the noise
;; actually layered, not replaced...


(defn make-context
  "Creates an audio context -- to be used with global state atom ctx."
  []
  (let [constuctor (or js/window.AudioContext
                       js/window.webkitAudioContext)]
    (constuctor.)))

(defonce ctx (make-context)) ;; this is our master context
(def length 22050) ;; length of the buffer

(def buf (.createBuffer ctx 1 length 44100))
;; Create the buffer to assign to our AudioSource.
;; Note the WebAudio pattern of using AudioContext's member functions to construct various objects

(def audio-source (.createBufferSource ctx)) ;; the node which contains the buffer and plays it

(def gain (.createGain ctx)) ;; a volume node! for volume!

(def a-mic (atom 0))  ;; this is an atom which evetually is assigned the value of the user's microphone MediaStreamSource object. It is needed to prevent garbage collection issues with Firefox.

(defonce white-noise-buffer (js/Float32Array. (into-array (repeatedly length #(rand 1)))))
;; all AudioBuffers must implement the Float32Array interface.
;; This array contains 22050 samples of random values between 0 and 1.

(defn fill-buf
  "Fills a supplied AudioContext.Buffer with a supplied Float32Array via .copyToChannel"
  [buf sample-vec]
  (.copyToChannel buf sample-vec 0))

(fill-buf buf white-noise-buffer) ;; Imperatively fill the buffer

(set! (.-buffer audio-source) buf) ;; Set the AudioBufferSource.buffer to our buffer

(set! (.-loop audio-source) true) ;; Set the loop conditional to true (it is natively false)

(.connect gain (.-destination ctx))

(set! (.-value (.-gain gain)) 1)

(defn mic-connect "This function just patches any audio-in to the gain. Because why not?"
  [mic-node] (.connect mic-node gain))

(defn mic-handler
  "Saves an atomic refrence to the microphone node to prevent garbage collection as well as connecting the reference to the output.This function is a callback, to be used on the js/Promise returned by getUserMedia. Once it is called, a-mic should be manipulatable as an audio node, but using atomic syntax."
  [stream]
  (reset! a-mic (.createMediaStreamSource ctx stream))
  (mic-connect (deref a-mic)))

(defn handler-test []
   (println "hey, I'm being called!"))

(defn error-handler
  "Callback to print any errors associated with the getUserMedia promise."
  [error]
  (println error))

(defn promise-handler
  "Provides some ease of use with the getUserMedia promise, wrapping the success and failure conditions."
  [promise on-success]
  (.catch (.then promise on-success) error-handler))

(promise-handler (js/navigator.mediaDevices.getUserMedia
                  (clj->js {:audio true})) mic-handler)
;; Imperatively connect up the microphone. Move this into a reagent component as soon as possible.

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
