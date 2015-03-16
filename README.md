# Firefly

Firefly is IPAC's Advanced Astronomy WEB UI Framework. It is for building a web-based Front end to access science archives. It provides sophisticated data visualization capabilities not available in many web-based archives. Our efforts to create a tightly integrated and user-friendly interface has generated numerous positive feedback from scientists. Users can browse and understand large volumes of archive data in a fraction of the time it took in the past.


#### Firefly Framework
Firefly is being reused for various archives because it is a framework of archive components. This has allowed us now to implement multiple archive systems at a fraction of the cost of the first system, Spitzer Heritage Archive.


#### Firefly Viewer

By default, this repository builds a default example webapp. This tool can be used for viewing fits data, catalogs, and xyplots. It is a general viewer tool for retrieving and viewing astronomy related data.

#### Firefly Tools

Firefly Tools exposes the most powerful components of Firefly in a way that can be used by any web page with no prerequisites. It allows any web developer access to Firefly's FITS visualizers or Table Tool with just a very few lines of JavaScript. The goal is to make these tools very easy to use with only a 10 minute learning curve.

An important feature is that the Firefly Tools server can be installed cross-site. In other words, it is not required to be on the same server as the web page. Firefly Tools can do this because it uses JSONP or CORS for the server communication. This allows Firefly Tools not to be limited by the server's Same Origin Policy and to give the developer a lot of flexibility. The web developer does not have to do any installation, but can simply just start using Firefly Tools.

### Visualizers
The firefly components contain 3 main visualizers for astronomy data.


#### Tabular Display
Firefly has implemented “Excel-like” tables on the webpage. In an easy, interactive way, a user can sort the results, filter the data on multiple constraints, hide or show columns or select data for download or visualization. The Firefly Tools server is optimized to show very large tables without significant performance degradation. We have tested it with over 10 million rows, 50 column tables.

#### Fits Visualization
Firefly provides a first-class FITS visualization on the Web without any plugins. All of the significant components you would expect to see in a basic desktop FITS application are available with data that Firefly displays. The FITS visualizer allows for plotting anything that the table shows that has a Lon and Lat. Therefore, users can overlay multiple catalogs over their FITS image and interact with it.

#### 2D Line Graphs
Firefly shows 2D line graphs interactively so that a user can read the data point values as he moves his mouse around or zooms in to investigate the data at a finer level. These graphs are used for spectrum or plotting table columns. The user can specify any column that the tables are showing. The user can also define equations from the rows for the XY Plot, such as (row1*row2/tan(row3)



## Setup

#### Prerequisites
 -  [Java 1.6.+] (http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html)
    This is the version Firefly is tested on.  Later versions should work as well, although it is not fully tested.


 -  [Gradle 2.3.+] (https://gradle.org/downloads)
    Gradle is an open source build automation system.


 -  [Tomcat 7.0.+] (http://tomcat.apache.org/download-70.cgi)
    Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies.

 -  [Node 0.12.0.+] (https://nodejs.org/download/)
    Javascript interpreter for command line environment, used for development tools

#### How to get Firefly source:

Go to page https://github.com/Caltech-IPAC/firefly.  Click on `Download ZIP`.  Expand the zip file after downloaded.
Another option if you have git installed is to run:

    $ git clone https://github.com/Caltech-IPAC/firefly.git


#### How to build Firefly:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :firefly:jar

This generates firefly.jar located at ./jars/build/.
You may include this jar into your project to build advanced astronomy web applications.




#### How to build and deploy Firefly Viewer:

In a terminal, cd into the `firefly` directory, then run:

    $ gradle :fftools:war

This generates fftools.war located at ./build/lib/.
Simply drop this file into your $CATALINA_HOME/webapps/ directory to deploy it.
$CATALINA_HOME is your Tomcat server installation directory.

Goto http://localhost:8080/fftools/ to launch Firefly Viewer.



## Code Examples Using Firefly Tools

#### Example 1
To load firefly Tools, you should include the above declaration in your HTML file. When Firefly Tools completes its loading, it calls a JavaScript function `onFireflyLoaded()`
Firefly Tools works by placing its components in a HTML `<div>` element with a specified ID.

The following are several examples of what can be done in the `onFireflyLoaded()` function. All test data can be found at `http://web.ipac.caltech.edu/staff/roby/demo`


```html
<div id="plot" style="width: 350px; height: 350px;"></div>
```


```js
function onFireflyLoaded() {
  var iv2= firefly.makeImageViewer("plot");
  iv2.plot({
     "Title"      :"Example FITS Image",
     "ColorTable" :"16",
     "RangeValues":firefly.serializeRangeValues("Sigma",-2,8,"Linear"),
     "URL"        :"http://web.ipac.caltech.edu/staff/roby/demo/wise-m31-level1-3.fits"});
}
```

#### Example 2
It is very easy to plot a table. The table can be used by itself or you can attach an image viewer to the table.
The source for the table tool can be an IPAC table, a CSV, or a TSV file.

The following examples creates a side-by-side HTML layout.  The table is plotted in the `<div>` id `table2Here`
and the related image is plotting in the `<div>`  id `previewHere`.
The table contains a column with the URL of an image related for that row.
Every time the user clicks on a row the image will update.



```html
  <div style="white-space: nowrap;">
    <div id="table2Here" style="display:inline-block; width: 600px; height: 250px;
                               margin : 5px 8px 0px 10px; border: solid 1px;"></div>
    <div id="previewHere" style="display:inline-block;
                            width: 250px; height: 250px; border: solid 1px;"></div>
  </div>
```


```js
 function onFireflyLoaded() {
    var table2Data= { "source" : "http://web.ipac.caltech.edu/staff/roby/demo/test-table4.tbl"};
    firefly.showTable(table2Data, "table2Here");
    firefly.addDataViewer( {"DataSource" : "URL",
                            "DataColumn" : "FITS",
                            "MinSize"    : "100x100",
                            "ColorTable" : "1",
                            "QUERY_ID"   : "table2Here"  }, "previewHere" );
 }
```

#### Example 3
Using a similar approach you can create a coverage plot from a table that has columns
with the four corners of each image. The HTML `<div>` declaration is omitted for brevity.


```js
 function onFireflyLoaded() {
    var table1Data= { "source" : "http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl"};
    firefly.showTable(table1Data, "tableHere");
    firefly.addCoveragePlot({"QUERY_ID" : "tableHere",
                             "MinSize"  : "100x100" }, "coverageHere" );
```

#### Example 4
In this example, we create four image viewers. Each belong to the same group ("wise-group").  We then set some global parameters so all the plots display the same.
Now we plot each of the four images by specifying the URL of the FITS file. By doing this, all the plotting controls will work on all four images simultaneously. The HTML `<div>` declaration is omitted for brevity.


```js
function onFireflyLoaded() {
   var w1= firefly.makeImageViewer("w1", "wise-group");
   var w2= firefly.makeImageViewer("w2", "wise-group");
   var w3= firefly.makeImageViewer("w3", "wise-group");
   var w4= firefly.makeImageViewer("w4", "wise-group");

   firefly.setGlobalDefaultParams({ "ZoomType"    : "TO_WIDTH",
                                    "ColorTable"  : "8",
                                    "ZoomToWidth" : "250" });

   w1.plot({ "URL"  : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band1.fits",
             "Title": "Wise band 1" });
   w2.plot({ "URL"  : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits",
             "Title": "Wise band 2" });
   w3.plot({ "URL"  : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band3.fits",
             "Title": "Wise band 3" });
   w4.plot({ "URL"  : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band4.fits",
             "Title": "Wise band 4" });
}
```
