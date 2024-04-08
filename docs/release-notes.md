# Firefly Release Notes

- You can find Firefly builds and the notes for running Firefly on the [Docker Page](https://hub.docker.com/r/ipac/firefly).
- See Firefly Docker guidelines [here](firefly-docker.md).
- To Access current (unstable) development—(Version _next_, unreleased) 
  - use docker tag: `nightly`
  - [Notes on the next version](next-release-details.md)

## Version 2024.1
- 2024.4.1 - (April 8, 2024),  _docker tag_: `latest`, `2024.1`, `2024.1.0`

### _Notes_
#### This release is a complete overhaul of the Firefly UI

#### New Features
- UI: Complete overhaul [All UI PRs](https://github.com/Caltech-IPAC/firefly/pulls?page=2&q=is%3Apr+milestone%3AUI-conversion)
  - New UI library [Joy UI](https://mui.com/joy-ui/getting-started/)
  - UI clean up
  - Support dark mode
  - Firefly is more skinable for other applications (fonts, colors)
  - New icons
  - Nicer colors, consistent color usage
  - Nicer fonts, consistent font usage
  - More consistency across UI (button layout, etc.)
  - Better support for multiple result layouts. Detailed Tri-view and Bi-view layout control
  - Added Landing page
  - Primary navigation UX (tabs across top) revamped
      - Clearer tab UX
      - Results tab
      - Sidebar for additional navigation and settings
- TAP: Object ID can be searched using "Select...IN" [Firefly-1450](https://github.com/Caltech-IPAC/firefly/pull/1526)
- TAP: TAP panels can now be locked into one TAP service
- Charts: Cascade-style plots for spectra [Firefly-1370](https://github.com/Caltech-IPAC/firefly/pull/1499)
- Upload: improved support [Firefly-1341](https://github.com/Caltech-IPAC/firefly/pull/1472)
- Tables: improved table error message handling [Firefly-1345](https://github.com/Caltech-IPAC/firefly/pull/1535)



### _Pull Requests in this release_
- [All Bug Fixes](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr+milestone%3a2024.1+label%3abug)
- All PRs
   - [UI conversion](https://github.com/caltech-ipac/firefly/pulls?q=is%3apr++milestone%3a2022.3+)
   - [Other 2024.1 PRs](https://github.com/Caltech-IPAC/firefly/pulls?q=is%3Apr+milestone%3AUI-conversion)



# Older Release notes 2019 - 2023
- [2023](older-release-notes-2023.md)
- [2022](older-release-notes-2022.md)
- [2019-2021](older-release-notes-2019-2021.md)