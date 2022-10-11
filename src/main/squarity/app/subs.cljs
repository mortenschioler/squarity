(ns squarity.app.subs
  (:require [re-frame.core :refer [reg-sub]]
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
     :game-over (assoc normal-board (:square question) :incorrect))))
