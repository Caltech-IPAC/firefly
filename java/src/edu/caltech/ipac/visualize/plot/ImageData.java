/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.Assert;
import nom.tam.fits.FitsException;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;

public class ImageData implements Serializable {


    public enum ImageType {TYPE_8_BIT, TYPE_24_BIT}
    public static final int RED= 0;
    public static final int GREEN= 1;
    public static final int BLUE= 2;

    private       FitsRead        _fitsreadAry[]= new FitsRead[3];
    private       ImageType       _imageType;
    private       FitsRead        _primaryFitsRead;
    private IndexColorModel _cm;
    private int             _colorTableID= 0;   // this is not as flexible as color model and will be set to -1 when color model is set
    private BufferedImage   _bufferedImage;
    private boolean         _imageOutOfDate= true;
    private int             _imageWidth= 0;
    private int             _imageHeight= 0;
    private final int       _x;
    private final int       _y;
    private final int       _width;
    private final int       _height;

    private WritableRaster  _raster; // currently onlu used with 24 bit images

   
//    public ImageData(FitsRead fr,  ImageType imageType ) throws FitsException {
//            this(fr,imageType, 0, FitsRead.getDefaultFutureStretch());
//    }

//    public ImageData(FitsRead fr,
//                     ImageType imageType,
//                     int colorTableID,
//                     RangeValues rangeValues) throws FitsException {
//        this(fr,imageType,colorTableID,rangeValues,
//             0,0,
//             fr.getImageHeader().naxis1,
//             fr.getImageHeader().naxis2);
//
//    }

    public ImageData(FitsRead fr,
                     ImageType imageType,
                     int colorTableID,
                     RangeValues rangeValues,
                     int x,
                     int y,
                     int width,
                     int height,
                     boolean constructNow) throws FitsException {

        _x= x;
        _y= y;
        _width= width;
        _height= height;

        _imageType= imageType;
        _colorTableID= colorTableID;
        _primaryFitsRead = fr;
        _fitsreadAry[RED]= fr;
        _cm = ColorTable.getColorModel(colorTableID);
        if (constructNow) prepareImage(rangeValues, true);
    }

    public BufferedImage getImage()       {
        if (_imageOutOfDate) constructImage();
        return _bufferedImage;
    }

    public void releaseImage() {
        _imageOutOfDate= true;
        _bufferedImage= null;
    }


    public void freeResources() {
        for(int i= 0; (i<_fitsreadAry.length); i++) {
            _fitsreadAry[i]= null;
        }
        _imageType= null;
        _primaryFitsRead= null;
        _cm= null;
        _bufferedImage= null;
        _raster= null;
        _imageOutOfDate= true;
    }

    public int getX() { return _x;}
    public int getY() { return _y;}

    public int getWidth() { return _width;}
    public int getHeight() { return _height;}

    public int getImageWidth() { return _imageWidth; }
    public int getImageHeight() { return _imageHeight; }

    private byte[] getDataArray(FitsRead fr) {
        byte retval[]= null;

        DataBufferByte db;
        if (fr!=null) {
            if (_raster==null) {
                db= (DataBufferByte) _bufferedImage.getRaster().getDataBuffer();
            }
            else {
                db= (DataBufferByte) _raster.getDataBuffer();
            }
            int dbIdx= -1;
            for(int i=0; (i<_fitsreadAry.length && dbIdx==-1); i++) {
                if(_fitsreadAry[i]==fr) dbIdx=i;
            }

            if(dbIdx>-1) {
                retval=db.getData(dbIdx);
            }
            else if(fr==_primaryFitsRead) {
                retval=db.getData(0);
            }
        }
        return retval;
    }


    private byte[] getDataArray(int idx) {
        DataBufferByte db;
        if (_raster==null) {
            db= (DataBufferByte) _bufferedImage.getRaster().getDataBuffer();
        }
        else {
            db= (DataBufferByte) _raster.getDataBuffer();
        }
        return db.getData(idx);
    }


    public void setColorModel(IndexColorModel color_model) {
        _colorTableID= -1;
        _cm=color_model;
        _imageOutOfDate=true;
    }

    public int getColorTableId() { return _colorTableID; }

    public void setColorTableId(int colorTableID) {
        setColorModel(ColorTable.getColorModel(colorTableID));
        _colorTableID= colorTableID;
    }

    /**
     * don't compute the color model.  Should only be call from ImageDataGroup
     * @param colorTableID the id
     */
    void setColorTableIdOnly(int colorTableID) {
        _colorTableID= colorTableID;
    }


    public IndexColorModel getColorModel() { return _cm; }

    public void setFitsRead(FitsRead fr, int colorBand) {
        Assert.argTst( (colorBand==RED || colorBand==GREEN || colorBand==BLUE),
                       "colorBand must be RED, GREEN, or BLUE");
        _fitsreadAry[colorBand]= fr;
        _imageOutOfDate= true;
    }

    public FitsRead getFitsRead(int colorBand) {
        Assert.argTst( (colorBand==RED || colorBand==GREEN || colorBand==BLUE),
                       "colorBand must be RED, GREEN, or BLUE");
        return _fitsreadAry[colorBand];
    }

    public void recomputeStretch(FitsRead fr, RangeValues rangeValues, boolean force) {
        boolean found= false;
        if (_primaryFitsRead==fr) found= true;
        for(int i=0; i<_fitsreadAry.length && !found; i++) {
            found= (fr==_fitsreadAry[i]);
        }
        Assert.argTst(found,
          "The FitsRead has not be registered with this ImageData class."+
          " It must be registered either by the constructor or by setFitsRead()");
        
        if (_raster!=null || _imageOutOfDate) {  // raster!=null means a 24 bit image (3 color)
            _imageOutOfDate= true;
            fr.setRangeValues(rangeValues);
            if (force) {
                fr.do_stretch(getDataArray(fr),
                              (_imageType==ImageType.TYPE_24_BIT),
                              _x,_x+_width-1,
                              _y, _y+_height-1);
            }
        }
        else {
            fr.setRangeValues(rangeValues);
            fr.do_stretch(getDataArray(fr),
                          (_imageType==ImageType.TYPE_24_BIT),
                          _x,_x+_width-1,
                          _y, _y+_height-1);
        }
    }


    private void prepareImage(RangeValues rangeValues, boolean constructNow) {
        if (_imageType==ImageType.TYPE_8_BIT) {
            _primaryFitsRead.setRangeValues(rangeValues);
        }
        else if (_imageType==ImageType.TYPE_24_BIT) {
            for(FitsRead fr : _fitsreadAry) {
                if(fr!=null)  fr.setRangeValues(rangeValues);
            }
        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        if (constructNow) constructImage();
    }


    private void constructImage() {

        if (_imageType==ImageType.TYPE_8_BIT) {
            _raster= null;
            _bufferedImage= new BufferedImage(_width,_height,
                                              BufferedImage.TYPE_BYTE_INDEXED, _cm);
            _primaryFitsRead.do_stretch(getDataArray(_primaryFitsRead),false,
                                        _x,_x+_width-1,
                                        _y, _y+_height-1);
//            start_pixel, last_pixel, start_line, last_line);
        }
        else if (_imageType==ImageType.TYPE_24_BIT) {
            _raster= Raster.createBandedRaster(
                                          DataBuffer.TYPE_BYTE, _width,_height,3, null);
            _bufferedImage= new BufferedImage(_width,_height,BufferedImage.TYPE_INT_RGB);

            for(int i=0; (i<_fitsreadAry.length); i++) {
                if(_fitsreadAry[i]!=null) {
                    _fitsreadAry[i].do_stretch(getDataArray(_fitsreadAry[i]),true,
                                               _x,_x+_width-1,
                                               _y, _y+_height-1);
//                    start_pixel, last_pixel, start_line, last_line);
                }
                else {
                    byte array[]= getDataArray(i);
                    for(int j=0; j<array.length; j++) array[j]= 0;
                }
            }
            _bufferedImage.setData(_raster);


        }
        else {
            Assert.tst(false, "image type must be TYPE_8_BIT or TYPE_24_BIT");
        }
        _imageWidth= _bufferedImage.getWidth();
        _imageHeight= _bufferedImage.getHeight();
        _imageOutOfDate=false;

    }
}
