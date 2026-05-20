(ns squarity.app.game.events-test
  (:require [cljs.spec.alpha :as s]
            [cljs.test :refer [deftest is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [re-frame.registrar :as registrar]
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

(defn step
  "Apply a re-frame event to cofx, returning the resulting effects map.
   Pure — never touches app-db. Handler is always last in the interceptor chain."
  [cofx event]
  (let [handler (last (registrar/get-handler :event (first event)))
        ctx     {:coeffects (assoc cofx :event event) :effects {}}]
    (:effects ((:before handler) ctx))))

(defn step-db
  "Apply a re-frame event to db, returning the new db."
  [db event]
  (:db (step {:db db} event)))

;; ── Event-based tests ────────────────────────────────────────────────────────

(deftest start-new-game-enters-in-progress
  (let [db (step-db (make-db {}) [:start-new-game])]
    (is (= :in-progress (:game-phase db)))
    (is (s/valid? ::db/db db))))

(deftest one-correct-answer-increments-score
  (let [db0  (step-db (make-db {}) [:start-new-game])
        sq   (get-in db0 [:current-question :square])
        db1  (step-db db0 [:answer (chess/color-of sq)])]
    (is (= :in-progress (:game-phase db1)))
    (is (= 1 (:score db1)))
    (is (s/valid? ::db/db db1))))

(deftest one-wrong-answer-ends-game
  (let [db0   (step-db (make-db {}) [:start-new-game])
        sq    (get-in db0 [:current-question :square])
        wrong (if (= :light (chess/color-of sq)) :dark :light)
        db1   (step-db db0 [:answer wrong])]
    (is (= :game-over (:game-phase db1)))
    (is (= :incorrect (get-in db1 [:current-question :phase])))
    (is (s/valid? ::db/db db1))))

(deftest last-tick-ends-game
  (let [db0 (make-db {:game-phase :in-progress
                      :time       (:time-resolution events/default-config)
                      :current-question {:square 0 :phase :unanswered}})
        db1 (step-db db0 [:tick])]
    (is (= :game-over (:game-phase db1)))
    (is (s/valid? ::db/db db1))))

(deftest two-correct-then-wrong-ends-game-with-score-2
  (let [db0  (step-db (make-db {}) [:start-new-game])
        db1  (step-db db0 [:answer (chess/color-of (get-in db0 [:current-question :square]))])
        db2  (step-db db1 [:answer (chess/color-of (get-in db1 [:current-question :square]))])
        sq   (get-in db2 [:current-question :square])
        db3  (step-db db2 [:answer (if (= :light (chess/color-of sq)) :dark :light)])]
    (is (= 2 (:score db3)))
    (is (= :game-over (:game-phase db3)))
    (is (s/valid? ::db/db db3))))

;; ── Sequence-based tests ─────────────────────────────────────────────────────

(def event-gen
  (gen/one-of
    [(gen/return [:start-new-game])
     (gen/let [color (gen/elements [:light :dark])] [:answer color])
     (gen/return [:tick])]))

(defn valid-step?
  "Checks phase-machine and score invariants across a single transition."
  [[event before after]]
  (let [bp (:game-phase before)
        ap (:game-phase after)]
    (and
      ;; :not-started can never transition directly to :game-over
      (not= [bp ap] [:not-started :game-over])
      ;; nothing ever transitions back to :not-started
      (or (= bp :not-started) (not= ap :not-started))
      ;; score is non-decreasing except when :start-new-game resets it
      (or (= (first event) :start-new-game)
          (>= (:score after) (:score before))))))

(defspec event-sequence-preserves-invariants
  (prop/for-all [events (gen/vector event-gen 1 20)]
    (let [initial       (make-db {})
          states        (reductions step-db initial events)
          before-states (butlast states)
          after-states  (rest states)]
      (and
        (every? (partial s/valid? ::db/db) states)
        (every? valid-step? (map vector events before-states after-states))))))

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
  (gen/let [score   score-gen
            time    (gen/choose 1 20000)
            lockout gen/small-integer
            square  square-gen]
    (make-db {:game-phase        :in-progress
              :score             score
              :time              time
              :lockout-remaining lockout
              :current-question  {:square square :phase :unanswered}})))

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
