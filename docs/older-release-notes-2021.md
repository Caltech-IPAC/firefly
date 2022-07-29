# Firefly older Release Notes - 2021

## Version 2021.4  (December 2021)
- 2021.4.1 (Feb 2022)
    - docker tag: `latest`, `release-2021.4`, `release-2021.4.1`
- 2021.4 (December 2021)
    - docker tag: `release-2021.4.0`

### _Notes_
#### New features - This release contains some significant new features and UI improvements
- _BigInt_: Support for json parsing ([Firefly-732](https://github.com/Caltech-IPAC/firefly/pull/1125))
- _BigInt_: Support for 64 bit integers in tables ([Firefly-732](https://github.com/Caltech-IPAC/firefly/pull/1121))
- _Spectrum_: Data conversion ([Firefly-843](https://github.com/Caltech-IPAC/firefly/pull/1131))
- _Spectrum_: FITS reading ([Firefly-851](https://github.com/Caltech-IPAC/firefly/pull/1139))
- _Spectrum_: FITS and FITS Cube Data Extraction ([Firefly-838](https://github.com/Caltech-IPAC/firefly/pull/1136))
- _Background_: Implement a new background model based on UWS ([Firefly-854](https://github.com/Caltech-IPAC/firefly/pull/1144))
- _Background_: Display better background job information ([Firefly-872](https://github.com/Caltech-IPAC/firefly/pull/1151))
- _WebApi_: Support TAP ObsCore components ([PR](https://github.com/Caltech-IPAC/firefly/pull/1134))
- Create dropdown to add MOC layers ([Firefly-853](https://github.com/Caltech-IPAC/firefly/pull/1152))
- Lazy read cube planes ([Firefly-874](https://github.com/Caltech-IPAC/firefly/pull/1142))
- IBE search processor support Multi position search ([Firefly-876](https://github.com/Caltech-IPAC/firefly/pull/1148))
- UI Changes across Firefly ([Firefly-832](https://github.com/Caltech-IPAC/firefly/pull/1123))
    - Improves space for small screen
    - Cleaner layout - more space given to data
    - Detail Image/HiPS readout now optional
    - Image/HiPS toolbar reorganization
    - Improved tab handling ([Firefly-855](https://github.com/Caltech-IPAC/firefly/pull/1154))


#### Notable Bug fixes
- fixed: Using new Horizons API for moving target ([Firefly-277](https://github.com/Caltech-IPAC/firefly/pull/1132))
- fixed: Sticky flipping and North Up  ([Firefly-858](https://github.com/Caltech-IPAC/firefly/pull/1150))
- fixed: Firefly slate table comes up on bottom instead of the side ([DM-32004](https://github.com/Caltech-IPAC/firefly/pull/1145))
- fixed: TAP search names tab correctly ([Firefly-882](https://github.com/Caltech-IPAC/firefly/pull/1148))
- fixed: Firefly-849: magnified image vanish ([Firefly-849](https://github.com/Caltech-IPAC/firefly/pull/1140))
- fixed: Firefly-842: SOFIA images not centered on screen ([Firefly-842](https://github.com/Caltech-IPAC/firefly/pull/1140))
    - fixed: Firefly-845: Datalink fail when image url string has a plus (+) sign ([Firefly-845](https://github.com/Caltech-IPAC/firefly/pull/1140))


##### _Pull Requests in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.4+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.4+)

##### _Patches 2021.4_
- 2021.4.1
    - IBE not passing credentials ([Firefly-938](https://github.com/Caltech-IPAC/firefly/pull/1189))
    - Better version tracking ([Firefly-915](https://github.com/Caltech-IPAC/firefly/pull/1165))


## Version 2021.3 (August 2021)
- 2021.3.3 (Oct 2021)
    - docker tag: `latest`, `release-2021.3`, `release-2021.3.3`
- 2021.3.2 (Aug 2021)
    - docker tag: `release-2021.3.2`
- 2021.3.1 (Aug 2021)
    - docker tag: `release-2021.3.1`
- 2021.3.0  (Aug 2021)
    - docker tag: `release-2021.3.0`
    - original release

### _Notes_
- Options to remove group images. ([Firefly-708](https://github.com/Caltech-IPAC/firefly/pull/1070))
- File saving names improved. ([Firefly-823](https://github.com/Caltech-IPAC/firefly/pull/1109))
- Improved accessibility for small screen with image search panel ([Firefly-800](https://github.com/Caltech-IPAC/firefly/pull/1107))
- This release contains many bug fixes:
    - fixed: HiPS mouse wheel zooming more accurate
    - fixed: thumbnail scroll not working
    - fixed: upload panel clear button not completely clearing data ([Firefly-825](https://github.com/Caltech-IPAC/firefly/pull/1116))
    - fixed: upload panel now shows progress feedback and indicator, now works when loading images ([Firefly-828](https://github.com/Caltech-IPAC/firefly/pull/1113))
    - fixed: Table Input fields sometimes show duplicate entries ([Firefly-829](https://github.com/Caltech-IPAC/firefly/pull/1115))
    - fixed: slate entries point not showing tap/table searches button correctly

##### _Patches 2021.3_
- 2021.3.1
    - Small Bug fixes
    - Improve TAP Ivoa.ObsCore detection ([DM-30780](https://github.com/Caltech-IPAC/firefly/pull/1119))
    - Improve TAP spacial validation ([PR](https://github.com/Caltech-IPAC/firefly/pull/1118))
- 2021.3.2
    - fixed: zoom down bouncing issue ([PR](https://github.com/Caltech-IPAC/firefly/pull/1122))
    - fixed: removed MultiProductViewer UI grid then single arrows ([PR](https://github.com/Caltech-IPAC/firefly/pull/1122))
    - fixed: close button issue on safari ([PR](https://github.com/Caltech-IPAC/firefly/pull/1122))
    - fixes: MultiProductViewer grid mode not using server side plot parameters([IRSA-4183](https://github.com/Caltech-IPAC/firefly/pull/1122))
    - Continued TAP Ivoa.ObsCore detection ([DM-30780](https://github.com/Caltech-IPAC/firefly/pull/1120))
- 2021.3.3
    - fixed: Use latest JPL/Horizons JSON name resolution api ([Firefly-277](https://github.com/Caltech-IPAC/firefly/pull/1132))
    - fixed: Firefly viewer unable to support multiple instances of different apps ([Firefly-859](https://github.com/Caltech-IPAC/firefly/pull/1135))

##### _Pull Requests in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.3+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.3+)



## Version 2021.2
- 2021.2.4 (July 2021)
    - docker tag: `latest`, `release-2021.2`, `release-2021.2.4`
- 2021.2.3 (June 2021)
    - docker tag: `release-2021.2.3`
- 2021.2.2 (June 2021)
    - docker tag: `release-2021.2.2`
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
- 2021.2.3
    - Fixed: initialization of userInfo object
    - Fixed: further refinement to error handling when retrieving TAP error documents
    - Fixed: Simbad name resolution issue ([Firefly-797](https://github.com/Caltech-IPAC/firefly/pull/1103))
    - Fixed: Mouse zoom not working correctly ([Firefly-803](https://github.com/Caltech-IPAC/firefly/pull/1103))
- 2021.2.4
    - Fixed: Image mask not zooming correctly ([Firefly-810](https://github.com/Caltech-IPAC/firefly/pull/1106))
    - Fixed: HiPS select table _i_ link not working ([Firefly-819](https://github.com/Caltech-IPAC/firefly/pull/1106))
    - Fixed: bias slider not working in 3 color mode ([PR](https://github.com/Caltech-IPAC/firefly/pull/1106))
    - Fixed: boolean table filters not working ([Firefly-805](https://github.com/Caltech-IPAC/firefly/pull/1104))


##### _Pull Requests in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.2+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.2+)



## Version 2021.1
- 2021.1.0  (February 2021)
    - docker tag: `release-2021.1`, `release-2021.1.0`
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

##### _Pull Requests in this release_
- [Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2021.1+label%3abug)
- [All PRs](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2021.1+)

