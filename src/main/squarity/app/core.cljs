(ns squarity.app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [squarity.app.events]
            [squarity.app.subs]
            [squarity.app.pages.main :as mainpage]))

(defn app
  []
  [:div
   [:h1 "Squarity!"]
   [mainpage/main]])

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
