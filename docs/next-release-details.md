# Notes for Next Release

## Version 2026.2
- 2026.2.0 — (tentatively planned for July 24, 2026), _Docker tag_: `nightly`

This stability release includes numerous bug fixes and introduces the first end-user installable version of Firefly.

#### General Features
- Updated Standalone Firefly application landing page — Firefly-1981 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1959)) 
- Change default menus and tabs for standalone Firefly application — Firefly-2028 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1967)) 
- Added support for end-user installation of Firefly — Firefly-1980 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1962)) 
- URL API: added SIA support and cleaned up of `hipsPanel` command — Firefly-2026 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1973)) 
- Fits Images: Give an error or sometimes silently ignore but not fail completely when encountering an unsupported 4d+ HDU — Firefly-2049 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1976)) 
- MOC: Added support for MOC 2.0, with `ORDERING=NUNIQ` — Firefly-2049 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1976)) 
- Tables: Optimize FITS table reading performance — Firefly-2049 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1976))
- Tables: improve slow loading of wide tables — Firefly-1746 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1980)) 


#### Infrastructure Updates
- Improved application security and configuration hardening — Firefly-1864 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1955)) 
- Upgraded react split pane — Firefly-1854 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1957)) 


#### Bug Fixes
- Improve duckdb memory usage — Firefly-2017 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1960)) 
- Fixed: WCS Match so east-right images align correctly — Firefly-2055 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1978)) 
- Fixed: GPU array overflow and optimized color lookup performance — Firefly-2056 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1979)) 


##### _Pull Requests in this Release_

- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2026.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2026.2+)
