(ns squarity.app.db
  (:require [reagent.core :as r]))

(def db (r/atom {:board-visibility :blank}))
