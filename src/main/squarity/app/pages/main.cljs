(ns squarity.app.pages.main
  (:require [re-frame.core :as re-frame]))

(def urls
  {:colored "url(img/board_colored.svg)"
   :blank  "url(img/board_colorless.svg)"})

(defn board
  [board-visibility]
  (let [url (urls board-visibility)]
    [:div {:style {:background-image url}
           :class "w-full h-full"}]))


(defn board-container
  [board-visibility]
  [:div {:class "w-96 h-96"
         :on-click #(re-frame/dispatch [:swap-board-color])}
   [board board-visibility]])

(defn main
  []
  (fn []
    (let [board-visibility @(re-frame/subscribe [:board-visibility])]
      [board-container 
       board-visibility])))