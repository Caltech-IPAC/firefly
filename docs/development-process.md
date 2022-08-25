## Firefly Development process

- _Note on examples_
    - This example shows the `dev` branch, if you are working on a rc branch always use the rc branch instead of `dev`
    - This example development branch will be `FIREFLY-123456-table-sort`
- Branch naming scheme 
    - Branch Scheme: `Ticket Name` - `word or two description`
    - Such As: Ticket fix as a table feature might be: `FIREFLY-123456-table-sort`

### Process
1. Create a branch using the naming scheme above from the `dev` branch
    - Create the branch from the base branch
        - Goto base branch and make sure it is current 
          - `git checkout dev`
          - `git pull origin dev`
        - Create and change to the new branch
          - `git checkout -b FIREFLY-123456-table-sort`
    - Do the work and commit often.
        - _develop code_
        - `git commit -a`
2. Rebased you to `dev`
    - This will get any new work done by other developers
        - `git checkout dev`
        - `git pull origin dev`
        - `git checkout FIREFLY-123456-table-sort`
        - `git rebase dev`
    - Test your code again
4. Create a pull request on github - when work is done
    - Push the branch - `git push  origin FIREFLY-123456-table-sort`
    - Go to github and create a pull request with that branch.
    - Build a test using Jenkins and put the URL in you PR.
    - Get feedback, do updates, commits, push the changes.
        - `git commit -a`
        - `git push  origin FIREFLY-123456-table-sort`
5. When Pull Request is approved...
    - Switch to `dev` and pull
        - `git checkout dev`
        - `git pull origin dev`
        - `git checkout FIREFLY-123456-table-sort`
    - Rebase
        - `git rebase -i dev`
        - While rebasing squash the commit, the `git rebase -i` will put you in and editor to select the commit to squash
    - Force push your rebased branch
        - `git push -f origin FIREFLY-123456-table-sort`
    - goto github
    - do the basic merge
    - Modify the default message to start with ticket name follow by comments 
      - `FIREFLY-123456: merge pull request...`
6. Local cleanup
    - `git fetch -p`
    - `git pull origin dev`
    - `git branch -d FIREFLY-123456-table-sort`
