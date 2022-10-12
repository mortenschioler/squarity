(ns squarity.app.pages.main
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as reagent-dom]
   [re-frame.core :as re-frame]
   [squarity.app.chess :as chess]))

(def square-classes
  {:dark "fill-[#b58863]"
   :light "fill-[#f0d9b5]"
   :hidden "fill-gray-600 stroke-gray-800 stroke-[0.035]"
   :active "fill-gray-50 stroke-gray-800 stroke-[0.035]"
   :incorrect "fill-red-600"})

(defn square
  [i class]
  [:rect {:width 1 :height 1 :id i :key i :x (chess/x i) :y (chess/y i) :class class}])

(defn board-svg
  [square-colors]
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :xlmns:x "http://www.w3.org/1999/xlink"
         :viewBox "0 0 8 8"
         :shape-rendering "crispEdges"
         :class "w-full"}
   [:g [:<> (map-indexed square (map square-classes square-colors))]]])

(defn board
  []
  (let [square-colors @(re-frame/subscribe [:square-colors])
        question @(re-frame/subscribe [:current-question])]
    [:div.relative
     [board-svg square-colors]
     [:div.absolute.inset-0.flex.flex-col.justify-center
      [:span.text-center.text-5xl.font-mono.text-gray-100.font-semibold.text-shadow
       (chess/name-of (:square question))]]]))

(defn score
  []
  (let [score @(re-frame/subscribe [:score])]
    [:<> [:span.text-gray-500.text-sm "Score " ] [:span score]]))

(defn time-display
  []
  (let [time @(re-frame/subscribe [:time])]
    [:span (str (.toFixed (/ time 1000) 1)  "s")]))
  
(defn header
  []
  [:div
   {:class "py-2 text-center text-xl font-semibold text-gray-700"}
   [:span
    {:class "w-1/2 inline-block"}
    [score]]
   [:span
    {:class "w-1/2 inline-block"}
    [time-display]]])

(defn guessing-buttons
  []
  [:div.grid.grid-cols-2.gap-x-2
   [:button
    {:class "rounded bg-[#b58863] py-3 text-lg text-gray-800 shadow-lg"
     :on-click #(re-frame/dispatch [:answer :dark])
     :title "Guess square is dark (hotkey 'd')"}
    "dark"]
   [:button
    {:class "rounded bg-[#f0d9b5] py-3 text-lg text-gray-800 shadow-lg"
     :on-click #(re-frame/dispatch [:answer :light])
     :title "Guess square is light (hotkey 'l')"}
    "light"]])

(defn new-game-button
  []
  [:div
   [:button
    {:class "w-full rounded bg-gray-200 py-3 text-lg text-gray-800 shadow-lg"
     :on-click #(re-frame/dispatch [:start-new-game])
     :title "Hotkey: Spacebar"}
    "Start new game"]])

(defn footer
  []
  (let [gamestate @(re-frame/subscribe [:game-phase])]
    [:div
     [:div
      {:class (when-not (#{:in-progress} gamestate) "hidden")}
      [guessing-buttons]]
     [:div
      {:class (when (#{:in-progress} gamestate) "hidden")}
      [new-game-button]]]))

(defn hotkeys
  [bindings element]
  (reagent/create-class
   {:display-name "hotkeys"
    :component-did-mount #(.focus (reagent-dom/dom-node %))
    :reagent-render (fn []
                      [:div {:tab-index 0
                             :class "focus:outline-none"
                             :on-key-down-capture (fn [e]
                                                    (when-let [f (get bindings (.-key e))]
                                                      (f e)))}
                       element])}))

(defn page
  []
  [hotkeys
   {"d" #(re-frame/dispatch [:answer :dark])
    "l" #(re-frame/dispatch [:answer :light])
    " " #(re-frame/dispatch [:start-new-game])}
   [:div.max-w-screen-sm.w-full.mx-auto
    [header]
    [board]
    [:div.mt-2
     [footer]]]])