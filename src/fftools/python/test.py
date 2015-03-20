from Firefly import displayFits, addRegion


#main program 

#check if the tomcat sever is running. If not start tomcat
#killTomcat()
#tomcat("start")

bid='myTest2'

#stop tomcat       
#tomcat("stop")
      
path="/Users/zhang/Work/LSST_Development/Python/scripts/" 
location=displayFits( "c.fits", path=path, bid=bid ) 
addRegion("c.reg" , location,  path=path )