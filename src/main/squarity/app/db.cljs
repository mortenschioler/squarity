(ns squarity.app.db
  (:require [cljs.spec.alpha :as s]))

;; ── Primitives ────────────────────────────────────────────────────────────────

(s/def ::game-phase #{:not-started :in-progress :game-over})
(s/def ::score      nat-int?)
(s/def ::time       int?)
;; Number of ms remaining in post-game lockout. Feature prevents input that the user
;; meant as an in-game answer to be registered
(s/def ::lockout-remaining int?)
(s/def ::clock      some?)
(s/def ::panel      #{:main})

;; ── Current question ──────────────────────────────────────────────────────────

(s/def ::square (s/int-in 0 64))
(s/def ::phase  #{:unanswered :incorrect})

(s/def ::current-question
  (s/keys :req-un [::square ::phase]))

;; ── Config ────────────────────────────────────────────────────────────────────

(s/def ::time-to-solve   pos-int?)
(s/def ::time-resolution pos-int?)
(s/def ::lockout-duration pos-int?)

(s/def ::config
  (s/keys :req-un [::time-to-solve ::time-resolution ::lockout-duration]))

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
  (s/keys :req-un [::game-phase
                   ::score
                   ::time
                   ::lockout-remaining
                   ::clock
                   ::config
                   ::panel
                   ::hotkeys]
          :opt-un [::current-question]))
