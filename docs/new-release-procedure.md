
# New release procedure


1. In the `rc-yyyy.x` branch, Edit the release notes and do the following:
   - Modify the note for the unreleased version to remove the `(unreleased, current development)` from the title
   - After looking at tags make sure all the important changes are included in the notes.
   - Start a new section for the next release, mark in the title with `(unreleased, current development)`
   - Make sure you edit the docker tags section of this release
   
1. Merge and Push
   - merge the `rc-yyyy.m` branch back into `dev`
   - push both branches
   
1. Tag the `rc-yyyy.m` branch with the release  `release-yyyy.m.r`
   - _example:_ the second release from branch `rc-2019.2` will the tagged with `release-2019.2.1`
   
1. Push tags to GitHub: `git push origin --tags`   

1. Build the docker with the following tags: `rc-yyyy.m`, `release-yyyy.m`,`release-yyyy.m.r`, `latest` 
   - _example:_ from the example above the release would be built with: `rc-2019.2`, `release-2019.2`,`release-2019.2.1`, `latest`
   - _notes:_ 
       - the `rc-yyyy.m` tag does not represent the release, it is just the most recent build of the branch
       - the `release-yyyy.m` tag always represents the latest release of the version
       
1. Go the the Firefly page on docker hub. https://cloud.docker.com/u/ipac/repository/docker/ipac/firefly
   - Edit the markdown to include the recent tags
   
1. Publish a new release on Github. The text should use the [release-page-template.md](release-page-template.md)


