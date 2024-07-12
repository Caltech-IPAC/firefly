### Info and guide to firefly package.json - 2/1/2020


If package.json allowed comments we would put this inline.


## Runtime Dependencies

We are striving to keep most of the runtime dependencies up-to-date. The react/UI dependences will change often.
The non-react dependencies do not change much.

##### UI: `react` - always strive to keep up-to-date
- react
- react-dom
- prop-types

##### UI: 3 party react base packages
- fixed-data-table-2
- rc-tree
- react-color
- react-datetime
- react-dnd (not current version, _see below_)
- react-dnd-html5-backend (not current version, _see below_)
- react-grid-layout
- react-select
- react-sizeme
- react-split-pane
- prismjs

##### Non- UI:
- enum
- immutability-helper
- isomorphic-fetch
- local-storage
- lodash
- moment
- point-in-polygon
- shallowequal
- validator
- redux (v3, v4 has breaking changes, _see below_)
- redux-logger
- redux-saga
- redux-thunk

### Runtime Dependencies - not updated

These runtime dependencies that are purposely not most the most recent version

#### `redux`
We have not yet updating redux to version 4. Webpack 4 requires calls to getState() happen only outside
of a reducer. We will probably never go to webpack 4.

From release notes:
> Throw if getState, subscribe, or unsubscribe called while dispatching (including inside a reducer)

https://github.com/reduxjs/redux/tree/v4.0.0

Redux is fairly simple conceptually.  There is no urgency to update.

#### `redux-saga`
Has some breaking changes going to version 1. It will need some testing if we upgrade.
We use very little of redux-saga so the breaking changes might not affect us.

#### `react-dnd` and `react-dnd-backend`
These are used by `eternal/FilePicker`.  This code will need to back updated to use the new versions.



## Build, Test, Doc Dependencies

#### `Babel`
Babel is completely up to date.  We will work to keep it that way every year.
Babel seems to change required package almost every update. When we goto the next
version please make sure all these package are still necessary

babel packages:
- @babel/core
- @babel/preset-react
- @babel/register
- @babel/plugin-transform-runtime
- @babel/preset-env

Runtime
- core-js
- regenerator-runtime

#### `Webpack`
Webpack is completely up to date.  We will work to keep it that way every year.
The loaders are mostly up to date, some require node 10.13

Webpack packages:
- webpack
- webpack-cli
- babel-loader
- style-loader
- css-loader


#### `Jest`
Version 24. (Version 25 requires node >10)


#### `JSDoc`
JSDoc is completely up to date. We should always keep this up-to-date

JSDoc packages:
- jsdoc
- jsdoc-jsx
- ink-docstrap
- publish-release
- taffydb  - JSDoc claims not to need this for 4.0 but I think ink-docstrap needs it

#### `ESLint`
ESLint is completely up to date. We should always keep this up-to-date

ESLint packages:
- eslint
- babel-eslint
- eslint-loader
- eslint-plugin-jsdoc
- eslint-plugin-jsx-a11y
- eslint-plugin-react
- eslint-plugin-react-hooks

