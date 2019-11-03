# Firefly


Note - if you are pulling, look at the [branches section](#branches)

[Release Notes are Here](docs/release-notes.md).

Firefly builds are available on Docker and the notes for using it are on the [Docker Page](https://hub.docker.com/r/ipac/firefly).


#### Firefly Framework
Firefly is being reused for various archives because it is a framework of archive components. This has allowed us now to implement multiple archive systems at a fraction of the cost of the first system, Spitzer Heritage Archive. Firefly has undergone an extensive re-write since then, adopted React/Redux framework for the client side, making it much  easier to work with other JavaScript libraries. Recently we have adopted Plotly.js graphing library.

#### Firefly Viewer

By default, this repository builds a default example webapp. This tool can be used for viewing FITS data, catalogs, and xyplots. It is a general viewer tool for retrieving and viewing astronomy related data.

#### Firefly APIs

Firefly APIs exposes the most powerful components of Firefly in a way that can be used by any web page with no prerequisites. It allows any web developer access to Firefly's FITS visualizers or Table Tool with just a very few lines of JavaScript. The goal is to make these APIs very easy to use with only a 10 minute learning curve.

An important feature is that the Firefly APIs server can be installed cross-site. In other words, it is not required to be on the same server as the web page. Firefly APIs can do this because it uses JSONP or CORS for the server communication. This allows Firefly APIs not to be limited by the server's Same Origin Policy and to give the developer a lot of flexibility. The web developer does not have to do any installation, but can simply just start using Firefly APIs.

### Visualizers
The firefly components contain three main visualizers for astronomy data.


#### Tabular Data Display
Firefly has implemented “Excel-like” tables on the webpage. In an easy, interactive way, a user can sort the results, filter the data on multiple constraints, hide or show columns, or select data for download or visualization. The Firefly APIs server is optimized to show very large tables without significant performance degradation. We have tested it with over 10 million rows, 50 column tables.

#### FITS Visualization
Firefly provides a first-class FITS visualization on the Web without any plugins. All of the significant components you would expect to see in a basic desktop FITS application are available with data that Firefly displays. The FITS visualizer allows for plotting any table data with Lon(RA) and Lat(Dec). Therefore, users can overlay multiple catalogs over their FITS image and interact with it.

#### 2D Graphs
Firefly shows 2D graphs interactively so that a user can read the data point values as he moves his mouse around or zooms in to investigate the data at a finer level. These graphs are used for spectrum or plotting table columns. The user can specify any column that the tables are showing. The user can also define expressions from the columns for the XY Plot, such as (col1*col2/tan(col3)). Table based charts share their data model with the table, so both table and chart can trigger or respond to the common events, such as filtering of the data. Firefly also supports histogram and heatmap graph.


## Branches
There are several branches the this repository.  Here are the ones that you should care about.

 - rc: the next release canidate branch.  This is the most stable branch.
 - master: Update once a week.  Used for our relatively stable, weekly build.
 - dev: this branch could be updated every day. It is the active development branch. It is not stable.
 - DM-xxx, FIREFLY-xxx, IRSA-xxx : Those branches are temporary for development feature branches.


## Setup

#### Prerequisites
 -  [Java 1.8] http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
    This is the version we are using to compile and run Firefly.  

 -  [Gradle 3.x] (https://gradle.org/downloads)
    Gradle is an open source build automation system.


 -  [Tomcat 7+] (http://tomcat.apache.org/download-70.cgi)
    Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies.

 -  [Node v6.9+] (https://nodejs.org/) <=9.0!
    Javascript interpreter for command line environment, used for development tools

#### Prepare before the build
 - Make sure you have  `<GRADLE>/bin`, `<JAVA>/bin`, and `<NODE>/bin` in your PATH.
 - Install yarn via npm:  npm install yarn -g


#### How to build Firefly.jar:

First `git clone` the firefly repository or download the zip (and unzip) to your target machine. Then install the prerequisites (node, java, tomcat).

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:jar

This generates firefly.jar located at ./jars/build/.
You may include this jar into your project to build advanced astronomy web applications.


#### How to build and deploy Firefly Viewer:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:war

To include the API documentation and tutorial into the war archive run:

    $ gradle :firefly:warAll    

This generates firefly.war located at ./build/dist/.
Simply drop this file into your $CATALINA_HOME/webapps/ directory to deploy it.
$CATALINA_HOME is your Tomcat server installation directory.

Goto http://localhost:8080/firefly to launch Firefly Viewer.
The documentation is accessible via http://localhost:8080/firefly/docs/js/index.html


#### Supported Browsers

Firefly supports the following  browsers:

 - safari >= 10, 
 - chrome >= 67
 - firefox >= 60 
 - edge >= 16


## More Docs

#### Firefly JavaScript API overview
See [firefly-api-overview.md](docs/firefly-api-overview.md)
 (Deprecated [docs/fftools-api-overview.md])

#### Firefly Remote API using Python overview
See [firefly-python-wrapper.md](docs/firefly-python-wrapper.md)

#### Code Examples Using Firefly APIs
See [firefly-api-code-examples.md](tutorial/firefly-api-code-examples.md)
 (Deprecated [docs/fftools-api-code-examples.md])

#### Setting up the Server correctly for FITS files
See [server-settings-for-fits-files.md](docs/server-settings-for-fits-files.md)

#### Changing the Firefly runtime environment
See [firefly-environment.md](docs/firefly-environment.md)

#### Adding external task launcher or Python Launcher to Firefly
See [firefly-python-launcher.md](docs/firefly-external-task-launcher.md)

#### Release branches and tags
See [tags-and-branches.md](docs/tags-and-branches.md)
