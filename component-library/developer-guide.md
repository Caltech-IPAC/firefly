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
```

### Subpath packages

The library is published with multiple entry points to avoid loading heavy dependencies unnecessarily:

| Subpath | Entry file | Why separate |
|---|---|---|
| `@ipac/firefly-component-library` | `index.js` | Core: theme, inputs, FieldGroup |
| `.../tables` | `src/tables/index.js` | Heavy: table state, virtual scroll |
| `.../charts` | `src/charts/index.js` | Heavy: Plotly.js |
| `.../images` | `src/images/index.js` | Heavy: FITS processing, WebGL |

Input components (ValidationField, TargetPanel, etc.) stay in the core package because they are lightweight MUI Joy wrappers with no heavy transitive dependencies.

### `initFirefly`

Every app using the library must call `initFirefly()` once before rendering any store-connected component. It sets the Firefly server URL and bootstraps the Redux store. In Storybook it is called in `beforeAll` inside `.storybook/preview.jsx`.

Store-connected components (`ValidationField`, `TargetPanel`, `ListBoxInputField`, `SuggestBoxInputField`, etc.) use `useFieldGroupConnector` and require both `initFirefly()` to have run and a parent `FieldGroup` component to provide the group context.

---

## Development setup

```bash
cd component-library
yarn install
```

---

## Modes

### 1. Storybook dev server

```bash
yarn storybook
```

Starts a live-reload dev server at `http://localhost:6006`. Stories load directly from Firefly source files — no `dist/` build step is needed. Changes to source files or stories are reflected immediately.

Use this mode when writing or debugging stories, or when iterating on a component.

---

### 2. Static Storybook build

```bash
yarn build-storybook
```

Produces a fully self-contained static site in `storybook-static/`. Open `storybook-static/index.html` in a browser to verify the output, or deploy the directory to any static host.

Use this mode to check the final published docs site, or to reproduce a build-time error that does not appear in the dev server.

---

### 3. Library build

```bash
yarn build
```

Compiles the library into `dist/` with both ES module (`*.js`) and CommonJS (`*.cjs`) outputs, one chunk per entry point (`index`, `tables`, `charts`, `images`). This is what gets published to npm.

Use this mode before publishing, or to test the built package locally (see below).

---

### 4. Testing the built dist locally

Use `yarn pack` to verify the built `dist/` in a consuming project without publishing. It produces the exact tarball that `yarn publish` would upload, including only the files listed in the `files` field of `package.json`, so it tests the real published surface rather than the full source tree.

```bash
yarn build
yarn pack
# creates package.tgz in component-library/

# in the test project:
yarn add /absolute/path/to/component-library/package.tgz
```

> **Yarn cache gotcha.** yarn caches `.tgz` installs by version number. If you rebuild and repack without bumping the version in `package.json`, consumers who installed via `yarn add /path/to/package.tgz` will silently get the cached copy instead of your new build. Either bump the version before repacking, or clear the cache first:
> ```bash
> yarn cache clean @ipac/firefly-component-library
> yarn add /path/to/component-library/package.tgz
> ```

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
import { ValidationField, FieldGroup } from '@ipac/firefly-component-library';

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
