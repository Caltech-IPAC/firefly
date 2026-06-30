# Firefly Component Library

A React component library for astronomical data visualization, built on
[Firefly](https://github.com/Caltech-IPAC/firefly), the web visualization
framework from [Caltech/IPAC](https://www.ipac.caltech.edu/).

The library provides ready-to-use components for displaying catalog tables, 
interactive charts, and astronomical imagery, including FITS and HiPS sky images, 
that integrate with a running Firefly server.

---

## Installation

```bash
yarn add @ipac/firefly-components
```

### Peer dependencies

These packages are expected to be provided by the host application, allowing 
our library to share your project's existing instance of React, Joy UI, 
etc. without duplicating core code or breaking your global theme context:

```bash
yarn add "react@^19.0.0" "react-dom@^19.0.0" \
  "@mui/joy@^5.0.0-beta.52" "@mui/material@^5.18" "@mui/icons-material@^5.18" "@mui/x-date-pickers@^6.19" \
  "@emotion/react@^11.11" "@emotion/styled@^11.11" \
  "moment@~2.30"
```

---

## Setup

Every app using this library must call **`initFirefly`** once before rendering
any components. It connects to the Firefly server that handles data processing,
FITS image serving, and catalog queries.

```js
import { initFirefly } from '@ipac/firefly-components';

await initFirefly({ serverUrl: 'https://your-firefly-server/firefly/' });
```

Call it at your app's entry point, before your root `ReactDOM.createRoot` render,
or in a top-level `async` setup function.

### The Firefly server

The `serverUrl` is the base URL of a running
[Firefly server](https://github.com/Caltech-IPAC/firefly). The server is
responsible for:

- Fetching and processing FITS image tiles
- Running catalog queries (IRSA, TAP, VO services)
- Computing chart data (histograms, scatter aggregation)
- Handling large table uploads and filters

| Environment | Typical URL |
|---|---|
| Local development | `http://localhost:8080/firefly/` |
| Shared dev server | `https://dev.server.example.edu/firefly/` |
| Production | `https://prod.server.example.edu/firefly/` |

The trailing slash is required. The default (if you omit `serverUrl`) is
`http://localhost:8080/firefly/`.

### React setup

Wrap your app in `CssVarsProvider` from MUI Joy and apply the Firefly theme:

```jsx
import { CssVarsProvider, extendTheme } from '@mui/joy';
import { initFirefly, defaultTheme } from '@ipac/firefly-components';
import '@ipac/firefly-components/styles';

await initFirefly({ serverUrl: 'https://your-firefly-server/firefly/' });

const theme = extendTheme(defaultTheme());

ReactDOM.createRoot(document.getElementById('root')).render(
  <CssVarsProvider theme={theme}>
    <App />
  </CssVarsProvider>
);
```

---

## Available components

The library is split into subpath packages so you only import what you need.

**`@ipac/firefly-components`** is the core package. It contains `initFirefly`,
the Firefly theme, form inputs, and field group utilities.

**`@ipac/firefly-components/tables`** contains catalog table components and
utilities for loading, searching, and manipulating tabular data.

**`@ipac/firefly-components/charts`** contains interactive chart components
(scatter plots, histograms, and more) that link directly to table data.

**`@ipac/firefly-components/images`** contains FITS image viewer components
for sky image display with WCS overlays and color stretch controls.

```js
import { initFirefly }       from '@ipac/firefly-components';
import { DataTable }         from '@ipac/firefly-components/tables';
import { HistogramChart }    from '@ipac/firefly-components/charts';
import { ImagePlot }         from '@ipac/firefly-components/images';
```

---

## Try it locally

This walks through creating a minimal React app that renders three linked components: 
a survey map (HiPS) viewer on the left, a scatter chart on the right, and a catalog table at the bottom, 
all driven by the same WISE dataset.

### 1. Create the app

```bash
yarn create vite test-firefly-comp-lib --template react
```

Do not run `yarn dev` yet — the library and its peer dependencies still need to be installed.

### 2. Install peer dependencies

```bash
cd test-firefly-comp-lib
```

See [Peer dependencies](#peer-dependencies) above for the full install command.

### 3. Install the library

Install the package from npm:

```bash
yarn add @ipac/firefly-components
```


Or install it from a local build:

```bash
# from component-library/
yarn build && yarn pack
# produces package.tgz

cd /path/to/test-firefly-comp-lib
yarn add /path/to/component-library/package.tgz
```

### 4. Set up the entry point

Replace `src/main.jsx`:

```jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import { CssVarsProvider, extendTheme } from '@mui/joy';
import { initFirefly, defaultTheme } from '@ipac/firefly-components';
import '@ipac/firefly-components/styles';
import { App } from './App';

await initFirefly({ serverUrl: 'http://localhost:8080/firefly/' });

ReactDOM.createRoot(document.getElementById('root')).render(
    <CssVarsProvider theme={extendTheme(defaultTheme())}>
        <App />
    </CssVarsProvider>
);
```

### 5. Create the welcome page

Create `src/App.jsx`:

```jsx
import React from 'react';
import {DataTable, makeFileRequest}
    from '@ipac/firefly-components/tables';
import {ScatterChart}
    from '@ipac/firefly-components/charts';
import {Coverage}
    from '@ipac/firefly-components/images';

const TABLE_URL = 'https://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl';
const TBL_ID = 'wise-demo';

const WISE_REQUEST = makeFileRequest('WISE Table', TABLE_URL, null, {tbl_id: TBL_ID});

export function App() {
    return (
        <div style={{display: 'flex', flexDirection: 'column', height: '100vh', padding: 8, gap: 8}}>
            <h2 style={{margin: 0}}>Firefly Component Library Demo</h2>
            <div style={{display: 'flex', gap: 8, flex: 1, minHeight: 0}}>
                <Coverage sx={{flex: 1}}/>
                <ScatterChart tbl_id={TBL_ID} x_axis='crval1' y_axis='crval2' sx={{flex: 1}}/>
            </div>
            <DataTable source={WISE_REQUEST} sx={{flex: '0 0 280px'}}/>
        </div>
    );
}
```

Selecting or filtering rows in the table highlights the corresponding points in the scatter chart
and corresponding overlay markers in the coverage image because they share the same `tbl_id`.

### 6. Run it

```bash
yarn dev
```

Open `http://localhost:5173`.

