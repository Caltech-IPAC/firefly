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
