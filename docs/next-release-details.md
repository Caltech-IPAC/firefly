
# Notes for next Release

## Version 2021.4  (unreleased)
- 2021.4 - development
    - docker tag: `nightly`

### _Notes_
- BigInt: Support for json parsing ([Firefly-732](https://github.com/Caltech-IPAC/firefly/pull/1125))
- BigInt: Support for 64 bit integers in tables ([Firefly-732](https://github.com/Caltech-IPAC/firefly/pull/1121)) 
- Spectrum: Data conversion ([Firefly-843](https://github.com/Caltech-IPAC/firefly/pull/1131))
- Spectrum: FITS reading ([Firefly-851](https://github.com/Caltech-IPAC/firefly/pull/1139))
- Spectrum: FITS and FITS Cube Data Extraction ([Firefly-838](https://github.com/Caltech-IPAC/firefly/pull/1136))
- Support TAP ObsCore components in WebApi ([PR](https://github.com/Caltech-IPAC/firefly/pull/1134))
- Lazy read cube planes ([Firefly-874](https://github.com/Caltech-IPAC/firefly/pull/1142))
- Implement a new background model based on UWS ([Firefly-854](https://github.com/Caltech-IPAC/firefly/pull/1144))
- Display better background job information ([Firefly-872](https://github.com/Caltech-IPAC/firefly/pull/1151))
- Create dropdown to add MOC layers ([Firefly-853](https://github.com/Caltech-IPAC/firefly/pull/1152))
- IBE search processor support Multi position search ([Firefly-876](https://github.com/Caltech-IPAC/firefly/pull/1148))
- Improved tab handling ([Firefly-855](https://github.com/Caltech-IPAC/firefly/pull/1154))
- UI Changes across Firefly ([Firefly-832](https://github.com/Caltech-IPAC/firefly/pull/1123))
  - Improves space for small screen
  - Cleaner layout - more space given to data
  - Detail Image/HiPS readout now optional
  - Image/HiPS toolbar reorganization


- Notable Bug fixes:
  - fixed: Using new Horizons API for moving target ([Firefly-277](https://github.com/Caltech-IPAC/firefly/pull/1132))
  - fixed: Sticky flipping and North Up  ([Firefly-858](https://github.com/Caltech-IPAC/firefly/pull/1150))
  - fixed: Firefly slate table come up on bottom instead of the side ([DM-32004](https://github.com/Caltech-IPAC/firefly/pull/1145))
  - fixed: TAP search names tab correctly ([Firefly-882](https://github.com/Caltech-IPAC/firefly/pull/1148))
  - fixed: Firefly-849: magnified image vanish ([Firefly-849](https://github.com/Caltech-IPAC/firefly/pull/1140))
  - fixed: Firefly-842: SOFIA images not centered on screen ([Firefly-842](https://github.com/Caltech-IPAC/firefly/pull/1140))
  - fixed: Firefly-845: Datalink fail when image url string has a plus (+) sign ([Firefly-845](https://github.com/Caltech-IPAC/firefly/pull/1140))


##### _Pull Requests in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.4+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.4+)

