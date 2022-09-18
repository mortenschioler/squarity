(ns squarity.app.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path]]))

(reg-event-db
 :init-db
 (constantly {:board-visibility :colored}))

(reg-event-db
 :swap-board-color
 (fn [db]
   (update db :board-visibility #(case % :blank :colored :colored :blank))))
