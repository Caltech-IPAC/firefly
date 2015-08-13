## Firefly python wrapper user guide ##

This document explains how to use the python library included in Firefly to interact with firefly image viewer. 

The python class name is **FireflyClient**.  It is located in firefly/src/fftools/python.   

##Getting Started

####Pre-requirements

1. Install [ws4py](http://ws4py.readthedocs.org/en/latest/sources/install )
   (If you are using conda package manager, you can use [conda-pipbuild](http://conda.pydata.org/docs/commands/build/conda-pipbuild.html) tool for building conda packages just using pip install.) 
2. Set up python environment
```bash
setenv PYTHONPATH /where-git-repository-is/firefly/src/fftools/python/display
```
	
To get a feel for API `cd firefly/src/fftools/test` and take a look at `initFF.py`, `initFF2.py`, `testAll.py`. If you are within an interpreter, `execfile('./initFF2.py')` is the best, because it leaves the connection to the browser open while allowing you to execute other commands.

####Import the FireflyClient into your script

   `from FireflyClient import *`
   
####Create an instance of the FireflyClient

```python
fc = FireflyClient( 'localhost:8080')
```
 or 
```python
fc=FireflyClient('localhost:8080', channel='myNewChannel')
```
 
####Launch Browser and load Firefly Tools
 
```python
fc.launchBrowser()
```

Wait until Firefly Tools finish loading and you see 'Waiting for data' before sending other commands.
 
####Show a Fits image
 
```python
data = '/your/path/yourFits.fits'
fitsPathInfo= fc.uploadFile(data)
fc.showFits(fitsPathInfo)
```   

The FITS viewer can take many, many possible parameters.  Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server. Others control the zoom, stretch, and color, title, and default overlays. The are also parameters to pre-process an image, such as crop, rotate or flip. You can also specify three color parameters and the associated files.

For the details of FITS plotting parameters see: [see fits-plotting-parameters.md](fits-plotting-parameters.md)

      

####Overlay a region

```python 
regFile=/your/path/yourRegion.reg
regPathInfo= fc.uploadFile(regFile)
fc.overlayRegion(regPathInfo)
``` 

####Show a table
 
```python 
table ='/your/path/yourTable.tbl'    
tablePathInfo = fc.uploadFile(table)
fc.showTable(tablePathInfo)
```

####Show a XY Plot
 
```python 
table ='/your/path/yourTable.tbl'    
tablePathInfo = fc.uploadImage(table)
fc.showXYPlot(fileOnServer=tablePathInfo, additionalParams={'xColExpr' : 'col1/col2', 'yCol' : 'col3', 'plotTitle' : 'col3 vs. col1/col2'})
```
See *XY Plot Visualization* parameters in [fftools-api-overview.md](fftools-api-overview.md) for the available XY Plot parameters.


###Run in python prompt

1. start python session by typing `python` in the terminal
2. enter: `execfile("yourScript.py")` at the python prompt

###Run in iPyton

1. start the iPython session by typing `iPython` in the terminal
2. enter the script name at the ipython prompt

###Run in iPython notebook










1. open a cell
2. type : `%run yourScript.py` into the cell
3. click run


**Example** (initFF.py) 

```python

import sys
import json

# add to the path directory with the data
sys.path.append('../python/display/')

from FireflyClient import *

host='localhost:8080'

def myCallback(event):
    # print event
    print "Event Received: "+json.dumps(event['data']);

fc= FireflyClient(host,'myChannel')

fc.launchBrowser()
# make sure user waits until the browser is ready to receive events
raw_input("Wait for browser to load Firefly Tools.   Press Enter to continue...")

try:
    fc.addListener(myCallback)

    # upload FITS file
    file= fc.uploadFile('data/c.fits')
    print 'uploadFile'

    # show uploaded FITS
    status= fc.showFits(file,'p1')
    print 'showFits success: %s' % status['success']

    # add user-defined action MyOp in the context menu of 'p1' FITS viewer
    status= fc.addExtension('AREA_SELECT','MyOp','p1','MyOpExtension')
    print 'addExtension success: %s' % status['success']

    # show another FITS from the URL
    pParams= { 'URL' : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits','ColorTable' : '5'}
    status= fc.showFits(fileOnServer=None, plotID='p2', additionalParams=pParams)
    print 'showFits success: %s' % status['success']

    # wait for events - do not exit the script
    print 'Waiting for events. Press Ctrl C to exit.'
    fc.waitForEvents()  # optional, gives code a place to park when thread is done

except KeyboardInterrupt:
    fc.disconnect()
    fc.session.close()

```
	    
##FireflyClient's methods

- *addListener(self, callback, name=ALL)*
   
   Sets the **callback** function to be called when the events with specified names happen on the firefly client.

- *removeListener(self, callback, name=ALL)*

  Removes the callback on the events with specified names.

- *waitForEvents(self)*
   
   Informs the client to pause and wait for the events from the server.

- *launchBrowser(self, url=None, channel=None)*

   Launches a browser with the Firefly Tools viewer. Do not specify any parameters unless you'd like to override the defaults. 

- *disconnect(self)*

   Disconnects from the Firefly Tools server, closes the communication channel.

- *uploadFile(self, path, preLoad=True)*

   Uploads a file to the Firefly Server. The uploaded file can be fits, region, and various types of table files.

- *uploadFitsData(self, stream)*
 
   Uploads a FITS file like an object to the Firefly server. The method should allows file like data to be streamed without using an actual file.
   
- *uploadTextData(self, stream)*
 
   Uploads a text file like an object to the Firefly server. The method should allows file like data to be streamed without using an actual file.
   

- *showFits(self, fileOnServer=None, plotID=None, additionalParams=None)*

   Shows a fits image.
    **fileOnServer** - the name of the file on the server.  If you used uploadFile() then it is the return value of the method. Otherwise it is a file that firefly has direct read access to.
    **plotID** - the id you assigned to the plot. This is necessary to further controlling the plot
    **additionalParam** - dictionary of any valid fits viewer plotting parameter, see [server-settings-for-fits-files.md](fits-plotting-parameters.md)


- *showTable(self, fileOnServer, title=None, pageSize=None)*

  Shows a table.
   **fileOnServer** - the name of the file on the server.  If you used uploadFile() then it is the return value of the method. Otherwise it is a file that firefly has direct read access to.
   **title** - title on table
   **pageSize** - how many rows are shown
		 
- *showFits(self, path, plotID=None, addtlParams=None)* 
     	 
       This method will load the fits located in the **path** and display the image in  
   the IRSA viewer.
      	 
- *showTable(self, path, title=None, pageSize=None)*

   This method displays the table located in the path 
           
- *addExtension(self, extType, title, plotId, extensionId, image=None)*

   Adds an extension to the plot. Extensions are context menus that allows you extend what firefly can do when certain actions happen.
    **extType** - may be 'AREA_SELECT', 'LINE_SELECT', or 'POINT'. todo: 'CIRCLE_SELECT'
    **title** - the title that the user sees
    **plotId** - the id of the plot to put the extension on
    **extensionId** - the id of the extension
    **image** - a url of an icon to display in the toolbar instead of title

###FireflyClient's Image control Methods 

- `pan(self, plotId, x, y)`

    Pan or scroll the image to center on the image coordinates passed.

    - plotId: plotId to which this region should be added, parameter may be string or a list of strings.
    - x: number, new center x position to scroll to
    - y: number, new center y position to scroll to
    - *return* status of call

- `zoom(self, plotId, x, y)`

    Zoom the image

    - plotId: plotId to which this region should be added, parameter may be string or a list of strings.
    - factor:  number, zoom factor for the image
    - *return* status of call
 
- `stretch(self, plotId, serializedRV)`

      Change the stretch of the image

    - plotId: plotId to which this region should be added, parameter may be string or a list of strings.
    - serializedRV: the range values parameter [see Fits Stretch help methods](#fireflyclients-fits-stretch-help-methods)
    - *return* status of call

###FireflyClient's Region Methods

- `overylayRegion(path, title=None, regionLayerId=None, plotId=None)`
     
    This method overlays a region file on an image
   
       - regionId: id of region overlay to create or add too
       - title: title of the region file
       - plotId: Target plotId to overlay the region on. may be a string or a list of strings. If None the the region is overlaid on all plots.
       - *return* status of call
     
- `removeRegion(regionId=None)`
     
    This method removes a region file from an image
    
- `overlayRegionData(regionData, regionLayerId, title=None, plotId=None)`
     
    Overlay a region on the loaded FITS images
       
       - regionData: a list of region entries
       - regionId: id of region overlay to create or add too
       - title: title of the region file
       - plotId: Target plotId to overlay the region on. may be a string or a list of strings. If None the the region is overlaid on all plots.
       - *return* status of call

- `removeRegionData(regionData, regionId, title=None)`

 Remove the specified region entries
 
     - param regionData: a list of region entries
     - param regionId: id of region to remove entries from
     - *return* status of call


Examples of adding and removing:
```python
fc= FireflyClient(host,'tt') # init connection

# to Add
reg= ['physical;point 211 201 # color=pink point=cross 9', 
      'physical;point 30 280 # color=red point=diamond 15 select=1',
      'physical;point 100 180 # color=green point=cross 10 select=1'
      ]
fc.overlayRegionData(reg,'reg3', 'My Region Data')

# to remove 2 or the 3 added
delReg= ['physical;point 211 201 # color=pink point=cross 9', 
         'physical;point 100 180 # color=green point=cross 10 select=1'
         ]
fc.removeRegionData(delReg,'reg3')
```
Examples of adding a call back to listen for region selection:
```python
fc= FireflyClient(host,'tt') # init connection
def myCallback(event):
        if 'region' in event['data']:
        print 'selected region: %s' % event['data']['region']

fc.addListener(myCallback)
```

###FireflyClient's Fits Stretch Help Methods

When setting the stretch through `FireflyClient.showFits()` or `FireflyClient.stretch()` you must create a serialized range values string.

There are two helper methods that  will construct it.  They are `createRangeValuesStandard()` and `createRangeValuesZScale()`. These both returns a string that can be passed to  `showFits()` or `stretch()`

The fits viewer method's (`FireflyClient.showFits()`)  last parameter is a dictionary which you can use to specify any of the parameters that the FITS viewer accepts ([see fits-plotting-parameters.md](fits-plotting-parameters.md)). One of this parameters, `'RangeValues'` , is used to specify the stretch algorithm and parameters. This parameter is serialized string that is too difficult construct yourself.  



*Example-*

```python
rv= fc.createRangeValuesStandard('LogLog', 'Percent',9,98)
params= {'RangeValues':rv}
status= fc.showFits(file,'p1',params)
```
*or*
```python
rv= fc.createRangeValuesZScale('Log',80, 500,44)
params= {'RangeValues':rv}
status= fc.showFits(file,'p1',params)
```
*or*
```python
rv= fc.createRangeValuesZScale(`Linear','Percent', 2,94)
status= fc.stretch('p1',rv)
```
**Methods**

```python
createRangeValuesStandard(algorithm, stretchType='Percent', lowerValue=1, upperValue=99)
```

  - stretchType: must be `'Percent'`,`'Absolute'`,`'Sigma'`
  - lowerValue: number, lower end of stretch
  - upperValue: number, upper end of stretch
    - algorithm: must be `'Linear'`, `Log'`,`'LogLog'`,`'Equal'`,`'Squared'`, `'Sqrt'`
  - *return:* a serialized range values string
   
```python
createRangeValuesZScale(algorithm, zscaleContrast=25, zscaleSamples=600, zscaleSamplesPerLine=120)
```

  - algorithm: must be `'Linear'`, `Log'`,`'LogLog'`,`'Equal'`,`'Squared'`, `'Sqrt'`
  - zscaleContrast
  - zscaleSamples
  - zscaleSamplesPerLine
  - *return:* a serialized range values string

