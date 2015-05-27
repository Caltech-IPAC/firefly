


## Firefly python wrapper user guide ##

This document explains how to use the python library included in Firefly to interact with firefly image viewer. 

The python class name is **FireflyClient**.  It is located in firefly/src/fftools/python.   

**Pre-requirements**

1. Install [ws4py](http://ws4py.readthedocs.org/en/latest/sources/install )
   (If you are using conda package manager, you can use [conda-pipbuild](http://conda.pydata.org/docs/commands/build/conda-pipbuild.html) tool for building conda packages just using pip install.) 
2. Set up python enviroment
			 `setenv PYTHONPATH /where-git-repository-is/firefly/src/fftools/python/display`
	
To get a feel for API `cd firefly/src/fftools/test` and take a look at `initFF.py`, `initFF2.py`, `testAll.py`. If you are within an interpreter, `execfile('./initFF2.py')` is the best, because it leaves the connection to the browser open while allowing you to execute other commands.

**Import the FireflyClient into your script**

   `from FireflyClient import *`
   

**Create an instance of the FireflyClient**

`fc = FireflyClient( 'localhost:8080')`
 or 
`fc=FireflyClient('localhost:8080', channel='myNewChannel')`
 
**Launch Browser and load Firefly Tools**
 
`fc.launchBrowser()` 

Wait until Firefly Tools finish loading and you see 'Waiting for data' before sending other commands.
 
**Show a Fits image**
 
```python
     data = '/your/path/yourFits.fits'
     fitsPathInfo= fc.uploadFile(data)
     fc.showFits(fitsPathInfo)
```   
<br>  
The FITS viewer can take many, many possible parameters.  Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server. Others control the zoom, stretch, and color, title, and default overlays. The are also parameters to pre-process an image, such as crop, rotate or flip. You can also specify three color parameters and the associated files.

For the details of FITS plotting parameters see: [see fits-plotting-parameters.md](fits-plotting-parameters.md)
      

**Overlay a region**

```python 
     regFile=/your/path/yourRegion.reg
     regPathInfo= fc.uploadFile(regFile)
     fc.overlayRegion(regPathInfo)
``` 

**Show a table**
 
```python 
     table ='/your/path/yourTable.tbl'    
     tablePathInfo = fc.uploadImage(table)
     fc.showTable(tablePathInfo)
```

**Run in python prompt**

1. start python session by typing `python` in the terminal
2. enter: `execfile("yourScript.py")` at the python prompt

**Run in iPyton**

1. start the iPython session by typing `iPython` in the terminal
2. enter the script name at the ipython prompt

**Run in iPython notebook**
  
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
	    
**FireflyClient's methods**
 
		 
- *showFits(self, path, plotID=None, addtlParams=None)* 
     	 
       This method will load the fits located in the **path** and display the image in  
   the IRSA viewer.
      	 
- *showTable(self, path, title=None, pageSize=None)*

   This method displays the table located in the path 
           
- *addExtension(self, extType, title, plotId, id, image=None)*
    
    This method adds a extension to the image

**FireflyClient's Region Methods**

- ```overylayRegion(path, title=None, regionId=None)```
     
    This method overlays a region file on an image
     
- ```removeRegion(regionId=None)```
     
    This method removes a region file from an image
    
- ```overlayRegionData(regionData, regionId, title=None)```
     
    Overlay a region on the loaded FITS images
       
       - regionData: a list of region entries
       - regionId: id of region overlay to create or add too
       - title: title of the region file
       - *return* status of call

- ```removeRegionData(regionData, regionId, title=None)```

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