(ns squarity.app.core
  (:require [reagent.dom :as rdom]
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
  (render))

(defn ^:dev/after-load reload! 
  []
  (render))
