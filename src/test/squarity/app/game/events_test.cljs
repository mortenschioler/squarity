(ns squarity.app.game.events-test
  (:require [cljs.spec.alpha :as s]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [squarity.app.chess :as chess]
            [squarity.app.db :as db]
            [squarity.app.game.events :as events]))

;; ── Helpers ──────────────────────────────────────────────────────────────────

(defn make-db [overrides]
  (merge {:game-phase        :not-started
          :score             0
          :time              (:time-to-solve events/default-config)
          :lockout-remaining 0
          :clock             :fake-clock
          :config            events/default-config
          :panel             :main
          :hotkeys           {}}
         overrides))

;; ── Generators ───────────────────────────────────────────────────────────────

(def square-gen (gen/choose 0 63))
(def score-gen  gen/nat)
(def time-resolution (get-in events/default-config [:time-resolution]))
(def answer-gen (gen/elements [:light :dark]))

(def question-gen
  (gen/let [square square-gen
            phase  (gen/elements [:unanswered :incorrect])]
    {:square square :phase phase}))

(def not-started-db-gen
  (gen/let [score   score-gen
            time    (gen/choose 0 20000)
            lockout gen/small-integer]
    (make-db {:game-phase        :not-started
              :score             score
              :time              time
              :lockout-remaining lockout})))

(def in-progress-db-gen
  (gen/let [score    score-gen
            time     (gen/choose 1 20000)
            lockout  gen/small-integer
            question question-gen]
    (make-db {:game-phase        :in-progress
              :score             score
              :time              time
              :lockout-remaining lockout
              :current-question  question})))

(def game-over-db-gen
  (gen/let [score    score-gen
            time     gen/small-integer
            lockout  gen/small-integer
            question question-gen]
    (make-db {:game-phase        :game-over
              :score             score
              :time              time
              :lockout-remaining lockout
              :current-question  question})))

(def any-db-gen
  (gen/one-of [not-started-db-gen in-progress-db-gen game-over-db-gen]))

(def not-in-progress-db-gen
  (gen/one-of [not-started-db-gen game-over-db-gen]))

;; ── Spec conformance ─────────────────────────────────────────────────────────

(defspec start-new-game-produces-valid-db
  (prop/for-all [db any-db-gen]
    (s/valid? ::db/db (events/start-new-game db))))

(defspec handle-answer-produces-valid-db
  (prop/for-all [db     any-db-gen
                 answer answer-gen]
    (s/valid? ::db/db (events/handle-answer db [:answer answer]))))

(defspec handle-tick-produces-valid-db
  (prop/for-all [db any-db-gen]
    (s/valid? ::db/db (events/handle-tick db))))

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
                 answer answer-gen]
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
