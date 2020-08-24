Firefly Development Environment
------------------------------- 

A Docker based local development environment for Firefly.  This includes all runtime and development
dependencies.  


#### Dependencies

 - Docker and Docker-Compose     
    Starts here https://docs.docker.com/get-docker/
 - docker-sync  
    http://docker-sync.io/


## Quick Start

#### Create source repositories

In these examples, `cm` is used as a top-level directory for these repositories.  But, it does not need to be named `cm`.
It can be named anything you want.

    mkdir cm && cd cm
    git clone https://github.com/Caltech-IPAC/firefly
    git clone https://github.com/Caltech-IPAC/firefly-help

On macOS, only shared paths are accessible by Docker.  
You can configure shared paths from `Docker -> Preferences... -> Resources -> File Sharing.`  
See https://docs.docker.com/docker-for-mac/osxfs/#namespaces for more info.

These paths are needed:

`cm`       : source files  
`~/.gradle` : `GRADLE_USER_HOME` directory.  This is used to inject build properties where needed.
              On the Mac, in the file selection box you can use `CMD + SHIFT + .` to see hidden files such as `.gradle`.

#### docker-sync

`docker-sync` works best on macOS version 10.15+.  If you have an older version and do not wish to
upgrade, search for 'docker-syncXcode 11 on macOS 10.14' to find a workaround.

To install as root.  
    
    sudo gem install docker-sync

Creates and starts the sync containers for Firefly source.

    cd cm/firefly
    docker-sync start
    
This will run in the background.  Initial start may take a little longer.  
Running start the second time will be a lot faster, since containers and volumes are reused.  

When you are done with development, and wish to stop `docker-sync`.

    cd cm/firefly
    docker-sync clean
    
    

#### Start Dev-Mode

Builds, deploy and launches `dev mode`. Firefly is accessible at http://localhost:8000/firefly/ while it continues 
to monitor client-side source code for changes.  Once changes are detected, it will recompile and deploy the updates.
Normally, reloading the browser is enough to pull in the changes.  `ctrl-c` to exit.  
If you made changes to server-side(java) code, you'll have to restart Dev-Mode.
    
    cd cm/firefly
    docker-compose up dev

#### Debugging

For client-side debugging, use the browser's `Developer Tools`.  
For server-side, Tomcat in the container is launched with JPDA remote debugging on port 5050.
Simply attach your debugger remotely to localhost:5050


#### Run tests

    cd cm/firefly
    docker-compose run test
    
Test requires Git LFS due to larger test data.  To setup, see https://developer.lsst.io/git/git-lfs.html

    cd cm
    git clone https://github.com/lsst/firefly_test_data

#### Cleanup

`docker-compose up` does clean up itself upon exit.  Run `docker-compose down` to removes containers, networks, volumes, 
and images created by up  
    

### Additional Information

The Docker development environment image is created from Firefly's latest release image.  This ensure that
it's always using the latest runtime env.  Upon first start, a `firefly-builder` image will be created locally.
This process may take up to several minutes depending on your computer and network performance.  
If any changes are made to `firefly/docker/builder/Dockerfile` or `ipac/firely`, run `docker-compose build` to recreate the image.

#### Performance

On first run, it will be noticeably slower both for `docker-sync` as well as `docker-compose`.  For `docker-sync`, 
it needs to create (copy) the Volume.  For `docker-compose`, a new `node_modules` is created from downloading and 
installing npm packages locally.  Once this is done, following runs should be much faster.

`.git` is a big directory.  To improve performance, it's excluded from `docker-sync`.  Due to this, `firefly` source 
directory in the container will not be recognized as a `git` repos.  Functions that requires `git` commands is lost, i.e. version info.  


#### Troubleshooting

- Request timed out when starting `docker-compose`
  First, try to cleanup by running `docker-compose down` and/or `dockery-sync clean`.  If all fails, restart Docker.