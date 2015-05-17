#Parameters for plotting FITS images

The FITS viewer can take many, many possible parameters.  Some parameters control how to get an image, a image can be retrieved from a service, a url, of a file on the server.
Others control the zoom, stretch, and color, title, and default overlays. The are also parameters to pre-process an image, such as crop, rotate or flip. 
You can also specify three color parameters and the associated files.

All parameters and values are strings.

The following is a overview of possible plotting parameters by category. Almost all parameters are optional. 

###Parameters plotting FITS file from a URL or on the server

| Parameter | Description |
| --------- | ----------- |
| `File` | File name of a file on the server. If you upload a file to firefly the you can use the return value in this parameter. |
| `URL` |Retrieve and plot the file from the specified URL. Required if Type=='URL' or if you want to plot an image referenced by URL, The url can be absolute or relative. | 

If you specify `URL` and it is relative then one of two things happen:

  - The url is made absolute based on the root path set in the method `firefly.setRootPath(path)` .      
  - The url is made absolute based on the host web page url. 
    

###Parameters plotting FITS files retrieved from a service

| Service Parameter | Description |
| --------- | ----------- |
| `Service` | Available services are: `IRIS`, `ISSA`, `DSS`, `SDSS`, `TWOMASS`, `MSX`, `DSS_OR_IRIS`, `WISE`. Required if `Type=='SERVICE'` or if you want to use a service. |
| `ObjectName` | the object name that can be looked up by NED or Simbad |
| `Resolver` | The object name resolver to use, options are: `NED`, `Simbad`, `NedThenSimbad`, `SimbadThenNed`, `PTF` |
| `SizeInDeg` | The radius or side (in degrees) depending of the service type, used with `Type=='SERVICE'` |
| `SurveyKeyBand` | So far only used with `'Type===SERVICE'` and `'Service===WISE'`. Possible values are: `1`, `2`, `3`, `4` |

`SurveyKey`:   The value of SurveyKey depends on the value of "Service".
The possible values for SurveyKey are listed below for each service:        
   
 - IRIS: 12, 25, 60, 100
 - ISSA: 12, 25, 60, 100
 - DSS: poss2ukstu_red, poss2ukstu_ir, poss2ukstu_blue, poss1_red, poss1_blue, quickv, phase2_gsc2,  phase2_gsc1
 - SDSS: u, g, r, i, z
 - TWOMASS: j, h, k
 - MSX: 3, 4, 5, 6
 - WISE: 1b, 3a

`WorldPt`:  This is target for service request.

  - WorldPt uses the format `"12.33;45.66;EQ_J2000"` for j2000.
  - The general syntax is `lon;lat;coordinate_sys`, e.g. `'12.2;33.4;EQ_J2000'` or `'11.1;22.2;GALACTIC'`
  - coordinate system can be: `'EQ_J2000'`, `'EQ_B1950'`, `'EC_J2000'`, `'EC_B1950'`, `'GALACTIC'`, or `'SUPERGALACTIC'`;

###Parameters that Color and Stretch

| Parameter | Description |
| --------- | ----------- |
| `RangeValues` |  A complex string for specify the stretch of this plot. Use the method firefly.serializeRangeValues() to produce this string |
| `ColorTable` | value 0 - 21 to represent different predefined color tables |




###Parameters that control the Flipping

| Flip Parameter | Description |
| --------- | ----------- |
| `FlipX` | Flip this image on the X axis |
| `FlipY` | Flip this image on the Y axis |


###Parameters that control the Rotate

| Rotate north Parameter | Description |
| --------- | ----------- |
| `RotateNorth` | Plot should come up rotated north, should be `"true"` to rotate north |
| `RotateNorthType` | coordinate system to use for rotate north, options: `EQ_J2000`, `EQ_B1950`, `EC_J2000`, `EC_B1950`, `GALACTIC`, or `SUPERGALACTIC`. The default is  `EQ_J2000` |
 
| Rotate to any angle Parameter | Description |
| --------- | ----------- |
| `Rotate` | set to rotate, if "true", the angle should also be set |
| `RotationAngle` | the angle to rotate to, use with `Rotate` |


###Parameters that control the Cropping

| Crop Parameter | Description |
| --------- | ----------- |
| `PostCrop` | `'true'` to crop.  If rotation is set then the crop will happen post rotation.|
| `PostCropAndCenter` | `'true'` to Crop and center the image before returning it. Note: `SizeInDeg` and `WorldPt` are required |
| `CropPt1` | One corner of the rectangle, in image coordinates, to crop out of the image, used with `CropPt2`. Syntax is "x;y" example: 12;1.5 |
| `CropPt2` | Second corner of the rectangle, in image coordinates, to crop out of the image, used with `'CropPt1'`. Syntax is "x;y" example: 12;1.5 |
|  `CropWorldPt1` | One corner of the rectangle, in world coordinates, to crop out of the image, used with `'CropWorldPt2'`   |
| `CropWorldPt2`| Second corner of the rectangle, in world coordinates, to crop out of the image, used with `CropWorldPt1`.|

Notes- 

 - `CropPt1` and `CropPt2` are diagonal of each other and `CropWorldPt1` and `CropWorldPt2` are diagonal of each other.
 - See documentation on WorldPt to find proper syntax

###Parameters that control the Zoom
  
   `ZoomType`:  Set the zoom type, based on the ZoomType other zoom set methods may be required
Notes for ZoomType:

 - `STANDARD` - default, when set, you may optionally define `'InitZoomLevel'` or the zoom will default to be 1x
 - `TO_WIDTH` - you must define `ZoomToWidth` and set a pixel width
 - `FULL_SCREEN` - you must define `ZoomToWidth` with a width and `'ZoomToHeight'` with a height
 - `ARCSEC_PER_SCREEN_PIX`- you must define `ZoomArcsecPerScreenPix`
 
| Zoom Parameter | Description |
| --------- | ----------- |
|`InitZoomLevel` | The level to zoom the image to. Used with ZoomType=='STANDARD' (which is the default).  *Example-* .5,2,8,.125 |
|`ZoomArcsecPerScreenPix` | Set the zoom level so it have the specified arcsec per screen pixel. Used with `ZoomType=='ARCSEC_PER_SCREEN_PIX'` and 'ZoomToWidth' |
      

      
      
###Parameters that control the title of the plot
`TitleOptions`:  The parameter sets other ways to title the plot. The following 4 values are options for title:

 - NONE - The default, use the value set in <code>Title</code>, if this is empty use the plot description that come from the server
 - PLOT_DESC - Use the plot description set by the server. This is meaningful when the server is using a service, otherwise it will be an empty string. <i>example-</i> 2mass or IRIS
 - FILE_NAME - Use the name of the FITS file. This is useful when plotting an uploaded file or a URL.
 - HEADER_KEY - Use the value of a FITS header name key.  The parameter `HeaderForKeyTitle` must be set to the FITS header card name
 - PLOT_DESC_PLUS - Use the server plot description but append some string to it.  The string is set in `'PlotDescAppend'`
   

| Title Parameter | Description |
| --------- | ----------- |
| `Title` |  Title of the plot |
| `PostTitle` |  A String to append at the end of the title of the plot. This parameter is useful if you are using one of the computed `TitleOpions` such as `FILE_NAME` or `HEADER_KEY` |
| `PreTitle` | A String to append at the beginning of the title of the plot. This parameter is useful if you are using one of the computed `TitleOptions` such as `FILE_NAME` or `HEADER_KEY` |
| `TitleFilenameModePfx` | A String to replace the default "from" when `TitleMode` is `FILE_NAME`, and the mode is `URL`. If the url contains a fits file name and there are more options then the firefly viewer will add a "from" to the front of the title. This parameter allows that string to be changed to something such as "cutout". |
| `PlotDescAppend` | A string to apppend to the end of the plot description set by the server.  This will be used for the plot title if the `TitleOptions` parameter is set to `PlotDescAppend`. |
| `HeaderKeyForTitle` | Sets the title to the fits header key if `TitleOptions` is set to `HEADER_KEY` |


###The Type parameter

The request `Type` parameter can be set specifically or it is implied from the `File`, `URL` or `Service` parameters which are mutually exclusive.  However, most of the type the type is inferred from these other parameters so it is unnecessary.  It the `Type` is specified is will do some checking on the other parameters..

`Type` is required is you want to plot a `BLANK` image or an `ALL_SKY` image.

 `Type`: Set the type of request. Based on the Type then one or more other parameters are required.
Options are:

  - `SERVICE`, for an image service
  - `FILE` for file on the server
  - `URL` for any url accessible FITS image file
  - `TRY_FILE_THEN_URL` try a file on the server first then try the url
  - `BLANK` make a blank image
  - `ALL_SKY`

###Other less commonly used parameters 

| Title Parameter | Description |
| --------- | ----------- |
| `GridOn` | Turn the coordinate grid on after the image is plotted. Normally the grid is turned on by a user action.  This option forces the grid to be on by default. value: `true` or `false` |
| `ContinueOnFail` | For 3 color, if this request fails then keep trying to make a plot with the other request |
| `SurveyKeyAlt` | TODO: Document this param |
| `UserDesc` |  TODO: Document this param |
| `UniqueKey` |  TODO: Document this param |
