__author__ = 'roby'


import sys
import json

# add to the path directory with the data

from FireflyClient import *

host='localhost:8080'

# from prompt: execfile('./aCallback.py')

# callback, where you can define what to do when an event is received
#

def myCallback(event):
    print "Event Received: "+json.dumps(event['data']);
    #print event
    if 'type' in event['data']:
        if event['data']['type']=='AREA_SELECT':
            #print '*************area select'
            pParams= { 'URL' : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits','ColorTable' : '5'}
            status= fc.showFits(fileOnServer=None, plotID='p2', additionalParams=pParams)
    elif 'region' in event['data']:
        print 'selected region: %s' % event['data']['region']


fc= FireflyClient(host,'tt')
fc.addListener(myCallback)

