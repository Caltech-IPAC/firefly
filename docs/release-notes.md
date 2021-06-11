# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly are on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development - (Version _next_, unreleased) use docker tag: `nightly`

## Version 2021.2
- 2021.2.2 (June 2021)
  - docker tag: `latest`, `release-2021.2`, `release-2021.2.2`
- 2021.2.1 (May 2021)
  - docker tag: `release-2021.2.1`
- 2021.2.0  (May 2021)
  - docker tag: `release-2021.2.0`
  - original release
    
### _Notes_
- This release contains many bugs fixes and new features
- TAP search panel have substantial enhancements for ObsCore searches
- First release for the Spectral Viewing mode for charts
- Target panel recognizes new type of ra/dec input
- Charting UI has had substantial clean up and bug fixes
- Docker container now supports setting the cleanup interval (`docker run --rm ipac/firefly:latest --help`)


##### _UI_
- Spectral Viewer ([Firefly-691](https://github.com/Caltech-IPAC/firefly/pull/1079))
- TAP ObsCore search ([PR](https://github.com/Caltech-IPAC/firefly/pull/1073))
- KOA added to List of TAP services ([PR](https://github.com/Caltech-IPAC/firefly/pull/1069))
- Charting UI improvements ([PR](https://github.com/Caltech-IPAC/firefly/pull/1082))

##### _Other_ 
- Docker cleanup interval ([Firefly-737](https://github.com/Caltech-IPAC/firefly/pull/1076))

##### _API_ 
- Coverage can handle tables with HMS columns ([Firefly-678](https://github.com/Caltech-IPAC/firefly/pull/1069))

##### _Patches 2021.2_
- 2021.2.1
  - Fixed: save image dialog failing to appear
- 2021.2.2
  - Fixed: mouse wheel / trackpad scrolling performance issue ([Firefly-793](https://github.com/Caltech-IPAC/firefly/pull/1098))
  - Fixed: Handle redirects when retrieving TAP errors ([DM-30073](https://github.com/Caltech-IPAC/firefly/pull/1092))
  - Fixed: problem is misusing the referer header


##### _Pull Request in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.2+)



## Version 2021.1
- 2021.1.0  (February 2021)
  - docker tag: `latest`, `release-2021.1`, `release-2021.1.0`
  - original release


### _Notes_
- This release contains many bugs fixes and new features
- FITS Image and HiPS visualizer have some significant performance improvements 
   - Zoom 
   - Color change
   - Bias and Contrast control
   - Mouse wheel zoom control
- First production release of TAP Search panel
- First production release of the upload anything panel, improved UI, now supports regions
- Less lines of java source than last release

##### _UI_
- FITS performance improvements and significant color improvements ([Firefly-646](https://github.com/Caltech-IPAC/firefly/pull/1016))
- Syntax Highlighting of ADQL ([PR](https://github.com/Caltech-IPAC/firefly/pull/1041))
- Service Descriptor Support ([Firefly-677](https://github.com/Caltech-IPAC/firefly/pull/1042))
- Blank HiPS projection for drawing ([Firefly-688](https://github.com/Caltech-IPAC/firefly/pull/1043))
- Ecliptic readout ([Firefly-567](https://github.com/Caltech-IPAC/firefly/pull/1058))
- Upload panel improvements (https://github.com/Caltech-IPAC/firefly/pull/1059)

##### _Other_ 
- Supported Browsers ([Firefly-690](https://github.com/Caltech-IPAC/firefly/pull/1046))
  - safari >= 12
  - chrome >= 81
  - firefox >= 79
  - edge >= 83

##### _Pull Request in this release_
- [bug fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.1+label%3abug)
- [all prs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.1+)


# Older Release 2019 - 2020

See [Older reelase notes 2019-2020](older-release-notes-2019-2020.md)
