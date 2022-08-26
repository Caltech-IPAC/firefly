# Firefly older Release Notes - 2019 - 2021



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




### Version 2020.3
- 2020.3.3 (December 2020)
    - docker tag: `release-2020.3.3`, `release-2020.3`
- 2020.3.2 (December 2020)
    - docker tag: `release-2020.3.2`
- 2020.3.1 (October 2020)
    - docker tag: `release-2020.3.1`
    - patch #1 (see [patches](#patches-20203))
- 2020.3.0  (October 2020)
    - docker tag: `release-2020.3.0`
    - original release

### _notes_
- this release contains many bugs fixes and focuses on stability
- less lines of source than last release
- some significant table updates
- data products viewer and web api are now production ready

##### _UI_
- Long table cell entries are handled better  ([Firefly-595](https://github.com/Caltech-IPAC/firefly/pull/1005))
- Small enhancements to TAP browser schema  ([Firefly-596](https://github.com/Caltech-IPAC/firefly/pull/999))
- Web API production ready ([Firefly-568](https://github.com/Caltech-IPAC/firefly/pull/987))
- Data products viewer production ready([Firefly-524](https://github.com/Caltech-IPAC/firefly/pull/979))
- Predefined Cell renders ([Firefly-574](https://github.com/Caltech-IPAC/firefly/pull/1015))


##### _API_
- Firefly Viewer will show a loading message started from the API ([Firefly-505](https://github.com/Caltech-IPAC/firefly/pull/1000))
- Predefined Cell renders ([Firefly-574](https://github.com/Caltech-IPAC/firefly/pull/1015))

##### _Patches 2020.3_
- 2020.3.3
    - Fixed: for DSS and SDSS now using the `https` server
- 2020.3.2
    - Fixed: regression bug with [Firefly-523](https://github.com/Caltech-IPAC/firefly/pull/955)
    - Fixed: small logging cleanup fix
    - Fixed: for coverage, gridOn option not working for HiPS
- 2020.3.1
    - Docker startup simplified when mapping directories ([Simplfy Docker file](https://github.com/Caltech-IPAC/firefly/pull/1038))
    - Many small bug fixes and regression issues:
      [IRSA-3757](https://github.com/Caltech-IPAC/firefly/pull/1040),
      [IRSA-3729](https://github.com/Caltech-IPAC/firefly/pull/1039),
      [IRSA-3771](https://github.com/Caltech-IPAC/firefly/pull/1037),
      [Firefly-676](https://github.com/Caltech-IPAC/firefly/pull/1036),
      [Firefly-674](https://github.com/Caltech-IPAC/firefly/pull/1035),
      [Firefly-670](https://github.com/Caltech-IPAC/firefly/pull/1034),
      [Firefly-525](https://github.com/Caltech-IPAC/firefly/pull/1033)

##### _Pull Request in this release_
- [bug fixes](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Apr+milestone%3A2020.3+label%3Abug)
- [All PRs](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Apr++milestone%3A2020.3+)



### Version 2020.2
- 2020.2 latest -  `release-2020.2`
- 2020.2.0 - `release-2020.2.0` - original release


##### _UI_
- when lock by click is on, there is an icon to copy coordinates  ([Firefly-548](https://github.com/Caltech-IPAC/firefly/pull/972))
- Web API: _beta_- not this will change, only for evaluation  ([Firefly-572](https://github.com/Caltech-IPAC/firefly/pull/965))
- Change tri-view Layout ([Firefly-533](https://github.com/Caltech-IPAC/firefly/pull/963))
- Better management of Web Sockets  ([Firefly-521](https://github.com/Caltech-IPAC/firefly/pull/961))
- Table filter comparison is not longer case insensitive  ([Firefly-502](https://github.com/Caltech-IPAC/firefly/pull/959))

##### _Infrastructure_
- Docker image is smaller  ([Firefly-547](https://github.com/Caltech-IPAC/firefly/pull/969))

#### _Notable Bug Fixes_
- Fixed, API: Jupyter lab startup is broken  ([Firefly-552](https://github.com/Caltech-IPAC/firefly/pull/970))
- Fixed: Full SDSS not loading  ([IRSA-2673](https://github.com/Caltech-IPAC/firefly/pull/968))
- Fixed: Image overlay grouping bugs ([IRSA-2651](https://github.com/Caltech-IPAC/firefly/pull/964))



##### _Pull Request in this release_

- [bug fixes](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Amerged+is%3Apr+milestone%3A2020.2+label%3Abug)
- [All PRs](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Amerged+is%3Apr+milestone%3A2020.2+)



### Version 2020.1

##### _Docker tags, releases and patches_

- 2020.1 latest -  `release-2020.1`
- 2020.1.0 - `release-2020.1.0` - original release
- 2020.1.1 - `release-2020.1.1` - patch 1
- 2020.1.2 - `release-2020.1.2` - patch 2


##### _UI_
- Tables now handle array type data ([Firefly-150](https://github.com/Caltech-IPAC/firefly/pull/922))
- Table Options UI Improvements ([Firefly-471](https://github.com/Caltech-IPAC/firefly/pull/928))
    - New `Advanced Filter` tab enabling SQL-like filtering
    - New `Table Meta` tab showing meta information that was once not accessible
    - Added additional column's meta when available
- WCS match Improvements ([Firefly-484](https://github.com/Caltech-IPAC/firefly/pull/937))
    - When matching to HiPS, images will rotate, if necessary
    - When HiPS matched to image - Firefly will change HiPS coordinate system if images has EQJ200 or Galactic rotated north up
- Better layout of Layer dialog for catalogs overlays ([Firefly-395](https://github.com/Caltech-IPAC/firefly/pull/919))
- Significant improvements in Data Products Viewing ([Firefly-460](https://github.com/Caltech-IPAC/firefly/pull/924))
    - Can choose from any HDU in FITS
    - Table HDUs are show as table and Charts
    - Firefly now reads 1D FITS images and shows as a chart
    - Choice of table with VO Tables
    - PDF and TAR are recognized and downloadable.

##### _API_
- Add fixed column feature to table API ([Firefly-442](https://github.com/Caltech-IPAC/firefly/pull/941))
- MOC overlay support setting the mode MOC_DEFAULT_STYLE to 'outline' or 'fill'
- API examples are at the `firefly/test.html` endpoint


##### _Patches 2020.2_
- 2020.1.2
    -  Fixed: MOC: update the fill color for round-up tiles ([Firefly-526](https://github.com/Caltech-IPAC/firefly/pull/958))
    -  Fixed: Color picker now updates when selection is changed ([Firefly-529](https://github.com/Caltech-IPAC/firefly/pull/957))

##### _Patches 2020.1_
- 2020.1.1
    -  fix fixed column cell transparency issue ([Firefly-523](https://github.com/Caltech-IPAC/firefly/pull/955))

##### _Pull Request in this release_

- [bug fixes](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Aclosed+is%3Apr+label%3Abug+milestone%3A2020.1)
- [All PRs](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Amerged+is%3Apr++milestone%3A2020.1+)

### Version 2019.4

##### _Docker tags, releases and patches_

- 2019.4 latest -  `release-2019.4`
- 2019.4.0 - `release-2019.4.0` - original release


##### _Changes_
- Table data is now formatted on the client side. ([DM-20248](https://github.com/Caltech-IPAC/firefly/pull/884))
- Fixed bug releated LSST footprints, introduced in last release. ([Firefly-435](https://github.com/Caltech-IPAC/firefly/pull/918))
- Significant improvements in the distance tool.  ([Firefly-56](https://github.com/Caltech-IPAC/firefly/pull/904))



### Version 2019.3

This release is focused on bug fixes and stability.

##### _Docker tags, releases and patches_

- 2019.3 latest -  `release-2019.3`
- 2019.3.2 - `release-2019.3.2` - patch #2 (see [patches](#patches-20193))
- 2019.3.1 - `release-2019.3.1` - patch #1 (see [patches](#patches-20193))
- 2019.3.0 - `release-2019.3.0` - original release


##### _UI_
- New Table of loaded Images tool to control and order which images are displayed or hidden  ([Firefly-175](https://github.com/Caltech-IPAC/firefly/pull/873))
- The coverage map and UI has been improved ([Firefly-88](https://github.com/Caltech-IPAC/firefly/pull/835))
- Fixed several WCS match bugs ([Firefly-179](https://github.com/Caltech-IPAC/firefly/pull/846))
- Support for Spectral images and cubes, wavelength readout ([Firefly-73](https://github.com/Caltech-IPAC/firefly/pull/810))
- Background monitor updated to be reusable and connected to workspace ([Firefly-384](https://github.com/Caltech-IPAC/firefly/pull/881))

##### _API_
- Plotly.js charting library was updated from version 1.38.3 to version 1.49.4. Plotly.js 1.43 converted `title` properties (e.g. `layout.title`) from strings into objects that contain `text` property along with new title placement properties `x`, `y`, `xref`, `yref`, `xanchor`, `yanchor` and `pad`. It also moved existing `title*` properties (e.g. `layout.titlefont`) under the `title` object (e.g. `layout.title.font`). See see [plotly.js change log](https://github.com/plotly/plotly.js/blob/master/CHANGELOG.md#1494----2019-08-22) for more information. ([Firefly-266](https://github.com/Caltech-IPAC/firefly/pull/883))

##### _Patches 2019.3_
- 2019.3.2
    - Logging error improved ([Firefly-434](https://github.com/Caltech-IPAC/firefly/pull/909))
    - fix pulldown for SOFIA footprint ([IRSA-3271](https://github.com/Caltech-IPAC/firefly/commit/1ebdd89e53c6efa293bc9cbfad3a689aabcfd5db))
    - Header popup panel was not updating as the FITS image changed ([IRSA-3272](https://github.com/Caltech-IPAC/firefly/pull/912))
    - Put back cube images ([Firefly-443](https://github.com/Caltech-IPAC/firefly/pull/913))
    - Fixing wavelength numbers and cleanup image config file ([IRSA-3284](https://github.com/Caltech-IPAC/firefly/pull/914))
- 2019.3.1
    - Tap query now more robust. Manually handle redirection. Include credential info only when redirected back to source. ([Firefly-414](https://github.com/Caltech-IPAC/firefly/pull/907))
    - Add build code that all for applications that are built on Firefly to reference a specified version of Firefly ([DM-20931](https://github.com/Caltech-IPAC/firefly/pull/862))

### Version 2019.2
##### _Docker tags, releases and patches_

- 2019.2.1 - `release-2019.2.1` - only available 2019.2 release

##### _UI_
- WCS Matching has have been completely revamped.  ([Firefly-75](https://github.com/Caltech-IPAC/firefly/pull/825),[Firefly-173](https://github.com/Caltech-IPAC/firefly/pull/857))
    - You can now either match or match and lock
    - Matching is by WCS, Target, Image corner, or image Center
- TPV Projection fixed ([IRSA-2895](https://github.com/Caltech-IPAC/firefly/pull/819))
- User can now navigate to HiPS or FITS position from the toolbar, using centering dropdown menu ([Firefly-91](https://github.com/Caltech-IPAC/firefly/pull/831))

##### _API_
- nothing of interest

### Version 2019.1

##### _Docker tags, releases and patches_

- 2019.1.0 - `release-2019.1.0` - original release

##### _UI_
- TAP search Implemented (Multiple tickets and PRs)
- DataLink partial implementation ([Firefly-71](https://github.com/Caltech-IPAC/firefly/pull/797))
- ivoa.ObsCore support ([Firefly-71](https://github.com/Caltech-IPAC/firefly/pull/797))

##### _API_
- TAP search can be started from the API


### Before

- Release notes were started as of version 2019.1


