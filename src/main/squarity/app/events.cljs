(ns squarity.app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path]]
            [squarity.app.chess :as chess]))

(reg-event-db
 :init-db
 (constantly {:game-phase :not-started}))

(defn random-square
  []
  (rand-int 64))

(defn pose-new-question
  [db]
  (let [previous-square (get-in db [:current-question :square])
        new-square (first (remove #{previous-square} (repeatedly random-square)))]
    (assoc db :current-question {:square new-square :phase :unanswered})))

(defn game-over
  [db]
  (-> db
      (assoc :game-phase :game-over)
      (assoc-in [:current-question :phase] :incorrect)))

(reg-event-db
 :start-new-game
 (fn [db]
   (-> db
       (assoc :game-phase :in-progress
              :score 0)
       (pose-new-question))))

(reg-event-db
 :answer
 (fn [db [_ answer]] 
   (if-not (= (:game-phase db) :in-progress)
     db
     (let [{:keys [square phase]} (:current-question db)]
       (cond
         (= (chess/color-of square) answer) (-> db
                                                (update :score inc)
                                                (pose-new-question))
         :else (-> db
                   (game-over)))))))