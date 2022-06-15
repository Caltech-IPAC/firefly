Working with Firefly Docker
--------------------------- 

With Docker, Firefly can be built and deploy with minimal dependencies.  

#### Dependencies

Docker and Docker-Compose     
- https://docs.docker.com/get-docker/
- https://docs.docker.com/compose/install/

**Note:**  Some versions of Docker includes docker compose.  In that case, you can skip docker-compose installation and
can use `docker compose` instead of `docker-compose`. 

## Quick Start

**Clone source**

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

**Build and start Firefly**

    cd cm/firefly
    docker-compose up firefly

Firefly is now up and running at http://localhost:8080/firefly/



## HOW-TO

**Monitor Firefly logs**

    docker-compose logs -f firefly


**Interactive bash shell in the container**

    docker-compose exec firefly bash


**Cleanly shutdown Firefly**

    docker-compose down

**Build Firefly from source**

    docker-compose build firefly

If you've made changes to the source, this will pick up the changes and create a new Docker image with your changes.  
Because Firefly uses multi-stage builds, Docker is smart enough to reuse unaffected layers to speed up the build process.  
However, if you want a complete clean build, add `--no-cache` to the above command.  
For more details on docker-compose usage, go here: https://docs.docker.com/compose/reference/ 

BUILD_TAG variable defaults to 'latest'.  You can tag this build by setting it to something else.


**Build Firefly from source with arguments**

By default, Firefly builds includes documentation and help pages.  To build only Firefly, 

    docker-compose build --build-arg target=war firefly

`target` is the gradle task used when building.  There are others, like `test`.


**To publish this build to DockerHub**

    docker-compose push

**To run Firefly from DockerHub**

If you want to run a version available from DockerHub.  
Set BUILD_TAG to the version you want, then 

    docker-compose pull && docker-compose up -d



#### Customize Firefly

Firefly has many configurable runtime parameters.  For more information, run

    docker run --rm ipac/firefly:latest --help

There are many `docker run` examples on how to use these parameters.  Consider add them into a docker-compose.yml for 
easy operation.  For information on how to do this, go here: https://docs.docker.com/compose/compose-file/compose-file-v3/

Docker-compose file allows for variables substitute.  You can set the values in the environment, by adding a line
to `firefly-docker.env` file.

For example, Firefly's `docker-compose.yml` can have additional environment variables.  The values can come from the 
`firefly-docker.env` file, like this

    cat firefly-docker.env
    ADMIN_PASSWORD=reset-me
    CLEANUP_INTERVAL=3h


