(ns squarity.app.game.subs
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

(reg-sub
 :locked-out?
 (fn [db]
   (pos? (:lockout-remaining db))))

(reg-sub
 :config
 (fn [db] (:config db)))

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
     :game-over (let [sq    (:square question)
                      phase (:phase question)]
                  (assoc normal-board sq
                         (if (= phase :incorrect)
                           (keyword (str "incorrect-" (name (chess/color-of sq))))
                           phase))))))
