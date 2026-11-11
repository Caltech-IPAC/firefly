# Firefly Developers

## Branches and tags
There are several branches in the repository.  Here are the ones that you should care about.

 - rc-YYYY.N: The next release candidate branch.  This is generally the most stable branch.
 - dev: This branch could be updated every day. It is the active development branch. It is not stable. Nightly builds are performed.
 - firefly-xxx-reason, IRSA-xxx: These branches are temporary development feature branches and are associated with corresponding tickets in IPAC Jira.
 - Tags: Firefly is tagged for releases and pre-releases
 - [See Details on tags and branches](tags-and-branches.md)

## Prerequisites
 -  [Java 21](https://openjdk.org/projects/jdk/21/)
    This is the version we are using to compile and run Firefly.  

 -  [Gradle 8.10](https://gradle.org/install/)
    Gradle is an open source build automation system.

 -  [Tomcat 11](https://tomcat.apache.org/download-11.cgi)
    Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies.

 -  [Node v26](https://nodejs.org/)
    Javascript interpreter for command line environment, used for development tools

## Prepare before the build
 - Make sure you have  `<GRADLE>/bin`, `<JAVA>/bin`, and `<NODE>/bin` in your PATH.
 - Install yarn via npm:  npm install yarn -g


## How to build Firefly.jar:

First `git clone` the firefly repository or download the zip (and unzip) to your target machine. Then install the prerequisites (node, java, tomcat).

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:jar

This generates firefly.jar located at ./jars/build/.
You may include this jar into your project to build advanced astronomy web applications.


## How to build and deploy Firefly Viewer:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:war

To include the API documentation and tutorial into the war archive run:

    $ gradle :firefly:warAll    

This generates firefly.war located at ./build/dist/.
Simply drop this file into your $CATALINA_HOME/webapps/ directory to deploy it.
$CATALINA_HOME is your Tomcat server installation directory.

Goto http://localhost:8080/firefly to launch Firefly Viewer.
The documentation is accessible via http://localhost:8080/firefly/docs/js/index.html
