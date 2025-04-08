# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)


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
