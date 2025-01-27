# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)

## Version 2025.1
- 2024.5.0 - (Jan 27, 2025),  _docker tag_: `2025.1`, `2025.1.0`, `latest`

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
 

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2025.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2025.1+)


# Older Release notes 2019 - 2023
- [2024](old-release-notes/older-release-notes-2024.md)
- [2023](old-release-notes/older-release-notes-2023.md)
- [2022](old-release-notes/older-release-notes-2022.md)
- [2019-2021](old-release-notes/older-release-notes-2019-2021.md)
