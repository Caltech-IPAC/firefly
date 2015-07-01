/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import nom.tam.fits.ImageData;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 6/19/15 LZ
 * Refactor the codes
 *
 * 6/30/15
 * This is cleaned refactor file.
 * NOTE:
 * I saved the CropAndCenter in myTestBranch where it contains both old and new codes.
 * I Use the TestFitsRead.java class to test this CropAndCenter. The test showed newly refactored codes produce
 * the same results as the old ones.  I am deleting the old codes in the master branch.  If something is wrong,
 * using "myTestBranch" to test it again.
 *
 *
 */
public class CropAndCenter  {

    public static FitsRead do_crop(FitsRead in_fits_read,
                                    double ra, double dec, double radius)
            throws FitsException {

        ImageHeader imageHeader= in_fits_read.getImageHeader();
        CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);
        Projection proj = imageHeader.createProjection(in_coordinate_sys);
        try
        {
            ProjectionPt proj_pt = proj.getImageCoords( ra, dec);
            int center_x = (int) proj_pt.getFsamp();
            int center_y = (int) proj_pt.getFline();
            double cdelt2 = imageHeader.cdelt2;
            int radius_pixels = (int) (radius / cdelt2);
            int min_x = center_x - radius_pixels;
            int max_x = center_x + radius_pixels;
            int  min_y = center_y - radius_pixels;
            int max_y = center_y + radius_pixels;

            if (SUTDebug.isDebug())
            {
                System.out.println("RBH do_crop  min_x = " + min_x +
                        "  min_y = " + min_y + "  max_x = " + max_x + "  max_y = " + max_y);
            }
            BasicHDU myHDU = in_fits_read.getHDU();

            Fits ret_fits = new Fits();
            BasicHDU out_HDU = splitFITSCube(myHDU, min_x, min_y, max_x, max_y);;
            ret_fits.addHDU(out_HDU);
            FitsRead[] fits_read_array = FitsRead.createFitsReadArray(ret_fits);

            return(fits_read_array[0]);

        }
        catch (ProjectionException pe)
        {
            if (SUTDebug.isDebug())
            {
                System.out.println("CropAndCenter: got ProjectionException: " + pe.getMessage());
            }
            throw new FitsException("Could not crop image.\n -  got ProjectionException: " + pe.getMessage());
        }



    }
    /*
      This method create a new Fits header based on the input Fits header
     */
    private static Header getNewHeader(Header headerIn, int newNaxis1,int newNaxis2, int min_x, int min_y) throws HeaderCardException {
        Header newHeader = clone_header(headerIn);

        newHeader.addValue("NAXIS1" , newNaxis1, null);
        newHeader.addValue("NAXIS2" , newNaxis2, null);

        float crpix1 = headerIn.getFloatValue("CRPIX1",Float.NaN);
        float crpix2 = headerIn.getFloatValue("CRPIX2",Float.NaN);
        float cnpix1 = headerIn.getFloatValue("CNPIX1",Float.NaN);
        float cnpix2 = headerIn.getFloatValue("CNPIX2",Float.NaN);

        if (headerIn.containsKey("PLTRAH")) {

            /* it's a PLATE projection */
            newHeader.addValue("CNPIX1" , cnpix1 + min_x, null);
            newHeader.addValue("CNPIX2" , cnpix2 + min_y, null);
        }
        else {
              newHeader.addValue("CRPIX1" , crpix1 - min_x, null);
            newHeader.addValue("CRPIX2" , crpix2 - min_y, null);
        }
        return newHeader;
    }

    /**
     * This method based on the naxis to create a new output data array and then create a new Fits ImageData.
     *
     * @return
     */
    private static ImageData getNewImageData(Object dataIn, int naxis,
                                            int naxis1, int naxis2,
                                            int newNaxis1, int newNaxis2,
                                           int minX, int maxX,
                                            int minY, int maxY) {
        switch (naxis) {
            case 2: {
                float[][] data = new float[newNaxis2][newNaxis1];
                int yOut = 0;
                for (int y = minY; y <= maxY; y++) {
                    int xOut = 0;
                    for (int x = minX; x <= maxX; x++) {
                        if ((x < 0) || (x >= naxis1) || (y < 0) || (y >= naxis2)) {
                            data[yOut][xOut] = Float.NaN;
                        } else {
                            data[yOut][xOut] = ((float[][]) dataIn)[y][x];
                        }
                        xOut++;
                    }
                    yOut++;
                }
                return new ImageData(data);
            }

            case 3: {
                float[][][] data = new float[1][newNaxis2][newNaxis1];
                int yOut = 0;
                for (int y = minY; y <= maxY; y++) {
                    int xOut = 0;
                    for (int x = minX; x <= maxX; x++) {
                      if ((x < 0) || (x >= naxis1) || (y < 0) || (y >= naxis2)) {
                            data[0][yOut][xOut] = Float.NaN;
                        } else {
                            data[0][yOut][xOut] = ((float[][][]) dataIn)[0][y][x];
                        }
                        xOut++;
                    }


                    yOut++;
                }
               return new ImageData(data);
            }

            case 4: {
                float[][][][] data = new float[1][1][newNaxis2][newNaxis1];
                int yOut = 0;
                for (int y = minY; y <= maxY; y++) {
                    int xOut = 0;
                    for (int x = minX; x <= maxX; x++) {
                        if ((x < 0) || (x >= naxis1) || (y < 0) || (y >= naxis2)) {
                            data[0][0][yOut][xOut] = Float.NaN;
                        } else {
                            data[0][0][yOut][xOut] = ((float[][][][]) dataIn)[0][0][y][x];
                        }
                        xOut++;
                    }
                    yOut++;
                }

                return new ImageData(data);
            }

        }
        return null;
    }
    /*
     This method get a new BasicHDU in the cropped area
     */
    private static BasicHDU splitFITSCube(BasicHDU hdu,
                                            int min_x, int min_y, int max_x, int max_y)
            throws FitsException {


        Header header = hdu.getHeader();
        int naxis = header.getIntValue("NAXIS",0);
        int naxis1 = header.getIntValue("NAXIS1",0);
        int naxis2 = header.getIntValue("NAXIS2",0);
        int naxis3 = naxis > 2?header.getIntValue("NAXIS3",0): 0;
        int naxis4 = naxis > 3?header.getIntValue("NAXIS4",0):0;
        float cdelt2 = hdu.getHeader().getFloatValue("CDELT2", Float.NaN);
        if (cdelt2 < 0) {
             min_y = naxis2 - min_y - 1;
             max_y = naxis2 - max_y - 1;
        }
        if ((naxis3 > 1) || (naxis4 > 1))
            throw new FitsException("cannot crop a FITS cube");
        if (min_x > max_x) {

            int temp_x = min_x;
            min_x = max_x;
            max_x = temp_x;
        }
        if (min_y > max_y) {

            int temp_y = min_y;
            min_y = max_y;
            max_y = temp_y;
        }

        int newNaxis1 = max_x - min_x + 1;
        int newNaxis2 = max_y - min_y + 1;

        Header newHeader =getNewHeader(header, newNaxis1, newNaxis2,min_x, min_y );

        Object dataIn = hdu.getData().getData();

        ImageData newImageData =getNewImageData(dataIn, naxis, naxis1, naxis2,
                 newNaxis1, newNaxis2, min_x, max_x, min_y, max_y);
        if (newImageData==null){
            throw new FitsException("create a new fits ImageData failed, please check your input");
        }

        return  new ImageHDU(newHeader, newImageData);


    }

    static Header clone_header(Header header)
    {
        // first collect cards from old header
        Cursor iter = header.iterator();
        String cards[] = new String[header.getNumberOfCards()];
        int i = 0;
        while (iter.hasNext())
        {
            HeaderCard card = (HeaderCard) iter.next();
             cards[i] = card.toString();
             i++;
        }
        return(new Header(cards));
    }


    static void usage()
    {
        System.out.println(
                "Usage: java edu.caltech.ipac.visualize.plot.CropAndCenter source ra dec radius destination");
        System.exit(0);
    }


    // main is for testing only
    public static void main(String[] args)throws IOException {

        int i;
        double ra = Double.NaN;
        double dec = Double.NaN;
        double radius = Double.NaN;
        Fits inFits = null;
       // Fits refFits = null;
        Fits newFits;
       // CropAndCenter crop;
        FitsRead[] fits_read_array;
        FitsRead fits_read_0 = null;

        if (args.length < 5)
            usage();

        String in_name = args[0];
        try
        {
            ra = Double.parseDouble(args[1]);
            dec = Double.parseDouble(args[2]);
            radius = Double.parseDouble(args[3]);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("bad number for ra or dec or radius");
            usage();
        }

       // crop = new CropAndCenter();
        String out_name = args[4];

        File file = new File(in_name);
        if (!file.exists())
        {
            System.out.println("ERROR: file does not exist: " + in_name);
            System.exit(1);
        }

        try
        {
            inFits = new Fits(in_name);
            fits_read_array = FitsRead.createFitsReadArray(inFits);
            fits_read_0 = fits_read_array[0];

        }
        catch (FitsException e)
        {
            System.out.println("got FitsException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }


        try
        {

            FitsRead newFitsRead = CropAndCenter.do_crop(fits_read_0, ra, dec, radius); //crop.do_crop(fits_read_0, ra, dec, radius); //
            newFits = newFitsRead.createNewFits();
            FileOutputStream fo = new java.io.FileOutputStream(out_name);
            BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
            newFits.write(o);

        }
        catch (FileNotFoundException e)
        {
            System.out.println("got FileNotFoundException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        catch (FitsException e)
        {
            System.out.println("got FitsException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        catch (OutOfMemoryError e)
        {
            System.out.println("got OutOfMemoryError");
            e.printStackTrace();
            System.exit(1);
        }
    }

}

