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
    private final int _tileSize;

    private final int _xPanels;
    private final int _yPanels;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ImageDataGroup(FitsRead fr,
                          ImageData.ImageType imageType,
                          int colorTableID,
                          RangeValues rangeValues,
                          int tileSize,
                          boolean constructNow) throws FitsException {
        ImageHeader hdr= fr.getImageHeader();
        _imageType= imageType;
        _width = hdr.naxis1;
        _height = hdr.naxis2;
        _tileSize= tileSize;

        int totWidth= hdr.naxis1;
        int totHeight= hdr.naxis2;

        int xPanels= totWidth / _tileSize;
        int yPanels= totHeight / _tileSize;
        if (totWidth % _tileSize > 0) xPanels++;
        if (totHeight % _tileSize > 0) yPanels++;

        _xPanels= xPanels;
        _yPanels= yPanels;

        _imageDataAry= new ImageData[_xPanels * _yPanels];

        int width;
        int height;
        for(int i= 0; i<_xPanels; i++) {
            for(int j= 0; j<_yPanels; j++) {
                width= (i<_xPanels-1) ? _tileSize : ((totWidth-1) % _tileSize + 1);
                height= (j<_yPanels-1) ? _tileSize : ((totHeight-1) % _tileSize + 1);
//                width= (i<_xPanels-1) ? _tileSize : (totWidth % _tileSize);
//                height= (j<_yPanels-1) ? _tileSize : (totHeight % _tileSize);
                _imageDataAry[(i*_yPanels) +j]= new ImageData(fr,imageType,
                                                  colorTableID,rangeValues,
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


    public void setFitsRead(FitsRead fr, int colorBand) {
        Assert.argTst( (colorBand==ImageData.RED ||
                        colorBand==ImageData.GREEN ||
                        colorBand==ImageData.BLUE),
                       "colorBand must be RED, GREEN, or BLUE");
        for(ImageData id : _imageDataAry) {
            id.setFitsRead(fr,colorBand);
        }
    }

    public FitsRead getFitsRead(int colorBand) {
        Assert.argTst( (colorBand==ImageData.RED ||
                        colorBand==ImageData.GREEN ||
                        colorBand==ImageData.BLUE),
                       "colorBand must be RED, GREEN, or BLUE");
        return _imageDataAry[0].getFitsRead(colorBand);
    }


    public void releaseImage() {
        for(ImageData id : _imageDataAry) {
            id.releaseImage();
        }
    }


    public void recomputeStretch(FitsRead fr, RangeValues rangeValues) {
        recomputeStretch(fr,rangeValues,false);
    }

    public void recomputeStretch(FitsRead fr,
                                 RangeValues rangeValues,
                                 boolean force) {
        for(ImageData id : _imageDataAry) {
            id.recomputeStretch(fr,rangeValues, force);
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
