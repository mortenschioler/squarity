(ns squarity.app.pages.main
  (:require [squarity.app.db :as db]))

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
         :on-click #(swap! db/db update :board-visibility (fn [s] (case s :colored :blank :blank :colored)))}
   [board board-visibility]])

(defn main
  []
  (fn []
    (let [{:keys [board-visibility]} @db/db]
      [board-container 
       board-visibility])))