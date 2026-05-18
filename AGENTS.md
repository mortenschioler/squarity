# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Squarity is a chess square-color trainer — a single-page ClojureScript app with no backend. The user is shown a square name (e.g. "e4") on a darkened board and must guess whether it is light or dark before time runs out.

## Commands

```bash
npm ci            # install dependencies (prefer over npm install)
npm run dev       # compile + watch (ClojureScript + Tailwind), serves on localhost:3000
npm run release   # production build with :advanced optimizations + minified CSS
npm run serve     # serve public/ on port 3000 (for inspecting a release build)
npm run repl      # connect a ClojureScript browser REPL (app must be running in browser)
npm run clean     # remove public/js
npm run nuke      # remove all generated artifacts and node_modules
```

There are no automated tests.

### Using the REPL

The REPL evaluates in the browser, so `npm run dev` must be running and the app must be open at `localhost:3000` before connecting.

Keep the REPL open as a persistent session — don't restart it per expression. Use named pipes:

```bash
# Start once
mkfifo /tmp/squarity_repl_in 2>/dev/null || true
tail -f /dev/null > /tmp/squarity_repl_in &
npm run repl < /tmp/squarity_repl_in > /tmp/squarity_repl_out 2>/dev/null &
until grep -q "cljs.user=>" /tmp/squarity_repl_out 2>/dev/null; do sleep 1; done

# Send an expression and wait for the prompt
BEFORE=$(wc -c < /tmp/squarity_repl_out)
echo '(+ 1 2)' > /tmp/squarity_repl_in
until tail -c +$((BEFORE+1)) /tmp/squarity_repl_out 2>/dev/null | grep -q "cljs.user=>"; do sleep 1; done
tail -c +$((BEFORE+1)) /tmp/squarity_repl_out
```

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
