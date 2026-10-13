# AGENTS.md

Guidance for AI coding agents working in the Firefly repo. Human-facing docs live in `README.md`, `CONTRIBUTING.md` and `docs/`.

## Project overview

Firefly is IPAC's astronomy data visualization and archive UI toolkit: a React/Redux client (images, tables, charts) backed by a Java server (Tomcat webapp). It's a large, mature codebase that has been through several generations of code patterns.

## Repo map

- `src/firefly/js/` — client code
  - `visualize/` images, HiPS and coordinates; `tables/` table model and UI; `charts/` Plotly-based charts
  - `ui/` components, dialogs and search panels; `core/` Redux store, reducers and sagas; `api/` public JS API; `util/` helpers
  - `*Cntlr.js` files (e.g. `charts/ChartsCntlr.js`) define a state slice's action types, `dispatch*` functions and reducer
  - tests live next to the code in `__tests__/` folders, named `<Module>-test.js`
- `src/firefly/java/edu/caltech/ipac/` — server code (`firefly/`, `table/`, `visualize/`, `util/`, `astro/`)
- `src/firefly/test/` — Java tests, mirroring the `java/` package layout
- `buildScript/` — Gradle build logic (`tasks.gincl` defines the tasks below)
- `docs/` — developer docs, e.g. `development-process.md`, `jstest-notes.md`

## Build, test, lint

```shell
gradle :firefly:buildAndDeploy        # build the war and deploy to local Tomcat — needed first, and after server (Java) changes
gradle :firefly:devMode               # watch JS and rebuild straight into the deployed webapp — the usual client dev loop
```

While iterating, check only what you touched:

```shell
yarn test-unit js/charts/__tests__/ChartUtil-test.js   # one Jest file, run from src/firefly/
npx eslint src/firefly/js/charts/ChartUtil.js          # lint changed files, run from repo root
```

Before calling a change done, run the full suites CI runs: `gradle :firefly:jsTest`, `gradle :firefly:jsLint`, and `gradle :firefly:test` if Java changed.

## Code conventions

- Match the surrounding code's style, reuse existing utilities, and follow established patterns rather than reinventing them. The exception is a pattern that's clearly legacy or bug-prone. Use the modern approach there, and say that you did.
- Use descriptive names — no single-letter or cryptic abbreviations like `t`, `xs`, `ll`; the name should say what the value holds.
- For debug output, use `Logger` from `util/Logger.js` rather than `console.log`. Turn it on in the browser console with `window.firefly.debug = 'debug'`; filter it with `window.firefly.debugTags = 'Table'`.
- Add short comments where the reason isn't obvious (a temporary hardcode, a subtle check); don't narrate what the code does.
- JSDoc: when a function also has rest params, document destructured props as `@param {object} props` / `@param props.field` to satisfy `jsdoc/check-param-names`.
- Tests: one helper per function under test, named after that function. Avoid chained generic helpers and hidden defaults — each test should show its inputs.
- Before upgrading a JS dependency, check `package-json-doc.md` for why some packages are held at older versions.

## How to approach a change

- **Find the nearest cousin first.** Read the full round trip of an existing analog — component *and* the reducer/controller/server handler under it — not just a similar-looking call site. It reveals the ordering, side effects and edge cases you'll need to handle.
- **Weigh decisions by blast radius, not diff size.** Local, reversible edits: just make them. New data flows, persisted state, endpoints, or edits to shared reducers/utilities: propose a short plan and get confirmation before editing.
- **Build and debug one layer at a time.** Prove an endpoint with a raw request before wiring UI; prove render before selection; selection before persistence.
- **Suspect stale state.** Redux state, server caches and long sessions outlive the code that filled them. When a bug contradicts verified code, try a hard reload or cache reset before re-reading the code.
- **Reactive flows need a live check.** For new subscription/effect chains across files, code review isn't enough — ask for the interaction to be exercised live, more than once.
- **Set scope early and keep it.** Name what's deferred; raise tempting adjacent fixes instead of folding them in.
- **Remove superseded code in the same edit** — old functions, unused imports, stale comments — once you've confirmed it's actually dead.

## Git workflow

- Branches: `FIREFLY-<ticket>-short-desc`, created from `dev` (or the current `rc-*` branch for release fixes). Commit messages start with the ticket, e.g. `FIREFLY-1234: ...`.
- PRs target `dev` (or the `rc-*` branch) and are squashed before merge. Full process: `docs/development-process.md`.
- Don't commit, push, or rebase unless asked.
