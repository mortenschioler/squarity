(ns squarity.app.subs
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
 :board-visibility
 (fn [db _]
   (:board-visibility db)))
