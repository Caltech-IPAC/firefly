
# New release procedure


1. **Update Release Notes.**
   In the `rc-yyyy.x` branch, edit the release notes and do the following (firefly/docs/release-notes.md):
   - Start a new section for this release
   - Move over any notes from firefly/docs/next-release-details.md
   - Reset firefly/docs/next-release-details.md (you can see what a reset looks like by looking at the last release)
   - After looking at milestone tags make sure all the important changes are included in the notes.
   - Make sure you edit the docker tags section of this release
   - Update the "Pull Request for this release section", change the text and the URLs for all PR and bug fixes 
   
2. **Ensure release passes Test**
   - `gradle :firefly:test`
   
3. **Commit, Tag**
   - commit your changes - _example message:_ "Release 2021.1.0: document updates"
   - tag the `rc-yyyy.m` branch with the release  `release-yyyy.m.r`
   - _example:_ 
      - the second release from branch `rc-2021.2` with the git tagged with `release-2021.2.1`
      - `git tag release-2021.2.1`
   
4. **Push to GitHub**: 
   - push the rc: _example:_ `git push origin rc-2021.1`
   - push the tags: `git push origin --tags`   

5. **Build docker images and deploy it to IRSA Kubernetes**
   - Best to use Jenkins: https://irsawebdev5.ipac.caltech.edu:8443/view/IRSA%20k8s/job/ikc_firefly/build
   - Build the docker with the following docker tags: `rc-yyyy.m`, `yyyy.m`,`yyyy.m.r`, `latest` 
   - _example:_ from the example above the release would be built with: `rc-2021.2`, `2021.2`,`2021.2.1`, `latest`
   - `BUILD_ENV`: Select 'ops'
   - `ACTION`: Select 'both'  
   - `DEPLOY_ENV`: Select 'ops' to have this release deploy to fireflyops.ipac.caltech.edu
   - _notes:_ 
       - the `rc-yyyy.m` docker tag does not represent the release, it is just the most recent build of the branch
       - the `yyyy.m` docker tag always represents the latest release of the version
       - the `latest` tag is always the latest formal release. (note- development release use `nightly`)
       
6. **Test the release.**
   - start docker on your laptop
   - `docker pull ipac/firefly:yyyy.m.r`
   - `docker run --rm  -p 8090:8080 -m 4G --name firefly ipac/firefly:yyyy.m.r`
   - Look at the main page: 
     - http://localhost:8090/firefly/
     - bring up version information, confirm build date and version
     - test one of the new features
   - Look at test pages
     - http://localhost:8090/firefly/test
   
7. **Merge RC, Start a new development cycle**
   - merge rc into dev, use `--no-ff` to create a new commit
     - `git checkout dev`
     - `git merge --no-ff <rc-branch>` 
   - Add the new dev cycle tag, but only if you just did the `.0` release
      - _Important:_ Only do this step if this on `.0` releases.
         - For example- if you just did the `2021.2.0` do this step. If you just did the `2021.2.1` skip this step.
     - Tag the dev branch with the new cycle with the form - `cycle-yyyy.x`
     - For example- If you just did the 2022.1.0 release, and we are beginning work on the 2022.2 cycle: 
       - on the dev branch
       - `git tag cycle-2022.2`
8. Update Docs and push
   - add any improvements to this file
   - commit and push dev, _example message_ - "Post 2021.1 release: dev clean up"
   - `git push origin dev`
   - push the tags: `git push origin --tags`
   
9. **Edit docker hub instructions**
   - Go the the Firefly page on docker hub. https://cloud.docker.com/u/ipac/repository/docker/ipac/firefly
   - Edit the markdown to include the recent tags
   
10. **Publish a new release on Github.**
    - The text should use the [release-page-template.md](release-page-template.md)
    - After using the template, copy the markdown (for this release only) from the release-notes.md
    - paste markdown at the end of the template

