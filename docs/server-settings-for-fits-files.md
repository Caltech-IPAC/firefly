Firefly has some configuration parameters to control how it reads FITS files. 


#### Security

FITS file reading security is turned off by default.  When the Firefly server is in production it should be turned on. 
Turning on FITS file security means the Firefly server will only read FITS files in the FITS search path or from the one of the work 
directories. To turn on security set `visualize.fits.Security` to `true`. Then set `visualize.fits.search.path` to a path of directories
where you might have FITS files.  Any sub-directory will also be included.

Example- 
```
visualize.fits.search.path =  "/root/my/path:/root/other/path"
visualize.fits.Security= true
```

#### Maximum FITS file size

By default Firefly will only read FITS files that are a maximum of 1.5 gigabytes. This can be changed with the `visualize.fits.MaxSizeInBytes` property.

```
visualize.fits.MaxSizeInBytes= 10737418240     // 10 gigs, if not there it defaults to 1.5 gigs
```

You will need to make sure that tomcat is started with enough memory to support reading large FITS files. Here are guide lines for selecting your server memory size.

 - Firefly uses about twice as much memory to read a file as is the file size.
 - You will need more memory to allow for smaller files.
 - Firefly allocates about 60% of the tomcat memory to readying FITS files.
 - Firefly will push files out of memory to make room for others but must have enough memory to do this juggling in and out of smaller files.
 - The juggle memory should be the max of 1 gig or the half the largest file you want to read.
 - Example - 
   
    - If you want to support to four concurrent users each reading three gigabyte files you should have: 
    - l.67 *( 4 users * 3 gig files * 2 for twice as much memory as file size + 3/2 gig juggling ) = 42.6 gigs
    - This sort of setting would support many, many users reading smaller files.
    - This is just a starting point you might need a little more or a little less
 


#### How to set Properties

Configuration properties can be set in various ways.

 - You can edit `firefly/config/app.config` and rebuild.   
 - You can also set it `~/.gradle/build.config` and rebuild.

Both of these will require you rebuilding the war. If you want to make sure it all works first, just go 
go to the deployment directory `webapps/firefly/WEB-INF/config` and edit `app.prop`. Then just restart
the tomcat server. Remember `app.prop` will get reset each time you redeploy the war file.
