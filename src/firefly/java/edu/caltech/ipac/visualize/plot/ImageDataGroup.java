/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.Assert;
import nom.tam.fits.FitsException;

import java.awt.image.IndexColorModel;
import java.util.Arrays;
import java.util.Iterator;
/**
 * User: roby
 * Date: Aug 21, 2008
 * Time: 1:22:48 PM
 */


/**
 * @author Trey Roby
 */
public class ImageDataGroup implements Iterable<ImageData> {

    private       ImageData  _imageDataAry[];
    private final ImageData.ImageType _imageType;


    private final int _width;
    private final int _height;


//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ImageDataGroup(FitsRead fitsReadAry[],
                          ImageData.ImageType imageType,
                          int colorTableID,
                          RangeValues rangeValues,
                          int tileSize,
                          boolean constructNow) throws FitsException {
        FitsRead fr= null;
        for(FitsRead testFr : fitsReadAry) {
            if (testFr!=null) {
                fr= testFr;
                break;
            }
        }
        Assert.argTst(fr, "fitsReadAry must have one non-null element.");
        ImageHeader hdr= fr.getImageHeader();
        _imageType= imageType;
        _width = hdr.naxis1;
        _height = hdr.naxis2;

        int totWidth= hdr.naxis1;
        int totHeight= hdr.naxis2;

        int xPanels= totWidth / tileSize;
        int yPanels= totHeight / tileSize;
        if (totWidth % tileSize > 0) xPanels++;
        if (totHeight % tileSize > 0) yPanels++;


        _imageDataAry= new ImageData[xPanels * yPanels];

        int width;
        int height;
        for(int i= 0; i<xPanels; i++) {
            for(int j= 0; j<yPanels; j++) {
                width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                _imageDataAry[(i*yPanels) +j]= new ImageData(fitsReadAry,imageType,
                                                  colorTableID,rangeValues,
                                                  tileSize*i,tileSize*j,
                                                  width, height, constructNow);
            }
        }
    }

    /**
     * LZ 07/20/15
     * @return
     */
    public ImageDataGroup(FitsRead fitsReadAry[],
                          ImageData.ImageType imageType,
                          IndexColorModel cm,
                          RangeValues rangeValues,
                          int tileSize,
                          boolean constructNow) throws FitsException {
        FitsRead fr= null;
        for(FitsRead testFr : fitsReadAry) {
            if (testFr!=null) {
                fr= testFr;
                break;
            }
        }
        Assert.argTst(fr, "fitsReadAry must have one non-null element.");
        ImageHeader hdr= fr.getImageHeader();
        _imageType= imageType;
        _width = hdr.naxis1;
        _height = hdr.naxis2;

        int totWidth= hdr.naxis1;
        int totHeight= hdr.naxis2;

        int xPanels= totWidth / tileSize;
        int yPanels= totHeight / tileSize;
        if (totWidth % tileSize > 0) xPanels++;
        if (totHeight % tileSize > 0) yPanels++;


        _imageDataAry= new ImageData[xPanels * yPanels];

        int width;
        int height;
        for(int i= 0; i<xPanels; i++) {
            for(int j= 0; j<yPanels; j++) {
                width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                _imageDataAry[(i*yPanels) +j]= new ImageData(fitsReadAry,imageType,
                        cm,rangeValues,
                        tileSize*i,tileSize*j,
                        width, height, constructNow);
            }
        }
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public Iterator<ImageData> iterator() {
        return Arrays.asList(_imageDataAry).iterator();
    }

    public int size() { return _imageDataAry.length; }

    public int getImageWidth() { return _width; }
    public int getImageHeight() { return _height; }

    public boolean isUpToDate() {
        for(ImageData id : _imageDataAry) {
            if (id.isImageOutOfDate()) return false;
        }
        return true;
    }

    public int getColorTableId() { return _imageDataAry[0].getColorTableId(); }

    public void setColorTableId(int colorTableID) {
        IndexColorModel cm= ColorTable.getColorModel(colorTableID);
        for(ImageData id : _imageDataAry) {
            id.setColorModel(cm);
            id.setColorTableIdOnly(colorTableID);
        }
    }

    public IndexColorModel getColorModel() {
        return _imageDataAry[0].getColorModel();
    }

    public void setColorModel(IndexColorModel cm) {
        for(ImageData id : _imageDataAry) {
            id.setColorModel(cm);
        }
    }

    public void markImageOutOfDate() {
        for(ImageData id : _imageDataAry) {
            id.markImageOutOfDate();
        }
    }


//    public void recomputeStretch(FitsRead fitsReadAry[], int idx, RangeValues rangeValues) {
//        recomputeStretch(fitsReadAry,idx,rangeValues,false);
//    }

    public void recomputeStretch(FitsRead fitsReadAry[],
                                 int idx,
                                 RangeValues rangeValues,
                                 boolean force) {
        for(ImageData id : _imageDataAry) {
            id.recomputeStretch(fitsReadAry,idx,rangeValues, force);
        }
    }

    public void freeResources() {
        if (_imageDataAry!=null) {
            for(ImageData d : _imageDataAry) {
                d.freeResources();
            }
            _imageDataAry= null;
        }
    }

//=======================================================================
//-------------- Method from LabelSource Interface ----------------------
//=======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

// =====================================================================
// -------------------- Factory Methods --------------------------------
// =====================================================================

}

