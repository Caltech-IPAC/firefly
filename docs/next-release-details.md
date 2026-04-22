# Notes for next Release

## Version 2026.1
- 2026.1.0 - (tentative planed May 7, 2026),  _docker tag_: `nightly`

This release has significant image and HiPS updates, alpha ASDF file suppor, 
background monitor and UWS improvements, major infrastructure updates, new drawint layer, and alert viewer 

#### General Features

- Alert Viewer: (only Rubin so far) Firefly-1935 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1916),[PR](https://github.com/Caltech-IPAC/firefly/pull/1917)), Firefly-1964 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1934),[PR](https://github.com/Caltech-IPAC/firefly/pull/1939))
- Improved Firefly on iPad, iPhone- Firefly-1947 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1913))
- New box mode for Search Select tool (currently only used with SPHEREx)- Firefly-1942 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1925))

#### Images and HiPS
- Use web gpu when available to process images- Firefly-1897 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1890))
- Images: optimizing tile generation, transfer, and memory usage- Firefly-1926 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1900)), Firefly-1949 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1918))
- Images: Support mask cubes- Firefly-1921 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1897))
- Images: Support Rubin v2 mask header format- Firefly-1974 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1932))
- Images: ASDF files: Alpha Support - Firefly-1931 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1905)), Firefly-1963 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1926))
- HiPS: add Ecliptic coordinate system- Firefly-1973 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1929))
- Add Legend Support- Firefly-1924 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1924))

#### Background Monitor & UWS
- Background Monitor: Move pct_complete to progress under uws:jobInfo- Firefly-1770 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1912))
- Background Monitor: Handle non-table results- Firefly-1919 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1907))
- Background Monitor: Fixed: dark mode/light mode bug - IRSA-7544 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1902))
- UWS Client: Allow errorSummary to exist even when the phase is not Error- IRSA-7589 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1914))
- UWS Client: Apply UWS destruction time for job cleanup and update job retention policy- Firefly-1830 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1898))
- UWS Client: Fixed: handle zip encoding- Firefly-1976 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1931))

#### Infrastructure Updates
- Added Github actions to build to GHCR- Firefly-1951 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1915))
- Added Github actions to build helm chart- Firefly-1966 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1927))
- Security: upgrade java packages and fix high and critical CVEs- Firefly-1982 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1942))
- firefly_client interation: Add version endpoint for version validation- Firefly-1931 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1936))
- Use MessagePack for Redis storage- Firefly-1925 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1901))
- Improved way firefly recognizes it is running in AWS- Firefly-1975 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1930))
 
#### Bug fix
- Fixed: Network issue: certain Rubin file not loading- Firefly-1946 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1911))
- Fixed: MultiProductViewer: improved handling of calibration files- IRSA-7247 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1909))
- Fixed: MultiProductViewer: improved titling- Firefly-1936 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1909))
- Fixed: firefly_client interation: functions don't switch to Results tab if any other tab is selected- Firefly-1833 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1906))
- Fixed: TAP: Vizier uploads now work- Firefly-1978 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1935))
- Fixed: non-datalink service descriptor interpreted wrong, ztf not showing lightcurve- Firefly-1978 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1935))
- Fixed: precision handling for SizeInputField- IRSA-7653 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1928))
- Fixed: tabel column resizes after multiple sorts- Firefly-1945 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1924))
- Fixed: url parsing: some older forms are fixed- Firefly-1958 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1923))
- Fixed: Heatmap failing to plot with bigint data- Firefly-1959 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1921))
- Fixed: Cutout UI round off error- Firefly-1995 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1941))
- Fixed: parsing cerntain type of targets cause crash, improved parsing- Firefly-1941 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1941))
 
                                        
##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2026.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2026.1+)
