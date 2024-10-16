# Notes for next Release

## Version 2024.3 (unreleased, rough target: Oct 8)
- 2024.3 - development
  - docker tag: `nightly`


### _Notes_
This Firefly release has a lot of new features, probably among the most new features packed into one release over the past several years. 
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
                                        
##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2024.3+)
