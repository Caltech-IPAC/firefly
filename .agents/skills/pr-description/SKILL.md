---
name: pr-description
description: Write a PR description for the current Firefly feature branch and copy it to the clipboard — Jira ticket link, a what/why summary sized to the change, and a Testing section with the branch's dev build URL. Use when the user asks to write, draft, or generate a PR/pull-request description, or invokes /pr-description.
---

Write a PR description for the current feature branch and copy it to the clipboard.

## Steps

1. **Find the base branch.** Firefly PRs target `dev`, or an `rc-*` branch for release fixes. Run `git fetch origin` and pick whichever of `origin/dev` and the `origin/rc-*` branches has the fewest commits in `git rev-list --count <base>..HEAD`. Use it as `<base>` below.

2. **Understand the changes.** Run these in parallel:
   - `git log <base>..HEAD --oneline`
   - `git diff <base>...HEAD --stat`

   Then read the key changed files to understand what the code actually does — not just the commit messages.

3. **Get the ticket** from the branch name (`FIREFLY-1234-short-desc` → `FIREFLY-1234`; also `IRSA-…`). Link it as `https://jira.ipac.caltech.edu/browse/<TICKET>`.

4. **Learn the author's style** from their recent PRs:
   - `gh pr list --repo Caltech-IPAC/firefly --author @me --state merged --limit 5 --json number,title,body`
   - Note the tone, tense, header levels, and bullets vs prose — and match them.
   - If the author has no past PRs, use the structure in step 5 as is.
   - Saved preferences from the author (memory, CLAUDE.md) take precedence over anything inferred here.

5. **Draft the description in markdown:**
   - **Ticket line** at the top, linking the Jira ticket (e.g. `Fixes [FIREFLY-1234](https://jira.ipac.caltech.edu/browse/FIREFLY-1234)`).
   - **What and why**, sized to the change. For a small fix: one tight paragraph covering the problem, the cause, and the fix. For a larger change: add bullets or a short section for non-obvious design decisions and their trade-offs. Skip obvious choices.
   - Briefly define library-specific terms in plain words.
   - If the change has a counterpart PR in another repo (e.g. an IFE PR), link it.
   - **`## Testing`** — first the dev build URL: `https://<branch-name-lowercased>.irsakubedev.ipac.caltech.edu/firefly/` (append the app path if the change is in a specific app). Then concrete steps a reviewer can follow: what to load, what to click, what they should see. Put caveats and known issues to verify here too.

6. **Show the draft** to the user and iterate until they approve. Remind them that the dev build URL only works once the branch's test build has been run.

7. **Copy the final version to the clipboard** with `pbcopy` on macOS (`xclip -selection clipboard` or `wl-copy` on Linux). If none of these is available, print it for the user to copy.
