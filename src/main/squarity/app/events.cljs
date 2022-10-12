(ns squarity.app.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path]]
            [squarity.app.chess :as chess]))

(def default-config
  {:time-to-solve 20000
   :time-resolution 100
   :timeout 500})

(reg-event-db
 :init-db
 (fn []
   {:game-phase :not-started
    :clock (js/setInterval #(re-frame/dispatch [:tick]) (:time-resolution default-config))
    :score 0
    :time (:time-to-solve default-config)
    :config default-config}))

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
      (assoc :game-phase :game-over)))

(defn incorrect
  [db]
  (assoc-in db [:current-question :phase] :incorrect))

(defn timeout
  [db]
  (assoc db :timeout (get-in db [:config :timeout])))

(reg-event-db
 :start-new-game
 (fn [db]
   (-> db
       (assoc :game-phase :in-progress
              :score 0
              :time (get-in db [:config :time-to-solve]))
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
         :else (-> db game-over incorrect))))))

(reg-event-db
 :tick
 (fn [db]
   (cond-> db
     (#{:in-progress} (:game-phase db)) (as-> db
                                          (update db :time - (get-in db [:config :time-resolution]))
                                          (cond-> db
                                            (<= (:time db) 0) (-> game-over timeout)))
     :always                             (update :timeout - (get-in db [:config :time-resolution])))))
