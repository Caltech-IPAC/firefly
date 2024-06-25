
# New Release Procedure Steps


### Release Notes
   In the `rc-yyyy.x` branch, edit the release notes and do the following (firefly/docs/release-notes.md):
   - Start a new section for this release
   - Move over any notes from firefly/docs/next-release-details.md
   - Reset firefly/docs/next-release-details.md (you can see what a reset looks like by looking at the last release)
   - After looking at milestone tags, make sure all the important changes are included in the notes.
   - Make sure you edit the docker tags section of this release
   - Update the "Pull Request for this release section" change the text and the URLs for all PR and bug fixes 
   
### Ensure release passes Test
   - `gradle :firefly:test`
   
### Commit and Tag
   - Commit your changes - _example message:_ "Release 2024.1.0: document updates"
   - Tag the `rc-yyyy.m` branch with the release  `release-yyyy.m.r`
   - _Example:_ 
      - the second release from branch `rc-2024.1` with the git tagged with `release-2024.1.1`
      - `git tag release-2024.1.1`
   
### Push
   - Push to GitHub
   - push the rc: _example:_ `git push origin rc-2024.1`
   - push the tags: `git push origin --tags`   

### Build docker images and deploy it to IRSA Kubernetes
   - Best to use Jenkins: https://irsajenkins.ipac.caltech.edu/job/firefly/build
   - Build the docker with the following docker tags: `rc-yyyy.m,yyyy.m,yyyy.m.r,latest` (comma-separated without spaces)
   - _example:_ from the example above the release would be built with: `rc-2024.3`, `2024.3`,`2024.3.1`, `latest`
   - `BUILD_ENV`: Select 'ops'
   - `ACTION`: Select 'both'  
   - `DEPLOY_ENV`: Select 'ops' to have this release deploy to `fireflyops.ipac.caltech.edu`
   - _notes:_ 
       - the `rc-yyyy.m` docker tag does not represent the release, it is just the most recent build of the branch
       - the `yyyy.m` docker tag always represents the latest release of the version
       - the `latest` tag is always the latest formal release. (note- development release use `nightly`)
       
### Test the Build
Try in the firefly dev build you just made
   
### Merge RC and Start a new development cycle
   - merge rc into dev, use `--no-ff` to create a new commit
     - `git checkout dev`
     - `git merge --no-ff <rc-branch>` 
#### Do only for major release
   - Add the new dev cycle tag, but only if you just did the `.0` release
      - _Important:_ Only do this step if this on `.0` releases.
      - For example- if you just did the `2024.2.0` do this step. If you just did the `2024.2.1` skip this step.
   - Tag the dev branch with the new cycle with the form - `cycle-yyyy.x`
   - For example- If you just did the 2024.1.0 release, and we are beginning work on the 2024.2 cycle: 
      - on the dev branch
      - `git tag cycle-2024.2`

### Update Docs
   - add any improvements to this file
   - commit and push dev, _example message_ - "Post 2024.2 release: dev clean up"
   - `git push origin dev`

### Update Docker Hub instructions
   - Go to the Firefly page on docker hub. https://cloud.docker.com/u/ipac/repository/docker/ipac/firefly
   - Edit the markdown to include the recent tags
   
### Publish on GitHub
   - The text should use the [release-page-template.md](release-page-template.md)
   - After using the template, copy the markdown (for this release only) from the release-notes.md
    - paste markdown at the end of the template

