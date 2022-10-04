# `squarity`: A simple chess vision trainer
> ⚠️ This application is still in pre-release, i.e. it is not yet available in a finished state.

## Gitpod
[![Open in Gitpod](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/mortenschioler/squarity.git)

## Frameworks and technology
Squarity is a Single Page Application (SPA) written in ClojureScript that uses Reagent and Re-Frame, compiled with shadow-cljs. Tailwind is used for CSS. There is no backend.

The application is hosted on Azure, in a personal Free tier account belonging to @mortenschioler. Continuous deployment is set up using GitHub Actions. The GitHub Actions workflow is defined in `.github/workflows/workflow.yml`.

The application can be accessed at https://squarity.azurewebsites.net/.

## Node Scripts

### Start App

This will compile the app in development mode, and watch for any changes in the code and Tailwind classes.
Open [http://localhost:3000](http://localhost:3000) to view the app in the browser.

```
npm run dev
```

This operation creates a `.shadow-cljs` folder in the project folder.

### Build Release Version of App

This compiles the app in production mode, using `:advanced` compilation settings. The finished build (under `public/js`) will be in the `public` folder, which can be deployed.

```
npm run release
```

This operation creates a `.shadow-cljs` folder in the project folder.

### Serve The App Build Locally

This will serve the finished build (from doing a production build via `npm run build`) on [http://localhost:5000](http://localhost:5000) in the browser.

```
npm run serve
```

It's not necessary to do this if the application was started in dev mode with `npm start` or `npm run watch`, but it can be useful for inspecting the result of a production build locally.

### Connect to a Build-specific Browser REPL

From a different Terminal, connect to a browser REPL for the specific build (only available while `yarn start` or `npm start` is running, that is, Shadow-CLJS is “watching” the code for changes). Note also that the build must be running in the browser (`localhost:3000`).

See [this section](https://shadow-cljs.github.io/docs/UsersGuide.html#build-repl) of the Shadow-CLJS documentation for more details.

```
npm run repl
```

### Connect to a Blank Browser REPL

This starts a blank CLJS REPL and will open an associated browser window where the code will execute. It is not connected to any specific build target. Since it is not connected to any build it does not do any automatic rebuilding of code when files change and does not provide hot-reload. If you close the browser window the REPL will stop working.

See [this section](https://shadow-cljs.github.io/docs/UsersGuide.html#browser-repl) of the Shadow-CLJS documentation for more details.

```
npm run browser-repl
```

### Connect to a Clojure REPL

A Clojure REPL is also provided in addition to the provided ClojureScript REPLs. This is can be used to control the shadow-cljs process and run all other build commands through it. You can start with a Clojure REPL and then upgrade it to a CLJS REPL at any point (and switch back).

See [this section](https://shadow-cljs.github.io/docs/UsersGuide.html#_clojure_repl) of the Shadow-CLJS documentation for more details.

```
npm run clojure-repl
```

### Remove Generated JS Code (“Clean”)

Remove (“clean”) the `public/js` folder and contents generated during compilation.

```
npm run clean
```

### Remove All Generated Code and Dependencies (“Nuke”)

Remove all (“nuke”) of the following:

- `public/js` folder and contents
- `.shadow-cljs` folder and contents
- `node_modules` folder and contents
- `package-lock.json` file (or `yarn.lock` file, if you specified the `yarn` option for your package manager)
- `out` folder and contents (containing tests)
- `report.html` file showing release build details

```
npm run nuke
```

Note that after this operation you will need to run `npm install` again before starting the app, to re-install the dependencies.

### Run a Shadow-CLJS Server

Shadow-CLJS can be fairly slow to start. To improve this Shadow-CLJS can run in “server mode” which means that a dedicated process is started which all other commands can use to execute a lot faster since they won’t have to start a new JVM/Clojure instance.

You can run the process in the foreground in a dedicated Terminal. Use CTRL+C to terminate the server.
