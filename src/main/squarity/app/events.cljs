(ns squarity.app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path]]))

(reg-event-db
 :init-db
 (constantly {:game-phase :not-started}))

(defn random-square
  []
  (rand-int 64))

(defn pose-new-question
  [db]
  (assoc db :current-question {:square (random-square) :phase :unanswered}))

(reg-event-db
 :start-new-game
 (fn [db]
   (-> db
       (assoc :game-phase :in-progress)
       (pose-new-question))))
