# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development - (Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on next version](next-release-details.md)


## Version 2022.2 
- 2022.2.4 (Sept 2022)
  - docker tag: `latest`, `2022.2`, `2022.2.4`
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
- Aitoff HiPS [Firefly-978](https://github.com/Caltech-IPAC/firefly/pull/1207)
- Improved dynamic field generation in service descriptors [Firefly-997](https://github.com/Caltech-IPAC/firefly/pull/1226)
- Support HiPs based target selection [Firefly-980](https://github.com/Caltech-IPAC/firefly/pull/1227)
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


##### _Patches 2022.2_
- 2022.2.1
  - Added IPAC Logo to version dialog([Firefly-1037](https://github.com/Caltech-IPAC/firefly/pull/1225))
  - Stretch dropdown shows checkbox if stretch selected ([Firefly-1029](https://github.com/Caltech-IPAC/firefly/pull/1225))
  - Fixed: TAP column table showing filters ([PR](https://github.com/Caltech-IPAC/firefly/pull/1244))
  - Fixed: Cube planes all change stretch ([Firefly-1038](https://github.com/Caltech-IPAC/firefly/pull/1225))
- 2022.2.2
  - Chart functions: now support `radians()` and `degrees()` [Firefly-1047](https://github.com/Caltech-IPAC/firefly/pull/1254))
  - Fixed: Table: Filter by selected row returned more than it should [Firefly-1049](https://github.com/Caltech-IPAC/firefly/pull/1250))
  - Multiple issues: [Firefly-1045](https://github.com/Caltech-IPAC/firefly/pull/1248))
    - Fixed: Expanding/restoring an image in MultiProductViewer causes reload: stretch and color are reset
    - Fixed: draw layer panel is hidden at times (zIndex was not correct)
    - Fixed: on resize or when popping up mouse readout: display wrongly zoomed to fit 
    - Fixed: Moc validation was not accepting a valid FITs TFORM header
    - Fixed: Better support for authorization in URLDownload.java
    - Fixed: Expose MOC in Target Hips Panel
    - Fixed: TAP search api did not init table correctly, some bugs in the examples
    - Stretch should be sticky: on MultiProductViewer, for multi HDU images, if the same extension type
- 2022.2.3
  - Fixed: Naif resolver too many results [Firefly-1065](https://github.com/Caltech-IPAC/firefly/pull/1264))
  - Fixed: Navigation icons sometimes do not show up in IRSA apps [IRSA-4769](https://github.com/Caltech-IPAC/firefly/pull/1263))
  - Improved: Recognize a "flux" column with charts in DataProduct Viewer  [Firefly-1068](https://github.com/Caltech-IPAC/firefly/pull/1265))
- 2022.2.4
  - Improved: Grouping in multi-product viewer is not per table [IRSA-4815](https://github.com/Caltech-IPAC/firefly/pull/1270))
  - Fixed: tap date selection feedback not updating [Firefly-1075](https://github.com/Caltech-IPAC/firefly/pull/1271))
  - Fixed: regression issue, pixel readout re-enabled for images, DM-36291



##### _Pull Requests in this release_
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


##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.1+)

##### _Patches 2022.1_
- 2022.1.1
  - Fixed: Not packaging proprietary data correctly ([IRSA-4570,IRSA-4571](https://github.com/Caltech-IPAC/firefly/pull/1209))
     


# Older Releases 2019 - 2021

See [Older release notes 2019-2021](older-release-notes-2019-2021.md)
