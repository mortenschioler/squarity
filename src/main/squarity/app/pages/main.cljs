(ns squarity.app.pages.main
  (:require
   [reagent.core :as reagent]
   [reagent.dom :as reagent-dom]
   [re-frame.core :as re-frame]
   [squarity.app.chess :as chess]))

(def square-classes
  {:dark "fill-[#b58863]"
   :light "fill-[#f0d9b5]"
   :hidden "fill-zinc-600 stroke-zinc-800 stroke-[0.035]"
   :active "fill-zinc-50 stroke-zinc-800 stroke-[0.035]"
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
      [:span.text-center.text-5xl.font-mono.text-slate-100.font-semibold.text-shadow
       (chess/name-of (:square question))]]]))

(defn score
  []
  (let [score @(re-frame/subscribe [:score])]
    [:span (str "Score: " score)]))

(defn time-display
  []
  (let [time @(re-frame/subscribe [:time])]
    [:span (.toFixed (/ time 1000) 1)]))

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

(defn main
  []
  [hotkeys
   {"d" #(re-frame/dispatch [:answer :dark])
    "l" #(re-frame/dispatch [:answer :light])
    " " #(re-frame/dispatch [:start-new-game])}

   [:div.max-w-screen-sm.w-full.mx-auto
    [header]
    [board]]])