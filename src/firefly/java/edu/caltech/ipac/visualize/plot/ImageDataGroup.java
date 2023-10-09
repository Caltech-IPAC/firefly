/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.RGBIntensity;

import java.awt.image.IndexColorModel;
import java.util.Arrays;
import java.util.Iterator;
/*
 * User: roby
 * Date: Aug 21, 2008
 * Time: 1:22:48 PM
 */


/**
 * @author Trey Roby
 */
public class ImageDataGroup implements Iterable<ImageData> {

    private ImageData[] imageDataAry;
    private final int _width;
    private final int _height;
    private final int tileSize;
    private final ImageData.ImageType imageType;
    private int colorTableID;
    private final RangeValues initRangeValues;
    private final ImageMask[] iMasks;

    private RGBIntensity _rgbIntensity; // for 3-color hew-preserving images only



//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================

    public ImageDataGroup(int dataWidth, int dataHeight,
                          ImageData.ImageType imageType,
                          int colorTableID,
                          RangeValues rangeValues,
                          int tileSize) {
        _width = dataWidth;
        _height = dataHeight;
        this.tileSize= tileSize;
        this.colorTableID= colorTableID;
        this.initRangeValues= rangeValues;
        this.imageType= imageType;
        this.iMasks= null;
        _rgbIntensity = null;
    }

    public ImageDataGroup(int dataWidth, int dataHeight, ImageMask[] iMasks, RangeValues rangeValues, int tileSize) {
        _width = dataWidth;
        _height = dataHeight;
        this.tileSize= tileSize;
        this.iMasks = iMasks;
        this.colorTableID= -1;
        this.initRangeValues= rangeValues;
        this.imageType= ImageData.ImageType.TYPE_8_BIT;
    }

    public ImageData[] getImageDataAry() {
        if (imageDataAry !=null) return imageDataAry;

        int totWidth= _width;
        int totHeight= _height;

        int xPanels= totWidth / tileSize;
        int yPanels= totHeight / tileSize;
        if (totWidth % tileSize > 0) xPanels++;
        if (totHeight % tileSize > 0) yPanels++;


        imageDataAry = new ImageData[xPanels * yPanels];
        for(int i= 0; i<xPanels; i++) {
            for(int j= 0; j<yPanels; j++) {
                int width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                int height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                imageDataAry[(i*yPanels) +j]= (iMasks==null) ?
                        new ImageData(imageType, colorTableID,initRangeValues, tileSize*i,tileSize*j, width, height) :
                        new ImageData( iMasks,initRangeValues, tileSize*i,tileSize*j, width, height);
            }
        }
        return imageDataAry;
    }


    public Iterator<ImageData> iterator() { return Arrays.asList(getImageDataAry()).iterator(); }

    public int size() { return getImageDataAry().length; }

    public boolean isUpToDate() {
        if (imageDataAry ==null) return true;
        for(ImageData id : getImageDataAry()) {
            if (id.isImageOutOfDate()) return false;
        }
        return true;
    }

    public int getColorTableId() { return colorTableID; }

    public void setColorTableId(int colorTableID) {
        this.colorTableID= colorTableID;
        IndexColorModel cm= ColorTable.getColorModel(colorTableID);
        for(ImageData id : getImageDataAry()) {
            id.setColorModel(cm);
            id.setColorTableIdOnly(colorTableID);
        }
    }

    public void markImageOutOfDate() {
        if (imageDataAry ==null) return;
        for(ImageData id : getImageDataAry()) {
            id.markImageOutOfDate();
        }
    }

    public void recomputeStretch(FitsRead[] fitsReadAry, int idx, RangeValues rangeValues) {
        boolean setRGBIntensity = false;
        if (rangeValues.rgbPreserveHue()) {
            if (_rgbIntensity == null) {
                _rgbIntensity = new RGBIntensity();
            }
            _rgbIntensity.addRangeValues(fitsReadAry, idx, rangeValues);
            setRGBIntensity = !Arrays.asList(fitsReadAry).contains(null);
        }

        for(ImageData id : getImageDataAry()) {
            if (setRGBIntensity) {
                id.setRGBIntensity(_rgbIntensity);
            }
            id.recomputeStretch(idx,rangeValues);
        }
    }
}
