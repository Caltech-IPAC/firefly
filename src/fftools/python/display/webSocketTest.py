__author__ = 'zhang'


from FireflyClient import *

try:

    path="/Users/zhang/lsstDev/data/"


    #open the first browser
    host ='localhost:8080'
    fc =FireflyClient(host)

    pathInfo= fc.uploadImage(path+"c.fits")
    print(pathInfo)

    #push a fits file
    raw_input("Load a FITS file.   Press Enter to continue...")
    fc.showFits( path+"c.fits" )

    raw_input("Overlay a region file.   Press Enter to continue...")
    fc.overylayRegion( path+"c.reg" )




    raw_input("Add extension.   Press Enter to continue...")
    fc.addExtension("AREA_SELECT", "testButton","myPlotID",  "myID")

    raw_input("Load table file.   Press Enter to continue...")
    fc.showTable( path+"2mass-m31-2412rows.tbl" )


    #open a second browser window
    host ='http://127.0.0.1:8080'
    fc1 =FireflyClient(host, channel='newChannel')

    #push a fits file
    raw_input("Load a FITS file.   Press Enter to continue...")
    fc1.showFits( path+"c.fits" )




    fc.run_forever()

except KeyboardInterrupt:
    raw_input("Press enter key to exit...")
    print ("Exiting main")
    fc.disconnect()
    fc.sesson.close()
