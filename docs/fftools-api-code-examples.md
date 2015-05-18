## Code Examples Using Firefly Tools

###Starting Firefly Tools in JavaScript
Firefly Tools are loaded when you load`fftools.nocache.js` from a server of your choice. For example, to load Firefly Tools from your local server, include the following declaration in your HTML file:

```html
 <script type="text/javascript" language="javascript" src="http://localhost:8080/fftools/fftools.nocache.js">   
```

Your html document must define some divs, where you will load the viewer widgets, and `onFireflyLoaded()` function.

When Firefly Tools completes loading, it calls `onFireflyLoaded()` JavaScript function. This is where you create Firefly components and place them into HTML `<div>` elements with specified IDs. 

The following are several examples of what can be done in the `onFireflyLoaded()` function. All test data can be found at `http://web.ipac.caltech.edu/staff/roby/demo`. 

#### Example 1

Create an image viewer and place it into the `<div>` id `plotHere`.

```html
<div id="plotHere" style="width: 350px; height: 350px;"></div>
```

```js
function onFireflyLoaded() {
  var iv2= firefly.makeImageViewer("plotHere");
  iv2.plot({
     "Title"      :"Example FITS Image",
     "ColorTable" :"16",
     "RangeValues":firefly.serializeRangeValues("Sigma",-2,8,"Linear"),
     "URL"        :"http://web.ipac.caltech.edu/staff/roby/demo/wise-m31-level1-3.fits"});
}
```

#### Example 2
It is very easy to plot a table. The table can be used by itself or you can attach an image viewer to the table. The source for the table tool can be an IPAC table, a CSV, or a TSV file.

The following examples creates a side-by-side HTML layout.  The table is plotted in the `<div>` id `table2Here` and the related image is plotting in the `<div>`  id `previewHere`. The table contains a column with the URL of an image related for that row. Every time the user clicks on a row the image will update.

```html
  <div style="white-space: nowrap;">
    <div id="tableHere" style="display:inline-block; width: 600px; height: 250px; margin : 5px 8px 0px 10px; border: solid 1px;"></div>
    <div id="previewHere" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
  </div>
```

```js
 function onFireflyLoaded() {
    var tableData= { "source" : "http://web.ipac.caltech.edu/staff/roby/demo/test-table4.tbl"};
    firefly.showTable(tableData, "tableHere");
    firefly.addDataViewer( {"DataSource" : "URL",
                            "DataColumn" : "FITS",
                            "MinSize"    : "100x100",
                            "ColorTable" : "1",
                            "QUERY_ID"   : "tableHere"  }, "previewHere" );
 }
```

#### Example 3
Using a similar approach you can create a coverage plot from a table that has columns with the four corners of each image. The HTML `<div>` declaration is omitted for brevity.


```html
  <div style="white-space: nowrap;">
    <div id="tableHere" style="display:inline-block; width: 600px; height: 250px; margin : 5px 8px 0px 10px; border: solid 1px;"></div>
    <div id="coverageHere" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
  </div>
```

```js
 function onFireflyLoaded() {
    var table1Data= { "source" : "http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl"};
    firefly.showTable(table1Data, "tableHere");
    firefly.addCoveragePlot({"QUERY_ID" : "tableHere",
                             "MinSize"  : "100x100" }, "coverageHere" );
 }
```


#### Example 4
In this example, we create four image viewers. Each belong to the same group `wise-group`. We then set some global parameters so all the plots display the same. Now we plot each of the four images by specifying the URL of the FITS file. By doing this, all the plotting controls will work on all four images simultaneously. 


```html
  <div style="white-space: nowrap;">
    <div id="w1" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w2" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w3" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
    <div id="w4" style="display:inline-block; width: 250px; height: 250px; border: solid 1px;"></div>
  </div>
```

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


