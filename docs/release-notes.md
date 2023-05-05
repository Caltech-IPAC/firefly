# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development - (Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on next version](next-release-details.md)

## Version 2023.1
- 2023.1.1 - (May 1, 2023)
  - docker tag: `latest`, `2023.1`, `2023.1.1`
- 2023.1.0 - (May 5, 2023)
  - docker tag: `2023.1.0`

### _Notes_
#### This release includes some significant new features, existing feature improvements, and code clean up

#### New Features
- TAP Upload: [Firefly-1142](https://github.com/Caltech-IPAC/firefly/pull/1317), [Firefly-1148](https://github.com/Caltech-IPAC/firefly/pull/1331), [Firefly-1189](https://github.com/Caltech-IPAC/firefly/pull/1337)
- Improved: TAP UI: [Firefly-1215](https://github.com/Caltech-IPAC/firefly/pull/1354)
- Improved: Click to search UI:  [Firefly-1152](https://github.com/Caltech-IPAC/firefly/pull/1326)
- Improved: Derived columns UI:  [Firefly-1153](https://github.com/Caltech-IPAC/firefly/pull/1330)
- Improved: Charting and Spectral UI layout: [Firefly-1183](https://github.com/Caltech-IPAC/firefly/pull/1348)
- Improved: 3-color selection: [Firefly-1134](https://github.com/Caltech-IPAC/firefly/pull/1310)
- UWS support: [Firefly-1128](https://github.com/Caltech-IPAC/firefly/pull/1308), [Firefly-1129](https://github.com/Caltech-IPAC/firefly/pull/1319)
- Faster image loading: [Firefly-1190](https://github.com/Caltech-IPAC/firefly/pull/1338)
- Table of loaded image available in pinned image section: [Firefly-1081](https://github.com/Caltech-IPAC/firefly/pull/1344)
- Obscore table packaging: [Firefly-1193](https://github.com/Caltech-IPAC/firefly/pull/1351)
- Embedded spacial search UI in Hips: [Firefly-1177](https://github.com/Caltech-IPAC/firefly/pull/1328) 


##### _Patches 2023.1_
- 2023.1.1
  - fixed: polygon searches force spaces after comma [Firefly-1234](https://github.com/Caltech-IPAC/firefly/pull/1376)


##### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2023.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2023.1+)



# Older Releases 2019 - 2022
See [Older release notes 2022](older-release-notes-2022.md)
See [Older release notes 2019-2021](older-release-notes-2019-2021.md)
