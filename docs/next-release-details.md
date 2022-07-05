# Notes for next Release

## Version 2022.2 (unreleased, target: end of July)
- 2022.2 - development
  - docker tag: `nightly`

### _Notes_
#### This release has notable UI, Infrastructure, and  API enhancements

#### UI Enhancements
- Charts support multiple charts from different tables [Firefly-994](https://github.com/Caltech-IPAC/firefly/pull/1229)
- Aitoff HiPS [Firefly-978](https://github.com/Caltech-IPAC/firefly/pull/1207)
- Improved dynamic field generation in service descriptors [Firefly-997](https://github.com/Caltech-IPAC/firefly/pull/1226)
- Support HiPs based target selection [Firefly-980](https://github.com/Caltech-IPAC/firefly/pull/1227)
- Filters are on for all search tables [Firefly-971](https://github.com/Caltech-IPAC/firefly/pull/1213)
- Improved large table loading performance [Firefly-978](https://github.com/Caltech-IPAC/firefly/pull/1206)

#### API Enhancements
- API: Add the capability to a create persistent resource from a table request [Firefly-982](https://github.com/Caltech-IPAC/firefly/pull/1214)

#### Infrastructure Enhancements
- Docker support passing firefly client options on startup [Firefly-885](https://github.com/Caltech-IPAC/firefly/pull/1227)
- Improved passing properties to docker [Firefly-996](https://github.com/Caltech-IPAC/firefly/pull/1225)
- Improved status page [Firefly-992](https://github.com/Caltech-IPAC/firefly/pull/1218)

#### Notable Bug fixes
- Fixed: WAVE_TAB: The algorithm is producing incorrect results [Firefly-989](https://github.com/Caltech-IPAC/firefly/pull/1224)
- Multiple table related bugs

##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2022.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.2+)
