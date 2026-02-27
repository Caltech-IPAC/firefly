
# GitHub Workflow

## Overview

This document provides an overview of GitHub Actions workflows, along with practical guidance on how to create, test, 
and maintain a workflow.



It covers:

* The basic structure and components of a GitHub Actions workflow
* Key concepts such as triggers, permissions, caching, multi-repository checkout, and build arguments
* How to create a new workflow
* How to safely develop and test workflows before merging
* Key steps in the Firefly build and publish workflow, which can be used as a template for other applications

The goal of this document is to provide general knowledge that can be reused when creating or modifying workflows.

---

## The Basic Structure and Components of a GitHub Actions Workflow

A GitHub Actions workflow is defined in a YAML file located under:

```
.github/workflows/
```

A minimal workflow consists of:

```yaml
name: Example Workflow

on:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Run a command
        run: echo "Hello World"
```

### Core Components

**1. `name`**
The display name shown in the Actions UI.

**2. `on` (Triggers)**
Defines when the workflow runs (manual, push, release, pull request, etc.).

**3. `jobs`**
A workflow can contain one or more jobs. Jobs run in parallel by default.

**4. `runs-on`**
Specifies the runner environment (for example `ubuntu-latest`).

**5. `steps`**
Individual tasks executed sequentially inside a job. Steps can:

* Use a reusable action (`uses:`)
* Execute shell commands (`run:`)

Workflows are declarative: you define what should happen, and GitHub executes it in the specified environment.

---

## Key Concepts

### Triggers

Triggers determine when a workflow runs.

Common examples:

```yaml
on:
  workflow_dispatch:        # Manual trigger
  push:
    branches: [ main ]      # On push to main
  release:
    types: [published]      # When a release is published
```

For development, `workflow_dispatch` is useful because it allows manual execution without affecting releases.

### Permissions

Each workflow receives a `GITHUB_TOKEN`. Its capabilities are controlled by:

```yaml
permissions:
  contents: read
  packages: write
```

* `contents: read` allows repository checkout.
* `packages: write` is required to push Docker images to GHCR.

Following the principle of least privilege improves security.

### Caching

To enable Docker layer caching, include the following options in your Docker build step:

```yaml
cache-from: type=gha
cache-to: type=gha,mode=max
```

* `cache-from: type=gha` restores previously saved Docker build layers from GitHub Actions cache.
* `cache-to: type=gha,mode=max` saves all build layers back to the GitHub Actions cache after the build completes.

This significantly reduces build time by reusing unchanged layers (such as dependency installation steps) instead of rebuilding them from scratch.

These settings are performance optimizations only and do not affect the correctness of the final image.

### Multi-Repository Checkout

A workflow can check out multiple repositories into different directories.

Example:

```yaml
- uses: actions/checkout@v4
  with:
    repository: <org>/my-app
    ref: <tag-or-branch>
    path: my-app

- uses: actions/checkout@v4
  with:
    repository: Caltech-IPAC/firefly
    ref: <tag-or-branch>
    path: firefly
```

This results in:

```
.
├── my-app/
└── firefly/
```

The Docker build `context` should be set appropriately (for example `context: .`) so both directories are visible during the build.

### Build Arguments

Build arguments pass dynamic values into the Docker build process.

In CLI form:

```
--build-arg env=ops --build-arg build_dir=my-app
```

In GitHub Actions:

```yaml
build-args: |
  env=ops
  build_dir=my-app
```

---

## How to Create a New Workflow

### Create the workflow file under:

   ```
   .github/workflows/
   ```

   Example:

   ```
   .github/workflows/build_publish.yml
   ```

### Add a basic structure with a safe trigger:

```yaml
name: Build and Publish Firefly

on:
  workflow_dispatch:
    inputs:
      push_image:
        description: "Push image to GHCR"
        required: false
        default: false
        type: boolean
  release:
    types: [published]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - run: echo "Workflow created"
```

### Develop and test the workflow

Work in a separate development branch (for example, `FIREFLY-1234-my-workflow`) while iterating on the workflow.  
The workflow exists only in this branch, it will **not appear in the GitHub Actions UI** until it is merged into the default branch (e.g., `dev`).  

To test safely before merging, use `act` locally. Refer to the **"How to Safely Develop and Test GitHub Workflows"** section for 
details on configuring and running `act`.

### Merge after validation

Create a pull request from your test branch into `dev`.

Once the pull request is reviewed and merged, the workflow will appear in GitHub Actions and begin running according to its configured triggers.

**Note:** Workflows must be located under `.github/workflows/` or GitHub will not detect them.

---

## Testing Workflow Changes Without Merging

When modifying an existing workflow, you do **not** need to merge it into `dev` in order to test it in GitHub Actions.

If the workflow includes a manual trigger:

```yaml
on:
  workflow_dispatch:
```

you can execute it directly from your feature branch.

### How to Run It

1. Push your branch to GitHub.
2. Go to **GitHub → Actions**.
3. Select the workflow.
4. Click **Run workflow**.
5. Choose your branch from the branch dropdown.
6. Click **Run workflow**.

GitHub will execute the version of the workflow file that exists in the selected branch.

### Important Notes

* The workflow must already exist in the repository (i.e., it must have been merged at least once previously).
* The workflow must define `workflow_dispatch` to allow manual triggering from the UI.
* The run uses the workflow YAML from the selected branch, not from `main`.

This approach allows you to safely iterate on workflow changes without prematurely merging them into the default branch.

---

## How to Safely Develop and Test Workflows Locally

During development, it is often useful to test the workflow locally before pushing changes to GitHub. 
This reduces iteration time and avoids unnecessary commits.

We use **`act`**, a tool that runs GitHub Actions locally using Docker.

### What is `act`?

`act` simulates GitHub Actions locally by:

* Reading your workflow file from `.github/workflows/`
* Spinning up a Docker container that mimics the GitHub runner
* Executing the workflow steps inside that container

This allows you to validate:

* YAML syntax
* Bash scripts
* Tag extraction logic
* Conditional logic
* Most Docker build steps

Without pushing to GitHub.

#### Installing `act`

On macOS:

```
brew install act
act --version
```


#### Running the Workflow Locally

To simulate a manual workflow dispatch:

```
act workflow_dispatch -W .github/workflows/build_publish.yml
```

If your workflow has inputs, you can pass them using `--input`:

```
act workflow_dispatch \
  --input tag_name=2025.4
  -W .github/workflows/build_publish.yml \
```

### Limitations of `act`

While very useful, `act` is not a perfect replica of GitHub runners.

Important limitations:

* Multi-platform builds (`linux/amd64,linux/arm64`) may not behave exactly like GitHub-hosted runners.
* QEMU setup can differ.
* Pushing to GHCR should be disabled when testing locally.
* Some marketplace actions behave slightly differently.

For multi-arch publishing, final validation should still be done on GitHub runners.

---


## Creating the Firefly Build and Publish Workflow

The workflow file must be located at:

```
.github/workflows/build_publish.yml
```

The resulting image is published to:
```
ghcr.io/Caltech-IPAC/firefly:<tag>
```

### Workflow Triggers

```yaml
on:
  workflow_dispatch:
  release:
    types: [published]
```

This allows:

* Manual execution from the Actions UI
* Automatic execution when a Release is published


### Extracting the Firefly Tag from Configuration (Optional)

It is common for applications using Firefly to specify the Firefly version in their configuration.
This ensures reproducible builds. Include this helper step to extract that tag for use in the workflow.

Example line in `app.config`:

```
firefly.tag.name = "release-xxxx.x.x"
```

Reusable step to extract the tag:
```yaml
- name: Read Firefly tag from config
  id: firefly_ref
  shell: bash
  run: |
    set -euo pipefail

    ref="$(
      grep -E '^[[:space:]]*firefly\.tag\.name[[:space:]]*=' suit/config/app.config | cut -d'"' -f2
    )"

    if [[ -z "$ref" ]]; then
      echo "ERROR: firefly.tag.name not found or malformed"
      exit 1
    fi

    echo "Using firefly tag: $ref"
    echo "ref=$ref" >> "$GITHUB_OUTPUT"
```

### Checkout the required repositories

#### Checkout Firefly

```yaml
- uses: actions/checkout@v4
  with:
    repository: Caltech-IPAC/firefly
    path: firefly
```

#### Checkout OnlineHelp

```yaml
- uses: actions/checkout@v4
  with:
    repository: Caltech-IPAC/firefly-help
    path: firefly-help
```

### Repository Structure

The workflow builds using this layout inside the GitHub runner:

```
.
├── <app-help>/  (applies when building standalone image, for example, firefly-help)
├── <app>/  (applies to app using Firefly, for example, IRSA Viewer)
│   └── config/app.config
└── firefly/
    └── docker/Dockerfile
```
`<app>` is the application using Firefly, which contains the configuration file specifying the Firefly tag.
`<app-help>` is the online help repository, which is needed when building the standalone image.
When git cloning, we specify the `path` to place the repositories in the correct structure for the Docker build context.


### Multi-Platform Docker Build

We use Docker Buildx to build multi-platform images.

Docker Buildx extends the standard docker build command and enables building images for multiple CPU 
architectures (such as linux/amd64 and linux/arm64) in a single workflow run.

#### Setup steps

```yaml
- uses: docker/setup-qemu-action@v3
- uses: docker/setup-buildx-action@v3
```

QEMU enables cross-platform builds by emulating different CPU architectures during the Docker build process.

### Build step

```yaml
- name: Build and push image
  uses: docker/build-push-action@v6
  with:
    context: .
    file: firefly/docker/Dockerfile
    platforms: linux/amd64,linux/arm64
    push: ${{ github.event_name == 'release' || inputs.push_image == 'true' }}
    tags: ghcr.io/Caltech-IPAC/firefly:${{ steps.image_tag.outputs.tag }}
    build-args: |
      env=ops
    cache-from: type=gha
    cache-to: type=gha,mode=max
```

Important notes:

* Dockerfile must point to firefly directory since it's shared between multiple apps
* `platforms` specify which architectures to build for
* `tags` specify the image name and tag to push to GHCR
* Use `image_tag` step to determine the tag to use based on the workflow trigger (release tag or Firefly tag)

---

#### Adding Build Arguments

Firefly docker build supports build arguments, which can be used to pass environment-specific variables into the Dockerfile.

Build arguments are passed using:
```yaml
build-args: |
  env=ops
```

Equivalent to CLI:

```
--build-arg env=ops
```

