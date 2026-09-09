# Developer Guide

This document covers the internal design of the Firefly Component Library and special cases that required non-obvious solutions.

---

## Architecture

### Repository layout

The component library lives inside the main Firefly monorepo at `component-library/`. It depends directly on Firefly source files under `src/firefly/js/` rather than maintaining copies.

```
firefly/
  src/firefly/js/       # Firefly source, shared with main app
  component-library/
    src/                # Library-specific wrappers and entry points
      charts/
      images/
      tables/
      init.js           # initFirefly() implementation
    stories/            # Storybook stories
    index.js            # Core package entry point
    vite.config.js      # Build config
    .storybook/         # Storybook config
    build.gradle        # Gradle tasks for this subproject
```

### Subpath packages

The library is published with multiple entry points to avoid loading heavy dependencies unnecessarily:

| Subpath | Entry file | Why separate |
|---|---|---|
| `@ipac/firefly-components` | `index.js` | Core: theme, inputs, FieldGroup |
| `.../tables` | `src/tables/index.js` | Heavy: table state, virtual scroll |
| `.../charts` | `src/charts/index.js` | Heavy: Plotly.js |
| `.../images` | `src/images/index.js` | Heavy: FITS processing, WebGL |

Input components (ValidationField, TargetPanel, etc.) stay in the core package because they are lightweight MUI Joy wrappers with no heavy transitive dependencies.

### `initFirefly`

Every app using the library must call `initFirefly()` once before rendering any store-connected component. It sets the Firefly server URL and bootstraps the Redux store. In Storybook it is called in `beforeAll` inside `.storybook/preview.jsx`.

Store-connected components (`ValidationField`, `TargetPanel`, `ListBoxInputField`, `AutoCompleteInput`, etc.) use `useFieldGroupConnector` and require both `initFirefly()` to have run and a parent `FieldGroup` component to provide the group context.

---

## Development setup

### Prerequisites

- Node.js v26 and yarn installed
- Firefly monorepo cloned
- Gradle (for build tasks that inject global properties)

### Install dependencies

```bash
gradle componentLibrary:installDeps
```

Dependencies are installed automatically before any build task, so this step is rarely needed explicitly.

---

## Modes

### 1. Storybook dev server

```bash
gradle componentLibrary:dev
```

Starts a live-reload dev server at `http://localhost:6006`. Stories load directly from Firefly source files — no `dist/` build step is needed. Changes to source files or stories are reflected immediately.

Global properties (browser version targets, build env, etc.) from `config/app.config` are injected automatically via Gradle.

---

### 2. Static Storybook build

```bash
gradle componentLibrary:buildStories
```

Produces a fully self-contained static site in `build/war/component-library/` inside the `:firefly` subproject. To include it in the Firefly WAR file:

```bash
gradle firefly:warAll
```

This runs the full build: `componentLibrary:buildStories` → `war` → `buildJsDoc` → `onlinehelp`.

---

### 3. Library build

```bash
gradle componentLibrary:buildLib
```

Compiles the library into `component-library/dist/` with both ES module (`*.js`) and CommonJS (`*.cjs`) outputs, one chunk per entry point (`index`, `tables`, `charts`, `images`). This is what gets published to npm.

---

### 4. Pack for local testing

```bash
gradle componentLibrary:pack
```

Builds the library then produces a tarball at `build/firefly-components.tgz`. Use it to test the built package in a consuming project without publishing:

```bash
# in the consuming project
yarn add /absolute/path/to/build/firefly-components.tgz
```

> **Yarn cache gotcha.** Yarn caches `.tgz` installs by version number. If you rebuild and repack without bumping the version in `package.json`, consumers will silently get the cached copy. Either bump the version before repacking, or clear the cache first:
> ```bash
> yarn cache clean @ipac/firefly-components
> yarn add /path/to/package.tgz
> ```

---

### 5. Publish to registry

```bash
gradle componentLibrary:publishLib
```

Builds the library then publishes to the npm registry in one step. `yarn pack` is not needed before publishing.

---

## Global properties and Gradle

The `vite.config.js` reads build-time constants (browser version targets, `BUILD_ENV`, etc.) from environment variables injected by Gradle. These originate from `config/app.config` and are forwarded as `FF___`-prefixed env vars by Gradle's `NODE` task.

`vite.config.js` collects them via `getGradleProps()` and merges them into `__PROPS__` with hardcoded defaults as fallback:

```js
__PROPS__: {
    MIN_SAFARI_VERSION: '17',   // default — overridden by FF___MIN_SAFARI_VERSION if set
    ...getGradleProps(),
}
```

When running `yarn storybook` or `yarn build` directly (without Gradle), the hardcoded defaults apply. When running via `gradle componentLibrary:dev` or `componentLibrary:buildLib`, values from `app.config` override the defaults.

---

## Storybook: source mode

Storybook loads directly from Firefly source files rather than from the built `dist/` bundle. This avoids the 9+ MB monolith bundle that would otherwise be loaded for every story.

The `viteFinal` config in `.storybook/main.js` sets up aliases so that bare `firefly/...` imports resolve into `src/firefly/js/`:

```js
resolve: {
    alias: {
        firefly: `${root}/src/firefly/js`,
        html:    `${root}/src/firefly/html`,
        ...
    },
}
```

### TDZ (Temporal Dead Zone) crash in dist mode

A TDZ crash appears as a `ReferenceError: Cannot access 'X' before initialization` at runtime after `yarn build`. It is a Rollup bundling artifact caused by circular imports. Rollup statically concatenates modules into a single file and must pick an evaluation order. When two modules depend on each other, one is always evaluated first, and the other's `const` or `let` bindings are in the TDZ at that point.

Native ES modules use live bindings and defer resolution until the binding is actually accessed, so the crash only appears in the built bundle, not in Storybook source mode.

**How to fix it:**

1. Find the binding named in the error message.
2. Identify which file declares it and which files import it, forming the cycle.
3. Extract that binding (typically a constant, enum, or type) into a new file that has no imports from the circular chain.
4. Update all affected files to import from the new file instead.

A constants file has no imports of its own, so it cannot be part of any cycle. Rollup can always evaluate it before anything else, guaranteeing its values are initialized before any other module reads them.

---

## Why Vite instead of webpack

The main Firefly application is built with webpack. The component library uses Vite instead for three reasons.

**Library build mode.** Vite's `build.lib` configuration is designed specifically for publishing npm packages. It produces clean ES module and CJS outputs, handles multiple entry points (subpath packages) naturally, and marks peer dependencies as external without extra configuration. The existing Firefly webpack config is tuned for application bundling: it inlines everything, outputs a single chunk, and has no concept of library consumers. Adapting it to produce a proper publishable library with multiple entry points would require significant rework.

**Storybook performance.** `@storybook/react-vite` serves stories through Vite's native ES module dev server. Each story loads only its own imports on demand, so the dev server starts immediately and individual stories load in milliseconds. A webpack-based Storybook must build an upfront bundle before any story can render, which in this codebase produces a 9+ MB bundle.

**Inline Web Worker.** Firefly loads one of its workers using webpack's `worker-loader` import convention. Vite handles the equivalent case natively via the `?worker&inline` query suffix. A small transform plugin in `vite.config.js` rewrites the webpack-style import to the Vite form, keeping the Firefly source unchanged while making the worker work correctly in the library build.

---

## Story conventions

Each story should satisfy three requirements.

### 1. Docs with parameter descriptions and example code

The global `DocsPage` in `.storybook/preview.jsx` renders a `<Source />` block on the Docs tab. This block requires `docs.source.code` to be set in the story's `parameters`, otherwise the entire Docs page fails to render.

Define `EXAMPLE_CODE` at module level and wire it into `parameters`:

```js
const EXAMPLE_CODE = `\
import { ValidationField, FieldGroup } from '@ipac/firefly-components';

() => (
    <FieldGroup groupKey='my-form'>
        <ValidationField fieldKey='email' label='Email' ... />
    </FieldGroup>
)`;

export default {
    ...
    parameters: {
        docs: { source: { code: EXAMPLE_CODE } },
    },
};
```

`EXAMPLE_CODE` is documentation, not story code, so it may live at module level.

### 2. Code tab that shows complete, runnable code

Storybook's Code tab renders the source of the story function itself. Do not define shared module-level variables (option arrays, URLs, helper functions) and reference them from multiple stories. Inline everything inside each story so the Code tab shows a complete, runnable example without requiring the reader to find shared state elsewhere in the file.

### 3. Disable Controls and Actions when not applicable

Most stories in this library do not use Storybook's auto-generated Controls or Actions panels. Disable them explicitly so the tabs do not appear and confuse readers:

```js
parameters: {
    controls: { disable: true },
    actions:  { disable: true },
},
```

---

### `FieldGroup` decorator with unique `groupKey`

Store-connected input stories wrap each story in a `FieldGroup`. Use `ctx.id` as the `groupKey` so that each story gets its own isolated Redux state, even when multiple stories are rendered on the same docs page:

```jsx
decorators: [
    (Story, ctx) => (
        <FieldGroup groupKey={ctx.id}>
            <Story />
        </FieldGroup>
    ),
],
```
