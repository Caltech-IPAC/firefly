Firefly from docker
----------------------- 

#### Docker:

- Firefly is containerized and can be pulled from docker
- Docker images allow users to run software from a container that has the required software stack.
- Documentation about docker can be found here: https://www.docker.com


#### How to run Firefly from a docker image?

`docker run -p 8090:8080  -e "MAX_JVM_SIZE=8G" --rm ipac/firefly`

Then, for example, 
- access Firefly tests from the browser with http://localhost:8090/firefly/test/
- Import and use Firefly API into your HTML using:

`<script  type="text/javascript" src="http://localhost:8090/firefly/firefly_loader.js"></script>`

More advanced command line here: [../dev/docker/base/start-examples.txt](../dev/docker/base/start-examples.txt)




