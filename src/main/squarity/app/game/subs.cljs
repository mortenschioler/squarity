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
 (fn [db] (:game-phase db)))

(reg-sub
 :game-status
 :<- [:game-phase]
 (fn [phase _] (:status phase)))

(reg-sub
 :game-over-reason
 :<- [:game-phase]
 (fn [phase _] (:reason phase)))

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
 :<- [:game-status]
 :<- [:config]
 (fn [[time status config] _]
   (and (= status :in-progress) (< time (:warning-threshold config)))))

(def normal-board (mapv chess/color-of (range 64)))

(def hidden-board (vec (repeat 64 :hidden)))

(reg-sub
 :square-colors
 :<- [:game-status]
 :<- [:current-question]
 :<- [:game-over-reason]
 (fn [[status question reason] _]
   (case status
     :not-started normal-board
     :in-progress (assoc hidden-board (:square question) :active)
     :game-over   (assoc normal-board (:square question)
                         (if (= :wrong-answer reason)
                           (chess/color-of (:square question))
                           :unanswered)))))

(reg-sub
 :incorrect-square
 :<- [:game-status]
 :<- [:current-question]
 :<- [:game-over-reason]
 (fn [[status question reason] _]
   (when (and (= :game-over status) (= :wrong-answer reason))
     (:square question))))
