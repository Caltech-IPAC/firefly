/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import nom.tam.fits.*;
import nom.tam.fits.ImageData;
import nom.tam.image.ImageTiler;
import nom.tam.util.ArrayFuncs;
import nom.tam.util.BufferedDataOutputStream;
import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.visualize.plot.projection.Projection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Crop an image directly from disk
 * LZ 11/15/16
 *   DM-8049
 *   Minor refactor the codes and remove the switch(bitpix).  Convert to float to do the calculation and convert to
 *   the original type defined by bitpixel when create ImageData.
 */


public class CropFile {

    static public boolean isCropable(Fits fits) {
        BasicHDU hdus[];
        try {
            hdus = fits.read();
        } catch (FitsException fe) {
            return (false);
        }
        if (hdus.length > 1) {
            return (false);
        }
        Header header = hdus[0].getHeader();
        int naxis = header.getIntValue("NAXIS");
        if (naxis < 3) {
            return (true);
        }
        int naxis3 = header.getIntValue("NAXIS3");
        if (naxis3 > 1) {
            return (false);
        } else {
            return (true);
        }
    }

    /**
     * Crop an image directly from a file on disk, given world coordinates
     *
     * @param fits   Fits object for file on disk
     * @param wpt    WorldPt of center of desired crop box
     * @param radius Radius in degrees of desired area
     * @return Fits object with cropped image
     */

    static public Fits do_crop(Fits fits, WorldPt wpt, double radius)
            throws FitsException, IOException, ProjectionException {
        ImageHDU h = (ImageHDU) fits.readHDU();
        Header old_header = h.getHeader();
        ImageHeader temp_hdr = new ImageHeader(old_header);
        CoordinateSys in_coordinate_sys = CoordinateSys.makeCoordinateSys(
                temp_hdr.getJsys(), temp_hdr.file_equinox);
        Projection in_proj = temp_hdr.createProjection(in_coordinate_sys);
        ProjectionPt ipt = in_proj.getImageCoords(wpt.getLon(), wpt.getLat());
        double x = ipt.getFsamp();
        double y = ipt.getFline();
        double x_size = 2 * radius / Math.abs(temp_hdr.cdelt1);
        if (SUTDebug.isDebug()) {
            System.out.println("x = " + x + "  y = " + y + "  x_size = " + x_size);

        }
        Fits out_fits = common_crop(h, old_header,
                (int) x, (int) y, (int) x_size, (int) x_size);
        return (out_fits);
    }

    /**
     * Crop an image directly from a file on disk, given image coordinates
     *
     * @param inFits Fits object for file on disk
     * @param min_x  first pixel of crop box
     * @param min_y  first line of crop box
     * @param max_x  last pixel of crop box
     * @param max_y  last line of crop box
     * @return Fits object with cropped image
     */

    static public Fits do_crop(Fits inFits, int min_x, int min_y, int max_x, int max_y)
            throws FitsException, IOException {
        int x_center = (min_x + max_x) / 2;
        int y_center = (min_y + max_y) / 2;
        int x_size = Math.abs(max_x - min_x);
        int y_size = Math.abs(max_y - min_y);

        ImageHDU h = (ImageHDU) inFits.readHDU();
        Header old_header = h.getHeader();

        Fits out_fits =
                common_crop(h, old_header, x_center, y_center, x_size, y_size);
        return (out_fits);
    }

    /**
     * Crop images from a FITS file with extensions, given image coordinates
     * Read the images directly from the FITS file on disk and write the
     * output to disk
     *
     * @param in_filename  FITS file on disk
     * @param out_filename output FITS file on disk
     * @param min_x        first pixel of crop box
     * @param min_y        first line of crop box
     * @param max_x        last pixel of crop box
     * @param max_y        last line of crop box
     */

    static public void crop_extensions(String in_filename, String out_filename,
                                       int min_x, int min_y, int max_x, int max_y)
            throws FitsException, IOException {
        Fits in_fits = new Fits(in_filename);
        Fits out_fits = new Fits();

        int x_center = (min_x + max_x) / 2;
        int y_center = (min_y + max_y) / 2;
        int x_size = Math.abs(max_x - min_x);
        int y_size = Math.abs(max_y - min_y);

        int extension = 0;
        while (true) {
            BasicHDU hdu = in_fits.getHDU(extension);
            BasicHDU new_hdu;
            if (hdu == null)
                break;
            if (hdu instanceof ImageHDU) {
                ImageHDU h = (ImageHDU) hdu;
                Header old_header = h.getHeader();
                int naxis = old_header.getIntValue("NAXIS");
                if (naxis == 0) {
            /* it's a null image - probably the primary image */
                    new_hdu = hdu;
                } else {
                    Fits temp_fits = common_crop(h, old_header,
                            x_center, y_center, x_size, y_size);
                    new_hdu = temp_fits.getHDU(0);
                }
            } else {
		/* not an ImageHDU - just copy input to output */
                new_hdu = hdu;
            }
            out_fits.addHDU(new_hdu);
            extension++;
        }

        FileOutputStream fo = new java.io.FileOutputStream(out_filename);
        BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
        out_fits.write(o);
    }


    /**
     * Crop an image directly from a file on disk with extension images, given image coordinates
     *
     * @param inFits    Fits object for file on disk
     * @param extension 0=primary header, 1 = first extension HDU, ...
     * @param min_x     first pixel of crop box
     * @param min_y     first line of crop box
     * @param max_x     last pixel of crop box
     * @param max_y     last line of crop box
     * @return Fits object with cropped image
     */

    static public Fits do_crop(Fits inFits, int extension,
                               int min_x, int min_y, int max_x, int max_y)
            throws FitsException, IOException {
        int x_center = (min_x + max_x) / 2;
        int y_center = (min_y + max_y) / 2;
        int x_size = Math.abs(max_x - min_x);
        int y_size = Math.abs(max_y - min_y);

        ImageHDU h = (ImageHDU) inFits.getHDU(extension);
        Header old_header = h.getHeader();

        Fits out_fits =
                common_crop(h, old_header, x_center, y_center, x_size, y_size);
        return (out_fits);
    }

//    /**
//    * Crop an image directly from a file on disk, given image coordinates
//    * @param fits Fits object for file on disk
//    * @param x_center center of crop box
//    * @param y_center center of crop box
//    * @param x_size width in pixels of crop box
//    * @param y_size height in pixels of crop box
//    * @return Fits object with cropped image
//    */
//
//    public Fits do_crop(Fits fits, int x_center, int y_center, 
//	int x_size, int y_size)
//	throws FitsException, IOException
//    {
//	ImageHDU h = (ImageHDU) fits.readHDU();
//	Header old_header = h.getHeader();
//	
//	Fits out_fits = 
//	    common_crop(h, old_header, x_center, y_center, x_size, y_size);
//	return (out_fits);
//    }

    /**
     * LZ 11/15/16
     *   DM-8049
     *   Minor refactor the codes and add this method to simplify the common_crop method
     * @param h
     * @param old_header
     * @param xCenter
     * @param yCenter
     * @param xSize
     * @param ySize
     * @return
     * @throws HeaderCardException
     */
    private static Header getNewHeader(ImageHDU h, Header old_header,
                                       int xCenter, int yCenter, int xSize, int ySize) throws HeaderCardException {
        Header newHeader = CropAndCenter.clone_header(old_header);

        int naxis1 = old_header.getIntValue("NAXIS1");
        int naxis2 = old_header.getIntValue("NAXIS2");

        int minX = xCenter - xSize / 2;
        int maxX = xCenter + xSize / 2;
        int minY = yCenter - ySize / 2;
        int maxY = yCenter + ySize / 2;
        if (minX < 0)
            minX = 0;
        if (maxX >= naxis1)
            maxX = naxis1 - 1;
        if (minY < 0)
            minY = 0;
        if (maxY >= naxis2)
            maxY = naxis2 - 1;
        int newNaxis1 = maxX - minX;
        int newNaxis2 = maxY - minY;

        if (SUTDebug.isDebug()) {
            System.out.println("minX = " + minX +
                    " maxX = " + maxX +
                    " minY = " + minY +
                    " maxY = " + maxY +
                    " newNaxis1 = " + newNaxis1 +
                    " newNaxis2 = " + newNaxis2);
        }

        newHeader.addValue("NAXIS1", newNaxis1, null);
        newHeader.addValue("NAXIS2", newNaxis2, null);

        float crpix1 = old_header.getFloatValue("CRPIX1", Float.NaN);
        newHeader.addValue("CRPIX1", (crpix1 - minX), null);
        float crpix2 = old_header.getFloatValue("CRPIX2", Float.NaN);
        newHeader.addValue("CRPIX2", (crpix2 - minY), null);

        if (newHeader.containsKey("PLTRAH")) {
	        /* it's a PLATE projection  */
            double x_pixel_offset, y_pixel_offset;

            x_pixel_offset = newHeader.getDoubleValue("CNPIX1");
            newHeader.addValue("CNPIX1", (x_pixel_offset + minX), null);
            y_pixel_offset = newHeader.getDoubleValue("CNPIX2");
            newHeader.addValue("CNPIX2", (y_pixel_offset + minY), null);
        }

        return newHeader;

    }

    /**
     *
     * LZ 11/15/16
     *   DM-8049
     *   Minor refactor this  method
     * @param h
     * @param old_header
     * @param xCenter
     * @param yCenter
     * @param xSize
     * @param ySize
     * @return
     * @throws FitsException
     * @throws IOException
     */
    static private Fits common_crop(ImageHDU h, Header old_header,
                                    int xCenter, int yCenter, int xSize, int ySize)
            throws FitsException, IOException {
        if (SUTDebug.isDebug()) {
            System.out.println("entering common_crop:  xCenter = " + xCenter +
                    "  yCenter = " + yCenter + "  xSize = " + xSize + "  ySize = " + ySize);
        }


	    /* first, do the header */
        Header newHeader = getNewHeader(h,old_header, xCenter, yCenter, xSize,  ySize);
        int newNaxis1 = newHeader.getIntValue("NAXIS1");
        int newNaxis2 = newHeader.getIntValue("NAXIS2");

	    /* now do the pixels */
        int[] tileOffset;
        int[] tileSize;
        int naxis3 = 0;
        ImageTiler tiler = h.getTiler();
        int naxis = old_header.getIntValue("NAXIS");
        int minX = xCenter - xSize / 2;
        int maxX = xCenter + xSize / 2;
        int minY = yCenter - ySize / 2;
        int maxY = yCenter + ySize / 2;

        if (naxis == 2) {
            tileOffset = new int[]{minY, minX};
            tileSize = new int[]{maxY - minY, maxX - minX};

        } else if (naxis == 3) {
            naxis3 = newHeader.getIntValue("NAXIS3");
            tileOffset = new int[]{0, minY, minX};
            tileSize = new int[]{naxis3, maxY - minY, maxX - minX};
        } else {
            throw new FitsException(
                    "Cannot crop images with NAXIS other than 2 or 3");
        }

        int dims2[] = new int[]{newNaxis1, newNaxis2};
        int dims3[] = new int[]{newNaxis1, newNaxis2, naxis3};

        ImageData neImageData = null;
       //convert the data to float
        float[] objm32 = (float[]) ArrayFuncs.convertArray(tiler.getTile(tileOffset, tileSize), Float.TYPE, true);
        if (naxis == 2) {
            // make 2dim
            float[][]  float2d = (float[][]) ArrayFuncs.curl(objm32, dims2);
            //convert back to original type
            neImageData = new ImageData(ArrayFuncs.convertArray(float2d, FitsRead.getDataType(newHeader.getIntValue("BITPIX")), true) );
        } else {

            // make 3dim
            float[][][]  float3d = (float[][][]) ArrayFuncs.curl(objm32, dims3);
            neImageData = new ImageData(ArrayFuncs.convertArray(float3d, FitsRead.getDataType(newHeader.getIntValue("BITPIX")), true) );
        }

        ImageHDU newImageHDU = new ImageHDU(newHeader, neImageData);
        Fits outFits = new Fits();
        outFits.addHDU(newImageHDU);
        return (outFits);
    }


    private static void usage() {
        System.out.println(
                "usage: java CropFile <input_file> <output_file> <min_x> <min_y> <max_x> <max_y>");
        System.out.println(
                "or: java CropFile <input_file> <output_file> <extension> <min_x> <min_y> <max_x> <max_y>");
        System.out.println(
                "or: java CropFile <input_file> <output_file> <ra> <dec> <radius>");
        System.exit(1);
    }

    public static void main(String args[])
            throws FitsException, IOException, ProjectionException {
        String in_name;
        String out_name;
        int min_x = 0;
        int min_y = 0;
        int max_x = 0;
        int max_y = 0;
        int extension = -1;
        double ra = 0.0;
        double dec = 0.0;
        double radius = 0.0;
        Fits out_fits;

        if (args.length == 6) {
            min_x = Integer.parseInt(args[2]);
            min_y = Integer.parseInt(args[3]);
            max_x = Integer.parseInt(args[4]);
            max_y = Integer.parseInt(args[5]);
        } else if (args.length == 7) {
            extension = Integer.parseInt(args[2]);
            min_x = Integer.parseInt(args[3]);
            min_y = Integer.parseInt(args[4]);
            max_x = Integer.parseInt(args[5]);
            max_y = Integer.parseInt(args[6]);
        } else if (args.length == 5) {
            ra = Double.parseDouble(args[2]);
            dec = Double.parseDouble(args[3]);
            radius = Double.parseDouble(args[4]);
        } else
            usage();

        in_name = args[0];
        out_name = args[1];

        File file = new File(args[0]);
        if (!file.canRead()) {
            System.err.println("Cannot open file " + args[0]);
            System.exit(1);
        }

        Fits f = new Fits(args[0]);

        System.out.println("CropFile.isCropable  = " + CropFile.isCropable(f));

        f = new Fits(args[0]);
        if (args.length == 5) {
            out_fits = CropFile.do_crop(f, new WorldPt(ra, dec), radius);
        } else if (args.length == 6) {
            out_fits = CropFile.do_crop(f, min_x, min_y, max_x, max_y);
        } else {
            out_fits = CropFile.do_crop(f, extension, min_x, min_y, max_x, max_y);
        }
        FileOutputStream fo = new java.io.FileOutputStream(out_name);
        BufferedDataOutputStream o = new BufferedDataOutputStream(fo);
        out_fits.write(o);
    }
}
