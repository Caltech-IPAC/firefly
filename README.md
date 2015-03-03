Firefly
--------------

Advanced Astronomy WEB UI Framework


### Prerequisites
 -  [Java 1.6.+] (http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html)
    This is the version Firefly is tested on.  Later versions should work as well, although it is not fully tested.


 -  [Gradle 2.3.+] (https://gradle.org/downloads)
    Gradle is an open source build automation system.


 -  [Tomcat 7.0.+] (http://tomcat.apache.org/download-70.cgi)
    Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies.


### How to get Firefly source:

Go to page https://github.com/Caltech-IPAC/firefly.  Click on `Download ZIP`.  Expand the zip file after downloaded.
Another option if you have git installed is to run:

    $ git clone https://github.com/Caltech-IPAC/firefly.git


### How to build Firefly:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:jar

This generates firefly.jar located at ./jars/build/.
You may include this jar into your project to build advanced astronomy web applications.



Firefly Viewer
--------------
##### TODO: Need a brief description of the viewer.


### How to build and deploy Firefly Viewer:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :fftools:war

This generates fftools.war located at ./build/lib/.
Simply drop this file into your $CATALINA_HOME/webapps/ directory to deploy it.
$CATALINA_HOME is your Tomcat server installation directory.

Goto http://localhost:8080/fftools/ to launch Firefly Viewer.

