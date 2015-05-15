# JavaScript Firefly Tools API

Firefly tools is an API that can be use from JavaScript. It allows you to user the main components of Firefly via an API. The following components are available.

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
 

###FITS Visualization

The following methods are available to create a FITS image viewer.  A FITS `ImageViewer` is created or referenced by calling the following

 - `firefly.makeImageViewer()`  - make an inline image viewer for a html document
 - `firefly.getExpandViewer()`  - makes an image viewer that will popup in the html document.
 - `firefly.getExternalViewer()` - gives a handle to launch the firefly tools web applications with a specified FITS file.


#### <b>ImageViewer

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



#####<b> ImageViewer.plot() method

The following is a list of possible parameters for the ImageViewer plotting. Almost all parameters are optional.
 Note that the request `Type` parameter can
 be set specifically or it is implied from the `File`, `URL` or `Service` parameters which are mutually exclusive.

| Parameter  | Description |
| ---------- | ----------- |
| object     | object literal with name/value pairs for all parameters |

 
 - **Type**: Set the type of request. Based on the Type then one or more other parameters are required.
Options are:
    - `SERVICE`, for an image service
    - `FILE` for file on the server
    - `URL` for any url accessible FITS image file
    - `TRY_FILE_THEN_URL` try a file on the server first then try the url
    - `BLANK` make a blank image
    - `ALL_SKY`

 - **File**: File name of a file on the server. Required if Type=='FILE' or if you want to plot a file on the server.
 - **URL**: Retrieve and plot the file from the specified URL. Required if Type=='URL' or if you want to plot an image referenced by URL.
 The url can be absolute or relative. If it is relative, one of two things happen:
    - The url is made absolute based on the root path set in the method `firefly.setRootPath(path)` .      
    - The url is made absolute based on the host web page url. 
    
 - **Service**
    - Available services are: IRIS, ISSA, DSS, SDSS, TWOMASS, MSX, DSS_OR_IRIS, WISE.
Required if Type=='SERVICE' or if you want to use a service
 - **WorldPt**:  This is target for service request for `Type=='SERVICE'`.
    - WorldPt uses the format "12.33;45.66;EQ_J2000" for j2000.
    - The general syntax is `lon;lat;coordinate_sys`, e.g. `'12.2;33.4;EQ_J2000'` or `'11.1;22.2;GALACTIC'`
    - coordinate system can be: `'EQ_J2000'`, `'EQ_B1950'`, `'EC_J2000'`, `'EC_B1950'`, `'GALACTIC'`, or `'SUPERGALACTIC'`;
 - **SizeInDeg**  The radius or side (in degrees) depending of the service type, used with `Type=='SERVICE'`
 -  **SurveyKey**:  Required if `Type=='SERVICE'`
The value of SurveyKey depends on the value of "Service".
The possible values for SurveyKey are listed below for each service:        
    - IRIS: 12, 25, 60, 100
    - ISSA: 12, 25, 60, 100
    - DSS: poss2ukstu_red, poss2ukstu_ir, poss2ukstu_blue, poss1_red, poss1_blue, quickv, phase2_gsc2,  phase2_gsc1
    - SDSS: u, g, r, i, z
    - TWOMASS: j, h, k
    - MSX: 3, 4, 5, 6
    - WISE: 1b, 3a
    </td>
 - **SurveyKeyBand**: So far only used with `'Type===SERVICE'` and `'Service===WISE'`. Possible values are: 1, 2, 3, 4
 - **ZoomType**:  Set the zoom type, based on the ZoomType other zoom set methods may be required
Notes for ZoomType:
    - STANDARD - default, when set, you may optionally define `'InitZoomLevel'` or the zoom will default to be 1x
    - TO_WIDTH - you must define <code>ZoomToWidth</code> and set a pixel width</li>
    - FULL_SCREEN - you must define <code>ZoomToWidth</code> with a width and `'ZoomToHeight'` with a height
    - ARCSEC_PER_SCREEN_PIX - you must define <code>ZoomArcsecPerScreenPix</code></li>
 - **InitZoomLevel**: The level to zoom the image to. Used with ZoomType=='STANDARD' (which is the default).  <br>Example:  .5,2,8,.125
 - **ZoomToWidth**: used with ZoomType=='TO_WIDTH or ZoomType=='FULL_SCREEN', this is the width in pixels. </td>
 - **ZoomToHeight**: used with "ZoomType==FULL_SCREEN", this is the height in pixels. </td>
 - **TitleOptions**:  Set other ways to title the plot. Options for title:
    - NONE - The default, use the value set in <code>Title</code>, if this is empty use the plot description that come from the server
    - PLOT_DESC - Use the plot description set by the server. This is meaningful when the server is using a service, otherwise it will be an empty string. <i>example-</i> 2mass or IRIS
    - FILE_NAME - Use the name of the FITS file. This is useful when plotting an uploaded file or a URL.
    - HEADER_KEY - Use the value of a FITS header name key.  This parameter <code>HeaderForKeyTitle</code> must be set to the card name.
    - PLOT_DESC_PLUS - Use the server plot description but append some string to it.  The string is set in `'PlotDescAppend'`

 - **Title**: Title of the plot
 - **PostTitle**: A String to append at the end of the title of the plot. This parameter is useful if you are using one of the computed <code>TitleOpions</code> such as <code>FILE_NAME</code> or <code>HEADER_KEY</code></td>
 - **PreTitle**: A String to append at the beginning of the title of the plot. This parameter is useful if you are using one of the computed <code>TitleOptions</code> such as <code>FILE_NAME</code> or <code>HEADER_KEY</code> </td>
 - **TitleFilenameModePfx**: A String to replace the default "from" when <code>TitleMode</code> is <code>FILE_NAME</code>, and the mode is <code>URL</code>.
    If the url contains a fits file name and there are more options then the firefly viewer added a "from" to the front of the title.
    This parameter allows that string to be changed to something such as "cutout".
 - **PlotDescAppend**: A string to apppend to the end of the plot description set by the server.  This will be used as the plot title if the <code>TitleOptions</code> parameter is set to <code>PlotDescAppend</code>. </td>
 - **RotateNorth**: Plot should come up rotated north, should be "true" to rotate north</td>
 - **RotateNorthType**: coordinate system to rotate north on, options: EQ_J2000, EQ_B1950, EC_J2000, EC_B1950,
        GALACTIC, or SUPERGALACTIC"
 - **Rotate**: set to rotate, if "true", the angle should also be set</td>
 - **RotationAngle**: the angle to rotate to, use with "Rotate"</td>
 - **FlipY**: Flip this image on the Y axis</td>
 - **HeaderKeyForTitle**: Use the value of a specified header for the title of the plot, use with multi image FITS files
 - **RangeValues**: A complex string for specify the stretch of this plot. Use the method firefly.serializeRangeValues() to produce this string
 - **ColorTable**: value 0 - 21 to represent different predefined color tables</td>

 - **PostCrop**: Crop and center the image before returning it. If rotation is set then the crop will happen post rotation.
Note: `SizeInDeg` and `WorldPt` are required to do `PostCropAndCenter`
 - **CropPt1**: One corner of the rectangle, in image coordinates, to crop out of the image, used with CropPt2 CropPt1 and CropPt2 are diagonal of each other
Syntax is "x;y" example: 12;1.5
 - **CropPt2**: Second corner of the rectangle, in image coordinates, to crop out of the image, used with `'CropPt1'`
`CropPt1` and `CropPt2` are diagonal of each other
Syntax is "x;y" example: 12;1.5
 - **CropWorldPt1**: One corner of the rectangle, in world coordinates, to crop out of the image, used with `'CropPt2'`
`CropPt1` and `CropPt2` are diagonal of each other.
Note-  See documentation on WorldPt to find proper syntax
 - **CropWorldPt2**: Second corner of the rectangle, in world coordinates, to crop out of the image, used with CropWorldPt1.
CropWorldPt1 and CropWorldPt2 are diagonal of each other.
Note-See documentation on WorldPt to find proper syntax
 - **ZoomArcsecPerScreenPix**: Set the zoom level so it have the specified arcsec per screen pixel. Use with
        ZoomType=='ARCSEC_PER_SCREEN_PIX' and 'ZoomToWidth'
 - **ContinueOnFail**: For 3 color, if this request fails then keep trying to make a plot with the other request
 - **ObjectName**: the object name that can be looked up by NED or Simbad</td>
 - **Resolver**: The object name resolver to use, options are: NED, Simbad, NedThenSimbad, SimbadThenNed, PTF
 - **GridOn**: Turn the coordinate grid on after the image is plotted. Normally the grid is turned on by a user action.  This option forces the grid to be on by default. Boolean value: true or false
 - **SurveyKeyAl**: TODO: Document this param</td>
 - **UserDesc**: TODO: Document this param</td>
 - **UniqueKey**: TODO: Document this param
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

See the <b>ImageViewer</b> for the details of each method.

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


#####Other Utility Methods for FITS visualization 

| Method  | parameters | Description |
| ------- | ---------- | ----------- |
|firefly.setGlobalDefaultParams(params) | a object literal such as ImageViewer.plot() uses |set global fallback params for every image plotting call |
|firefly.setRootPath(rootURLPath) |the root URL to be prepended to any relative URL. |sets the root path for any relative URL. If this method has not been called then relative URLs use the page's root.|




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

| Paremeter | Description |
| ------------ | ------------- |
| source | required; location of the ipac table.  url or file path.|
|chartTitle |title of the chart |
|xCol | required; column to use for x values (can be an expression, containing multiple column names) |
| yCol | required; column to use for y values (can be an expression, containing multiple column names) |
| errorCol | column to use for y value errors (must be column name, expressions are not supported) |
| orderCol | column to use to separate series from each other, different series are shown in different colors |
| plotStyle | line|linePoints|points, defaults to points |
| showLegend | always|onExpand, defaults to onExpand |
| plotTitle | header for the plot |


*example-*
```js
firefly.showPlot(
    {'source' : 'http://web.ipac.caltech.edu/staff/roby/demo/SPITZER_S5_3539456_01_merge.tbl'
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

###Adding Context Extensions to FITS viewer

Context extensions all the FITS viewer to present extra menu items when the FITS viewer is doing some operations.

 - Area Select (square)
 - Line Select
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
 - 'LINE_SELECT' -When the user draw a line this menu will be activated.
 - 'POINT' - When any point on the plot is clicked 
 - 'CIRCLE_SELECT' - When the user draws a circle (*not yet supported, coming soon*)

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
|div         | string, the div to put the image viewer into |

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
                        'RangeValues'  firefly.serializeRangeValues(
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

[see fftools-api-code-examples.md](#fftools-api-code-examples.md)


