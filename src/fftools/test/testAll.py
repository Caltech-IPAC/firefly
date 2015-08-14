__author__ = 'zhang'



import sys
import json

# add to the path directory with the data
sys.path.append('../python/display/')

from FireflyClient import *

# from prompt: execfile('./initFF2.py')

# callback, where you can define what to do when an event is received
#
def myCallback(event):
    # print event
    print "Event Received: "+json.dumps(event['data']);


try:

    path= 'data/'

    pythonVersion = sys.version_info[0]
    if(pythonVersion!=2):
        print('ERROR: this release only works with python major version 2, to make it work with version 3, please'
              'change "raw_input" to "input_" ')


    #open the first browser
    host ='localhost:8080'
    fc= FireflyClient(host,'myChannel')

    fc.launchBrowser()

    raw_input("Add a listener.   Press Enter to continue...")
    status= fc.addListener(myCallback)

    fitsPathInfo= fc.uploadFile(path+"c.fits")
    #push a fits file
    raw_input("Load a FITS file.   Press Enter to continue...")
    status= fc.showFits( fitsPathInfo, plotId='p1' )
    time.sleep(1)
    print 'showFits success: %s' % status['success']

    raw_input("pan the plot.   Press Enter to continue...")
    status= fc.pan('p1', 10, 10)
    print 'pan success: %s' % status['success']

    raw_input("zoom the plot.   Press Enter to continue...")
    status= fc.zoom('p1', 0.5)
    print 'zoom success: %s' % status['success']

    raw_input("change the stretch of  the plot.   Press Enter to continue...")
    rv= fc.createRangeValuesStandard('log', 'Percent',9,98)
    #does not work right yet
    status= fc.stretch('p1', rv)
    print 'stretch success: %s' % status['success']

    tablePathInfo = fc.uploadFile(path+"2mass-m31-2412rows.tbl")
    raw_input("Load table file.   Press Enter to continue...")
    status= fc.showTable( tablePathInfo )
    print 'showTable success: %s' % status['success']

    regPathInfo= fc.uploadFile(path+"c.reg")
    raw_input("Overlay a region file.   Press Enter to continue...")
    status= fc.overlayRegion( regPathInfo )



    raw_input("Add extension.   Press Enter to continue...")
    status= fc.addExtension("AREA_SELECT", "testButton","myPlotID",  "myID")

    delReg= ['physical;point 211 201 # color=pink point=cross 9',
         'physical;point 100 180 # color=green point=cross 10 select=1'
         ]
    status= fc.removeRegionData(delReg,'reg3')


    fc.run_forever()

except KeyboardInterrupt:
    raw_input("Press enter key to exit...")
    print ("Exiting main")
    fc.disconnect()
    fc.session.close()
