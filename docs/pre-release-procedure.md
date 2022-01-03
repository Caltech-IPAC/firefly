# Pre-release procedure

1. Change to dev branch and pull all new changes
2. Ensure release passes Test
    - `gradle :firefly:test`
3. Tag
   - Use the pre-release tag style of pre-#-yyyy.mm
   - _Example:_ 
        - for the forth pre-release of the 2023.2 cycle: `pre-4-2023.2`
        - `git tag pre-4-2023.2`
4. Push the tag: `git push origin --tags`
5. **Build docker images and deploy it to IRSA Kubernetes**
    - Best to use Jenkins: https://irsawebdev5.ipac.caltech.edu:8443/view/IRSA%20k8s/job/ikc_firefly/build
    - Build the docker with the following docker based of the git tag: example - `2023.2-pre-4`
    - `BUILD_ENV`: Select 'ops'
    - `ACTION`: Select 'both'
    - `DEPLOY_ENV`: Select 'dev' to have this release deploy to fireflydev.ipac.caltech.edu
