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
   :active "#334155" ; slate-50
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
   [:g squares]])

(defn <>
  [children]
  (vec (conj children :<>)))

(def colored-squares
  (<> (map-indexed square (map color-of (range 64)))))

(def colorless-squares
  (<> (map (comp with-outline square) (range 64) (repeat (:gray colors)))))

(defn question-board
  [question]
  (assoc-in colorless-squares [(inc question) 1 :fill] (:active colors)))

(defn board
  []
  (let [game-phase @(re-frame/subscribe [:game-phase])
        question @(re-frame/subscribe [:current-question])]
    [board-svg
     (case game-phase
       :not-started colored-squares
       :in-progress [question-board (:square question)])]))


(defn main
  [] 
  (fn []
    [:div
     [board]
     [:button {:on-click #(re-frame/dispatch [:start-new-game])} "Start game"]]))