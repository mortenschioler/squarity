(ns squarity.app.pages.main
  (:require [re-frame.core :as re-frame]))

(defn x
  [i]
  (mod i 8))

(defn y
  [i]
  (quot i 8))

(def colors
  {:dark "#b58863" ; lichess dark
   :light "#f0d9b5" ; lichess light
   :gray "#f1f5f9" ; slate-100
   })

(defn square
  [i color]
  [:rect {:width 1 :height 1 :fill color :id i :key i
          :x (x i) :y (y i)}])

(defn with-outline
  [square]
  (update square 1 assoc :stroke "#111111" :stroke-width "0.035"))

(defn light?
  [i]
  ; The fact that the width/height of a chess board, 8,
  ; is a power of the binary base, lets us take advantage of bit
  ; arithmetics. The 0'th bit toggles when x is incremented,
  ; while the 3 bit toglles with y is incremented. This is when
  ; indexing is done from top-left to bottom-right.
  (= (bit-test i 0) (bit-test i 3)))

(defn color-of
  [i]
  (if (light? i)
    (:light colors)
    (:dark colors)))

(defn board-svg
  [squares]
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :xlmns:x "http://www.w3.org/1999/xlink"
         :viewBox "0 0 8 8"
         :shape-rendering "crispEdges"
         :class "w-96 h-96"}
   [:g
    [:<> squares]]])

(def colored-board
  [board-svg (map (fn [i] (square i (color-of i))) (range 64))])

(def colorless-board
  [board-svg (map (fn [i] (with-outline (square i (:gray colors)))) (range 64))])

(defn board-container
  [board-visibility]
  [:div {:class "w-12 h-12"
         :on-click #(re-frame/dispatch [:swap-board-color])}
   (case board-visibility
     :colored colored-board
     :blank colorless-board)])

(defn main
  []
  (fn []
    (let [board-visibility @(re-frame/subscribe [:board-visibility])]
      [board-container 
       board-visibility])))