
# New release procedure

1. In the `rc-yyyy.x` branch, Edit `firefly/config/app.config` with the correct version.
   - Modify:
     - `BuildMajor` = <year>
     - `BuildMinor` = <release of the year>
     - `BuildRev` = <patch #>

1. In the `rc-yyyy.x` branch, Edit the release notes and do the following:
   - Modify the note for the unreleased version to remove the `(unreleased, current development)` from the title
   - After looking at docker tags make sure all the important changes are included in the notes.
   - Start a new section for the next release, mark in the title with `(unreleased, current development)`
   - Make sure you edit the docker tags section of this release
   
1. Merge and Push
   - merge the `rc-yyyy.m` branch back into `dev`
   - push both branches
   
1. Git tag the `rc-yyyy.m` branch with the release  `release-yyyy.m.r`
   - _example:_ the second release from branch `rc-2019.2` with the git tagged with `release-2019.2.1`
   
1. Push tags to GitHub: `git push origin --tags`   

1. Build the docker with the following docker tags: `rc-yyyy.m`, `release-yyyy.m`,`release-yyyy.m.r`, `latest` 
   - _example:_ from the example above the release would be built with: `rc-2019.2`, `release-2019.2`,`release-2019.2.1`, `latest`
   - _notes:_ 
       - the `rc-yyyy.m` docker tag does not represent the release, it is just the most recent build of the branch
       - the `release-yyyy.m` docker tag always represents the latest release of the version
       
1. Go the the Firefly page on docker hub. https://cloud.docker.com/u/ipac/repository/docker/ipac/firefly
   - Edit the markdown to include the recent tags
   
1. Test the release.
   - start docker on your laptop
   - `docker pull ipac/firefly:release-yyyy.m.r`
   - `docker run --rm  -p 8090:8080 -m 4G --name firefly ipac/firefly:release-yyyy.m.r`
   - `http://localhost:8090/firefly/`
   - bring up version information, confirm build date and version
   - test one of the new features
      
   
1. Publish a new release on Github. The text should use the [release-page-template.md](release-page-template.md)

1. In the `dev` branch, Edit `firefly/config/app.config` with the correct version.
   - Modify:
     - `BuildMajor` = 0
     - `BuildMinor` = 0
     - `BuildRev` = 0
   - push dev
