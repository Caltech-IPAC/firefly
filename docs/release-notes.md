# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)


## Version 2024.3
- 2024.3.5 - (Dec 12, 2024),  _docker tag_: `2024.3.5`, `2024.3`, `latest`
- 2024.3.4 - (Dec 10, 2024),  _docker tag_: `2024.3.4`,
- 2024.3.3 - (Dec 2, 2024),  _docker tag_: `2024.3.3`
- 2024.3.2 - (Nov 5, 2024),  _docker tag_: `2024.3.2`
- 2024.3.1 - (Oct 24, 2024),  _docker tag_: `2024.3.1`
- 2024.3.0 - (Oct 18, 2024),  _docker tag_: `2024.3.0`

### _Notes_
This Firefly release has a lot of new features, probably among the most new features packed into one release over the past several years.

The release has a big data focus. Firefly now handles very large tables and will seamlessly visualize this data.
Table loading is faster and can handle much larger tables. Large catalog overlays are significantly improved.
Several chart bugs related to big data are fixed.

It also includes many, many bug fixes, clean up, and optimization (not all are listed below).

#### Major Features
- Tables: parquet support- Firefly-1477 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1582))
- Tables: save table as parquet using `parquet.votable`- Firefly-1550 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1633))
- Tables: internal data optimization using duckdb - Firefly-1477 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1582))
- Tables: Drawing Overlay Color in table tabs- Firefly-1510 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1600))
- Images/HiPS: hierarchical catalogs- Firefly-1537 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1607))
- Data product viewer: recognizes service descriptor defined cutouts- Firefly-1491 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1580))

#### New Features
- Images: Improve image sorting and filtering- Firefly-1448 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1543))
- Images: Improve line extraction- Firefly-1560 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1627))
- Table: Improved table from fits image - Firefly-1180 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1627))
- AWS: runid job name is table name- Firefly-1533 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1618))
- TAP: Save users added TAP servers as preference- Firefly-1558 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1623))
- TAP: set search title- Firefly-1510 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1600))

#### Bug fix / cleanup
- Fixed: Firefly-1535, reversal of axes bug ([PR](https://github.com/Caltech-IPAC/firefly/pull/1632))
- Fixed: Dialog sizing- Firefly-1555 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1626)), Firefly-1553 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1624))
- Fixed: Chart related bugs- Firefly-1521 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1614))
- Fixed: Images: Color dropdown color wrongly invert in dark mode- Firefly-1547 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1612))
- Fixed: Charts: changing x/y axis does not work- IRSA-6084 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1608))
- Fixed: not parsing gaia datalink correctly- Firefly-1529 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1601))
- Fixed: chart is not recognizing short- Firefly-1516 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1595))
- Fixed: popup not closing until second click- Firefly-1514 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1589))
- Fixed: firefly not supporting ellipse in the region save- Firefly-1582 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1654))
- Fixed: import JWST footprint- IRSA-6024 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1660))
- Cleanup: Better recognition of VO table Utype- Firefly-1534 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1636))
- Cleanup: Better tap sizing- Firefly-1562 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1634))
- Cleanup: TAP: ADQL dark mode screen- Firefly-1509 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1590))

#### Not user facing
- Web API: improved hipsPanel endpoint- Firefly-1541 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1622))
- Datalink: small bug fixes: Firefly-1560, Firefly-1180 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1627)),
- Datalink: Recognized datalink table in upload-  Firefly-1523 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1597))

#### Infrastructure
- Java 21-  Firefly-1559 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1628))
- plot.ly 2.32-  Firefly-1504 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1579))
- nom.tam.fits 1.20-  Firefly-1512 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1585))
- other package updates- Firefly-1513 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1587)), Firefly-1503 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1581))

### _Patches 2024.3_

- 2024.3.5
  - Bug fix: Coverage not showing in Safari and older Firefox < 131 ([commit](https://github.com/Caltech-IPAC/firefly/commit/5270b4e3e88c3e5d9eb5bc77d95f8e45dcf99ad2))
- 2024.3.4
  - Enhancement: Mouse Readout: can copy to Python SkyCoord- Firefly-1572 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1674))
  - Bug fix: null ra/dec should not be plotted on coverage map- Firefly-1597 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1679))
  - Bug fix: Coverage map not sizing correctly- Firefly-1614 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1679))
  - Bug fix: filtering on spectral plots with >1- Firefly-1627 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1627))

- 2024.3.3
  - Enhancement: Spectrum guesser: recognize more tables- Firefly-1620 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1673))
  - Bug fix: URL Api: Image loading sometimes failing- Firefly-1621 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1672))
  - Bug fix: URL Api: Table loading following redirects- Firefly-1617 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1672))
  - Enhancement: Table: Added mode to do link parsing in previous way- ([commit](https://github.com/Caltech-IPAC/firefly/commit/7167b490ee8caab46d358c15d9e559321cf8ab07))

- 2024.3.2
  - Bug fix: Coverage now testing up to 36 million rows Firefly-1601 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1661))
  - JS API: config to disable the menu Firefly-1595 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1663))
  - Bug fix: Coverage center not computed correctly ([commit](https://github.com/Caltech-IPAC/firefly/commit/f3e0adb366626b323f2326c02ec8113d3c591682))
  - Bug fix: rendering issues with data product table without chart ([commit](https://github.com/Caltech-IPAC/firefly/commit/cc5d0e73f1be10d577a67103cd0984a0b058061d))
  - Bug fix: DCE crashed when in wrong state. Firefly-1604 ([commit](https://github.com/Caltech-IPAC/firefly/commit/9ecdd0e1eda1119990b6a29356188d38997f37f5))

- 2024.3.1
  - Bug fix: lock by click not changing images with shift-click and updating correct value ([PR](https://github.com/Caltech-IPAC/firefly/pull/1660))
  - Bug fix: data product viewer will use a scrollbar with pngs- Firefly-1589 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1660))
  - Bug fix: more drawing layers are now sticky until user remove them-  Firefly-1587 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1660))
  - Bug fix: coordinate system options for mouse readout and grid are consistent-  Firefly-1584 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1660))
  - Bug fix: JS API: table in expanded more not switching tabs- -  IRSA-6431 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1661))
  - Bug fix: Catalogs were not load with certain type of table uploads: Firefly-1583 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1657))
  - Bug fix: Upload panel not showing table summary in certain cases: Firefly-1584 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1659))
  - When looking for center ra/dec columns in a table, if more than one column has the same UCD, the parser will prefer a floating column over a string 

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2024.3+)



## Version 2024.2
  - 2024.2.5 - (July 31, 2024),   _docker tag_: `2024.2`, `2024.2.5`
  - 2024.2.4 - (July 24, 2024),   _docker tag_: `2024.2.4`
  - 2024.2.3 - (July 1, 2024),   _docker tag_: `2024.2.3`
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

- 2024.2.5 
   - Bug fix: bug in table column analyzing ([Commit](https://github.com/Caltech-IPAC/firefly/commit/a94a467367f47c9612030a36ada6b37f24546dac))
   - Bug fix: Active row watch not handling SSA tables correctly ([Commit](https://github.com/Caltech-IPAC/firefly/commit/0316fea849aa67cf49342cdb1c2b2b4ad72dab4b))
- 2024.2.4 
   - Bug fix: Issue with Hydra template slot props  ([Commit](https://github.com/Caltech-IPAC/firefly/commit/1aec3593bb6c6fc817dff7cb797acfd5e75a43a7))
   - Bug fix: MultiProductViewer: 3 color button showing in single mode  ([Commit](https://github.com/Caltech-IPAC/firefly/commit/9209284c00605319e8e40f596766f15e82eb402e))
   - Bug fix: Issues for WISE application ([Commit](https://github.com/Caltech-IPAC/firefly/commit/58185602365cea888941798f15e5e03c378589c3)), ([Commit](https://github.com/Caltech-IPAC/firefly/commit/009ad7bd80e83f6ceae4d694b06d69a018393278))
   - Bug fix: comment displayed in JSX code ([Commit](https://github.com/Caltech-IPAC/firefly/commit/031ffd63e89f53213f53a68b344939427a1de124))
- 2024.2.3
   - regression issue fixed with storing headers: ([PR](https://github.com/Caltech-IPAC/firefly/commit/0761857553bf74e0a00fa5c2478feaa7bb805609))
- 2024.2.2
  - Bug fix: FITS table reader failing on byte columns ([Commit](https://github.com/Caltech-IPAC/firefly/commit/b28b11f7912252e053128de0f8cd3a4ddb868896))
  - Bug fix: Regression issue. mask not going away with color dropdown ([Commit](https://github.com/Caltech-IPAC/firefly/commit/fa9439b533f08b72757f6ea480c0602c45d210f5))
  - Bug fix: source id extracted from image search- IRSA-5367 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1571))
- 2024.2.1
  - Bug fix: Avoid IllegalStateException related to recycled request objects in Tomcat ([Commit](https://github.com/Caltech-IPAC/firefly/commit/02ea84b4d3cc758fb426341356cf2ef07920ceb6))
  - Bug fix: regression when parsing non-cube fits tables ([Commit](https://github.com/Caltech-IPAC/firefly/commit/c95b830ab9a57487d517db31f6d50c967228e4aa))

### _Pull Requests in this release_
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
