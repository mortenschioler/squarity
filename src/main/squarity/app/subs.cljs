(ns squarity.app.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :game-phase
 (fn [db _]
   (:game-phase db)))

(reg-sub
 :current-question
 (fn [db]
   (:current-question db)))
