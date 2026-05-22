(ns squarity.app.db
  (:require [cljs.spec.alpha :as s]))

;; ── Primitives ────────────────────────────────────────────────────────────────

(s/def ::status #{:not-started :in-progress :game-over})
(s/def ::reason #{:timeout :wrong-answer})
(s/def ::game-phase (s/keys :req-un [::status] :opt-un [::reason]))
(s/def ::score      nat-int?)
(s/def ::time       int?)
;; Number of ms remaining in post-game lockout. Feature prevents input that the user
;; meant as an in-game answer to be registered
(s/def ::lockout-remaining int?)
(s/def ::clock      some?)
(s/def ::panel      #{:main})

;; ── Current question ──────────────────────────────────────────────────────────

(s/def ::square (s/int-in 0 64))

(s/def ::current-question
  (s/keys :req-un [::square]))

;; ── Config ────────────────────────────────────────────────────────────────────

(s/def ::time-to-solve    pos-int?)
(s/def ::time-resolution  pos-int?)
(s/def ::lockout-duration  nat-int?)
(s/def ::warning-threshold nat-int?)

(s/def ::config
  (s/keys :req-un [::time-to-solve ::time-resolution ::lockout-duration ::warning-threshold]))

;; ── Hotkeys ───────────────────────────────────────────────────────────────────

(s/def ::dispatch-vector
  (s/and vector? seq (comp keyword? first)))

(s/def ::keydown
  (s/map-of string? ::dispatch-vector))

(s/def ::panel-hotkeys
  (s/keys :opt-un [::keydown]))

(s/def ::hotkeys
  (s/map-of ::panel ::panel-hotkeys))

;; ── App DB ────────────────────────────────────────────────────────────────────

(s/def ::db
  (s/and
    (s/keys :req-un [::game-phase
                     ::score
                     ::time
                     ::lockout-remaining
                     ::clock
                     ::config
                     ::panel
                     ::hotkeys]
            :opt-un [::current-question])
    ;; :current-question is required once a game has started
    (fn [{:keys [game-phase current-question]}]
      (if (#{:in-progress :game-over} (:status game-phase))
        (some? current-question)
        true))
    ;; time remaining is positive while the game is running
    (fn [{:keys [game-phase time]}]
      (if (= :in-progress (:status game-phase))
        (pos? time)
        true))))
