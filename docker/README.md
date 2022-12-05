Working with Firefly Docker
--------------------------- 

With Docker, Firefly can be built and deploy with minimal dependencies.  

#### Dependencies

Docker and Docker-Compose     
- https://docs.docker.com/get-docker/
- https://docs.docker.com/compose/install/

**Note:**  Some versions of Docker includes docker compose.  In that case, you can skip docker-compose installation and
can use `docker compose` instead of `docker-compose`. 

### Clone source

In this example, `cm` is used as a top-level directory for these repositories.  But, it does not need to be named `cm`.
It can be named anything you want.

    mkdir cm && cd cm
    git clone https://github.com/Caltech-IPAC/firefly
    git clone https://github.com/Caltech-IPAC/firefly-help

On macOS, only shared paths are accessible by Docker.  
You can configure shared paths from `Docker -> Preferences... -> Resources -> File Sharing.`  
See https://docs.docker.com/docker-for-mac/osxfs/#namespaces for more info.

These paths are needed:  
`cm`: source files  


### Build Firefly

    cd cm/firefly
    docker-compose build firefly

If you've made changes to the source, this will pick up the changes and create a new Docker image with your changes.  
Because Firefly uses multi-stage builds, Docker is smart enough to reuse unaffected layers to speed up the build process.  
However, if you want a completely clean build, add `--no-cache` to the above command.  
For more details on docker-compose usage, go here: https://docs.docker.com/compose/reference/

BUILD_TAG environment variable defaults to 'latest'.  You can tag this build by setting it to something else.

### Run Firefly

    cd cm/firefly
    docker-compose up firefly

Firefly is now up and running at http://localhost:8080/firefly/


### Run Firefly in DevMode

> :info: **If you are using Mac Docker Desktop**:
> Docker on Mac has had some performance issues since the beginning. These are related to volume performance, 
> the way volumes are mounted, and the underlying osxfs filesystem.  
> Release 4.6 fixes this with the introduction of virtiofs.  
> 
> To use virtiofs, you must have macOS 12.2 or later and the new Virtualization Framework enabled.
> Go to Docker Desktop -> Preferences -> Experimental Features 
> and enable the two features.

DevMode is Firefly development mode.  When running, it will detect changes in the source code, and automatically
deploy it.  Reload the page in your browser to see the changes.  

    cd cm/firefly
    docker-compose up dev

DevMode only applies to client-side code.  If you made changes to server-side(.java) code, you need to rerun `docker-compose up dev` 
to have it rebuild and redeploy.

#### Debugger

Client side debugging is done via the browser's DevTool.  

To debug server side code, attach your debugger to a remote JVM on localhost:5050.  Of course, this only works
while DevMode is running.


### HOW-TO

**Interactive bash shell in the container**

    docker-compose exec firefly bash


**Cleanly shutdown Firefly**

    docker-compose down

This will stop Firefly and removes everything created by `up`.


**To publish this build to DockerHub**

    docker-compose push firefly

**To run Firefly from DockerHub**

If you want to run a version available from DockerHub.  

Set BUILD_TAG to the version you want.  Pull then run the image.

These are examples in `bash`

    export BUILD_TAG=mytag
    docker-compose pull firefly && docker-compose up firefly

or, in one-line

    (export BUILD_TAG=mytag; docker-compose pull firefly && docker-compose up firefly)


#### Customize Firefly

Firefly has many configurable runtime parameters.  For more information, run

    docker run --rm ipac/firefly:latest --help

There are many `docker run` examples on how to use these parameters.  Consider add them into a compose.yml for 
easy operation.  For information on how to do this, go here: https://docs.docker.com/compose/compose-file/compose-file-v3/

Docker-compose file allows for variables substitute.  You can set the values in the environment, by adding a line
to `firefly-docker.env` file.

For example, Firefly's `compose.yml` can have additional environment variables.  The values can come from the 
`firefly-docker.env` file, like this

    cat firefly-docker.env
    ADMIN_PASSWORD=reset-me
    CLEANUP_INTERVAL=3h


