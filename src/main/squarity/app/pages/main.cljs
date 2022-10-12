(ns squarity.app.pages.main
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as reagent-dom]
   [re-frame.core :as re-frame]
   [squarity.app.chess :as chess]))

(def square-classes
  {:dark "fill-[#b58863]"
   :light "fill-[#f0d9b5]"
   :hidden "fill-gray-500 stroke-gray-700 stroke-[0.02]"
   :active "fill-gray-200 stroke-gray-700 stroke-[0.02]"
   :incorrect "fill-red-600"
   :unanswered "fill-gray-200"})

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

(defn settings
  []
  [:div.mx-auto.h-40.w-full.max-w-screen-sm.bg-green-100
   [:div.bg-yellow-100.text-center
    [:label {:for "foo"} "Foo"]
    [:input#foo.w-120.inline.text-center.outline-none
     {:step "100",
      :width "2",
      :min "0",
      :value "42",
      :type "number"}]]])
  
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
    {:class "rounded bg-[#b58863] py-3 text-lg text-gray-800 shadow-lg focus:outline-none"
     :on-click #(re-frame/dispatch [:answer :dark])
     :title "Guess square is dark (hotkey 'd')"}
    "dark"]
   [:button
    {:class "rounded bg-[#f0d9b5] py-3 text-lg text-gray-800 shadow-lg focus:outline-none"
     :on-click #(re-frame/dispatch [:answer :light])
     :title "Guess square is light (hotkey 'l')"}
    "light"]])

(defn new-game-button
  []
  [:div
   [:button
    {:class "w-full rounded bg-green-300 disabled:bg-gray-300 disabled:text-gray-500 p-3 text-lg text-gray-800 shadow-lg focus:outline-none"
     :disabled @(re-frame/subscribe [:timeout])
     :on-click #(re-frame/dispatch [:start-new-game])
     :title "Hotkey: Spacebar"}
    "Start new game"]])

(defn setting
  [[k v]]
  [:div
   {:key k}
   [:label.mr-8
    {:for k}
    k]
   [:input
    {:type "number"
     :defaultValue v
     :id k
     :name k}]])

(defn settings-overlay
  []
  (let [hidden (not  @(re-frame/subscribe [:settings-open]))
        config @(re-frame/subscribe [:config])]
    [:div
     {:class (when hidden "hidden")}
     [:div.fixed.h-screen.w-full.bg-gray-300.top-0.left-0.opacity-50.z-10] 
     [:div.fixed.z-20.h-screen.w-full.top-0.left-0.text-center.align-middle
      [:div.py-40.max-w-screen-sm.bg-white.m-auto.rounded-lg.mt-12
       (into [:<> (map setting config)])]]]))

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
   [:div
    [:div.max-w-screen-sm.w-full.mx-auto.h-screen-bg-gray-50
     [header]
     [board]
     [:div.mt-2 
      [footer]]
     [:button
      {:on-click #(re-frame/dispatch [:open-settings])}
      "Open settings"]]
    [settings-overlay]]])