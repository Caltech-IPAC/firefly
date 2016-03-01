# JavaScript Firefly Tools API

Firefly tools is an API that can be used from JavaScript. It allows you to use the main components of Firefly via an API. The following components are available.

 - [FITS Visualizer](#fits-visualization)
 - [Table](#table-visualization)
 - [XY Plotter](#xy-plot-visualization)
  
 
Beyond that some of the components can be setup to share the same data model.  Therefore you can do the following combinations.
 
 - [Connect FITS viewer coverage plot to a table](#connecting-coverage-plot-to-table)
 - [Connecting FITS Viewers to table with image meta data](#connecting-fits-viewers-to-table). As user selects different rows in the table the FITS images changes.
 - [Connecting XY Viewers to table](#connecting-xy-viewers-to-table). A Table with any data and a XY Plot showing plots from any two columns of that table.
 - Tri-view: A Table, FITS coverage, and XY Plot together showing the same data.
  
Firefly tools also allows you to expand certain components and receive events back when actions happen.

 - [Add context menus](#adding-context-extensions-to-fits-viewer) for when a user selects a box, line, circle, highlights a point.
 - [Receive events](#getting-events) from these context menus and from any overlay data plotted .


###Starting Firefly Tools in JavaScript
Getting started with firefly tools involves three basic steps.

 1. Load the javascript file `fftools.nocache.js`
 2. Define some divs in you html document where you will load the viewer widgets.
 3. Define `onFireflyLoaded()` function. When `fftools.nocache.js` loads it will call this function.
 
This is all best explained with a code example. This examples creates a div with id `myID`, loads firefly, and plots a fits fiIe in the `onFireflyLoaded` function.

```html
<!doctype html>
<html>
<head><title>Demo of Firefly Tools</title></head>
<body>

<!-- need a div id to plot to -->
<div id="myID" style="width: 350px; height: 350px;"></div>

<script type="text/javascript">
 {
      // this function must exist, called when fftools loads
   var onFireflyLoaded= function() {        
       var primaryViewer= firefly.makeImageViewer('myID');
       primaryViewer.plot({
          "URL"       : "http://someHost.org/someFile.fits",
          "ZoomType"  : "TO_WIDTH"});
   };
 }
</script>

<!-- script name is fftools.nocache.js -->
<script src='fftools.nocache.js'></script>

</body>
</html>
```

 

###FITS Visualization

A FITS `ImageViewer` is created or referenced by calling the following methods:

 - `firefly.makeImageViewer()`  - makes an inline image viewer for a html document
 - `firefly.getExpandViewer()`  - makes an image viewer that will popup in the html document.
 - `firefly.getExternalViewer()` - gives a handle to launch the firefly tools web applications with a specified FITS file.


#### ImageViewer

`firefly.makeImageViewer(div,group)` - Create a new ImageViewer object in the specified div.
    parameters:
    
| Parameter  | type | Description |
| ---------- | ---- | ----------- |
| `div` | string |The div to put the ImageViewer in. |
| `group` | string (optional) | The plot group to associate this image viewer. All ImageViewers with the same group name will  operate together for zooming, color changing, etc. |
| *return*  | ImageViewer | an ImageViewer object |


`ImageViewer` has the follows methods.

| Method | Description |
| ---------- | ----------- |
| `plot()` | plot a FITS image |
| `plotURL()` | convenience method to plot an image referenced by url |
| `plotFile()` | convenience method to plot an image file on the server |
| `plotFileOrURL()` | convenience method to plot an image referenced by file or url |
| `setDefaultParams()`  |  set parameters that will apply to  `plot` calls |



#####**ImageViewer.plot() method**

| Parameter  | Description |
| ---------- | ----------- |
| object     | object literal with name/value pairs for all parameters |


The FITS viewer can take many, many possible parameters.  Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server. Others control the zoom, stretch, and color, title, and default overlays. There are also parameters to pre-process an image, such as crop, rotate or flip. You can also specify three color parameters and the associated files.

For the details of FITS plotting parameters see: [see fits-plotting-parameters.md](fits-plotting-parameters.md)
 
<br>
*Examples*- 
```js
var primaryViewer= firefly.makeImageViewer('primaryID');
primaryViewer.plot({
    "URL"       : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits",
    "Title"     : "Some WISE image",
    "ZoomType"  : "TO_WIDTH"
});
```
*or*
```js
var iv= firefly.makeImageViewer("serviceHere","group1");
iv.plot( {  "Type"      : "SERVICE",
            "Service"   : "TWOMASS",
            "UserDesc"  : "Test",
            "Title"     : "2mass from service",
            "ZoomType"  : "STANDARD",
            "InitZoomLevel" : "1",
            "GridOn"     : "true",
            "SurveyKey"  : "k",
            "WorldPt"    : "10.68479;41.26906;EQ_J2000",
            "SizeInDeg"  : ".12",
            "AllowImageSelection" : "true" } );
```


#####**ImageViewer.plotURL() method**

| Parameter  | Description |
| ---------- | ----------- |
| url     | string with the url of a FITS image to plot, all other parameters are defaulted, (see ImageViewer.setDefaultParams)|


#####**ImageViewer.plotFile() method**

| Parameter  | Description |
| ---------- | ----------- |
| file     | string with full path of a FITS file on the server to plot, all other parameters are defaulted, (see ImageViewer.setDefaultParams)|


#####**ImageViewer.plotFileOrURL()** method

This shortcut method can be used when there are two ways to access the same file but for some reason the local access is not always available.
It will try the file first then the URL. All other parameters are defaulted, (see ImageViewer.setDefaultParams)

| Parameter  | Description |
| ---------- | ----------- |
| file     | string with full path of a FITS image file on the server to plot |
| url     | string with the url of a FITS image file to plot |


#####**ImageViewer.setDefaultParams()** method


Set parameters that will apply to call future FITS plots. See the documentation on `plot()` for defaults.

| Parameter  | Description |
| ---------- | ----------- |
| object     | object literal with name/value pairs for all parameters |



####**ExternalViewer** and **ExpandedViewer** 

`firefly.getExternalViewer()` - Get access to the Firefly tools viewer.  The firefly tools viewer will run in a browser tab or window.  It is used to plot an image.
 
 `firefly.getExpandedViewer()` -  Get access to the Firefly tools viewer in the expanded mode.  It is used to plot an image to full screen in a browser tab or window.  This is a little used feature.
   
Both viewers have the same methods as `ImageViewer`.

| Method | Description |
| ---------- | ----------- |
| `plot()` | plot a FITS image |
| `plotURL()` | convenience method to plot an image referenced by url |
| `plotFile()` | convenience method to plot an image file on the server |
| `plotFileOrURL()` | convenience method to plot an image referenced by file or url |
| `setDefaultParams()`  |  set parameters that will apply to  `plot` calls |

See the **ImageViewer** for the details of each method.

####Region Support

#####**firefly.overlayRegionData()** method

`firefly.overlayRegionData(regionData, regionLayerId, title, plotId)` - overlay region data on an image plot with the given id

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionData | array | an array of strings, each describing a ds9 region |
| regionLayerId | string | region layer id |
| title  | string | title for the layer, displaying regions |
| plotId | string | a string uniquely describing the plot |


#####**firefly.removeRegionData()** method

`firefly.removeRegionData(regionData, regionLayerId)` - remove region data from the given region layer

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionData | array | an array of strings, each describing a ds9 region |
| regionLayerId | string | region layer id |
 
*Example*:                               
```js
function onFireflyLoaded() {
    // add a callback function, which will be called on region selection
    var extFunc= function(data) {
        console.log("Region Select Callback "+JSON.stringify(data, null, 4));
    }
    var extension= {
        plotId : "plot",
        extType: "REGION_SELECT",
        callback: extFunc
    };
    var actions= firefly.appFlux.getActions('ExternalAccessActions');
    actions.extensionAdd(extension);
    // create image viewer and add a FITS    
    var iv2= firefly.makeImageViewer('plot');
    iv2.plot({    
        "Title"      :"Example FITS Image'",
        "URL"        :"http://web.ipac.caltech.edu/staff/roby/demo/wise-m31-level1-3.fits"});
    var x;
	var y;
    var regions	=[];
    // fill regions array with 10x10 square regions (in image pixel coordinates)
	for (x=0; x<10; x++) {
	    for (y=0; y<10; y++) {
            regions.push('box '+(100*x)+' '+(100+100*y)+' 100 100 0 # color=red');
        }
    }
    firefly.overlayRegionData(regions,'region1', '10x10 square regions', 'plot');
    // in 10 sec. (you would do it on some event) remove corner squares
    setTimeout(function() {
        var regionsToDelete = [
            'box 0 100 100 100 0 # color=red',
            'box 900 100 100 100 0 # color=red',
            'box 900 1000 100 100 0 # color=red',
            'box 0 1000 100 100 0 # color=red'
        ];
        firefly.removeRegionData(regionsToDelete, 'region1');
    }, 10000);
}
```


#### Utilities

#####**firefly.serializeRangeValues()** method

`firefly.serializeRangeValues(stretchType,lowerValue,upperValue,algorithm)` - serialize a stretch request into a string, for use with the "RangeValues" parameter

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| stretchType | string | the type of stretch may be 'Percent', 'Absolute', 'Sigma' |
| lowerValue  |number | lower value of stretch, based on stretchType |
| upperValue  |number | upper value of stretch, based on stretchType |
| algorithm | string | the stretch algorithm to use, may be 'Linear', 'Log', 'LogLog', 'Equal', 'Squared', 'Sqrt' |
| *return* | string | a serialized version of range values to be passed as a viewer parameter |

*Example*:                               
```js
iv.plot( {  'Type'      : 'SERVICE',
            'Service'   : 'TWOMASS',
            'SurveyKey' : 'k',
            'ObjectName': 'm33',
            'Title'     : 'FITS Data',
            'RangeValues' : firefly.serializeRangeValues("Sigma",-2,8,"Linear")
            });
```


#####**Other Utility Methods for FITS visualization** 

| Method  | parameters | Description |
| ------- | ---------- | ----------- |
|firefly.setGlobalDefaultParams(params) | a object literal such as ImageViewer.plot() uses |set global fallback params for every image plotting call |
|firefly.setRootPath(rootURLPath) |the root URL to be prepended to any relative URL. |sets the root path for any relative URL. If this method has not been called then relative URLs use the page's root.|

#####**Tracking the mouse on the FITS Viewer** 

The following example will plot a fits image then add a callback to get the mouse readout and log it to the console.

```js
var primaryViewer= firefly.makeImageViewer('primaryID');
primaryViewer.plot({
    "URL"       : "http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits",
    "Title"     : "Some WISE image",
    "ZoomType"  : "TO_WIDTH"
});

var showReadout= function(data) {
     if (data.type==='PLOT_MOUSE_READ_OUT') {
         // data contains gal,j2000,image & screen points plus flux when mouse pauses
         console.log(data);
     }
};

var mouseReadoutExt= {
                plotId : 'primaryID',
                extType: 'PLOT_MOUSE_READ_OUT',
                callback: showReadout
            };
actions.extensionAdd(mouseReadoutExt);


```



###Table Visualization

Usage: firefly.showTable(parameters, div)

Parameters ia an object literal.  div is the div to load the table into.
Below is a list of all possible parameters.

| Parameter | Description |
| ------------ | -------------- |
| source      | required; location of the ipac table.  url or file path.|
| alt_source  | use this if source does not exists.|
|type        | basic or selectable; default to basic if not given|
|filters     | `col_name` [=\|!=\|<\|>\|<=\|>=\] `value` *or* `col_name` `IN` (v1[,v2]*)|`
|sortInfo    | SortInto=`ASC`\|`DESC`,`col_name`|
|pageSize    | positive integer|
|startIdx    | positive integer|
|fixedLength | true|false, default to true|
|tableOptions | see below|
|(variables)* | value|

tableOptions:  option=true|false [,option=true|false]*
            show-filter
            show-popout
            show-title
            show-toolbar
            show-options
            show-paging
            show-save


*Example-*
```js
var params = {
     source : 'http://web.ipac.caltech.edu/tbl_test/test.tbl',
     alt_source : '/Users/loi/test.tbl',
     pageSize : '100',
     tableOptions : 'show-popout=false,show-units=true',
     var1 : 'value1',
     var2 : 'value2'
}
firefly.showTable(params, fv1);
```
The Table tools currently supports the following file formats:

 - IPAC Table file format
 - CSV - first row should be column headers
 - TSV - first row should be column header
 - FITS Tables

###XY Plot Visualization

Usage: firefly.showPlot(parameters, div)

 - parameters are an object attributes
 - div is the div to load the XY Plot into.

Parameters object literal can contain the following values

| Parameter | Description |
| ------------ | ------------- |
| source | required; location of the ipac table.  url or file path.|
| chartTitle |title of the chart |
| xCol | column to use for x values |
| xColExpr | an expression to use for y values, can contain multiple column names ex. log(col) or (col1-col2)/col3 |
| yCol | column to use for y values |
| yColExpr | an expression to use for y values, can contain multiple column names ex. log(col) or (col1-col2)/col3 |
| errorCol | column to use for y value errors (must be column name, expressions are not supported) |
| orderCol | column to use to separate series from each other, different series are shown in different colors |
| plotStyle | line|linePoints|points, defaults to points |
| showLegend | always|onExpand, defaults to onExpand |
| plotTitle | header for the plot |


*example-*
```js
firefly.showPlot(
    {'source' : 'http://web.ipac.caltech.edu/staff/roby/demo/SPITZER_S5_3539456_01_merge.tbl',
    'chartTitle' : 'SPITZER_S5_3539456_01_merge.tbl',
    'xCol' : 'wavelength',
    'yCol' : 'flux_density',
    'errorCol' : 'error',
    'orderCol' : 'order',
    'plotStyle' : 'line',
    'showLegend' : 'always', 
    'plotTitle' : 'Sample Merged Spectra Table'
   }, 'divname');
```

XY Plot supports the same table formats as Table does:

 - IPAC Table file format
 - CSV - first row should be column headers
 - TSV - first row should be column headers
 - FITS Tables

###Histogram Visualization

Usage: firefly.showHistogram((parameters, div)

 - parameters are an object attributes
 - div is the div to load the XY Plot into.

Parameters object literal can contain the following values

| Parameter | Description |
| ------------ | ------------- |
| data | required; bin data - 2 dimensional array, each row has 3 columns: first col - number of points in the bin, second col - binMin, third col - binMax. Gaps and variable bin size are supported.|
| descr |title of the histogram |
| binColor | hex color  |
| height | height of the histogram, default is 400px |
| logs | optional; 'x', 'y', or 'xy' - make the specified axes logarithmic|
| reversed | optional; 'x', 'y', or 'xy' - reverse the specified axes |


*example-*
```js
firefly.showHistogram(
    {
        'descr' : 'sample histogram',
        'binColor' : '#bdbdbd',
        'height' : 200,
        // specify the data to plot histogram
        // for now it's an array of rows,
        // first col - number of points in the bin, second col - binMin, third col - binMax
        'data': [
            [1,-2.5138013781265,-2.0943590644815],
            [4,-2.0943590644815,-1.8749167508365],
            [11,-1.8749167508365,-1.6554744371915],
            [12,-1.6554744371915,-1.4360321235466],
            [18,-1.4360321235466,-1.2165898099016],
            [15,-1.2165898099016,-1.1571474962565],
            [20,-1.15714749625658,-0.85720518261159],
            [24,-0.85720518261159,-0.77770518261159],
            [21,-0.77770518261159,-0.55826286896661],
            [36,-0.55826286896661,-0.33882055532162],
            [40,-0.33882055532162,-0.11937824167663],
            [51,-0.11937824167663,0.10006407196835],
            [59,0.10006407196835,0.21850638561334],
            [40,0.21850638561334,0.31950638561334],
            [42,0.31950638561334,0.53894869925832],
            [36,0.53894869925832,0.75839101290331],
            [40,0.75839101290331,0.9778333265483],
            [36,0.9778333265483,1.1972756401933],
            [23,1.1972756401933,1.4167179538383],
            [18,1.4167179538383,1.6361602674833],
            [9,1.6361602674833,1.8556025811282],
            [12,1.8556025811282,2.0750448947732],
            [0,2.0750448947732,2.2944872084182],
            [4,2.2944872084182,2.312472786789]
        ]        
    },'divname'
);
```



###Adding Context Extensions to FITS viewer

Context extensions make it possible to add user-defined actions on certain operations. When an extension is added, FITS viewer will present an extra menu item in the context menu of the operation, on which the extension is defined. These are the operations on which context extensions can be added:

  - Area Select (square)
  -  Line Select
  - Point Select
  - Circle Select (*coming soon*)

The best way to describe how to add an extension, is to see the code.

```js
  var extFunc= function(data) {
      // do something when the extension is selected called.
  }

 var extension= {  // object literal to create extension definition
                id : "MySpecialExt",       // extension id
                plotId : "primaryID",      // plot to put extension on
                title : "Get Quadrant",    // title use sees
                toolTip : "a tool tip",    // tooltip
                extType: "POINT",          // type of extension
                callback: extFunc          // function (defined above) for callback
            };

 // get the actions object and call extension add
var actions= firefly.appFlux.getActions('ExternalAccessActions');
actions.extensionAdd(extension);
```



To add an extension to a fits viewer create a object literal with the following fields.

| name | type | description |
| ---- | ---- | ----- |
| id   | string | any string id that you want to give the extension |
| plotId | string | the plot ID to put the extension on.  (will be the same as the div name)|
| imageURL | string, url |url of an image icon (icon should be 24x24) to show in the context menu | 
| title | string | title that the user will see if not image icon is supplied |
| toolTip | string | tooltip the viewer will use for your extension |
| extType | string | extension type, must be 'AREA_SELECT', 'LINE_SELECT', 'POINT', or 'CIRCLE_SELECT' (*details below*) |
| callback | function | the function to call when the extension is selected (*details below*) |

 extType details:
 
  - 'AREA_SELECT' - When the user draws a square this menu will be activate
  - 'LINE_SELECT' - When the user draw a line this menu will be activated. 
  - 'POINT' - When any point on the plot is clicked
  - 'REGION_SELECT' - When the user selects a region 
  - 'CIRCLE_SELECT' - When the user draws a circle (*not yet supported, coming soon*)
  - 'PLOT_MOUSE_READ_OUT' - When the user moves or pauses his mouse over the fits viewer

callback function takes one parameter,  an object literal, the fields vary depend on the extension type-
 *todo - need to document callback object literal parameters*



###Getting Events

*todo put event docs here*

###Connecting FITS Viewers to table

If you have created a table, and that table has image metadata so that a line in the table references a fits file, then you can connect a FITS ImageViewer to the table.

`firefly.addDataViewer(params, div)` 

| parameters | type        |
| ---------- | ----------- |
|params      | object literal |
|div         | string, the div id to put the image viewer into |

parameters:

Plotting parameters for FITS Viewer plus additional parameters. In addition to the standard ImageViewer parameters the following are also supported.

 - **QUERY_ID**: Required. This is the string that connects this DataViewer to the table.
                            It should be the same string that you specified as the div parameter when you created the table
 - **DataSource**: Required. How the fits file is accessed. Can be URL, .....
 - **DataColumn**: Required. Column where to find the file name
 - **MinSize**: Required. This parameter is required but will probably not be necessary in the future. Needs
                            to the a width and height in the form "widthXheight". 
                            For example "100X100".
    
```js
firefly.showTable(
   {'source' : 'http://web.ipac.caltech.edu/staff/roby/demo/wd/WiseQuery.tbl'},
   'tableHere');

firefly.addDataViewer( {'DataSource'  : 'URL',
                        'DataColumn'  : 'file',
                        'MinSize'     : '100x100',
                        'ColorTable'  : '10',
                        'RangeValues' : firefly.serializeRangeValues(
                                                     "Sigma",-2,8,"Linear"),
                        'QUERY_ID'    : 'tableHere'  },
                                   'previewHere' );
```

###Connecting Coverage plot to table

`firefly.addCoveragePlot(params, div)` - add a coverage plot to a div

| Parameters | Type        |
| ---------- | ----------- |
|params      | object literal |
|div         | string, the div to put the image viewer into |

param:

Plotting parameters for FITS Viewer plus additional parameters. In addition to the standard ImageViewer parameters the following are also required.

 - **QUERY_ID**: Required. This is the string that connects this DataViewer to the table.
                            It should be the same string that you specified as the div parameter when you created the table
 - **MinSize**: Required. This parameter is required but will probably not be necessary in the future. Needs
                            to the a width and height in the form "widthXheight". 
                            For example "100X100".
                            
Coverage is computed by looking at columns in the tables. The coverage can show coverage for point sources by showing an x for the RA and Dec. 
It can also show a box if the four corners are specified. 
If you use the default column names you do not have to specify how to determine the center or the four corners.

 - **CornerColumns**:  Determines if the coverage will use the box style and what corners will be used.
                            Should be specified as a string with the values comma separated. For example-
                            "ra1,dec1,ra2,dec2,ra3,dec3,ra4,dec4". If this parameter is not specified then the example
                            is the default.
                            If you specify "CornerColumns" then you must also specify "CenterColumns".
 - **CenterColumns**:   Determines if the coverage will use the x style (if "CornerColumns" is not specified)
                            and what is the center point.
                            Should be specified as a string with the values comma separated. For example-
                            "ra,dec". If this parameter is not specified then the example is the default.
                            
```js
firefly.showTable({"source" : "http://web.ipac.caltech.edu/staff/roby/demo/wd/WiseQuery.tbl"},
                   "tableHere");

firefly.addCoveragePlot({"QUERY_ID" : "tableHere",
                         "CornerColumns" : "lon1,lat1,lon2,lat2,lon3,lat3,lon4,lat4",
                         "CenterColumns" : "lon,lat",
                         "MinSize"    : "100x100" },
                         "coverageHere" );
```                                     


###Connecting XY Viewers to table

`firefly.addXYPlot(params, div)` - add an XY Plot to a div

| parameters | type        |
| ---------- | ----------- |
|params      | object literal |
|div         | string, the div to put the image viewer into |


param:

 - **QUERY_ID**: Required. This is the string that connects this XY Plot to the table.
                     It should be the same string that you specified as the div 
                     parameter when you created the table.
 - **xCol**:     The name of x column, can be an expression based on multiple columns. 
                     If no column is specified, the first numeric column is used as an x column.
 - **yCol**:     The name of y column, can be an expression based on multiple columns. 
                      If no column is specified, the first numeric non-x column is used as a y column. 


```js
firefly.showTable({"source" : "http://web.ipac.caltech.edu/staff/roby/demo/wd/WiseQuery.tbl",
                                           "type" : "selectable"}, "tableHere");
firefly.addXYPlot({"QUERY_ID" : "tableHere",
                   "xCol" : "frame_num",
                   "yCol" : "band"}, "xyPlotHere" );
```

###More Code Examples

[see fftools-api-code-examples.md](fftools-api-code-examples.md)
