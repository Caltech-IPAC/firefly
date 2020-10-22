
# New release procedure

1. **Set the Version in config**
   - In the `rc-yyyy.x` branch, Edit `firefly/config/app.config` with the correct version.
   - Modify:
     - `BuildMajor =` _year_
     - `BuildMinor =` _release of the year_
     - `BuildRev =` _patch #_

1. **Update Release Notes.**
   In the `rc-yyyy.x` branch, edit the release notes and do the following (firefly/docs/release-notes.md):
   - Modify the note for the unreleased version to remove the `(unreleased, current development)` from the title
   - After looking at milestone tags make sure all the important changes are included in the notes.
   - Start a new section for the next release, mark in the title with `(unreleased, current development)`
   - Make sure you edit the docker tags section of this release
   - Update the "Pull Request for this release section", change the text and the URLs for all PR and bug fixes 
   
1. **Ensure release passes Test**
   - `gradle :firefly:test`
   
1. **Commit, Tag**
   - commit you changes - _example message:_ "Release 2020.1.0: document updates"
   - tag the `rc-yyyy.m` branch with the release  `release-yyyy.m.r`
   - _example:_ 
      - the second release from branch `rc-2020.2` with the git tagged with `release-2020.2.1`
      - `git tag release-2020.2.1`
   
1. **Push to GitHub**: 
   - push the rc: _example:_ `git push origin rc-2020.1`
   - push the tags: `git push origin --tags`   

1. **Build docker images and deploy it to IRSA Kubernetes**
   - Best to use Jenkins: http://irsawebdev5.ipac.caltech.edu:8100/view/IRSA%20k8s/job/ikc_firefly/build
   - Build the docker with the following docker tags: `rc-yyyy.m`, `release-yyyy.m`,`release-yyyy.m.r`, `latest` 
   - _example:_ from the example above the release would be built with: `rc-2020.2`, `release-2020.2`,`release-2020.2.1`, `latest`
   - `ACTION`: Select 'both'  
   - `DEPLOY_ENV`: Select 'ops' to have this release deploy to fireflyops.ipac.caltech.edu
   - _notes:_ 
       - the `rc-yyyy.m` docker tag does not represent the release, it is just the most recent build of the branch
       - the `release-yyyy.m` docker tag always represents the latest release of the version
       - the `latest` tag is always the latest formal release. (note- development release use `nightly`)
       
1. **Test the release.**
   - start docker on your laptop
   - `docker pull ipac/firefly:release-yyyy.m.r`
   - `docker run --rm  -p 8090:8080 -m 4G --name firefly ipac/firefly:release-yyyy.m.r`
   - `http://localhost:8090/firefly/`
   - bring up version information, confirm build date and version
   - test one of the new features
   
1. **Merge RR, Reset the Version in config to development, Push dev**
   - merge rc into dev
   - In the `dev` branch, Edit `firefly/config/app.config` so that you are resetting 
   - Modify:
     - `BuildMajor =` _year_
     - `BuildMinor = -1`
     - `BuildRev = 0`
   - add any improvements to this file
   - commit and push dev, _example message_ - "Post 2020.1 release: dev clean up"
   
1. **Edit docker hub instructions**
   - Go the the Firefly page on docker hub. https://cloud.docker.com/u/ipac/repository/docker/ipac/firefly
   - Edit the markdown to include the recent tags
   
1. **Publish a new release on Github.**
   - The text should use the [release-page-template.md](release-page-template.md)
   - After using the template, copy the markdown (for this release only) from the release-notes.md
   - paste markdown at the end of the template

