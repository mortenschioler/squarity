(ns squarity.app.pages.main
  (:require [re-frame.core :as re-frame]
            [squarity.app.chess :as chess]))

(def colors
  {:dark "#b58863" ; lichess dark
   :light "#f0d9b5" ; lichess light
   :gray "#f1f5f9" ; slate-100
   :active "#334155" ; slate-50
   :incorrect "#dc2626" ; red-600
   })

(defn color-of
  [i]
  (colors (chess/color-of i)))

(defn square
  [i color]
  [:rect {:width 1 :height 1 :fill color :id i :key i
          :x (chess/x i) :y (chess/y i)}])

(defn with-outline
  [square]
  (update square 1 assoc :stroke "#111111" :stroke-width "0.035"))

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

(defn answer-board
  [answer]
  (assoc-in colored-squares [(inc answer) 1 :fill] (:incorrect colors)))

(defn board
  []
  (let [game-phase @(re-frame/subscribe [:game-phase])
        question @(re-frame/subscribe [:current-question])]
    [board-svg
     (case game-phase
       :not-started colored-squares
       :in-progress [question-board (:square question)]
       :game-over   [answer-board (:square question)])]))


(defn score
  []
  (let [score @(re-frame/subscribe [:score])]
    [:span (str "Score: " score)]))


(defn hotkeys
  [bindings & elements]
  [:div {:tab-index 0
         :on-key-down-capture (fn [e]
                                (when-let [f (get bindings (.-key e))]
                                  (f e)))}
   [:<> elements]])

(defn main
  []
  [hotkeys
   {"d" #(re-frame/dispatch [:answer :dark])
    "l" #(re-frame/dispatch [:answer :light])
    " " #(re-frame/dispatch [:start-new-game])}
   [:div
    [board]
    [:button {:on-click #(re-frame/dispatch [:start-new-game])} "Start game"]
    [:button {:on-click #(re-frame/dispatch [:answer :light])} "Light"]
    [:button {:on-click #(re-frame/dispatch [:answer :dark])} "Dark"]
    [score]]])