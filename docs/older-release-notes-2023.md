# Firefly older Release Notes - 2023



## Version 2023.3
- 2023.3.8 - (April 1, 2024),  _docker tag_: `2023.3.8`
- 2023.3.6 - (March 13, 2024),  _docker tag_: `2023.3.6`
- 2023.3.5 - (Feb 29, 2024),  _docker tag_: `2023.3.5`
- 2023.3.4 - (Feb 28, 2024),  _docker tag_: `2023.3.4`
- 2023.3.3 - (Feb 8, 2024),  _docker tag_: `2023.3.3`
- 2023.3.2 - (Feb 7, 2024),  _docker tag_: `2023.3.2`
- 2023.3.1 - (Dec 13, 2023),  _docker tag_: `2023.3.1`
- 2023.3.0 - (Dec 12, 2023),  _docker tag_: `2023.3.0`

### _Notes_
#### This release includes many feature improvements across Firefly

#### New Features
- VO: Support DataLink 1.1: [Firefly-1325](https://github.com/Caltech-IPAC/firefly/pull/1439)
- VO: Full multi-product support for UWS: [Firefly-1282](https://github.com/Caltech-IPAC/firefly/pull/1423)
- VO: Support Multi spectrum table: [Firefly-1314](https://github.com/Caltech-IPAC/firefly/pull/1438)
- Multi Product Viewer: Handle multiple spectra at once via datalink table: [Firefly-1324](https://github.com/Caltech-IPAC/firefly/pull/1442)
- Multi Product Viewer: Handle related image via datalink table [Firefly-1298](https://github.com/Caltech-IPAC/firefly/pull/1420)
- Spectral Viewer: Support spectral redshift correction: [Firefly-1304](https://github.com/Caltech-IPAC/firefly/pull/1443)
- Table: Allow saving coordinates from a table as "regions": [Firefly-1235](https://github.com/Caltech-IPAC/firefly/pull/1452)
- Table: Support null values for all table data types: [Firefly-1297](https://github.com/Caltech-IPAC/firefly/pull/1447)
- Image Viewer: Improve readout handling for long-valued pixels and other issues: [Firefly-1317](https://github.com/Caltech-IPAC/firefly/pull/1435)
- Image Viewer: Line extraction will extract wavelength if applicable: [Firefly-871](https://github.com/Caltech-IPAC/firefly/pull/1451)
- HiPS Viewer: MOC overlays now default to new outline mode: [Firefly-1350](https://github.com/Caltech-IPAC/firefly/pull/1456)
- Drag and drop for file uploads: [Firefly-1310](https://github.com/Caltech-IPAC/firefly/pull/1426)
- Improved property sheet features: [Firefly-1352, Firefly-1307](https://github.com/Caltech-IPAC/firefly/pull/1455)
- Image Row Viewer: Component for SPHEREx UI [Firefly-1346](https://github.com/Caltech-IPAC/firefly/pull/1449)
- Performance: Update nom.tam.fits(1.18.2) and Starlink (4.1.4): [Firefly-1262](https://github.com/Caltech-IPAC/firefly/pull/1428)

#### Notable Bug Fixes
- Fixed: DCE: Coverage FOV not computed correctly: [Firefly-1336, fixed with Firefly-1346 implementation](https://github.com/Caltech-IPAC/firefly/pull/1449)
- Fixed: Table: enum values with commas now display correctly in filter menu: [Firefly-1333](https://github.com/Caltech-IPAC/firefly/pull/1445)
- Fixed: ADQL text-entry field swallowing "(Command)-(backquote)" on macOS: [Firefly-1072](https://github.com/Caltech-IPAC/firefly/pull/1436)

### _Patches 2023.3_
- 2023.3.8
    - wise patches [PR](https://github.com/Caltech-IPAC/firefly/pull/1525)
- 2023.3.6
    - access to NEOWISE release year 10 [PR,IRSA-5732](https://github.com/Caltech-IPAC/firefly/pull/1515)
- 2023.3.5
    - Bug fix: undefined exception
- 2023.3.4
    - Bug fix: table sorting no right in DCE [PR](https://github.com/Caltech-IPAC/firefly/pull/1504)
    - Bug fix: packaging download background failing [Firefly-1419](https://github.com/Caltech-IPAC/firefly/pull/1504)
    - Bug fix:  [IRSA-1507](https://github.com/Caltech-IPAC/firefly/pull/1504)
- 2023.3.3
    - Bug fix: Obscore Packaging: a double extension was added
- 2023.3.2
    - Obscore Packaging: Improved config with option for file names from server. [IRSA-5700](https://github.com/Caltech-IPAC/firefly/pull/1495)
    - FITS: more even zooming levels. [Firefly-1289](https://github.com/Caltech-IPAC/firefly/pull/1495)
    - TAP config: can specify a HiPS for a TAP server. [Firefly-1375](https://github.com/Caltech-IPAC/firefly/pull/1495)
    - API: added all export functions and constants from Table. [Firefly-1375](https://github.com/Caltech-IPAC/firefly/pull/1489)
- 2023.3.1
    - Fixed: TAP visual query builder ("`select_info` is undefined")

### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.3+)


## Version 2023.2
- 2023.2.6 - (Nov 20, 2023),  _docker tag_: `latest`, `2023.2`, `2023.2.6`
- 2023.2.5 - (unreleased version)
- 2023.2.4 - (Sept 28, 2023),  _docker tag_: `2023.2.4`
- 2023.2.3 - (Sept 21, 2023), _docker tag_: `2023.2.3`
- 2023.2.2 - (Sept 19, 2023), _docker tag_: `2023.2.2`
- 2023.2.1 - (Sept 14, 2023), _docker tag_: `2023.2.1`
- 2023.2.0 - (Aug 25, 2023), _docker tag_: `2023.2.0`

### _Notes_
#### This release includes some significant TAP features and a new property sheet support

#### New Features
- TAP: Improved support for joins: [Firefly-1257](https://github.com/Caltech-IPAC/firefly/pull/1406)
- TAP: Improved upload object ID search: [Firefly-1150](https://github.com/Caltech-IPAC/firefly/pull/1401)
- Property Sheet (detail table) support for any table: [Firefly-1256](https://github.com/Caltech-IPAC/firefly/pull/1404)
- Support Multi-dimensional WAVE-TAB algorithm: [Firefly-1169](https://github.com/Caltech-IPAC/firefly/pull/1383)
- Target Panel: Always show EQ J2000 coordinates: [Firefly-1234](https://github.com/Caltech-IPAC/firefly/pull/1385)
- Obscore data results shows search target: [Firefly-1291](https://github.com/Caltech-IPAC/firefly/pull/1411)

#### Other changes
- Read FITS tables directly with `nom.tam.fits` (removed star table): [Firefly-1232](https://github.com/Caltech-IPAC/firefly/pull/1390)
- Improved DCE: [Firefly-1286](https://github.com/Caltech-IPAC/firefly/pull/1408), [Firefly-1250](https://github.com/Caltech-IPAC/firefly/pull/1391)
- Firefly now uses React 18: [Firefly-1127](https://github.com/Caltech-IPAC/firefly/pull/1396)
- Firefly now uses Mavin central: [Firefly-1258](https://github.com/Caltech-IPAC/firefly/pull/1397)

#### Notable Bug Fixes
- Fixed: error in processing s_region values containing zeros: [Firefly-1281](https://github.com/Caltech-IPAC/firefly/pull/1413)
- Fixed: TAP UI crashes when selecting TAP_SCHEMA table: [Firefly-1292](https://github.com/Caltech-IPAC/firefly/pull/1412)

### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2023.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2023.2+)

### _Patches 2023.2_
- 2023.2.6
    - Fixed: API: Property sheet bug in - [Firefly-1347](https://github.com/Caltech-IPAC/firefly/pull/1448)
    - Fixed: API: noChartToolbar parameter ignored- [Firefly-1340](https://github.com/Caltech-IPAC/firefly/pull/1444)
- 2023.2.5
    - unreleased version, fixes a race condition - [Firefly-1338](https://github.com/Caltech-IPAC/firefly/pull/1437)
- 2023.2.4
    - Fixed: Firefly support for IPAC Table's short-form data type, e.g. 'i', 'f', 'b', etc. [commit](https://github.com/Caltech-IPAC/firefly/commit/475a4aff57374bfa70b01308e63402971f1fc291)
- 2023.2.3
    - Fixed: More Hips render issues [Firefly-1313](https://github.com/Caltech-IPAC/firefly/pull/1432)
        - missing tiles on deep zoom
        - more deep zoom rendering cleanup
        - optimized retrieving all sky image
- 2023.2.2
    - Fixed: HiPS rendering clean up [Firefly-1312](https://github.com/Caltech-IPAC/firefly/pull/1431)
    - Fixed: projections: if not specified `CD1_1` and `CD2_2` default to `CDELT1/2` [Firefly-1315](https://github.com/Caltech-IPAC/firefly/pull/1431)
    - Fixed: React 18 issue with API. We need to recreate the root for unrender/render [commit](https://github.com/Caltech-IPAC/firefly/commit/5e5b88c7c734fb76db54587d05bcdeda1c53eb6d)
- 2023.2.1
    - Fixed: HiPS tile pixels should not be smoothed at highest resolution [Firefly-1311](https://github.com/Caltech-IPAC/firefly/pull/1429)
    - Small bug fixes related to SHA Release [PR](https://github.com/Caltech-IPAC/firefly/pull/1425), [PR](https://github.com/Caltech-IPAC/firefly/pull/1425)

## Version 2023.1
- 2023.1.5 - (June 15, 2023)
    - docker tag: `latest`, `2023.1`, `2023.1.5`
- 2023.1.4 - (June 7, 2023)
    - docker tag: `2023.1.4`
- 2023.1.3 - (May 16, 2023)
    - docker tag: `2023.1.3`
- 2023.1.2 - (May 10, 2023)
    - docker tag: `2023.1.2`
- 2023.1.1 - (May 1, 2023)
    - docker tag: `2023.1.1`
- 2023.1.0 - (May 1, 2023)
    - docker tag: `2023.1.0`

### _Notes_
#### This release includes some significant new features, existing feature improvements, and code clean up

#### New Features
- TAP Upload: [Firefly-1142](https://github.com/Caltech-IPAC/firefly/pull/1317), [Firefly-1148](https://github.com/Caltech-IPAC/firefly/pull/1331), [Firefly-1189](https://github.com/Caltech-IPAC/firefly/pull/1337)
- Improved: TAP UI: [Firefly-1215](https://github.com/Caltech-IPAC/firefly/pull/1354)
- Improved: Click to search UI:  [Firefly-1152](https://github.com/Caltech-IPAC/firefly/pull/1326)
- Improved: Derived columns UI:  [Firefly-1153](https://github.com/Caltech-IPAC/firefly/pull/1330)
- Improved: Charting and Spectral UI layout: [Firefly-1183](https://github.com/Caltech-IPAC/firefly/pull/1348)
- Improved: 3-color selection: [Firefly-1134](https://github.com/Caltech-IPAC/firefly/pull/1310)
- UWS support: [Firefly-1128](https://github.com/Caltech-IPAC/firefly/pull/1308), [Firefly-1129](https://github.com/Caltech-IPAC/firefly/pull/1319)
- Faster image loading: [Firefly-1190](https://github.com/Caltech-IPAC/firefly/pull/1338)
- Table of loaded image available in pinned image section: [Firefly-1081](https://github.com/Caltech-IPAC/firefly/pull/1344)
- ObsCore table packaging: [Firefly-1193](https://github.com/Caltech-IPAC/firefly/pull/1351)
- Embedded spatial search UI in Hips: [Firefly-1177](https://github.com/Caltech-IPAC/firefly/pull/1328)

### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2023.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2023.1+)

### _Patches 2023.1_
- 2023.1.5
    - Fixed: heatmap failed due to uncaught exception [FIREFLY-1255](https://github.com/Caltech-IPAC/firefly/pull/1395)
    - Fixed: grid buttons missing from MultiProductViewer on the right hand side [commit](https://github.com/Caltech-IPAC/firefly/commit/afbfbe9962131e755642c50c773128a1a9e59f65)
    - Upgraded XStream to latest version due to Java's modularization enforcement [IRSA-5327](https://github.com/Caltech-IPAC/firefly/pull/1394)
- 2023.1.4
    - Fixed: Not catching spatial input errors [Firefly-1252](https://github.com/Caltech-IPAC/firefly/pull/1392)
    - Fixed: Not giving correct error in failed TAP search [Firefly-1253](https://github.com/Caltech-IPAC/firefly/pull/1389)
- 2023.1.3
    - Latest chrome release breaking GPU processing [Firefly-1247](https://github.com/Caltech-IPAC/firefly/pull/1384)
    - Additional small bug fixes [Firefly-1248](https://github.com/Caltech-IPAC/firefly/pull/1386)
- 2023.1.2
    - fixed: Handle undefined page size [Firefly-1245](https://github.com/Caltech-IPAC/firefly/pull/1380)
    - fixed: `obs_title` not showing up, wavelength wrong [Firefly-1246](https://github.com/Caltech-IPAC/firefly/pull/1381)
- 2023.1.1
    - fixed: polygon searches force spaces after comma [Firefly-1234](https://github.com/Caltech-IPAC/firefly/pull/1376)
