# Release Notes 

Firefly builds and the notes for using running it are on the [Docker Page](https://hub.docker.com/r/ipac/firefly).

See Firefly docker guidelines [here](firefly-docker.md).

### Version _next_ (unreleased, current development)

- docker tag: `nightly`

##### _UI_
- Table data is now formatted on the client side. ([DM-20248](https://github.com/Caltech-IPAC/firefly/pull/884))
- Significant improvements in the distance tool.  ([Firefly-56](https://github.com/Caltech-IPAC/firefly/pull/904))

### Version 2019.3 

This release is focused on bug fixes and stability.

##### _Docker tags, releases and patches_

- 2019.3 latest -  `release-2019.3` 
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
- DataLink partial implementation ([Firelfy-71](https://github.com/Caltech-IPAC/firefly/pull/797))
- ivoa.ObsCore support ([Firelfy-71](https://github.com/Caltech-IPAC/firefly/pull/797))

##### _API_
- TAP search can be started from the API


### Before 

- Release notes were started as of version 2019.1

