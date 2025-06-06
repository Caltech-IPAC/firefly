# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)


## Version 2025.3
- 2025.3.0 - (June 6, 2025), planned _docker tag_: `2025.3.0`, `2025.3`, `latest`

#### This release has a complete revamp of the Job monitor and extensive work to support Rubin, SPHEREx, and Euclid needs

#### Major Features
- Job Monitor (formally Background Monitor): Complete revamp: Firefly-1698 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1742)), Firefly-1735 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1760)), Firefly-1327 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1765)), Firefly-1749 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1770))
- Packaging: download script supports cutouts- Firefly-1662 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1715))
- Packaging: download script checks for duplicates, better naming- Firefly-1704 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1745)),
- Images: Wavelength readout includes bandwidth- Firefly-1482 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1733))
- Color Dialog: color lock button more prominent, Hue preserving better integrated, disabled bound checking for data range- Firefly-1740, Firefly-48 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1766))
- URL API: goto tab: Firefly-1336 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1763))

#### TAP
- Support for recognizing and querying array ra/dec `xtype==='point'`, `pos.eq;meta.main` columns- Firefly-1763 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1760))
- Added CANFAR TAP service: https://ws-uv.canfar.net/youcat Firefly-1763 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1760))
- Updated the MAST TAP URL: https://mast.stsci.edu/vo-tap/api/v0.1/caom  Firefly-1728 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1759))
- Improved Scheme and tables navigation-  Firefly-1733 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1759))
- Server logging, for working issues: A TAP UWS call will now log a synchronous version url - Firefly-1733 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1759))

#### Data Product Viewer Updates
- Supports pdf, yaml, json and plain text files - Firefly-1701([PR](https://github.com/Caltech-IPAC/firefly/pull/1741))
- Better UI with service descriptors and catalogs with service descriptors - Firefly-1730 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1741))
- Support related grid for more types of image products- Firefly-1743 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1767))
- Charts that are spectra or timeseries are pinned with the table- Firefly-1755 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1777))
- Tables keep state when switching to another tab- Firefly-1772 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1785))

#### Enhancements to support applications built on Firefly
- Improved Dock layout-  IRSA-6898 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1769))
- More flexible layout with EmbedPositionSearchPanel-  IRSA-6794 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1771))
- Better UI feedback in EmbedPositionSearchPanel-  IRSA-6747 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1739))
- Improve Wavelength panel- Firefly-1723, Firefly-1726 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1758))
- Create a WavelengthInputField component to handle trailing units- Firefly-1653 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1734))
- Generalize UploadTableSelector for handling shape fit columns- Firefly-1720 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1751))
- Improved DCE SIA support - Firefly-1469 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1742))

#### Not user facing
- Images: FITS memory management optimizations- Firefly-1725([PR](https://github.com/Caltech-IPAC/firefly/pull/1680))
- Packaging: download scripting and zipping behave the same way- Firefly-1693 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1738))
- Better handling of Redis failure and reconnection- Firefly-1727 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1754))

#### Bug fix
- Fixed: Added columns go away- Firefly-1721 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1764))
- Fixed: Filtering not working for some very small selections- Firefly-1734 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1762))
- Fixed: Catalog not scrolling on center change- Firefly-6890 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1762))
- Fixed: Table image point upload does not work anymore- Firefly-1390 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1761))
- Fixed: Client tables reset scroll on column width change- Firefly-1729 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1757))
- Fixed: No error when upload table can't be ready- Firefly-1695 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1756))
- Fixed: Better handle data product catalog that connect to images- Firefly-1718 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1747))
- Fixed: Failed to recognize single column CSV file- Firefly-1715 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1746))
- Fixed: Refine search region stays on when it shouldn't- Firefly-1706 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1744))
- Fixed: TAP search from selection tool is broken- Firefly-1195 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1744))
- Fixed: chart filter icon does not work on column mapped to a label- Firefly-1378 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1726))
- Fixed: Table column filter misinterpret 'NOT LIKE'- Firefly-1265 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1773))
- Fixed: Charts X/Y Ratio ui cutting off problem- Firefly-1753 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1775))
- Fixed: when HiPS is changed, the associated MOC is added- Firefly-1667 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1786))
- Fixed: table upload is only uploaded page size and not whole table- Firefly-1771 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1788))
- Fixed: table names had a zero appended to the name when there are zero rows- ([commit](https://github.com/Caltech-IPAC/firefly/commit/0384e4f7b966c7e8c4cca3b0c4be63ae8e4fb5a1))
- Fixed: admin/status page list of host using shared work area sorted wrong - ([commit](https://github.com/Caltech-IPAC/firefly/commit/60f0144c5ec2604ace58cb32a242cec76cf13b75))


### _Patches 2025.3_
_initial release_



## Version 2025.2
- 2025.2.3 - (April 8, 2025), _docker tag_: `2025.2.3`, `2025.2`, `latest`
- 2025.2.2 - (March 20, 2025), _docker tag_: `2025.2.2`,
- 2025.2.1 - (March 14, 2025), _docker tag_: `2025.2.1`
- 2025.2.0 - (March 13, 2025), _docker tag_: `2025.2.0`

#### Major Features
- Improved datalink cutout handling- Firefly-1666([PR](https://github.com/Caltech-IPAC/firefly/pull/1717)), Firefly-1662 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1715))
- Healpix catalog display will even larger tables (50 million or more)- Firefly-1661 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1713))
- Readout options are saved as preferences- Firefly-1660 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1707))
- Target Panel: example clickable- Firefly-441 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1696))
- Time Series: Now shows loaded file name in period finder- IRSA-4725 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1721))

#### Not user facing
- Improved job completion framework- Firefly-1609([PR](https://github.com/Caltech-IPAC/firefly/pull/1719)),
- Docker: Firefly Entrypoint Extractions- CADC-13454 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1708))
- Docker: Improved ingestion of parameters- Firefly-1648 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1698))

#### Bug fix
- Better Simbad search errors- IRSA-6654 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1714))
- Fixed multiple MOC issues- Firefly-1663 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1716))
- Result showing search target is more accurate- IRSA-6574 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1712))
- Table: does not function correctly when a column contains non-ASCII characters- Firefly-1616 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1716))


### _Patches 2025.2_
- 2025.2.3
  - Enhancement: Enable experimental LSDB panel behind url api option - Firefly-1702: ([PR](https://github.com/Caltech-IPAC/firefly/pull/1740))
  - Bug fix: Multi product viewer referencing tar files crash- Firefly-1688: ([PR](https://github.com/Caltech-IPAC/firefly/pull/1732))
  - Bug fix: Image filtering with checkbox broken- Firefly-1689: ([PR](https://github.com/Caltech-IPAC/firefly/pull/1732))
- 2025.2.2
  - Bug fix: datalink processing not recognizing application/fits as image ([commit](https://github.com/Caltech-IPAC/firefly/commit/f5b447e6a22e1ef53131b61926efc516411527af))
- 2025.2.1
  - Bug fix: prevent excessive thread use- Firefly-1683 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1727))
  - Bug fix: computation of healpix for visible tables to rough ([commit](https://github.com/Caltech-IPAC/firefly/commit/48df3f87857b01209a9cb0e54876c8ed2f1d2721))

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2025.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2025.2+)



## Version 2025.1
- 2025.1.1 - (Feb  6, 2025),  _docker tag_: `2025.1`, `2025.1.1`
- 2025.1.0 - (Jan 27, 2025),  _docker tag_: `2025.1.0`

### _Notes_
#### Major updates in ingesting tables, SIAv2 and better handling of obscore table cutouts

#### Major Features
 - Improve cutout handling- Firefly-1633([PR](https://github.com/Caltech-IPAC/firefly/pull/1689)),
Firefly-1581([PR](https://github.com/Caltech-IPAC/firefly/pull/1581))
 - SIAv2 implementation- Firefly-1622([PR](https://github.com/Caltech-IPAC/firefly/pull/1677))
 - Support image sub-highlighting- Firefly-1571([PR](https://github.com/Caltech-IPAC/firefly/pull/1642))

#### Not user facing
 - Improved table ingest speed- Firefly-1592([PR](https://github.com/Caltech-IPAC/firefly/pull/1667)), 
Firefly-1591([PR](https://github.com/Caltech-IPAC/firefly/pull/1662)), Firefly-1592([PR](https://github.com/Caltech-IPAC/firefly/pull/1667)) 
 - Improve file type detection- Firefly-1615([PR](https://github.com/Caltech-IPAC/firefly/pull/1670))
 - Alternate way to set a FIREFLY_OPTIONS entry using `OP_path_to_option` style- Firefly-1641([PR](https://github.com/Caltech-IPAC/firefly/pull/1691)), 

#### Bug fix
- Fixed: cascade combining spectra broken- Firefly-1635([PR](https://github.com/Caltech-IPAC/firefly/pull/1683)), 
- Fixed: improved mouse readout for bottom layout- Firefly-1624([PR](https://github.com/Caltech-IPAC/firefly/pull/1682)), 
- Fixed: table tab color wrong- Firefly-1612([PR](https://github.com/Caltech-IPAC/firefly/pull/1681)), 

### _Patches 2025.1_

- 2025.1.1
  - Bug fix: Sometime heatmap fails to display after filtering- Firefly-1652 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1700))
  - Bug fix: NaNs values should not show up in Heatmap- Firefly-1649 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1701))

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2025.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2025.1+)


# Older Release notes 2019 - 2023
- [2024](old-release-notes/older-release-notes-2024.md)
- [2023](old-release-notes/older-release-notes-2023.md)
- [2022](old-release-notes/older-release-notes-2022.md)
- [2019-2021](old-release-notes/older-release-notes-2019-2021.md)
