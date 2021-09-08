/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.plotdata;
/**
 * Author: Lijun
 * Date: 03/24/15
 */

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import nom.tam.fits.*;
import nom.tam.util.ArrayFuncs;
import nom.tam.fits.ImageData;
import nom.tam.fits.HeaderCardException;

/**
 * This class flips the FitsRead image to either x (naxis2) or y(naxis1) direction
 * Example:
 *  FlipXY flip = FlipXY(fitsRead, "xAxis");
 *  FitsRead newFitsRead = flip.doFlip();
 */
public class FlipXY {
    private ImageHeader inImageHeader;
    private Header inFitsHeader;
    private int inNaxis1;
    private int inNaxis2;
    private String direction;
    FitsRead fitsRead;



    /**c
     * constructor
     * @param inFitsRead - a FitsRead object
     * @param fipDirection - a String, "xAxis" or "yAsix"
     */
    public FlipXY(FitsRead inFitsRead, String fipDirection) throws FitsException {
        fitsRead = inFitsRead;
        direction = fipDirection;
        inFitsHeader = FitsReadUtil.cloneHeaderFrom(inFitsRead.getHeader());
        inImageHeader = new ImageHeader(inFitsHeader);
        inNaxis1 = inImageHeader.naxis1;
        inNaxis2 = inImageHeader.naxis2;

    }

    private int getDataDimension( ){


        int naxis3 = inImageHeader.naxis > 2 ? inFitsHeader.getIntValue("NAXIS3", 0) : 0;
        int naxis4 = inImageHeader.naxis > 3 ? inFitsHeader.getIntValue("NAXIS4", 0) : 0;
        return ( naxis4 != 0 ? 4: ( naxis3 != 0 ? 3:2 ) ) ;

    }


    /**
     * This method does the flip according to the flip  direction.
     * @return
     * @throws FitsException
     */
      public FitsRead doFlip()throws FitsException {


          if (inFitsHeader == null) {

              if (SUTDebug.isDebug()) {
                  Logger.info("HDU null! (input image)");
              }
              throw new FitsException("HDU null! (input image)");
          }

          BasicHDU hdu = fitsRead.getHDU();

        int dim = getDataDimension();

        //convert the raw float data to array
        Object inData = ArrayFuncs.curl(fitsRead.getRawFloatAry(), hdu.getAxes());

        Object fdata;

        if (direction.equalsIgnoreCase("yAxis")) {
            fdata = doFlipInY(inData, dim);
        } else if (direction.equalsIgnoreCase("xAxis")) {
            fdata = doFlipInX(inData, dim);
        } else {
            throw new FitsException(
                    "Cannot flip a PLATE projection image");

        }

        //convert to the type to the same type as in the hdu
        Object data =  ArrayFuncs.convertArray(fdata, FitsReadUtil.getDataType(hdu.getBitPix()), true);
        ImageData newImageData =  new ImageData(data);
        //clone the inFitsRead header and then modify it
        Header outFitsHeader = getOutFitsHeader();
        ImageHDU outHDU = new ImageHDU(outFitsHeader, newImageData);
        Fits newFits = new Fits();
        newFits.addHDU(outHDU);

        FitsRead[] outFitsRead = FitsReadFactory.createFitsReadArray(newFits);
        FitsRead fr = outFitsRead[0];

        return fr;
    }

    /**
     * Do the flip along yAxis (naxis1)
     * @param inData
     * @param dim
     * @return
     */
    private Object doFlipInY(Object inData, int dim) {

           Object obj = null;

            switch (dim) {
                case 2:

                     float[][] newData2D = new float[inNaxis2][inNaxis1];
                     float[][] inData2D = (float[][]) inData;

                     for (int line = 0; line < inNaxis2; line++) {
                         int in_index = inNaxis1 - 1;
                         for (int out_index = 0; out_index < inNaxis1; out_index++) {
                             newData2D[line][out_index] = inData2D[line][in_index];
                             in_index--;
                         }
                     }
                     obj = newData2D;
                    break;

                case 3:
                    float[][][] newData3D = new float[1][inNaxis2][inNaxis1];
                    float[][][] inData3D = (float[][][]) inData;

                    for (int line = 0; line < inNaxis2; line++) {

                        int in_index = inNaxis1 - 1;
                        for (int out_index = 0; out_index < inNaxis1; out_index++) {

                            newData3D[0][line][out_index] =
                                    inData3D[0][line][in_index];
                            in_index--;
                        }
                    }
                    obj = newData3D;
                    break;
                case 4:
                    float[][][][] newData4D = new float[1][1][inNaxis2][inNaxis1];
                    float[][][][] inData4D = (float[][][][]) inData;
                    for (int line = 0; line < inNaxis2; line++) {
                        int in_index = inNaxis1 - 1;
                        for (int out_index = 0; out_index < inNaxis1; out_index++) {
                            newData4D[0][0][line][out_index] =
                                    inData4D[0][0][line][in_index];
                            in_index--;
                        }
                    }
                    obj = newData4D;
                    break;

            }

        return obj;
    }

    /**
     * Do the flip along xAxis (naxis2)
     * @param inData
     * @param dim
     * @return
     */
    private Object doFlipInX(Object inData, int dim) {


        Object obj = null;

            switch (dim) {
                case 2:
                    float[][] newData2D = new float[inNaxis2][inNaxis1];
                    float[][] inData2D = (float[][]) inData;

                    for (int line = 0; line < inNaxis1; line++) {
                        int in_index = inNaxis2 - 1;
                        for (int out_index = 0; out_index < inNaxis2; out_index++) {
                            newData2D[out_index][line] = inData2D[in_index][line];
                            in_index--;
                        }
                    }
                    obj = newData2D;
                    break;

                case 3:
                    float[][][] newData3D = new float[1][inNaxis2][inNaxis1];
                    float[][][] inData3D = (float[][][]) inData;

                    for (int line = 0; line < inNaxis1; line++) {

                        int in_index = inNaxis2 - 1;
                        for (int out_index = 0; out_index < inNaxis2; out_index++) {
                            newData3D[0][out_index][line] =
                                    inData3D[0][in_index][line];
                            in_index--;
                        }
                    }
                    obj = newData3D;
                    break;
                case 4:
                    float[][][][] newData4D = new float[1][1][inNaxis2][inNaxis1];
                    float[][][][] inData4D = (float[][][][]) inData;
                    for (int line = 0; line < inNaxis1; line++) {
                        int in_index = inNaxis2 - 1;
                        for (int out_index = 0; out_index < inNaxis2; out_index++) {
                            newData4D[0][0][out_index][line] =
                                    inData4D[0][0][in_index][line];
                            in_index--;
                        }
                    }
                    obj = newData4D;
                    break;

            }

        return obj;
    }
    /**
     * Based on the input fits header to create an output fits header
     * @return
     * @throws HeaderCardException
     */
    private Header getOutFitsHeader() throws HeaderCardException {

        Header outFitsHeader = FitsReadUtil.cloneHeaderFrom(inFitsHeader);
        outFitsHeader.resetOriginalSize();
        outFitsHeader.addValue("CRPIX1",
                inImageHeader.naxis1 - inImageHeader.crpix1 + 1, null);

        if (inImageHeader.using_cd) {

            addCDCards(outFitsHeader);
        } else {

            outFitsHeader.addValue("CDELT1", -inImageHeader.cdelt1, null);
        }

        if (inImageHeader.map_distortion) {

            // negate all even coefficients
            if (inImageHeader.a_order >= 0) {
                addABXOrderCards(outFitsHeader, "A_", inImageHeader.a_order, inImageHeader.a);

            }

            if (inImageHeader.b_order >= 0) {

                addABXOrderCards(outFitsHeader, "B_", inImageHeader.b_order, inImageHeader.b);
            }

            if (inImageHeader.ap_order >= 0) {

                addABXOrderCards(outFitsHeader, "AP_", inImageHeader.ap_order, inImageHeader.ap);
            }

            if (inImageHeader.bp_order >= 0) {
                addABXOrderCards(outFitsHeader, "BP_", inImageHeader.bp_order, inImageHeader.bp);
            }
        }
        return outFitsHeader;
    }

    /**
     *
     * @param outFitsHeader
     * @param xOrder
     * @param length
     * @param values
     * @throws HeaderCardException
     */
    private void addABXOrderCards( Header outFitsHeader, String xOrder, double length, double[][] values) throws HeaderCardException {

        for (int i = 0; i <= length; i += 2) {// do only odd i values
            for (int j = 0; j <= length; j++) {
                if (i + j <= length) {
                    String  keyword = xOrder + i + "_" + j;
                    outFitsHeader.addValue(keyword, - values[i][j], null);

                }
            }
        }
        return;
    }

    /**
     * Add the cd1 and cd2 cards if they exist in the inFitsHeader
     * @param outFitsHeader
     * @throws HeaderCardException
     */
    private void addCDCards( Header outFitsHeader) throws HeaderCardException {

        if (inFitsHeader.containsKey("CD1_1")) {
            double cd1_1 = inFitsHeader.getDoubleValue("CD1_1");
            double cd2_1 = inFitsHeader.getDoubleValue("CD2_1");
            outFitsHeader.addValue("CD1_1", -cd1_1, null);
            outFitsHeader.addValue("CD2_1", -cd2_1, null);
        } else if (inFitsHeader.containsKey("CD001001")) {
            double cd1_1 = inFitsHeader.getDoubleValue("CD001001");
            double cd2_1 = inFitsHeader.getDoubleValue("CD002001");
            outFitsHeader.addValue("CD001001", -cd1_1, null);
            outFitsHeader.addValue("CD002001", -cd2_1, null);
        } else if (inFitsHeader.containsKey("PC1_1")) {
            double cd1_1 = inFitsHeader.getDoubleValue("PC1_1");
            double cd2_1  = inFitsHeader.getDoubleValue("PC2_1");
            outFitsHeader.addValue("PC1_1", -cd1_1, null);
            outFitsHeader.addValue("PC2_1", -cd2_1, null);
        }
    }

}
