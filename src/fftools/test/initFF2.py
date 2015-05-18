__author__ = 'roby'

import sys
import os
sys.path.append('/hydra/cm/firefly/src/fftools/python/display/')

from FireflyClient import *

host ='localhost:8080'


# fc.addListener(helloCallback, "de")

# use with execfile('./initFF2.py')

def helloCallback(event):
    # print event
    print "worldpt 1: " +event['data']['wpt1']

fc =FireflyClient(host,'tt')
fc.addListener(helloCallback)
file= fc.uploadFile('data/c.fits')
status= fc.showFits(file,'p1')
print 'success: %s' % status['success']
fc.addExtension('AREA_SELECT','myop','p1','as1')

pParams= { 'URL' : 'http://web.ipac.caltech.edu/staff/roby/demo/wise-m51-band2.fits',
           'ColorTable' : '5'}
status= fc.showFits(fileOnServer=None, plotID='abc', additionalParams=pParams)
print 'success: %s' % status['success']
    # fc.run_forever()


