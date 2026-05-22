# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Squarity is a chess square-color trainer — a single-page ClojureScript app with no backend. The user is shown a square name (e.g. "e4") on a darkened board and must guess whether it is light or dark before time runs out.

## Commands

```bash
npm ci            # install dependencies (prefer over npm install)
npm run release   # production build with :advanced optimizations + minified CSS
npm run serve     # serve public/ on port 3000 (for inspecting a release build)
npm run clean     # remove public/js
npm run nuke      # remove all generated artifacts and node_modules
```

### Running tests

```bash
npx shadow-cljs compile test && node out/test.js
```

The `:test` build compiles to `out/test.js` (a Node.js target). Run before every commit.

### Starting the dev server

```bash
bin/dev-start   # start shadow-cljs + Tailwind watch + headless browser; fully self-contained
bin/dev-stop    # stop all of the above and any running REPL session
```

`bin/dev-start` is idempotent — safe to call if the server is already running. No manual browser visit needed; a headless Chromium instance (via Playwright) opens `localhost:3000` automatically and serves as the REPL runtime.

### Using the REPL

```bash
bin/repl '(+ 1 2)'            # evaluate a ClojureScript expression
bin/repl --complete 'chess/'  # complete a symbol prefix
bin/lint                       # lint src/ with clj-kondo
bin/screenshot                 # capture the live app page to /tmp/squarity_screenshot.png
node src/dev/browser-reload.js # full page reload — resets app state but disconnects the REPL (see pitfalls)
```

`bin/repl` communicates with shadow-cljs's nREPL server via babashka. Sessions persist across calls — no startup delay after the first use. The session starts in the `squarity.dev` namespace, which provides aliases: `chess`, `events`, `subs`, `views`, `rf`.

`bin/screenshot` connects to the same headless browser via CDP and screenshots its current state. Drive the app to any state via `bin/repl` first, then screenshot to inspect it visually.

For exploratory expressions, use a `(comment ...)` block inside `squarity.dev` and send forms individually via `bin/repl`. Do not run `npm run repl` directly.

### REPL pitfalls

**Stale subscriptions after hot-reload**: Shadow-cljs re-registers event handlers on hot-reload but does not flush re-frame's subscription cache (`re-frame.subs/query->reaction`). Components keep reading from old stale reactions. `reload!` in `core.cljs` clears the cache before every re-render, so edits to any source file are safe. If you ever flush the cache manually, you must call `(squarity.app.core/render)` immediately after — skipping the render leaves components with dead reactions and the DOM stops updating.

**`browser-reload.js` temporarily disconnects the REPL**: A full page reload creates a new JS execution context. Shadow-cljs reconnects its REPL WebSocket after the page loads, but reconnection completes slightly after `networkidle`. Calling `bin/repl` immediately after `browser-reload.js` can evaluate in the pre-reload context, causing a split-brain where the REPL reports one app state while the screenshot shows another. Wait ~2 seconds after a browser reload before using `bin/repl`. For subscription-staleness issues, prefer calling `(squarity.app.core/reload!)` via REPL — it clears the cache and re-renders in-place without a page reload or REPL disconnect.

**`dispatch` vs `dispatch-sync` from the REPL**: `re-frame/dispatch` is async — reading `app-db` in the same REPL expression sees the pre-dispatch state. Always use `rf/dispatch-sync` when sequencing multiple dependent events from the REPL.

### Workflow habits

After every edit to `src/`:

1. **Lint** — `bin/lint`. Fix all warnings before proceeding; none are acceptable to leave.
2. **Screenshot** — `bin/screenshot` after UI changes. Drive the app to the relevant state via `bin/repl` first so the screenshot reflects real runtime state, not just initial load.
3. **Test** — Must run before every commit; may run at any point during development when useful. Two ways:
   - **Via REPL** (fast, interactive — preferred during development):
     ```clojure
     (squarity.test-runner/run)
     ```
     `squarity.test-runner` is required by `squarity.dev` and kept current by hot-reload. To add a new test namespace, add it to both the `:require` and the `run-tests` call in `squarity.test-runner`.
   - **Via Node** (canonical, used in CI):
     ```bash
     npx shadow-cljs compile test && node out/test.js
     ```

Do not wait to be asked for any of these.

## Architecture

The stack is **Reagent + Re-Frame** (ClojureScript), compiled by **shadow-cljs**, styled with **Tailwind CSS**. All source lives under `src/main/squarity/app/`.

### Re-Frame data flow

```
keyboard / button clicks
        ↓
  events.cljs  ← dispatches that transform app-db
        ↓
    app-db
        ↓
   subs.cljs   ← subscriptions that derive view-ready data
        ↓
  views.cljs   ← Reagent components
```

### App-db shape

```clojure
{:game-phase        {:status :not-started | :in-progress | :game-over
                     :reason :timeout | :wrong-answer}  ; :reason present only when :game-over
 :score             int
 :time              ms-remaining (counts down from config :time-to-solve)
 :lockout-remaining ms-remaining-in-post-game-lockout
 :clock             JS interval handle
 :current-question  {:square int (0–63)}  ; present once a game has started
 :config            {:time-to-solve 20000, :time-resolution 100, :lockout-duration 500}
 :panel             :main   ; active keymap panel
 :hotkeys           {:main {:keydown {"l" [:answer :light]
                                      "d" [:answer :dark]
                                      " " [:start-new-game]}}}}
```

### Key files

- `chess.cljs` — pure functions: square index → x/y/name/color. Squares are indexed 0–63 left-to-right, top-to-bottom (a8=0, h8=7, a1=56, h1=63).
- `db.cljs` — `cljs.spec.alpha` spec for the full app-db shape, including three content invariants: `:current-question` is required once a game has started; `:time` is positive while `:in-progress`; and the active question is always `:unanswered` during play.
- `game/events.cljs` — all Re-Frame event handlers. Contains game logic (answer checking, time countdown, game-over transitions). Also owns the hotkey dispatch mechanism: the `:keyboard` event reads `db[:hotkeys][panel][press-type][key]` and re-dispatches whatever vector it finds.
- `game/subs.cljs` — subscriptions. `:square-colors` is the main derived value: a 64-element vector of color keywords used by the board renderer; it shows the full board when not in-progress, and a hidden board with one `:active` square during play.
- `game/views.cljs` — Reagent components. `board-svg` renders an 8×8 SVG using a 0–8 viewBox (each square is 1 unit). Tailwind classes for square colors are in `square-classes`.
- `effects.cljs` — registers the `:log` Re-Frame effect (just `println`).
- `core.cljs` — mounts the app, fires `:init-db`, and wires the global `keydown` listener.

### Extending :config

All tunable runtime values (durations, thresholds, counts) must live in `app-db`'s `:config` map — never hardcoded in views or subscription bodies. To add a new config value:

1. Add it to `default-config` in `events.cljs` (the map merged into `app-db` on `:init-db`).
2. Add its spec to `::config` in `db.cljs`.
3. If not already present, add a `:config` subscription in `subs.cljs`.
4. In any subscription that uses the value, add it as a `:<-` input signal rather than reading `app-db` directly.

This keeps the UI layer pure and ensures the value is a reactive input signal — components re-render when it changes.

### Adding a hotkey

Add an entry to the `:hotkeys` map in `:init-db` (in `events.cljs`) under the relevant panel and press type. The value must be a Re-Frame dispatch vector.

## Testing

Tests live in `src/test/squarity/app/game/events_test.cljs` and use `clojure.test.check` for property-based testing alongside `cljs.test` for concrete `deftest` cases.

### Test helpers

`step` and `step-db` are pure re-frame dispatchers that bypass `app-db`:

```clojure
(step  cofx event)   ; cofx map → effects map, via re-frame.registrar
(step-db db event)   ; db → new db, convenience wrapper over step
```

They look up the handler from the live registrar (`re-frame.registrar/get-handler :event id`) and call it directly. The handler is always the last interceptor in the chain — a re-frame invariant. No `app-db` mutation; tests are pure functions.

Use `step-db` to construct db states by threading meaningful events rather than building maps by hand:

```clojure
(-> (make-db {})
    (step-db [:start-new-game])
    (step-db [:answer :light])
    ...)
```

### Test structure

- **Spec conformance** — each event handler is checked against `::db/db` over 100 generated inputs per game phase.
- **Concrete event flows** — `deftest` cases asserting specific outcomes of multi-step event sequences.
- **Sequence invariants** — `event-sequence-preserves-invariants` generates random sequences of up to 20 events, folds them through `step-db`, and checks both `s/valid? ::db/db` and phase/score invariants at every intermediate state.

## Deployment

Hosted on Azure Static Web App. CI/CD runs via `.github/workflows/workflow.yml` on push to `main`: builds with `npm run release` and deploys `public/` to Azure. Infrastructure is declared in `azuredeploy.bicep`.
