## Branch and Tag Naming


## Release Overview

When we do a release, we will create a release candidate branch and tag the commit for each version of the release in that branch.
When a rc branches and tags are named by the current year and the release number of that year.  Release tags include the revision of the release.


## Branch naming

### Development

Development happens on the `dev` branch

### Ticket branches

Tickets are implemented in branches and then merge in after an approved pull request

 - Branch Scheme: `Ticket Name` - `word or two description`
 - Such As: Ticket fix as a table feature might be: `Firefly-123456-table-sort`
 - Such As: Ticket fix an image bug: `Firefly-456-image-zoom`

### Release Candidates

 - Branch Scheme: `rc` - _year_ *.*  _release # of that year_    
 - Such as: `rc-2019.2`

_Example:_ The first release of 2019 branch name will the `rc-2019.1`, the next one will be `rc-2019.2`

## Tag naming

Firefly will use tags to label a release, a prerelease and a development cycles.

### Release

 - Tag Scheme: `release` - _year_ *.*  _release # of that year_ *.* _revision of release_
 - Such as: `release-2019.2.3`


   _Example:_ The release versions for the second 2022 release branch will be tagged. `release-2022.2.0`, `release-2022.2.1`, `release-2022.2.2`, etc. These tags will all be in the `rc-2022.2` branch.

### Prerelease

- Tag Scheme: `pre` *-* _prerelease number_ *-* _year_ *.*  _release # of that year_ 
- Such as: `pre-2-2019.2`


_Example:_ The prerelease versions third 2023 dev cycle will be tagged. `pre-1-2023.3`, `pre-2-2023.3`, `pre-3-2023.3`, etc. These tags will all be in the `dev` branch.


### Development Cycle tags

At the beginning of a development cycle in the `dev` branch, the first commit will be tagged with the cycle number.

- Tag Scheme: `cycle` - _year_ *.*  _release # of that year_
- Such as: `cycle-2022.4`


_Example:_ After the`2022.2.0` release. The first commit in the `dev` branch will label the next cycle. It will be tagged `cycle-2022.3` 
