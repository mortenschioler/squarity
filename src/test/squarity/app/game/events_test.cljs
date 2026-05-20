(ns squarity.app.game.events-test
  (:require [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [squarity.app.chess :as chess]
            [squarity.app.game.events :as events]))

(defn make-db [overrides]
  (merge {:game-phase :not-started
          :score      0
          :time       (:time-to-solve events/default-config)
          :lockout-remaining 0
          :config     events/default-config
          :panel      :main
          :hotkeys    {}}
         overrides))

(def square-gen (gen/choose 0 63))
(def score-gen  gen/nat)
(def time-resolution (get-in events/default-config [:time-resolution]))

(def in-progress-db-gen
  (gen/let [score  score-gen
            time   (gen/choose 1 20000)
            square square-gen]
    (make-db {:game-phase       :in-progress
              :score            score
              :time             time
              :current-question {:square square :phase :unanswered}})))

(def not-in-progress-db-gen
  (gen/let [phase  (gen/elements [:not-started :game-over])
            score  score-gen
            square square-gen]
    (make-db {:game-phase       phase
              :score            score
              :current-question {:square square :phase :unanswered}})))

;; ── pose-new-question ────────────────────────────────────────────────────────

(defspec new-question-square-is-valid
  (prop/for-all [square square-gen]
    (let [db     (make-db {:current-question {:square square :phase :unanswered}})
          new-sq (get-in (events/pose-new-question db) [:current-question :square])]
      (<= 0 new-sq 63))))

(defspec new-question-phase-is-unanswered
  (prop/for-all [square square-gen]
    (let [db (make-db {:current-question {:square square :phase :unanswered}})]
      (= :unanswered (get-in (events/pose-new-question db) [:current-question :phase])))))

(defspec new-question-differs-from-previous
  (prop/for-all [square square-gen]
    (let [db (make-db {:current-question {:square square :phase :unanswered}})]
      (not= square (get-in (events/pose-new-question db) [:current-question :square])))))

;; ── handle-answer ────────────────────────────────────────────────────────────

(defspec correct-answer-increments-score
  (prop/for-all [db in-progress-db-gen]
    (let [square  (get-in db [:current-question :square])
          new-db  (events/handle-answer db [:answer (chess/color-of square)])]
      (= (:score new-db) (inc (:score db))))))

(defspec wrong-answer-ends-game
  (prop/for-all [db in-progress-db-gen]
    (let [square     (get-in db [:current-question :square])
          wrong      (if (= :light (chess/color-of square)) :dark :light)
          new-db     (events/handle-answer db [:answer wrong])]
      (= :game-over (:game-phase new-db)))))

(defspec answer-noop-when-not-in-progress
  (prop/for-all [db     not-in-progress-db-gen
                 answer (gen/elements [:light :dark])]
    (= db (events/handle-answer db [:answer answer]))))

;; ── handle-tick ──────────────────────────────────────────────────────────────

(defspec tick-decreases-time
  (prop/for-all [db (gen/let [score  score-gen
                              extra  gen/nat
                              square square-gen]
                      (make-db {:game-phase       :in-progress
                                :score            score
                                :time             (+ time-resolution extra 1)
                                :current-question {:square square :phase :unanswered}}))]
    (= (:time (events/handle-tick db))
       (- (:time db) time-resolution))))

(defspec tick-ends-game-when-time-expires
  (prop/for-all [db (gen/let [score  score-gen
                              square square-gen]
                      (make-db {:game-phase       :in-progress
                                :score            score
                                :time             time-resolution
                                :current-question {:square square :phase :unanswered}}))]
    (= :game-over (:game-phase (events/handle-tick db)))))

(defspec tick-does-not-change-score
  (prop/for-all [db in-progress-db-gen]
    (= (:score db) (:score (events/handle-tick db)))))

(defspec tick-has-no-effect-on-game-phase-when-not-in-progress
  (prop/for-all [db not-in-progress-db-gen]
    (= (:game-phase db) (:game-phase (events/handle-tick db)))))
