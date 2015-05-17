__author__ = 'zhang'


import sys
import os
sys.path.append('/hydra/cm/firefly/src/fftools/python/display/')

from FireflyClient import *

try:

    path= os.getcwd() + '/data/'

    pythonVersion = sys.version_info[0]
    if(pythonVersion!=2):
        print('ERROR: this release only works with python major version 2, to make it work with version 3, please'
              'change "raw_input" to "input_" ')

    #open the first browser
    host ='localhost:8080'
    fc =FireflyClient(host,'tt')

    fitsPathInfo= fc.uploadFile(path+"c.fits")


    #push a fits file
    raw_input("Load a FITS file.   Press Enter to continue...")
    fc.showFits( fitsPathInfo )

    regPathInfo= fc.uploadFile(path+"c.reg")
    raw_input("Overlay a region file.   Press Enter to continue...")
    fc.overylayRegion( regPathInfo )

    raw_input("Add extension.   Press Enter to continue...")
    fc.addExtension("AREA_SELECT", "testButton","myPlotID",  "myID")

    tablePathInfo = fc.uploadFile(path+"2mass-m31-2412rows.tbl")
    raw_input("Load table file.   Press Enter to continue...")
    fc.showTable( tablePathInfo )


    fc.run_forever()
except KeyboardInterrupt:
    raw_input("Press enter key to exit...")
    print ("Exiting main")
    fc.disconnect()
    fc.sesson.close()
