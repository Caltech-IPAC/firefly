# Component Library API Design

**Status:** Proposed.

Public API design for `@ipac/firefly-components` ahead of 1.0. Read this alongside
`developer-guide.md` (build, Storybook, tooling) before implementing.

This document covers the design areas identified so far. More may be added as they come up.

1. **Declarative components** that share one common API.
2. **An imperative API** exposed as curated named ESM exports.
3. **Object API**: an API that operates on a bound object (one Firefly component, e.g. a table),
   an easy on-ramp for common tasks, with the full imperative API underneath when you need more.
4. **Theming** exposed to the host app.
5. **Initialization** through a React provider component, with `initFirefly()` kept for
   non-React use.

**Terms used here:**

- **Wrapper:** one of the library's React components around a Firefly component
  (`DataTable`, `ImagePlot`, …).
- **Native ID props:** the ID names the existing Firefly components use: `tbl_id`, `tbl_ui_id`, `chartId`,
  `plotId`, `viewerId`, `groupKey`.
- **API object:** an object whose methods act on one Firefly component (a table, chart, image, …),
  identified by its native ID (see §3).

**Scope:** all work stays inside `component-library/`. Firefly source under
`src/firefly/js/` is imported, not modified.

---

## 1. Declarative components with a shared contract

Every wrapper (`DataTable`, `TableGroup`, `ChartPanel`, `ScatterChart`, `HistogramChart`,
`ImagePlot`, `Coverage`, and any new ones) follows the same prop contract.

### Prop groups

```jsx
<DataTable
    tbl_id="wise-tbl-id"  source={request} // identity + what to show
    options={{showToolbar: false}}         // passed to the wrapped Firefly component
    onSelect={...}  onLoaded={...}         // callbacks
    sx={...}  slotProps={{toolbar: ...}}   // styling
    ref={apiRef}                           // API object
/>
```

| Prop | Purpose |
|---|---|
| Native ID prop | The component's store key (see below) |
| `source` | Where the component gets its data. Optional; omit it to use data already in the store (see below) |
| `options` | Props passed through to the wrapped Firefly component (`TablePanel`, `ChartPanel`, …), e.g. `showToolbar`, `expandable`. Accepted keys are whatever that component takes; each wrapper documents the common ones. |
| `on*` | Callbacks for supported events, e.g. `onSelect`, `onLoaded` (see Events) |
| `sx` | Joy `sx`, applied to the root |
| `slotProps` | Optional; deep customization of inner parts through Joy's slot conventions |
| `ref` | Returns the component's API object (see §3) |

### Use Firefly native IDs

Use the native ID props, the same names as the imperative Firefly API. 
Props that refer to another component use the same names (charts take `tbl_id`).

- Auto-generate the ID with `useId()` when it's omitted. This only applies when `source` is
  given; using data already in the store needs an explicit ID.
- Document all ID props in one table, since the casing is mixed (`tbl_id` vs `chartId`).

### `source`: where the component gets its data

The native ID prop names the data in the store. `source` says where that data comes from,
and it's optional:

- **`source` given → the component loads its data.** It dispatches the request on mount and
  stores the result under its ID. If data with that ID is already in the store, it's
  replaced. The component owns that data.
- **`source` omitted (`undefined` or `null`) → the component uses data already in the store**
  under its ID, loaded by a script, a search panel or another component. It dispatches
  nothing. If the data isn't there yet, the component shows an empty state and renders it
  when it arrives, so a layout can render first and a script can load the data later.

```jsx
// load data from a source
<DataTable  tbl_id="wise-tbl-id"     source={makeFileRequest('WISE', url)} />
<ChartPanel chartId="wise-scatter-id"
            source={makeScatterChartData({tbl_id: 'wise-tbl-id', x: 'ra', y: 'dec'})} />
<ImagePlot  plotId="wise-img-id"     source={WebPlotRequest.makeURIPlotRequest(url)} />

// use data already in the store
<DataTable tbl_id="wise-tbl-id" />
```

| Domain | `source` | Builders |
|---|---|---|
| Table | table request, or an in-memory table model (has `tableData`) | `makeTblRequest`, `makeFileRequest` |
| Chart | `chartData` (the object `dispatchChartAdd` takes) | `makeScatterChartData`, `makeHistogramChartData` |
| Image | `WebPlotRequest` | `WebPlotRequest.makeURIPlotRequest`, … |

- Charts follow the same rule: `ChartPanel` takes `source`, like every other wrapper. Its
  value is a `chartData` object; that's Firefly's name for the object type, not a prop name.
  Tables are linked per trace (`tbl_id` on the trace, columns as `'tables::colName'`), so
  one chart can draw from several tables.
- Shortcut components (`ScatterChart`, `HistogramChart`) are `ChartPanel` plus a builder,
  so the build logic lives in one place.

Each domain is responsible for turning its `source` into the data stored in the store.

### `slotProps`

A component may expose `slotProps` to allow deep customization of its inner parts through
Joy's slot conventions. Which slots it exposes is up to each component, and it documents them.

### Events

Callbacks are flat `on*` props, the usual React convention.

```jsx
<DataTable tbl_id="wise-tbl-id" source={request}
    onSelect={(selectInfo, { tbl_id, model }) => { ... }}
    onHighlight={(rowIdx, { tbl_id, model }) => { ... }}
    onLoaded={(model, { tbl_id }) => { ... }}
/>
```

#### Common callbacks

Every component supports these:

| Callback | `value` |
|---|---|
| `onLoaded` | the loaded model |
| `onError` | the error |
| `onRemove` | the native ID of the removed data |

Each component can add its own callbacks (e.g. `onSort` on tables) and documents them.

#### Conventions

- **Naming:** camelCase props starting with `on`, e.g. `onSelect`. In the Object API, the
  same event drops the `on`: `onSelect` ↔ `api.on('select', ...)`,
  `onActiveChange` ↔ `api.on('activeChange', ...)`.
- **Signature:** every callback receives `(value, ctx)`.
  - `value`: the new state of what changed, e.g. the new selection.
  - `ctx`: the native ID prop and the current model; more fields may be added later.
- **Callbacks aren't passed down.** The wrapper handles its `on*` props itself and doesn't
  pass them to the wrapped Firefly component.
- **Built on `api.on()`** (see §3). The props are a thin layer in the shared hook, with no
  state-comparison logic of their own.
- **No events are missed on mount.** Subscribe before loading the `source`, so a load that
  finishes right away (e.g. an in-memory table model) still fires `onLoaded`.
- **Users don't memoize callbacks.** Inline callbacks must just work, without `useCallback`,
  and the latest callback is always the one called. How is up to the implementation.

### Lifecycle

- **`source` changes reload the data.** Users don't memoize `source`: a `source` built inline
  on every render must not cause a reload unless its contents changed. How changes are
  detected is up to the implementation.
- **Unmounting never removes data.** Data stays in the store after the component unmounts,
  so tabs and conditional rendering don't throw it away or re-run server queries on remount.
  To remove or reload data, use the Object API: `api.remove()` and `api.reload()` (see §3).

  | | With `source` (loads its data) | Without `source` (uses existing data) |
  |---|---|---|
  | On mount | dispatches the request | dispatches nothing |
  | On unmount | leaves the data alone | leaves the data alone |

### Shared implementation

Most wrappers do the same work: resolve the ID, load the `source`, subscribe the `on*`
callbacks, and attach the API object to `ref`. Put this in shared code that every wrapper
reuses, so each wrapper only supplies its domain-specific parts, such as how to turn its
`source` into data in the store.

---

## 2. Imperative API

### Named ESM exports per subpath

The library exposes its imperative API as **named ESM exports** from each subpath (`/tables`,
`/charts`, `/images`), the way modern React libraries do. This gives an explicit public
surface, plus static types and autocomplete.

- **Curated allow-lists only.** Each subpath exports an explicit list; no `export *`. Each
  export is something we promise to keep working, so add exports when they're needed, not
  because they exist.
- **What goes on the lists:** the API object factory, request builders (`makeFileRequest`,
  `makeTblRequest`, `WebPlotRequest`), and the most useful `dispatch*` and getter functions.
  Base the first version on what the stories and wrappers actually use.
- **`window.firefly` is not part of the library's API.** The library doesn't export, document
  or depend on it. Don't add a `getFireflyApi()` or similar.

## 3. Object API

The Object API is **new in this library**, an addition to the standard imperative API in §2.
It gives you an **API object**: a plain object with functions on it, tied to one ID. It's a
convenient way to act on one Firefly component, with or without React. There are two
ways to get one.

**From a factory, by ID** (scripts, code outside the component, data loaded elsewhere):

```js
import { bindTable } from '@ipac/firefly-components/tables';

const wiseTable = bindTable('wise-tbl-id');
wiseTable.highlight(10);
wiseTable.sort('w1mpro', 'ASC');
const model = wiseTable.getModel();

const off = wiseTable.on('select', (selectInfo, ctx) => { ... });
off();   // unsubscribe
```

**From a component, through its `ref` prop.** Every wrapper's `ref` returns its API object,
the same object the factory returns for that ID. This is useful when the ID was
auto-generated and you don't know it:

```jsx
function WiseTable() {
    const apiRef = useRef();
    return (
        <>
            <Button onClick={() => apiRef.current.sort('w1mpro', 'ASC')}>Sort by W1</Button>
            <DataTable ref={apiRef} source={request} />
        </>
    );
}
// apiRef.current.tbl_id gives the generated ID, to pass to other components or scripts
```

`ref` is empty during the first render, so call its methods from effects and event handlers.

Factories (`bind*`): `bindTable(tbl_id)`, `bindChart(chartId)`, `bindImage(plotId)`. They live
next to the wrappers (`src/tables/bindTable.js`, etc.) and are exported from their subpaths.

**Rules:**

1. **API objects hold no state.** An API object is just the ID plus methods that read the store when
   called. Creating one is cheap, two API objects for the same ID behave the same, and an API object
   can exist before its target does.
2. **Missing targets are safe.** `getModel()` returns `undefined` for an unknown ID. Actions
   do nothing and log through `Logger` (`util/Logger.js`); they don't throw.
3. **Methods are thin calls into the curated exports.** The API object has no logic of its own.
4. **Keep the surface small.** Each method is public API. Start with:
   - Base (all API objects): the native ID prop (`tbl_id` / `chartId` / `plotId`), `getModel()`,
     `reload()` (re-run the original request), `remove()` (remove the data from the store),
     `on(event, callback)`
   - Table: `highlight`, `sort`, `filter`, `getSelected`
   - Image: `zoom`, `recenter`
   - Chart: `getChartData`

   Adding methods later doesn't break anyone. Removing them does.

**API objects work on any target, not just library components.** Tables in a `TableGroup`,
tables loaded by search panels, or tables pushed by the server can all be used through
`bindTable(tbl_id)`.

### How the declarative and imperative styles fit together

| Declarative | Imperative |
|---|---|
| props → action dispatched on mount | curated `dispatch*` exports |
| `onSelect={...}` | `api.on('select', ...)` |
| `ref` | `bindTable(tbl_id)` |

Implementation: each wrapper returns the factory's object from its `ref`:

```js
import { useImperativeHandle } from 'react';

useImperativeHandle(ref, () => bindTable(tbl_id), [tbl_id]);
```

`useImperativeHandle` is a built-in React hook that sets what a parent gets in `ref.current`,
here the API object instead of a DOM element. It rebuilds the object when `tbl_id` changes.
In React 19, `ref` is a regular prop on function components, so no `forwardRef` is needed.
See [React docs](https://react.dev/reference/react/useImperativeHandle).

---

## 4. Theming

> **Status: Proposed.** Discuss before implementing.

Proposed:

- **`createFireflyTheme(overrides)`**: deep-merges the user's overrides onto `defaultTheme()`
  and returns a Joy theme.
- **`<FireflyThemeProvider theme={...} colorScheme="light|dark|system">`**: wraps
  `CssVarsProvider` and `JoyToMUIThemeWrapper`. Some Firefly UI (e.g. date pickers) still uses
  MUI Material, so users shouldn't have to add that wrapper themselves.
- **Fitting into an app that already has a Joy theme:** document how to extend the host theme
  instead of nesting a second provider. Consider a `cssVarPrefix` option so Firefly's CSS
  variables don't collide with the host's `--joy-*` variables.
- **Theming outside Joy:** Plotly chart colors and image overlay/region colors don't come from
  the Joy theme. A small `firefly` section in the theme (chart color sequence, overlay
  defaults), read by the chart and image code, is needed for a consistent look,
  especially in dark mode.


---

## 5. Initialization

`<FireflyProvider>` connects the app to a Firefly server and sets up Firefly's store, inside
React. It renders a fallback until Firefly is ready, and an error if initialization fails.

```jsx
import { FireflyProvider } from '@ipac/firefly-components';

<FireflyProvider serverUrl="https://your-firefly-server/firefly/"
                 fallback={<Spinner/>}
                 errorFallback={(error) => <ConnectionError error={error}/>}>
    <App />
</FireflyProvider>
```

- **Same work as `initFirefly()`.** The provider calls `initFirefly()` internally; it adds
  React-style loading and error handling, so apps don't await anything before rendering.
- **`initFirefly()` stays** for scripts and non-React use. The provider is an addition, not
  a replacement.
- **Runs once.** Initialization happens once per page, even if the provider remounts or more
  than one is rendered.
- **Natural home for theming.** The provider can also accept the theme props from §4, so one
  component sets up both the server connection and the theme.
