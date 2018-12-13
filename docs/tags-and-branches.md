## Release/RC branches and tags

When we do a release, we will create a release candidate branch and tag the commit for each version of the release in that branch.

When a rc branches and tags are named by the current year and the release number of that year.  Release tags include the revision of the release.


### Branch naming

 - Branch Scheme: `rc` - _year_ *.*  _release # of that year_    
 - Such as: `rc-2019.2`

_Example:_ The first release of 2019 branch name will the `rc-2019.1`, the next one will be `rc-2019.2`

### Tag naming

All the versions of that release will be tagged using the same similar scheme but adding a revision number at the end.

 - Tag Scheme: `release` - _year_ *.*  _release # of that year_ *.* _revision of release_
 - Such as: `release-2019.2.3`


_Example:_ the release versions for the second 2019 release branch will be tagged. `release-2019.2.0`, `release-2019.2.1`, `release-2019.2.2`, etc. These tags will all be in the `rc-2019.2` branch.
