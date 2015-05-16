

## Firefly python wrapper user guide ##

This document explains how to use the python library included in Firefly to interact with firefly image viewer. 

The python class name is **FireflyClient**.  It is located in firefly/src/fftools/python.   

 -  **Pre-requirements**

		 1. install [ws4py](http://ws4py.readthedocs.org/en/latest/sources/install )
		 2. Set up python enviroment
			 setenv PYTHONPATH /path/to/firefly/python
	

 - **Import the FireflyClient into your script**
   from FireflyClient import *
   

 - **Create an instance of the FireflyClient**
 
      fc = FireflyClient( 'localhost:8080')
      
      or 
      
      fc=FireflyClient('localhost:8080', channel='myNewChannel')
 

 - **Show a Fits image**
 
      data = /your/path/yourFits.fits'
   
      fitsPathInfo= fc.uploadImage(data)
   
      fc.showFits(fitsPathInfo)
      

 - **Overlay a region**
 
     regFile=/your/path/yourRegion.reg
     
     regPathInfo= fc.uploadImage(regFile)
     
     fc.overlayRegion(regPathInfo)


 - **Show a table**
 
     table =/your/path/yourTable.tbl
     
     tablePathInfo = fc.uploadImage(table)
     
     fc.showTable(tablePathInfo)

 - **Run in python prompt**
 
      1. start python session by typing "python" in the terminal
      2. enter: execfile("yourScript.py") at the python prompt

 - **Run in iPyton**
 
       1. start the iPython session by typing "iPython" in the terminal
       2. enter the script name at the ipython prompt

 - **Run in iPython notebook**
 
          1. open a cell
          2. type : "%run yourScript.py" into the cell
          3. click run


 - **Example** (webSocketTest.py) 




    
    from FireflyClient import *
    try:
      #open the first browser
      path="/Users/zhang/lsstDev/data/"
      host ='localhost:8080'
      fc =FireflyClient(host)

	
	  #push a fits file
      raw_input("Load a FITS file.   Press Enter to continue...")
      fitsPathInfo= fc.uploadImage(path+"c.fits"
      fc.showFits( fitsPathInfo )

      raw_input("Overlay a region file.   Press Enter to continue...")
      regPathInfo= fc.uploadImage(path+"c.reg")
      fc.overylayRegion(regPathInfo )
    

      print("showing the extension...")
      raw_input("Add extension.   Press Enter to continue...")
      fc.addExtension("AREA_SELECT", "testButton","myID")

      raw_input("Load table file.   Press Enter to continue...")
      tablePathInfo = fc.uploadImage(path+"2mass-m31-2412rows.tbl")
      fc.showTable( tablePathInfo )

      #open a second browser window
      fc1 =FireflyClient(channel='newChannel')

      #push a fits file
      raw_input("Load a FITS file.   Press Enter to continue...")
      fc1.showFits( fitsPathInfo )

      fc.run_forever()

    except KeyboardInterrupt:
        raw_input("Press enter key to exit...")
        print ("Exiting main")
        fc.disconnect()
        fc.sesson.close()

    

 - **FireflyClient's methods**
 
		 
     - *showFits(self, path, plotID=None, addtlParams=None)* 
     	 
       This method will load the fits located in the **path** and display the image in  
   the IRSA viewer.
      	 
     - *showTable(self, path, title=None, pageSize=None)*
     
         This method will display the table located in the path 
           
      
     - *overylayRegion(self, path,  extType='reg', title=None, id=None, image=None)*
     
         This method is going to overlay a region or an image on the existing image
     
    - *addExtension(self, extType, title, plotId, id, image=None)*
    
	    This method is going to add a extension to the image

