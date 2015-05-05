/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot;
/**
 * User: roby
 * Date: 5/5/15
 * Time: 2:56 PM
 */


import edu.caltech.ipac.firefly.visualize.Band;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class ActiveFitsReadGroup implements Serializable {

    private static final int LENGTH= 3;

    private FitsRead fitsReadAry[]= new FitsRead[LENGTH];
    private boolean inUse[]= new boolean[LENGTH];
//    private String cacheKey[]= new String[LENGTH];
//    private int imageIdx[]= new int[LENGTH];
//    private File fitsFile[]= new File[LENGTH];


    public void setFitsRead(Band band, FitsRead fr) {
        fitsReadAry[band.getIdx()]= fr;
        inUse[band.getIdx()]= (fr!=null);
    }

    public FitsRead getFitsRead(Band band) {
        return fitsReadAry[band.getIdx()];
    }

    public FitsRead[] getFitsReadAry() { return fitsReadAry; }

//    public String getCacheKey(Band band) {
//        return cacheKey[band.getIdx()];
//    }
//
//    public void setCacheKey(Band band, String cacheKey) {
//        this.cacheKey[band.getIdx()] = cacheKey;
//    }
//
//    public int getImageIdx(Band band) {
//        return imageIdx[band.getIdx()];
//    }
//
//    public void setImageIdx(Band band, int imageIdx) {
//        this.imageIdx[band.getIdx()] = imageIdx;
//    }

//    public File getFitsFile(Band band) {
//        return fitsFile[band.getIdx()];
//    }

    public void freeResources(boolean freeFRResource) {
        for(int i= 0; (i<fitsReadAry.length);i++) {
            if (fitsReadAry[i]!=null && freeFRResource)  fitsReadAry[i].freeResources();
            fitsReadAry[i]= null;
        }
    }

    public ActiveFitsReadGroup makeCopy() {
        ActiveFitsReadGroup copy= new ActiveFitsReadGroup();
        for(int i=0; i<fitsReadAry.length; i++) {
            copy.fitsReadAry[i]= fitsReadAry[i];
//            copy.cacheKey[i]= cacheKey[i];
//            copy.imageIdx[i]= imageIdx[i];
//            copy.fitsFile[i]= fitsFile[i];
        }
        return copy;
    }
}
