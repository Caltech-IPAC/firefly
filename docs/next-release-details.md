# Notes for next Release

## Version 2025.3
- 2025.3.0 - (June 6, 2025), planned _docker tag_: `2025.3.0`, `2025.3`, `nightly`

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


### _Patches 2025.3_
_initial release_
                                        
##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2025.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2025.3+)
