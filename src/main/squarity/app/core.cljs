(ns squarity.app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [squarity.app.events]
            [squarity.app.subs]
            [squarity.app.views :as main]))

(defn app
  []
  [main/mainpage])

(defn render 
  []
  (rdom/render [app] (.getElementById js/document "root")))

(defn ^:export main 
  []
  (re-frame/dispatch-sync [:init-db])
  (render))

(defn ^:dev/after-load reload! 
  []
  (render))
