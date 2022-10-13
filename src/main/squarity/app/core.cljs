(ns squarity.app.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [squarity.app.effects]
            [squarity.app.game.events] 
            [squarity.app.game.subs]
            [squarity.app.game.views :as game.views]))

(defn app
  []
  [game.views/page])

(defn render 
  []
  (rdom/render [app] (.getElementById js/document "root")))

(defn ^:export main 
  []
  (re-frame/dispatch-sync [:init-db])
  (render) 
  (.addEventListener js/document "keydown" #(re-frame/dispatch [:keyboard :keydown {:k (.-key %)}])))

(defn ^:dev/after-load reload! 
  []
  (render))
