# Firefly older Release Notes - 2022



## Version 2022.3
- 2022.3.2 - (March 2023)
  - docker tag: `2022.3`, `2022.3.2`
- 2022.3.1 - (Jan 2023)
  - docker tag: `2022.3`, `2022.3.1`
- 2022.3.0 - (Dec 2022)
  - docker tag: `2022.3.0`

### _Notes_

#### This release has some significant new features and code clean up

#### New Features
- Spectral Charts support combining spectra: [Firefly-1041](https://github.com/Caltech-IPAC/firefly/pull/1274)
- Click to Search: [Firefly-1086](https://github.com/Caltech-IPAC/firefly/pull/1275)
- Derived columns in tables: [Firefly-1042](https://github.com/Caltech-IPAC/firefly/pull/1283)

#### UI Enhancements
- Improvements to extraction: [Firefly-1060](https://github.com/Caltech-IPAC/firefly/pull/1280)
- Improved MOC uploading: [Firefly-1124](https://github.com/Caltech-IPAC/firefly/pull/1294)
- Datalink driven searchform: [Firefly-993](https://github.com/Caltech-IPAC/firefly/pull/1298)
- Improving chart/table pinning: [Firefly-1024](https://github.com/Caltech-IPAC/firefly/pull/1299)
- Improved mask layer UI: [Firefly-1133](https://github.com/Caltech-IPAC/firefly/pull/1304)
- TAP panel stability and features: [Firefly-1115](https://github.com/Caltech-IPAC/firefly/pull/1286)

#### Infrastructure Enhancements
- Plot.ly updated to 2.18.0: [Firefly-1079](https://github.com/Caltech-IPAC/firefly/pull/1272)
- Improved status page with version: [Firefly-1116](https://github.com/Caltech-IPAC/firefly/pull/1295)

### _Patches 2022.3_
- 2022.3.1
  - fixed an undefined exception when SearchActions are empty
- 2022.3.2
  - Updates for neowise year 9 release [PR](https://github.com/Caltech-IPAC/firefly/pull/1329)
  - fixed: in certain cases multi-product view navigation not working when non-dataproduct tables are showing [Firefly-1182](https://github.com/Caltech-IPAC/firefly/pull/1329)

### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.3+)


## Version 2022.2
- 2022.2.6 (Dec 2022)
  - docker tag: `2022.2`, `2022.2.6`
- 2022.2.5 (Oct 2022)
  - docker tag: `2022.2.5`
- 2022.2.4 (Sept 2022)
  - docker tag: `2022.2.4`
- 2022.2.3 (Sept 2022)
  - docker tag: `2022.2.3`
- 2022.2.2 (Aug 2022)
  - docker tag: `2022.2.2`
- 2022.2.1 (July 2022)
  - docker tag: `2022.2.1`
- 2022.2 - (July 2022)
  - docker tag: `2022.2.0`

### _Notes_
#### This release has notable UI, Infrastructure, and  API enhancements

#### UI Enhancements
- Charts support multiple charts from different tables [Firefly-994](https://github.com/Caltech-IPAC/firefly/pull/1229)
- Aitoff HiPS [Firefly-968](https://github.com/Caltech-IPAC/firefly/pull/1207)
- Improved dynamic field generation in service descriptors [Firefly-997](https://github.com/Caltech-IPAC/firefly/pull/1226)
- Support HiPS-based target selection [Firefly-980](https://github.com/Caltech-IPAC/firefly/pull/1227)
- Filters are on for all search tables [Firefly-971](https://github.com/Caltech-IPAC/firefly/pull/1213)
- Improved large table loading performance [Firefly-978](https://github.com/Caltech-IPAC/firefly/pull/1206)

#### API Enhancements
- API: Add the capability to a create persistent resource from a table request [Firefly-982](https://github.com/Caltech-IPAC/firefly/pull/1214)

#### Infrastructure Enhancements
- Docker support passing firefly client options on startup [Firefly-885](https://github.com/Caltech-IPAC/firefly/pull/1227)
- Improved passing properties to docker [Firefly-996](https://github.com/Caltech-IPAC/firefly/pull/1225)
- Improved status page [Firefly-992](https://github.com/Caltech-IPAC/firefly/pull/1218)

#### Notable Bug fixes
- Fixed: WAVE_TAB: The algorithm is producing incorrect results [Firefly-989](https://github.com/Caltech-IPAC/firefly/pull/1224)
- Multiple table related bugs


### _Patches 2022.2_
- 2022.2.1
  - Added IPAC Logo to version dialog ([Firefly-1037](https://github.com/Caltech-IPAC/firefly/pull/1225))
  - Stretch dropdown shows checkbox if stretch selected ([Firefly-1029](https://github.com/Caltech-IPAC/firefly/pull/1225))
  - Fixed: TAP column table showing filters (Firefly-1036, [PR](https://github.com/Caltech-IPAC/firefly/pull/1244))
  - Fixed: Cube planes all change stretch ([Firefly-1038](https://github.com/Caltech-IPAC/firefly/pull/1225))
- 2022.2.2
  - Chart functions: now support `radians()` and `degrees()` ([Firefly-1047](https://github.com/Caltech-IPAC/firefly/pull/1254))
  - Fixed: Table: Filter by selected row returned more than it should ([Firefly-1049](https://github.com/Caltech-IPAC/firefly/pull/1250))
  - Multiple issues: ([Firefly-1045](https://github.com/Caltech-IPAC/firefly/pull/1248))
    - Fixed: Expanding/restoring an image in MultiProductViewer causes reload: stretch and color are reset
    - Fixed: draw layer panel is hidden at times (zIndex was not correct)
    - Fixed: on resize or when popping up mouse readout: display wrongly zoomed to fit
    - Fixed: Moc validation was not accepting a valid FITs TFORM header
    - Fixed: Better support for authorization in URLDownload.java
    - Fixed: Expose MOC in Target Hips Panel
    - Fixed: TAP search api did not init table correctly, some bugs in the examples
    - Stretch should be sticky: on MultiProductViewer, for multi HDU images, if the same extension type
- 2022.2.3
  - Fixed: Naif resolver too many results ([Firefly-1065](https://github.com/Caltech-IPAC/firefly/pull/1264))
  - Fixed: Rendering of service-descriptor-based searches on TAP results (Rubin) ([Firefly-1066](https://github.com/Caltech-IPAC/firefly/pull/1263))
  - Fixed: Navigation icons sometimes do not show up in IRSA apps ([IRSA-4769](https://github.com/Caltech-IPAC/firefly/pull/1263))
  - Improved: Recognize a "flux" column with charts in DataProduct Viewer ([Firefly-1068](https://github.com/Caltech-IPAC/firefly/pull/1265))
- 2022.2.4
  - Improved: Grouping in multi-product viewer is not per table ([IRSA-4815](https://github.com/Caltech-IPAC/firefly/pull/1270))
  - Fixed: tap date selection feedback not updating ([Firefly-1075](https://github.com/Caltech-IPAC/firefly/pull/1271))
  - Fixed: regression issue, pixel readout re-enabled for images ([DM-36291](https://jira.lsstcorp.org/browse/DM-36291))
- 2022.2.5
  - Fixed: null pointer exception in `BaseIUbeDataSource.java`
- 2022.2.6
  - Fixed: some service descriptors without a `standardID` would not load correctly



### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.2+)


## Version 2022.1
- 2022.1.1 (April 2022)
  - docker tag: `latest`, `2022.1`, `2022.1.1`
- 2022.1.0 (March 2022)
  - docker tag: `2022.1.0`

### _Notes_
#### This release is focused on small enhancements, fixing bugs, and updating infrastructure. There are no new UI changes in this release.

#### Enhancements
- API: provide way to disable extraction [Firefly-907](https://github.com/Caltech-IPAC/firefly/pull/1175)
- Extraction no longer turns off when new images are added [Firefly-908](https://github.com/Caltech-IPAC/firefly/pull/1174)
- More image dataset added [IRSA-4468](https://github.com/Caltech-IPAC/firefly/pull/1191)
- FITS and HiPS info dialogs better integrated [Firefly-323](https://github.com/Caltech-IPAC/firefly/pull/1185)
- TAP: Automatic quoting of non-standard TAP columns, especially helpful when searching VizieR [Firefly-935](https://github.com/Caltech-IPAC/firefly/pull/1182)
- Tables: Turn on units by default; raise category-column threshold [Firefly-969, Firefly-970](https://github.com/Caltech-IPAC/firefly/pull/1201)

#### Notable Bug fixes
- Fixed: Some TAP entries truncated [Firefly-719](https://github.com/Caltech-IPAC/firefly/pull/1178)
- Fixed: repeating headers are not showing [Firefly-926](https://github.com/Caltech-IPAC/firefly/pull/1173)
- Fixed: Saving filtered tables [Firefly-931](https://github.com/Caltech-IPAC/firefly/pull/1180)
- Fixed: occasionally images get stuck a loading state  [Firefly-933](https://github.com/Caltech-IPAC/firefly/pull/1202)

#### Infrastructure updates
- Java updated to v `17`  [Firefly-932](https://github.com/Caltech-IPAC/firefly/pull/1192)
- log2j updated to `2.17.1` [Firefly-933](https://github.com/Caltech-IPAC/firefly/pull/1188)
- Most javascript libraries updated [Firefly-917](https://github.com/Caltech-IPAC/firefly/pull/1166)
- _Build_: Gradle updated to `7.4` [Firefly-932](https://github.com/Caltech-IPAC/firefly/pull/1192)
- _Build_: Webpack updated to `5.6`, build now required node `12+` [Firefly-917](https://github.com/Caltech-IPAC/firefly/pull/1166)
- _Build_: Improved docker environment [Firefly-924](https://github.com/Caltech-IPAC/firefly/pull/1166)


### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.1+)

### _Patches 2022.1_
- 2022.1.1
  - Fixed: Not packaging proprietary data correctly ([IRSA-4570,IRSA-4571](https://github.com/Caltech-IPAC/firefly/pull/1209))
     

