# Notes for next Release

## Version 2024.2 (unreleased, rough target: June 15)
- 2024.2 - development
  - docker tag: `nightly`, `2024.2`, `2024.2.0`

### _Notes_
#### This release has a lot of bug fixes and clean up after the JoyUI conversion. It also includes some long requested updates.

#### New Features

- Images: Improved image sorting and filtering- Firefly-1448, [PR](https://github.com/Caltech-IPAC/firefly/pull/1543)
- Images: Support non-celestial coordinate readout- Firefly-1468, [PR](https://github.com/Caltech-IPAC/firefly/pull/1553)
- HiPS: Separated HiPS search panel out of images panel- Firefly-1465, [PR](https://github.com/Caltech-IPAC/firefly/pull/1547)
- TAP: Show overflow indicator if present - Firefly-1396, [PR](https://github.com/Caltech-IPAC/firefly/pull/1542)
- TAP: Improved obscore support- Firefly-1187, [PR](https://github.com/Caltech-IPAC/firefly/pull/1551)
- Tables: Improved support for null values- Firefly-1471, [PR](https://github.com/Caltech-IPAC/firefly/pull/1549)
- Python API: Improved Tri-view support- Firefly-1483, [PR](https://github.com/Caltech-IPAC/firefly/pull/1583)
- UI: Polygon input no longer requires commas- IRSA-5492, [PR](https://github.com/Caltech-IPAC/firefly/pull/1559)

#### Bug fix and clean up
- Bug fix: 
   - Images: readout but with compressed files- Firefly-1476, [PR](https://github.com/Caltech-IPAC/firefly/pull/1558)
   - The main menu better adjust with fonts sizes- Firefly-1472, [PR](https://github.com/Caltech-IPAC/firefly/pull/1550)
   - binned plot and chart-saving bugs- Firefly-1480 [PR](https://github.com/Caltech-IPAC/firefly/pull/1562)
   - TAP: Hidden columns included in column count- Firefly-1486 [PR](https://github.com/Caltech-IPAC/firefly/pull/1564)
- Clean up: 
   - icons- IRSA-5925, [PR](https://github.com/Caltech-IPAC/firefly/pull/1524), Firefly-1488, [PR](https://github.com/Caltech-IPAC/firefly/pull/1565)
   - Table Info dialog- Firefly-1464, [PR](https://github.com/Caltech-IPAC/firefly/pull/1546)
   - Improved HiPS toolbar- Firefly-1473, [PR](https://github.com/Caltech-IPAC/firefly/pull/1551)
   - TAP: table selection- Firefly-1478, [PR](https://github.com/Caltech-IPAC/firefly/pull/1560)
   - Table related cleanup- Firefly-1479, Firefly-1481, Firefly-1484, [PR](https://github.com/Caltech-IPAC/firefly/pull/1563)
   - Embedded search panel clean up and improvement- IRSA-5916, [PR](https://github.com/Caltech-IPAC/firefly/pull/1539), Firefly-1451, [PR](https://github.com/Caltech-IPAC/firefly/pull/1548)

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2024.2+)
