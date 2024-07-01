# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)

## Version 2024.2
  - 2024.2.3 - (July 1, 2024),   _docker tag_: `latest`, `2024.2`, `2024.2.3`
  - 2024.2.2 - (June 25, 2024),  _docker tag_: `2024.2.2`
  - 2024.2.1 - (June 24, 2024),  _docker tag_: `2024.2.1`
  - 2024.2.0 - (June 21, 2024),  _docker tag_: `2024.2.0`

### _Notes_
#### This release has a lot of bug fixes and clean up after the JoyUI conversion. It also includes some long requested updates.

#### New Features

- Images: Improved image sorting and filtering- Firefly-1448 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1543))
- Images: Support non-celestial coordinate readout- Firefly-1468 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1553))
- HiPS: Separated HiPS search panel out of images panel- Firefly-1465 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1547))
- TAP: Show overflow indicator if present- Firefly-1396 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1542))
- TAP: Improved obscore support- Firefly-1187 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1551))
- Tables: Improved support for null values- Firefly-1471 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1549))
- Python API: Improved Tri-view support- Firefly-1483 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1583))
- UI: Polygon input no longer requires commas- IRSA-5492 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1559))
- UI: Working views now use JoyUI Skeleton- Firefly-1494 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1574))

#### Bug fix and clean up
- Bug fixes:
  - Data product table handling- Firefly-1462 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1543))
  - Images: readout wrong with compressed files- Firefly-1476 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1558))
  - The main menu better adjust with font sizes- Firefly-1472 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1550))
  - Binned plot and chart-saving bugs- Firefly-1480 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1562))
  - TAP: Hidden columns included in column count- Firefly-1486 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1564))
  - Table: Column resets after a derived column fixed- Firefly-1494 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1574))
  - Remove non-functional option to email on background query completion- Firefly-1499 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1570))
  - Some components have double tooltips in Safari- Firefly-1501 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1572))
  - Issues with backgrounded downloads- Firefly-1502 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1574))
- Clean up:
  - Icons- IRSA-5925 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1524)), Firefly-1488 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1565)), Firefly-1506 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1576))
  - Table Info dialog- Firefly-1464 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1546))
  - Improved HiPS toolbar- Firefly-1473 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1551))
  - TAP: table selection- Firefly-1478 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1560))
  - Table related cleanup- Firefly-1479, Firefly-1481, Firefly-1484 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1563))
  - Typos in visible text- Firefly-1487 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1561))
  - UI: background handling- Firefly-1494 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1574))
  - Embedded search panel clean up and improvement- IRSA-5916 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1539)), Firefly-1451 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1548))
  - 
### _Patches 2024.2_
- 2024.2.1
  - Bug fix: Avoid IllegalStateException related to recycled request objects in Tomcat ([Commit](https://github.com/Caltech-IPAC/firefly/commit/02ea84b4d3cc758fb426341356cf2ef07920ceb6))
  - Bug fix: regression when parsing non-cube fits tables ([Commit](https://github.com/Caltech-IPAC/firefly/commit/c95b830ab9a57487d517db31f6d50c967228e4aa))
- 2024.2.2
  - Bug fix: FITS table reader failing on byte columns ([Commit](https://github.com/Caltech-IPAC/firefly/commit/b28b11f7912252e053128de0f8cd3a4ddb868896))
  - Bug fix: Regression issue. mask not going away with color dropdown ([Commit](https://github.com/Caltech-IPAC/firefly/commit/fa9439b533f08b72757f6ea480c0602c45d210f5))
  - Bug fix: source id extracted from image search- IRSA-5367 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1571))
- 2024.2.3
   - regression issue fixed with storing headers: ([PR](https://github.com/Caltech-IPAC/firefly/commit/0761857553bf74e0a00fa5c2478feaa7bb805609))

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2024.2+)



## Version 2024.1
- 2024.1.1 - (April 15, 2024),  _docker tag_: `latest`, `2024.1`, `2024.1.1`
- 2024.1.0 - (April 8, 2024),  _docker tag_: `2024.1.0`

### _Notes_
#### This release is a complete overhaul of the Firefly UI

#### New Features
- UI: Complete overhaul [All UI PRs](https://github.com/Caltech-IPAC/firefly/pulls?page=2&q=is%3Apr+milestone%3AUI-conversion)
  - New UI library [Joy UI](https://mui.com/joy-ui/getting-started/)
  - UI clean up
  - Support dark mode
  - Firefly is more skinable for other applications (fonts, colors)
  - New icons
  - Nicer colors, consistent color usage
  - Nicer fonts, consistent font usage
  - More consistency across UI (buttons, layout, etc.)
  - Better support for multiple result layouts. Detailed Tri-view and Bi-view layout control
  - Added Landing page
  - Primary navigation UX (tabs across top) revamped
      - Clearer tab UX
      - Results tab
      - Sidebar for additional navigation and settings
- TAP: Object ID can be searched using "Select...IN" [PR:Firefly-1450](https://github.com/Caltech-IPAC/firefly/pull/1526)
- TAP: TAP panels can now be locked to one TAP service or TAP obscore service
- Charts: Cascade-style plots for spectra [PR:Firefly-1370](https://github.com/Caltech-IPAC/firefly/pull/1499)
- Upload: improved support [PR:Firefly-1341](https://github.com/Caltech-IPAC/firefly/pull/1472)
- Tables: improved table error message handling [PR:Firefly-1445](https://github.com/Caltech-IPAC/firefly/pull/1535)

### _Patches 2024.1_
- 2024.1.1
  - Bug fix: API: app options being overwritten via api [PR:Firefly-1457](https://github.com/Caltech-IPAC/firefly/pull/1538)
  - Bug fix: Tables: crash when Simbad search fails [PR:Firefly-1458](https://github.com/Caltech-IPAC/firefly/pull/1538)


### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.1+label%3abug)
- All PRs
   - [UI conversion](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.3+)
   - [Other 2024.1 PRs](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Apr+milestone%3AUI-conversion)


# Older Release notes 2019 - 2023
- [2023](older-release-notes-2023.md)
- [2022](older-release-notes-2022.md)
- [2019-2021](older-release-notes-2019-2021.md)
