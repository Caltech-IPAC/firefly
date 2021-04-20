# Firefly older Release Notes - 2019 - 2020

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


