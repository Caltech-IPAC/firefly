# Notes for Next Release

## Version 2026.1
- 2026.1.0 — (tentatively planned for May 7, 2026), _Docker tag_: `nightly`

This release includes Alert viewer, significant image and HiPS updates, alpha ASDF file support, 
background monitor and UWS improvements, major infrastructure updates, a new drawing layer, and many bug fixes.

#### General Features

- Alert Viewer (Rubin only so far) — Firefly-1935 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1916), [PR](https://github.com/Caltech-IPAC/firefly/pull/1917)), Firefly-1964 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1934), [PR](https://github.com/Caltech-IPAC/firefly/pull/1939)), Firefly-1997 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1943)), Firefly-2003 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1947))
- Improved Firefly on iPad and iPhone — Firefly-1947 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1913))
- New box mode for Search Select tool (currently only used with SPHEREx) — Firefly-1942 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1925))

#### Images and HiPS

- Use new WebGPU when available to process images or HiPS — Firefly-1897 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1890))
- Images: Optimized tile generation, transfer, and memory usage — Firefly-1926 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1900)), Firefly-1949 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1918))
- Images: Support mask cubes — Firefly-1921 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1897))
- Images: Support Rubin v2 mask header format — Firefly-1974 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1932))
- Images: ASDF files (alpha support) — Firefly-1931 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1905)), Firefly-1963 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1926))
- HiPS: Add Ecliptic coordinate system — Firefly-1973 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1929))
- Add legend support — Firefly-1924 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1920))

#### Background Monitor & UWS

- Background Monitor: Move `pct_complete` to `progress` under `uws:jobInfo` — Firefly-1770 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1912))
- Background Monitor: Handle non-table results — Firefly-1919 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1907))
- Background Monitor: Fixed dark mode/light mode bug — IRSA-7544 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1902))
- Background Monitor: small UI improvements — IRSA-7687 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1944))
- UWS Client: Allow `errorSummary` to exist even when the phase is not `ERROR` — IRSA-7589 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1914))
- UWS Client: Apply destruction time for job cleanup and update job retention policy — Firefly-1830 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1898))
- UWS Client: Fixed handling of ZIP encoding — Firefly-1976 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1931))

#### Infrastructure Updates

- Added GitHub Actions to build and publish to GHCR — Firefly-1951 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1915))
- Added GitHub Actions to build Helm charts — Firefly-1966 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1927))
- Security: Upgraded Java packages and fixed high and critical CVEs — Firefly-1982 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1942))
- `firefly_client` integration: Added version endpoint for validation — Firefly-1931 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1936))
- Use `MessagePack` for Redis storage — Firefly-1925 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1901))
- Improved detection of Firefly running in AWS — Firefly-1975 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1930))
 
#### Bug Fixes

- Fixed MultiProductViewer: improved handling of calibration files — IRSA-7247 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1909))
- Fixed MultiProductViewer: improved titling — Firefly-1936 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1909))
- Fixed MultiProductViewer: cutout UI rounding error — Firefly-1995 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1941))
- Fixed TAP: VizieR uploads now work — Firefly-1978 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1935))
- Fixed Tables: column resizing after multiple sorts — Firefly-1945 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1924))
- Fixed Tables: not able to filter some access_url columns — Firefly-2002 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1949)) 
- Fixed Chart: heatmap plotting failure with `bigint` data — Firefly-1959 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1921))
- Fixed Images: parsing of certain target types causing crashes; improved parsing — Firefly-1996 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1941))
- Fixed Images: mask should be read as long not float — Firefly-1999 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1944))
- Fixed Images: network issue: certain Rubin images not loading — Firefly-1946 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1911))
- Fixed `firefly_client` integration: functions did not switch to Results tab when another tab was selected — Firefly-1833 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1906))
- Fixed non-datalink service descriptor parsing; ZTF light curves not displaying — Firefly-1978 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1935))
- Fixed precision handling for SizeInputField — IRSA-7653 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1928))
- Fixed URL parsing for some legacy formats — Firefly-1958 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1923))
- Fixed failed circular websocket reconnection attempts — Firefly-1990 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1946))
- Fixed Upload panel should not show a column checked filter — Firefly-1977 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1948)) 

---

##### _Pull Requests in this Release_

- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2026.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2026.1+)
