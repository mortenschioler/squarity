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

There are no automated tests.

### Starting the dev server

```bash
bin/dev-start   # start shadow-cljs + Tailwind watch in background; prints URL when ready
bin/dev-stop    # stop the dev server and any running REPL session
```

`bin/dev-start` is idempotent — safe to call if the server is already running.

### Using the REPL

The REPL evaluates in the browser. `bin/dev-start` must be running and the app must be open at `localhost:3000` before the REPL will connect. On WSL, open the URL manually in your Windows browser.

```bash
bin/repl '(+ 1 2)'            # evaluate a ClojureScript expression
bin/repl --complete 'chess/'  # complete a symbol prefix
bin/lint                       # lint src/ with clj-kondo
```

`bin/repl` communicates with shadow-cljs's nREPL server via babashka. Sessions persist across calls — no startup delay after the first use. The session starts in the `squarity.dev` namespace, which provides aliases: `chess`, `events`, `subs`, `views`, `rf`.

For exploratory expressions, use a `(comment ...)` block inside `squarity.dev` and send forms individually via `bin/repl`. Do not run `npm run repl` directly.

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
{:game-phase        :not-started | :in-progress | :game-over
 :score             int
 :time              ms-remaining (counts down from config :time-to-solve)
 :timeout           ms-remaining-in-post-game-lockout
 :clock             JS interval handle
 :current-question  {:square int (0–63), :phase :unanswered | :incorrect}
 :config            {:time-to-solve 20000, :time-resolution 100, :timeout 500}
 :panel             :main   ; active keymap panel
 :hotkeys           {:main {:keydown {"l" [:answer :light]
                                      "d" [:answer :dark]
                                      " " [:start-new-game]}}}}
```

### Key files

- `chess.cljs` — pure functions: square index → x/y/name/color. Squares are indexed 0–63 left-to-right, top-to-bottom (a8=0, h8=7, a1=56, h1=63).
- `game/events.cljs` — all Re-Frame event handlers. Contains game logic (answer checking, time countdown, game-over transitions). Also owns the hotkey dispatch mechanism: the `:keyboard` event reads `db[:hotkeys][panel][press-type][key]` and re-dispatches whatever vector it finds.
- `game/subs.cljs` — subscriptions. `:square-colors` is the main derived value: a 64-element vector of color keywords used by the board renderer; it shows the full board when not in-progress, and a hidden board with one `:active` square during play.
- `game/views.cljs` — Reagent components. `board-svg` renders an 8×8 SVG using a 0–8 viewBox (each square is 1 unit). Tailwind classes for square colors are in `square-classes`.
- `effects.cljs` — registers the `:log` Re-Frame effect (just `println`).
- `core.cljs` — mounts the app, fires `:init-db`, and wires the global `keydown` listener.

### Adding a hotkey

Add an entry to the `:hotkeys` map in `:init-db` (in `events.cljs`) under the relevant panel and press type. The value must be a Re-Frame dispatch vector.

## Deployment

Hosted on Azure Static Web App. CI/CD runs via `.github/workflows/workflow.yml` on push to `main`: builds with `npm run release` and deploys `public/` to Azure. Infrastructure is declared in `azuredeploy.bicep`.
