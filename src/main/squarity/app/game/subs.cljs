(ns squarity.app.game.subs
  (:require [clojure.set :as set]
            [re-frame.core :refer [reg-sub]]
            [squarity.app.chess :as chess]))

(reg-sub
  :time
  (fn [db]
    (:time db)))

(reg-sub
 :game-phase
 (fn [db _]
   (:game-phase db)))

(reg-sub
 :current-question
 (fn [db]
   (:current-question db)))

(reg-sub
 :score
 (fn [db]
   (:score db)))

(reg-sub
 :locked-out?
 (fn [db]
   (pos? (:lockout-remaining db))))

(reg-sub
 :config
 (fn [db] (:config db)))

(reg-sub
 :panel
 (fn [db] (:panel db)))

(reg-sub
 :hotkeys
 (fn [db] (:hotkeys db)))

(reg-sub
 :event->hotkey
 :<- [:hotkeys]
 :<- [:panel]
 (fn [[hotkeys panel] _]
   (set/map-invert (get-in hotkeys [panel :keydown]))))

(reg-sub
 :time-warning?
 :<- [:time]
 :<- [:game-phase]
 :<- [:config]
 (fn [[time phase config] _]
   (and (= phase :in-progress) (< time (:warning-threshold config)))))

(def normal-board (mapv chess/color-of (range 64)))

(def hidden-board (vec (repeat 64 :hidden)))

(reg-sub
 :square-colors
 :<- [:game-phase]
 :<- [:current-question]
 (fn [[phase question] _]
   (case phase
     :not-started normal-board
     :in-progress (assoc hidden-board (:square question) :active)
     :game-over (assoc normal-board (:square question)
                       (if (= :incorrect (:phase question))
                         (chess/color-of (:square question))
                         :unanswered)))))

(reg-sub
 :incorrect-square
 :<- [:game-phase]
 :<- [:current-question]
 (fn [[phase question] _]
   (when (and (= phase :game-over) (= :incorrect (:phase question)))
     (:square question))))
