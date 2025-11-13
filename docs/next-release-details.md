# Notes for next Release

## Version 2025.5
- 2025.5.0 - (tentative planed Nov 17, 2025),  _docker tag_: `nightly`

#### This release includes broad range of features Job Monitor cleanup, Spectrum/Chart cleanup, Packaging/Download cleanup, S3 support, more FITS projections supported and improved color support

#### Major Features
- S3 Support- Firefly-1840 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1840)) 
- Concurrent downloads- Firefly-1869 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1858)) 
- Images: SDSS: Upgrade to dr17 and images bz2 compression support- Firefly-1848 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1842))
- Images: Pan-STARRS pixel readout support- Firefly-168 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1861))
- Images: HPX projection-  Firefly-1889 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1874))
- Images: Stereographic projection- Firefly-1834 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1854))
- Images: Improve extract tool behavior for MEFs- Firefly-1866 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1868))
- Images: User can not set the nan-pixel color and add reverse color maps- Firefly-1867 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1883))
- MOC: Support additional MOCs- IRSA-7208 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1862))
- Name Resolution: add types to resolution display- Firefly-1891 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1877))
- SPHEREx: Support spectrum-cutout view-  Firefly-1887 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1870)), Firefly-1888 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1873))
- Charts: Add LaTex support for unit labels- Firefly-1788 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1836))

#### Cleaned Up and improved
- Job Monitor: Firefly-1839 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1847)), Firefly-1809 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1841)), Firefly-1802 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1849)), Firefly-1824 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1865)),
- Download & Packaging: Firefly-7274 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1855)), IRSA-7344 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1864)) 
- Charts: IRSA-7248 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1860)), Firefly-1884 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1869)), Firefly-1851 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1872))
 
#### Infrastructure Updates
- Upgrade: Tomcat 11- Firefly-1836 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1851)) 
- Upgrade: React 19 && JS libraries- Firefly-1835 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1866)) 
- Upgrade: Java libraries security patches- Firefly-1805 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1856)) 
- Docker: Multi-platform build (`amd64` and `aarch64`)- Firefly-1836 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1875)) 
- Improved Redis integration: changed java client to Lettuce- Firefly-1883 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1860)) 

#### Bug fix
- Fixed: TAP selected cols number missing upon filtering Firefly-1638 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1850))
- Fixed: Incorrect pixel readout in 3-color with reprojection- Firefly-1865 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1857))
- Fixed: Line extraction doesn't work in image scroll mode- Firefly-1863 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1862))
- Fixed: All table rows highlight with secondary highlight- Firefly-1881 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1862))
- Fixed: Cube visualizer shows 0.0 um for all planes- Firefly-1870 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1862))
- Fixed: grid options not showing in MultiProductViewer- IRSA-7404 ([PR](https://github.com/Caltech-IPAC/firefly/pull/1871))

                                        
##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2025.5+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2025.5+)
