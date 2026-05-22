(ns squarity.app.game.events
  (:require [re-frame.core :as re-frame :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path]]
            [squarity.app.chess :as chess]))

(def default-config
  {:time-to-solve 20000 ; The duration of one game
   :time-resolution 100 ; The number of ms the game clock (:time) is decremented at a time
   :lockout-duration 0  ; When a game ends by timeout, a "lockout" is enstated lasting this number of milliseconds
   :warning-threshold 5000}) ; Timer turns red below this many ms remaining

(reg-event-db
 :init-db
 (fn []
   {:game-phase {:status :not-started}
    :clock (js/setInterval #(re-frame/dispatch [:tick]) (:time-resolution default-config))
    :score 0
    :time (:time-to-solve default-config)
    :lockout-remaining 0
    :config default-config
    :panel :main
    :hotkeys {:main {:keydown {"l" [:answer :light]
                               "d" [:answer :dark]
                               " " [:start-new-game]}}}}))

(defn random-square
  []
  (rand-int 64))

(defn pose-new-question
  [db]
  (let [previous-square (get-in db [:current-question :square])
        new-square (first (remove #{previous-square} (repeatedly random-square)))]
    (assoc db :current-question {:square new-square})))

(defn- in-progress? [db]
  (= :in-progress (get-in db [:game-phase :status])))

(defn- timer-expired? [db]
  (<= (:time db) 0))

(defn game-over
  [db reason]
  (assoc db :game-phase {:status :game-over :reason reason}))

(defn start-lockout
  [db]
  (assoc db :lockout-remaining (get-in db [:config :lockout-duration])))

(defn start-new-game [db]
  (-> db
      (assoc :game-phase {:status :in-progress}
             :score 0
             :time (get-in db [:config :time-to-solve]))
      (pose-new-question)))

(reg-event-db :start-new-game start-new-game)

(defn handle-answer [db [_ answer]]
  (if-not (in-progress? db)
    db
    (let [{:keys [square]} (:current-question db)]
      (if (= (chess/color-of square) answer)
        (-> db (update :score inc) pose-new-question)
        (-> db (game-over :wrong-answer))))))

(reg-event-db :answer handle-answer)

(defn- advance-timer [db time-elapsed]
  (let [db (update db :time - time-elapsed)]
    (cond-> db
      (timer-expired? db) (-> (game-over :timeout) start-lockout))))

(defn handle-tick [db]
  (let [time-elapsed (get-in db [:config :time-resolution])
        db           (update db :lockout-remaining - time-elapsed)]
    (cond-> db
      (in-progress? db) (advance-timer time-elapsed))))

(reg-event-db :tick handle-tick)


(reg-event-fx
 :keyboard
 (fn [{:keys [db]} [_ press-type {:keys [k]}]]
   (when-let [event (get-in db [:hotkeys (:panel db) press-type k])]
     {:dispatch event})))

(reg-event-fx
 :log
 (fn [_ [_ msg]]
   {:log msg}))