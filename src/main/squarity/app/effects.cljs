(ns squarity.app.effects
  (:require 
   [re-frame.core :as re-frame :refer [reg-fx]]))

(reg-fx :log println)
