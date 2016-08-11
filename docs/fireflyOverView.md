# Firefly

Firefly is IPAC's Advanced Astronomy WEB UI Framework. It is for building a web-based Front end to access science archives. It provides sophisticated data visualization capabilities not available in many web-based archives. Our efforts to create a tightly integrated and user-friendly interface has generated numerous positive feedback from scientists. Users can browse and understand large volumes of archive data in a fraction of the time it took in the past.

Note - if you are pulling, look at the [branches section](#branches)


#### Firefly Framework
Firefly is being reused for various archives because it is a framework of archive components. This has allowed us now to implement multiple archive systems at a fraction of the cost of the first system, Spitzer Heritage Archive.


#### Firefly Viewer

By default, this repository builds a default example webapp. This tool can be used for viewing FITS data, catalogs, and xyplots. It is a general viewer tool for retrieving and viewing astronomy related data.

#### Firefly Tools

Firefly Tools exposes the most powerful components of Firefly in a way that can be used by any web page with no prerequisites. It allows any web developer access to Firefly's FITS visualizers or Table Tool with just a very few lines of JavaScript. The goal is to make these tools very easy to use with only a 10 minute learning curve.

An important feature is that the Firefly Tools server can be installed cross-site. In other words, it is not required to be on the same server as the web page. Firefly Tools can do this because it uses JSONP or CORS for the server communication. This allows Firefly Tools not to be limited by the server's Same Origin Policy and to give the developer a lot of flexibility. The web developer does not have to do any installation, but can simply just start using Firefly Tools.

### Visualizers
The firefly components contain 3 main visualizers for astronomy data.


#### Tabular Display
Firefly has implemented “Excel-like” tables on the webpage. In an easy, interactive way, a user can sort the results, filter the data on multiple constraints, hide or show columns or select data for download or visualization. The Firefly Tools server is optimized to show very large tables without significant performance degradation. We have tested it with over 10 million rows, 50 column tables.

#### FITS Visualization
Firefly provides a first-class FITS visualization on the Web without any plugins. All of the significant components you would expect to see in a basic desktop FITS application are available with data that Firefly displays. The FITS visualizer allows for plotting anything that the table shows that has a Lon and Lat. Therefore, users can overlay multiple catalogs over their FITS image and interact with it.

#### 2D Line Graphs
Firefly shows 2D line graphs interactively so that a user can read the data point values as he moves his mouse around or zooms in to investigate the data at a finer level. These graphs are used for spectrum or plotting table columns. The user can specify any column that the tables are showing. The user can also define equations from the rows for the XY Plot, such as (row1*row2/tan(row3))

## Branches
There are several branches the this repository.  Here are the ones that you should care about.

 - rc: the next release canidate branch.  This is the most stable branch.
 - master: Update once a week.  Used for out stable, weekly build.
 - dev: this branch is could be updated every day. It is the active development branch. It is not stable.
 - x-??? : Any branch that begins with x- is temporary and used development feature branches


## Setup

#### Prerequisites
 -  [Java 1.8] http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
    This is the version we are using to compile and run Firefly.

 -  [Gradle 2.3+] (https://gradle.org/downloads)
    Gradle is an open source build automation system.


 -  [Tomcat 7+] (http://tomcat.apache.org/download-70.cgi)
    Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies.

 -  [Node 0.12.0.+] (https://nodejs.org/download/)
    Javascript interpreter for command line environment, used for development tools

#### Prepare before the build
 - Make sure you have  `<GRADLE>/bin`, `<JAVA>/bin`, and `<NODE>/bin` in your PATH.


#### How to build Firefly:

First `git clone` the firefly repository or download the zip (and unzip) to your target machine. Then install the prerequisites (node, java, tomcat).

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:jar

This generates firefly.jar located at ./jars/build/.
You may include this jar into your project to build advanced astronomy web applications.


#### How to build and deploy Firefly Viewer:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:war

This generates firefly.war located at ./build/lib/.
Simply drop this file into your $CATALINA_HOME/webapps/ directory to deploy it.
$CATALINA_HOME is your Tomcat server installation directory.

Goto http://localhost:8080/firefly/ to launch Firefly Viewer.

# JavaScript Firefly Tools API

Firefly tools is an API that can be used from JavaScript. High-level API allows to render the main components of Firefly and make them share the same data model. Low-level API gives direct access to Firefly React components and application state. Firefly tools API also allows to dispatch or add a listener to virtually any action, available to Firefly internally. This makes it possible to extend Firefly by executing custom code on various UI events, like mouse move or region selection.

The main Firefly components are:

 - [FITS Visualizer](#fits-visualization)
 - [Table](#table-visualization)
 - [XY Plotter](#xy-plot-visualization)

These components can be setup to share the same data model.  Therefore you can do the following combinations:

 - [Connect FITS viewer coverage image to a table](#connecting-coverage-image-to-table)
 - [Connect XY plot to a table](#connecting-xy-viewers-to-table). A Table with any data and a XY Plot showing plots from any two columns of that table.
 - Tri-view: Table, FITS coverage, and XY Plot together showing the same data.

Lower level API is built around the following modules:

 - `firefly.ui` - UI components
 - `firefly.util` - utilities
 - `firefly.action` - actions, changing application state

More information about lower level API can be found here:

 - [Rendering UI Components](#rendering-ui-components)
 - [Dispatching and Watching Actions](#dispatching-and-watching-actions)
 - [Other utilities](#other-utility-methods)
 - [Utility methods for FITS visualization](#utility-methods-for-fits-visualization)
 - [Region Support](#region-support)


###Starting Firefly Tools in JavaScript
Getting started with firefly tools involves three basic steps.

 1. Load the javascript file `firefly_loader.js`
 2. Define some divs in the html document where you will load the viewer widgets.
 3. Define `onFireflyLoaded()` function. When `firefly_loader.js` loads it will call this function.

This is all best explained with a code example. This examples creates a div with id `myID`, loads firefly, and plots a FITS file in the `onFireflyLoaded` function.

```html
<!doctype html>
<html>
    <head><title>Demo of Firefly Tools</title></head>
    <body>
        <!-- need a div id to plot to -->
        <div id="myID" style="width: 350px; height: 350px;"></div>
        <script type="text/javascript">
        {
            // this function must exist, called when firefly loads
            var onFireflyLoaded= function() {
                firefly.showImage('myId', {
                    plotId: 'myImageId',
                    url: 'http://someHost.org/someFile.fits',
                });
            };
        }
        </script>
        <!-- script name is firefly_loader.js -->
        <script src='firefly_loader.js'></script>
    </body>
</html>
```

###Remote Firefly Tools Interface

The interface to remotely communicate to the firefly viewer:

 - `firefly.getViewer()` - gives a handle to launch the firefly tools in another browser window or tab

| Parameter  | type | Description |
| ---------- | ---- | ----------- |
| `channel` | string |the channel id string, if not specified then one will be generated |
| `file` | string | file the html of the viewer to launch. In time there will be several |
| *return*  | object | viewer interface |


###Rendering UI components

Firefly React components can be found in `firefly.ui` module.

`firefly.util.renderDOM(div, Component, props)` - render React component into the DOM tree

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| div | string or Object | a div element or a string id of the div element |
| Component | Object | a React component |
| props | Object | properties for the React component |

`firefly.util.unrenderDOM(div)` - remove the rendered element

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| div | string or Object | a div element or a string id of the div element |


###Dispatching and watching actions

Firefly is built around [React](https://facebook.github.io/react/docs/why-react.html)/[Redux](http://redux.js.org) paradigm. The idea of React is that it will always render the state of your component.
Redux manages that state of your application. The data in the state is immutable. It cannot change directly.
When the program fires an action describing the change to the data, a new version of the state is created with the changes.
Then React re-renders the parts that are different. Therefore, application goes from one state change to the next.

![diagram](https://cask.scotch.io/2014/10/V70cSEC.png)

Being able to dispatch and listen to Firefly actions makes it possible to write new code, which could control or interact with Firefly widgets.

#####**firefly.util.addActionListener(actionType,callBack)**

`firefly.action.type` object is a handle to Firefly action types. Action types are its properties.
`firefly.actions` object contains action dispatchers.

Each action has two properties: type and payload. Payload is different from action to action and is an object constructed from the parameters of the corresponding action dispatcher.
Hence, action payload is documented in action dispatcher function. Most of action dispatcher functions are camel-cased `dispatch[ActionType]`.
For example, action with type `READOUT_DATA` is dispatched with a function named `dispatchReadoutData`.

`firefly.util.addActionListener(actionType,callBack)` - add a listener to any action type

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| actionType | a string or and array of strings | Each string is an action constant from firefly.action.type |
| callback  | function | The callback will be called with two parameters: action object and state object. If it returns true the listener will be removed. |
| *return* | function | a function that will remove the listener |


###Other Utility Methods

The rest of Firefly utilities are split into the following modules by function:

- `firefly.util.image` - image utilities
- `firefly.util.table` - table utilities
- `firefly.util.chart` - chart utilities
