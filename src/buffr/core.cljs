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

(defonce white-noise-buffer (js/Float32Array. (into-array (repeatedly length #(rand 1)))))
;; all AudioBuffers must implement the Float32Array interface.
;; This array contains 22050 samples of random values between 0 and 1.

(defn fill-buf
  "Fills a supplied AudioContext.Buffer with a supplied Float32Array via .copyToChannel"
  [buf sample-vec]
  (.copyToChannel buf sample-vec 0))

(fill-buf buf white-noise-buffer) ;;Imperatively fill the buffer

(set! (.-buffer audio-source) buf) ;; Set the AudioBufferSource.buffer to our buffer

(set! (.-loop audio-source) true) ;; Set the loop conditional to true (it is natively false)

; (.connect audio-source gain)

; (.connect audio-source (.-destination ctx)) ;; Connect the audio graph


(defn mic-connect [mic-node] (.connect mic-node (.-destination ctx)))

(defn mic-handler [stream] (mic-connect (.createMediaStreamSource ctx stream)))

(js/navigator.mediaDevices.getUserMedia {:audio true} mic-handler js/console.warn)
(.start audio-source) ;; hell yeah, we've got some sound

(defonce app-state (atom {:text "Hello world!"}))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
)
