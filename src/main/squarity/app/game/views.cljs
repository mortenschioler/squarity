(ns squarity.app.game.views
  (:require
   [re-frame.core :as re-frame]
   [squarity.app.chess :as chess]))

(def square-classes
  {:dark       "fill-[#b58863]"
   :light      "fill-[#f0d9b5]"
   :hidden     "fill-gray-500 stroke-gray-700 stroke-[0.02]"
   :active     "fill-gray-200 stroke-gray-700 stroke-[0.02]"
   :unanswered "fill-gray-200"})

(defn key-label [k]
  (let [hotkey (if (= k " ") "Space" (.toUpperCase k))]
    {:tooltip (str "Hotkey: (" hotkey ")")
     :hotkey hotkey}))

(def overlay-stroke-width 0.06)
(def overlay-inset (/ overlay-stroke-width 2))

(defn square
  [i color]
  [:rect {:width 1 :height 1 :id i :key i :x (chess/x i) :y (chess/y i) :class (square-classes color)}])

(defn board-svg
  [square-colors incorrect-sq]
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :xlmns:x "http://www.w3.org/1999/xlink"
         :viewBox "0 0 8 8"
         :shape-rendering "crispEdges"
         :class "w-full"}
   (into [:g] (map-indexed square square-colors))
   (when incorrect-sq
     (let [x (chess/x incorrect-sq) y (chess/y incorrect-sq)]
       [:rect {:x (+ x overlay-inset) :y (+ y overlay-inset)
               :width (- 1 overlay-stroke-width) :height (- 1 overlay-stroke-width)
               :fill "none" :stroke "#dc2626" :stroke-width overlay-stroke-width}]))])

(defn board
  []
  (let [square-colors @(re-frame/subscribe [:square-colors])
        question      @(re-frame/subscribe [:current-question])
        incorrect-sq  @(re-frame/subscribe [:incorrect-square])]
    [:div.relative
     [board-svg square-colors incorrect-sq]
     [:div.absolute.inset-0.flex.flex-col.justify-center
      [:span.text-center.text-5xl.font-mono.text-gray-100.font-semibold.text-shadow
       (chess/name-of (:square question))]]]))

(defn score
  []
  (let [score @(re-frame/subscribe [:score])]
    [:<> [:span.text-gray-500.text-sm "Score " ] [:span score]]))

(defn time-display
  []
  (let [time    @(re-frame/subscribe [:time])
        warning? @(re-frame/subscribe [:time-warning?])]
    [:span {:class (when warning? "text-red-500 font-bold")}
     (str (.toFixed (/ time 1000) 1) "s")]))
  
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
  (let [e->k @(re-frame/subscribe [:event->hotkey])
        e->key-label (comp key-label (partial get e->k))] 
    [:div.grid.grid-cols-2.gap-x-2
     (let [e [:answer :dark]
           {:keys [hotkey tooltip]} (e->key-label e)]
       [:button
        {:class    "rounded bg-[#b58863] py-3 text-lg text-gray-800 shadow-lg focus:outline-none"
         :on-click #(re-frame/dispatch e)
         :title    tooltip}
        (str "dark (" hotkey ")")])
     (let [e [:answer :light]
           {:keys [hotkey tooltip]} (e->key-label e)]
      [:button
       {:class    "rounded bg-[#f0d9b5] py-3 text-lg text-gray-800 shadow-lg focus:outline-none"
        :on-click #(re-frame/dispatch e)
        :title    tooltip}
       (str "light (" hotkey ")")])]))

(defn new-game-button
  []
  (let [e->k @(re-frame/subscribe [:event->hotkey])
        e [:start-new-game]
        {:keys [hotkey tooltip]} (key-label (get e->k e))]
    [:div
     [:button
      {:class     "w-full rounded bg-green-300 disabled:bg-gray-300 disabled:text-gray-500 p-3 text-lg text-gray-800 shadow-lg"
       :disabled  @(re-frame/subscribe [:locked-out?])
       :on-click  #(re-frame/dispatch e)
       :title     tooltip
       :tab-index -1}
      (str "Start new game (" hotkey ")")]]))

(defn footer
  []
  (let [status @(re-frame/subscribe [:game-status])]
    [:div
     (if (= :in-progress status)
       [guessing-buttons]
       [new-game-button])]))

(defn page
  []
  [:div.max-w-screen-sm.w-full.mx-auto.h-screen-bg-gray-50
   [header]
   [board]
   [:div.mt-2
    [footer]]])