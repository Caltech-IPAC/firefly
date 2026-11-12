Firefly from Docker
----------------------- 

#### Docker:

- Firefly is containerized and can be pulled from docker
- Docker images allow users to run software from a container that has the required software stack.
- Documentation about Docker can be found here: https://www.docker.com


#### How to run Firefly from a docker image?

To run latest or nightly dev:

- `docker run -p 8090:8080 -m 4g --rm ipac/firefly:latest`
- `docker run -p 8090:8080 -m 4g --rm ipac/firefly:nightly`


Then, for example, 
- Access Firefly with: http://localhost:8090/firefly/
- JavaScript API
  - Import and use the JavaScript Firefly API embedded into your HTML using:
     - `<script  type="text/javascript" src="http://localhost:8090/firefly/firefly_loader.js"></script>`
  - Examples of using the JS API are on the test page: http://localhost:8090/firefly/test/
  - [Documentation Here](./firefly-api-overview.md)



For more advanced Docker start commands, see the Firefly Docker Hub page: https://hub.docker.com/r/ipac/firefly

See all tags here: https://hub.docker.com/r/ipac/firefly/tags


#### GitHub Container Registry (GHCR)

Release images are also published to GHCR, built for both `linux/amd64` and `linux/arm64`:

- `docker run -p 8090:8080 -m 4g --rm ghcr.io/caltech-ipac/firefly:<version>`

See all versions here: https://github.com/Caltech-IPAC/firefly/pkgs/container/firefly

The [Helm chart](../helm/README.md) uses the GHCR image by default.
