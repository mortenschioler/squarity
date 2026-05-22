(ns squarity.test-runner
  (:require [cljs.test :as t]
            [squarity.app.game.events-test]))

(defn run []
  (t/run-tests 'squarity.app.game.events-test))
