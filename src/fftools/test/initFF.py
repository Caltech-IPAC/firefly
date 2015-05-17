import sys
import os
sys.path.append('/hydra/cm/firefly/src/fftools/python/display/')

from FireflyClient import *

host ='localhost:8080'


# fc.addListener(helloCallback, "de")

# use with execfile('./initFF.py')

def helloCallback(event):
    print event
    print "worldpt 1: " +event['data']['wpt1']

try:
    fc =FireflyClient(host,'tt')
    fc.addListener(helloCallback)
    file= fc.uploadFile('data/c.fits')
    fc.showFits(file,'p1')
    fc.addExtension('AREA_SELECT','myop','p1','as1')
    fc.run_forever()


except KeyboardInterrupt:
    raw_input("Press enter key to exit...")
    print ("Exiting main")
    fc.disconnect()
    fc.sesson.close()

