# JavaScript Firefly Tools API

Firefly tools is an API that can be used from JavaScript. High-level API allows to render the main components of Firefly and make them share the same data model. Low-level API gives direct access to Firefly React components and application state. Firefly tools API also allows to dispatch or add a listener to virtually any action, available to Firefly internally. This makes it possible to extend Firefly by executing custom code on various UI events, like mouse move or region selection.

The main Firefly components are:

 - [FITS Visualizer](#fits-visualization)
 - [Table](#table-visualization)
 - [Charts](#xy-plot-and-histogram-visualization)
   
These components can be setup to share the same data model.  Therefore you can do the following combinations:
 
 - [Connect FITS viewer coverage image to a table](#connecting-coverage-image-to-table)
 - [Connect XY plot to a table](#connecting-charts-to-table). A Table with any data and a XY Plot showing plots from any two columns of that table.
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
 - [Adding Context Extensions to FITS viewer](#adding-context-extensions-to-fits-viewer)
 - [Region Support](#region-support)


### Starting Firefly Tools in JavaScript
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

### Remote Firefly Tools Interface
 
The interface to remotely communicate to the firefly viewer:

`firefly.getViewer()` - gives a handle to launch the firefly tools in another browser window or tab
 
| Parameter  | type | Description |
| ---------- | ---- | ----------- |
| `channel` | string |the channel id string, if not specified then one will be generated |
| `file` | string | file the html of the viewer to launch. In time there will be several |
| *return*  | object | viewer interface |


### Rendering UI components

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

<br>
*Example:* 
```js
const props = {
    // data is an array of rows,
    // first col - nPoints or number of points in the bin, second col - binMin, third col - binMax
    // supports variable length bins
    data : [
        [1,1,10],
        [10,10,100],
        [100,100,1000],
        [1000,1000,10000],
        [100,10000,100000],
        [10,100000,1000000],
        [1,1000000,10000000]
    ],
    width: 400,
    height: 200,
    logs: 'xy',
    desc: 'Both X and Y are using log scale',
    binColor: '#336699'
};
// render Histogram component in the div with 'histogramHere' id
firefly.util.renderDOM('histogramHere', firefly.ui.Histogram, props);
```


### Dispatching and watching actions

Firefly is built around [React](https://facebook.github.io/react/docs/why-react.html)/[Redux](http://redux.js.org) paradigm. The idea of React is that it will always render the state of your component. 
Redux manages that state of your application. The data in the state is immutable. It cannot change directly. 
When the program fires an action describing the change to the data, a new version of the state is created with the changes. 
Then React re-renders the parts that are different. Therefore, application goes from one state change to the next.

![diagram](https://cask.scotch.io/2014/10/V70cSEC.png) 

Being able to dispatch Firefly actions and listen to them makes it possible to write new code, which could control Firefly widgets or interact with them. 

An external script would trigger UI change by calling an action dispatcher and react to UI action by adding a listener to this action.
 
##### **firefly.util.addActionListener(actionType,callBack)**

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


### Other Utility Methods 

The rest of Firefly utilities are split into the following modules by function:

- `firefly.util.image` - image utilities
- `firefly.util.table` - table utilities
- `firefly.util.chart` - chart utilities



### FITS Visualization

A FITS image viewer is created by calling the following method:

 - `firefly.showImage()`  - shows FITS image viewer in a div

    
| Parameter  | type | Description |
| ---------- | ---- | ----------- |
| `targetDiv` | string |target div to put the image in |
| `request` | object | the request object literal with the plotting parameters |


- `setGlobalImageDef()`  - set global fallback params for every image plotting call  


| Parameter  | Description |
| ---------- | ----------- |
| object     | params a object literal such as any image plot or showImage uses |



The FITS viewer can take many, many possible parameters.  Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server. Others control the zoom, stretch, and color, title, and default overlays. There are also parameters to pre-process an image, such as crop, rotate or flip. You can also specify three color parameters and the associated files.

For the details of FITS plotting parameters see: [see fits-plotting-parameters.md](fits-plotting-parameters.md)
 
<br>
*Examples:* 
```js
firefly.setGlobalImageDef({
    ZoomType  : 'TO_WIDTH'
} );
firefly.showImage('myDiv', {
    URL      : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
    Title    : 'Some WISE image'
});
```
*or*
```js
firefly.showImage('myDiv', {
    Type      : 'SERVICE',
    plotGroupId : 'myGroup',
    Service  : 'WISE',
    Title     : 'Wise',
    GridOn     : true,
    SurveyKey  : 'Atlas',
    SurveyKeyBand  : '2',
    WorldPt    : '10.68479;41.26906;EQ_J2000',
    SizeInDeg  : '.12',
    RangeValues : firefly.util.image.RangeValues.serializeSimple('Sigma',-2,8,'Linear'),
    AllowImageSelection : true 
})
```

### Deprecated ImageViewer interface

The following `ImageViewer` interface is deprecated. Please use `firefly.showImage()` instead

 - `firefly.makeImageViewer()`  - makes an inline image viewer for a html document
 - `firefly.getExpandedViewer()`  - makes an image viewer to fill the screen in a browser tab or window, little used feature.
 - `firefly.getExternalViewer()` - makes an image viewer which will run in a browser tab or window. It is used to plot an image.

Deprecated `ImageViewer` methods:

| Method | Description |
| ---------- | ----------- |
| `plot()` | plot a FITS image |
| `plotURL()` | convenience method to plot an image referenced by url |
| `plotFile()` | convenience method to plot an image file on the server |
| `plotFileOrURL()` | convenience method to plot an image referenced by file or url |
| `setDefaultParams()`  |  set parameters that will apply to  `plot` calls |



### Utility methods for FITS visualization 

##### **firefly.util.image.getPrimePlot** method

`firefly.util.image.getPrimePlot(plotId)` - get the plot object by its plot id

If `plotId` is omitted, active plot is returned. 

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| plotId | string | plot id |

`plotState` property of the plot object gives access to the information about current state of the plot, such as zoom level, rotation information, working and original FITS file names, etc. 

*Example:*
```js
function getWorkingFitsFile(pt) {
    const activePlot = firefly.util.image.getPrimePlot();
    const plotState = activePlot.plotState;
    return plotState.getWorkingFitsFileStr();
}
```

##### **firefly.util.image.CCUtil**

`firefly.util.image.CCUtil` - coordinate conversion utilities

- `firefly.util.image.CCUtil.getImageCoords(plot,pt` - get image coordinates of the point
- `firefly.util.image.CCUtil.getWorldCoords(plot,pt)` - get world coordinates of the point
- `firefly.util.image.CCUtil.getViewPortCoords(plot,pt)` - get viewport coordinates of the point
- `firefly.util.image.CCUtil.getScreenCoords(plot,pt)` - get screen coordinates of the point

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| plot | Object | Firefly plot object |
| pt  | Object | Firefly point object |

*Example:*
```js
function getImagePt(pt) {
    const activePlot = firefly.util.image.getPrimePlot();
    return pt ? firefly.util.image.CCUtil.getImageCoords(activePlot, pt) : undefined;
}
```

##### **firefly.util.image.serializeSimpleRangeValues** method

`firefly.util.image.serializeSimpleRangeValues(stretchType,lowerValue,upperValue,algorithm)` - serialize a stretch request into a string, for use with the "RangeValues" parameter

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| stretchType | string | the type of stretch may be 'Percent', 'Absolute', 'Sigma' |
| lowerValue  |number | lower value of stretch, based on stretchType |
| upperValue  |number | upper value of stretch, based on stretchType |
| algorithm | string | algorithm the stretch algorithm to use, may be 'Linear', 'Log', 'LogLog', 'Equal', 'Squared', 'Sqrt' |
| *return* | string | a serialized version of range values to be passed as a viewer parameter |

*Example:*                               
```js
firefly.showImage('imageDiv', {
    plotId: 'p1',
    Service: 'TWOMASS',
    Title  : '2MASS from service',
    SurveyKey  : 'k',
    WorldPt    : '10.68479;41.26906;EQ_J2000',
    RangeValues : firefly.util.image.serializeSimpleRangeValues("Sigma",-1,2,"Linear"),
    SizeInDeg  : '.12'
});
```

`firefly.util.image.initAutoReadout(readoutComponent, props)` - initialize the auto readout.

To use minimal readout, do the following:

*Example:*                               
```js
    firefly.util.image.initAutoReadout(ui.DefaultApiReadout,
        {
            MouseReadoutComponent:firefly.ui.PopupMouseReadoutMinimal, 
            showThumb:false,
            showMag:false
        }
    );
```


### Adding Context Extensions to FITS viewer

Context extensions make it possible to add user-defined actions on certain operations. When an extension is added, FITS viewer will present an extra menu item in the context menu of the operation, on which the extension is defined. These are the operations on which context extensions can be added:

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
                id : 'MySpecialExt',       // extension id
                plotId : 'primaryID',      // plot to put extension on
                title : 'My Op',           // title use sees 
                toolTip : "a tool tip",    // tooltip
                extType: "POINT",          // type of extension
                callback: extFunc          // function (defined above) for callback
            };
 firefly.util.image.extensionAdd(extension);
 // to remove the extension added above, use firefly.util.image.extensionRemove('MySpecialExt') 
```


To add an extension to a fits viewer create a object literal with the following fields:

| name | type | description |
| ---- | ---- | ----- |
| id   | string | any string id that you want to give the extension |
| plotId | string | the plot ID to put the extension on.  (will be the same as the div name)|
| imageUrl | string, url |url of an image icon (icon should be 24x24) to show in the context menu | 
| title | string | title that the user will see if not image icon is supplied |
| toolTip | string | tooltip the viewer will use for your extension |
| extType | string | extension type, must be 'AREA_SELECT', 'LINE_SELECT', 'POINT', or 'CIRCLE_SELECT' (*details below*) |
| callback | function | the function to call when the extension is selected (*details below*) |

 extType details:
 
  - 'AREA_SELECT' - When the user draws a square this menu will be activate
  - 'LINE_SELECT' - When the user draw a line this menu will be activated. 
  - 'POINT' - When any point on the plot is clicked 
  - 'CIRCLE_SELECT' - When the user draws a circle (*not yet supported, coming soon*)

callback function takes one parameter,  an object literal, the fields vary depend on the extension type -
 *todo - need to document callback object literal parameters*


### Region Support

##### **firefly.action.dispatchCreateRegionLayer** method

`firefly.action.dispatchCreateRegionLayer(regionId, layerTitle, fileOnServer ='', regionAry=[], plotId = [], selectMode)` - overlay region data on an image plots with the given ids

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionId | string | region layer id |
| layerTitle | string | title for the layer, displaying regions |
| fileOnServer | string | region file on the server |
| regionAry | array | an array of strings, each describing a ds9 region |
| plotId | string or array | a plot id or an array of plot ids |
| selectMode | object | rendering features for the selected region |

Note: if no plotId is given, the region layer is created on all plots.


##### **firefly.action.dispatchDeleteRegionLayer** method

`firefly.action.dispatchDeleteRegionLayer(regionId, plotId)` - remove region layer from the given plot

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionId | string | region layer id |
| plotId | string or array | a plot id or an array of plot ids |

Note: if no plotId is given, the region layer is removed from all plots.


##### **firefly.action.dispatchAddRegionEntry** method

`firefly.action.dispatchAddRegionEntry(regionId, regionChanges, plotId=[], layerTitle='', selectMode = {})` - add region data to the given region layer

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionId | string | region layer id |
| regionChanges | array | an array of strings, each describing a ds9 region |

Other parameters are optional drawing layer creation parameters, similar to those of `firefly.action.dispatchCreateRegionLayer`.
It is possible to create region layer by adding the first region to it.



##### **firefly.action.dispatchRemoveRegionEntry** method

`firefly.action.dispatchRemoveRegionEntry(regionId, regionChanges)` - remove region data from the given region layer

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| regionId | string | region layer id |
| regionChanges | array | an array of strings, each describing a ds9 region |

When the last region of the drawing layer is removed, the drawing layer is automatically deleted.
 
 
##### **firefly.action.dispatchSelectRegion** method

`firefly.action.dispatchSelectRegion(drawLayerId, selectedRegion)` - select region from a drawing layer

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| drawLayerId | string | region layer id |
| selectedRegion | string | string describing ds9 region |


##### **firefly.util.image.getSelectedRegion** method

`firefly.util.image.getSelectedRegion(drawLayerId)` - get ds9 string for the selected region in the given drawing layer. The returned string will always have coordinate system (image, physical, j2000, ...) in the front.

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| drawLayerId | string | region layer id |

*Example:*                               
```js
function onRegionSelect(action) {
    if (action.payload.selectedRegion !== null) {
        const regionStr = firefly.util.image.getSelectedRegion(action.payload.drawLayerId);
        console.log('Selected region string: '+regionStr);
    }
}
function onFireflyLoaded() {
    const req = {
        plotId: 'image1',
        URL: 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
        Title: 'WISE m51'
    };
    firefly.showImage('image1_div', req);
    // parameters to dispatchCreateRegionLayer: regionId, layerTitle, fileOnServer ='', regionAry=[], plotId = []
    const regionAry = [
        'image; box 250 250 100 80 0 # color=red',
        'image; box 250 250 50 40 0 # color=red'
    ];
    firefly.action.dispatchCreateRegionLayer('region1', 'Region Layer 1', null, regionAry, ['image1', 'image2']);
    // add a listener to detect region selection changes
    firefly.util.addActionListener(firefly.action.type.REGION_SELECT, onRegionSelect);
}
```


### Table Visualization

Creating table request

- `firefly.util.table.makeFileRequest(title, source, alt_source, options={})` - make a table request from a file
- `firefly.util.table.makeTblRequest(id, title, params={}, options={}`
- `firefly.util.table.makeIrsaCatalogRequest(title, project, catalog, params={}, options={})`

options - table request options, object with the following properties:


| Property  | Type | Description |
| ---------- | ---- | ----------- |
| startIdx | number | the starting index to fetch, defaults to zero |
| pageSize  | number | the number of rows per page, defaults to 100 |
| filters  | string | list of conditions separted by comma`(,)`. Format:  `(col_name|index) operator value. Operator is one of '> < = ! >= <= IN'` |
| sortInfo | string | sort information.  Format:  `(ASC|DESC),col_name[,col_name]*` |
| inclCols | string | list of columns to select: column names separted by comma `(,)`|
| decimate | string | decimation information |
| META_INFO | object | meta information passed as key/value pair to server then returned as tableMeta |
| use | string | optional, one of 'catalog_overlay', 'catalog_primary', 'data_primary' |
| tbl_id | string | a unique id of a table, auto-created if not given |

- `firefly.showTable(targetDiv, request, options)`

Parameters ia an object literal.  div is the div to load the table into.
Below is a list of all possible parameters.


| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| targetDiv | string | target div to put the table in |
| request  | object | request object the table is created from |
| options  |number | table options |

options -  table options, object with the following properties:


| Property  | Type | Description |
| ---------- | ---- | ----------- |
| tbl_group | string | the group this table belongs to, defaults to 'main' |
| removable  | boolean | true if this table can be removed from view, defaults to true |
| showUnits  | boolean | defaults to false |
| showFilters | boolean | defaults to false |
| selectable | boolean | defaults to true |
| expandable | boolean | defaults to true |
| showToolbar | boolean | defaults to true |
| border | boolean | defaults to true |


*Example:*
```js
tblReq = firefly.util.table.makeFileRequest(null, 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',null,
            { 
                pageSize: 15,
                META_INFO: {CENTER_COLUMN: 'crval1;crval2;EQJ2000'}
            });
firefly.showTable('table_div', tblReq);
```
The Table tools currently supports the following file formats:

 - IPAC Table file format
 - CSV - first row should be column headers
 - TSV - first row should be column header
 - FITS Tables



### XY Plot and Histogram Visualization

- `firefly.showXYPlot(targetDiv, parameters)`
- `firefly.showHistogram(targetDiv, parameters)`

| Parameter  | Type | Description |
| ---------- | ---- | ----------- |
| targetDiv | string | target div to put the XY Plot in |
| parameters  | object | XY plot parameters |


XYPlot parameters object literal can contain the following values.

| Parameter | Description |
| ------------ | ------------- |
| source | location of the ipac table: url or file path.|
| xCol | column or expression to use for x values, can contain multiple column names ex. log(col) or (col1-col2)/col3 |
| yCol | column or expression to use for y values, can contain multiple column names ex. log(col) or (col1-col2)/col3 |
| xyRatio | optional; aspect ratio between 1 and 10, if not defined the chart will fill all available space |
| stretch | 'fit' to fit plot into available space or 'fill' to fill the available width; applied when xyPlotRatio is defined |
| xLabel | label to use with x axis |
| yLabel | label to use with y axis |
| xUnit | unit for x axis |
| yUnit | unit for y axis |
| xOptions | comma separated list of x axis options: grid,flip,log |
| yOptions | comma separated list of y axis options: grid,flip,log |


Histogram parameters object literal can contain the following values.

| Parameter | Description |
| ------------ | ------------- |
| source | location of the ipac table: url or file path.|
| col | column or expression to use for histogram, can contain multiple column names ex. log(col) or (col1-col2)/col3 |
| xyRatio | optional; aspect ratio between 1 and 10, if not defined the chart will fill all available space |
| numBins | number of bins for fixed bins algorithm (default) |
| falsePositiveRate | false positive rate for bayesian blocks algorithm |
| xOptions | comma separated list of x axis options: flip,log |
| yOptions | comma separated list of y axis options: flip,log |

*Example:*
```js
chartParams = {
    source: 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',
    xCol: 'ra1',
    yCol: 'dec1'
};
firefly.showXYPlot('chart_div', chartParams);
```

XY Plot and Histogram support the same table formats as Table:

 - IPAC Table file format
 - CSV - first row should be column headers
 - TSV - first row should be column headers
 - FITS Tables


### Connecting Coverage image to table

`firefly.showCoverage= (div,options)` - add a coverage image to a div

| Parameters | Type        |
| ---------- | ----------- |
|div         | string, the div to put the image viewer into |
|params      | object literal, plotting parameters for FITS Viewer |
 
                            
Coverage is a an image, which includes all points of a catalog. Coverage image size is computed by looking at columns in the tables. If you use the default column names you do not have to specify how to determine the center or the four corners.

 - **ALL_CORNERS**:   Determines if the coverage will use the box style and what corners will be used.
                        Should be specified as a string with the values comma separated. For example-
                        "ra1,dec1,ra2,dec2,ra3,dec3,ra4,dec4". If this parameter is not specified then the example
                        is the default.
                        If you specify CORNER_COLUMNS then you must also specify CENTER_COLUMN.
 - **CENTER_COLUMN**: Determines if the coverage will use the x style (if CORNER_COLUMNS is not specified)
                        and what is the center point.
                        Should be specified as a string with the values comma separated. For example-
                        "ra,dec". If this parameter is not specified then the example is the default.
                            
                            
```js
const tblReq = firefly.util.table.makeFileRequest(null, 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',null,
                    { 
                        META_INFO: {
                            CENTER_COLUMN: 'crval1;crval2;EQJ2000'
                        }
                    });
firefly.showTable('table_div', tblReq);
firefly.showCoverage('coverage_div', {gridOn:true})
``` 
                                   
You can show point sources on your coverage image by telling Firefly that the table should be used as a catalog overlay and which columns define RA and Dec. When `CatalogCoordColumns` attribute is present, `CENTER_COLUMN` is optional.

```js
const tblReq = firefly.util.table.makeFileRequest(null, 'http://web.ipac.caltech.edu/staff/roby/demo/WiseDemoTable.tbl',null,
                    { 
                        META_INFO: {
                            CatalogOverlayType: 'WISE', 
                            CatalogCoordColumns: 'crval1;crval2;EQJ2000'
                        }
                    });
firefly.showTable('table_div', tblReq);
firefly.showCoverage('coverage_div', {gridOn:true})
``` 

### Connecting Charts to table

`firefly.showXYPlot(targetDiv, parameters)` - add an XY Plot to a div
`firefly.showHistogram(targetDiv, parameters)` - add a Histogram to a div

Multiple charts can be connected to the same table.

| parameters | type        |
| ---------- | ----------- |
|targetDiv   | string, the div to put the XY plot into |
|parameters  | object literal |

The most important parameters, relevant to all charts, connected to a table are:

 - **`tbl_id`**:   Table ID, the string that connects this XY Plot to the table data.
 - **`tbl_group`**: Table group, when specified instead of `tbl_id`, the plot will reflect the data in the active table of the group.                    
                      
If both `tbl_id` and `tbl_group` are missing, the XY viewer will be connected to the active table in the main group.

Please, see `firefly.showXYPlot` and `firefly.showHistogram` for the list of other parameters.

*Example:*
```js
tblReq = firefly.util.table.makeIrsaCatalogRequest('wise catalog', 'WISE', 'allwise_p3as_psd',
    {   
        position: '10.68479;41.26906;EQ_J2000',
        SearchMethod: 'Cone',
        radius: 300
    });
firefly.showXYPlot('xyplot_div', {tbl_id: tblReq.tbl_id, xCol: 'w1mpro+w4mpro', yCol: 'w2mpro'});
firefly.showHistogram('histogram1_div', {tbl_id: tblReq.tbl_id, col: 'w1mpro+w4mpro'});
firefly.showHistogram('histogram2_div', {tbl_id: tblReq.tbl_id, col: 'w2mpro'});
```


### More Code Examples

[see firefly-api-code-examples.md](../tutorial/firefly-api-code-examples.md)
