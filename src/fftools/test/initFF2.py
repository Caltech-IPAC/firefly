__author__ = 'roby'

import sys
import json

# add to the path directory with the data
sys.path.append('../python/display/')

from FireflyClient import *

host='localhost:8080'

# from prompt: execfile('./initFF2.py')

# callback, where you can define what to do when an event is received
#
def myCallback(event):
    # print event
    print "Event Received: "+json.dumps(event['data']);

fc= FireflyClient(host,'myChannel')

fc.launchBrowser()
# walkaround to make sure other actions do not happen before the browser is ready to receive events
raw_input("Wait for browser to load Firefly Tools.   Press Enter to continue...")

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


