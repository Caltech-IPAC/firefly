/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;
import edu.caltech.ipac.util.Assert;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.Cursor;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


public class Crop
{

    public static Fits do_crop(Fits inFits, int min_x, int min_y, int max_x, int max_y)
            throws FitsException
    {
        if (SUTDebug.isDebug())
        {
            System.out.println("RBH do_crop  min_x = " + min_x +
                    "  min_y = " + min_y + "  max_x = " + max_x + "  max_y = " + max_y);
        }
        BasicHDU out_HDU = null;
        Fits ret_fits = new Fits();
        BasicHDU[] myHDUs = inFits.read();
        out_HDU = split_FITS_cube(myHDUs[0], min_x, min_y, max_x, max_y);
        ret_fits.addHDU(out_HDU);
        return(ret_fits);
    }

    private static BasicHDU split_FITS_cube(BasicHDU hdu,
                                            int min_x, int min_y, int max_x, int max_y)
            throws FitsException
    {
        int x,y;
        int x_out, y_out;
        byte[][] data8;
        byte[][][] data8x3;
        byte[][][][] data8x4;
        int[][] data32;
        int[][][] data32x3;
        int[][][][] data32x4;
        short[][] data16;
        short[][][] data16x3;
        short[][][][] data16x4;
        float[][] datam32;
        float[][][] datam32x3;
        float[][][][] datam32x4;
        double[][] datam64;
        double[][][] datam64x3;
        double[][][][] datam64x4;
        Header header = hdu.getHeader();
        ImageData new_image_data = null;
        int bitpix = header.getIntValue("BITPIX",-1);
        int naxis = header.getIntValue("NAXIS",0);
        int naxis1 = header.getIntValue("NAXIS1",0);
        int naxis2 = header.getIntValue("NAXIS2",0);
        int naxis3 = 0;
        if (naxis > 2)
            naxis3 = header.getIntValue("NAXIS3",0);
        int naxis4 = 0;
        if (naxis > 3)
            naxis4 = header.getIntValue("NAXIS4",0);
        float crpix1 = header.getFloatValue("CRPIX1",Float.NaN);
        float crpix2 = header.getFloatValue("CRPIX2",Float.NaN);
        float cnpix1 = header.getFloatValue("CNPIX1",Float.NaN);
        float cnpix2 = header.getFloatValue("CNPIX2",Float.NaN);
        float cdelt2 = header.getFloatValue("CDELT2",Float.NaN);
        if (cdelt2 < 0)
        {
            min_y = naxis2 - min_y - 1;
            max_y = naxis2 - max_y - 1;
        }
        //System.out.println("crpix1 = " + crpix1 + "  cnpix1 = " + cnpix1);
        if ((naxis3 > 1) || (naxis4 > 1))
            throw new FitsException("cannot crop a FITS cube");
        if (min_x > max_x)
        {
            int temp_x = min_x;
            min_x = max_x;
            max_x = temp_x;
        }
        if (min_y > max_y)
        {
            int temp_y = min_y;
            min_y = max_y;
            max_y = temp_y;
        }
        if (min_x < 0)
            min_x = 0;
        if (max_x >= naxis1)
            max_x = naxis1 - 1;
        if (min_y < 0)
            min_y = 0;
        if (max_y >= naxis2)
            max_y = naxis2 - 1;
        /*
      if ((min_x < 0) || (max_x > naxis1) || (min_y < 0) || (max_y > naxis2))
          throw new FitsException("crop value out of range");
      */
        int new_naxis1 = max_x - min_x + 1;
        int new_naxis2 = max_y - min_y + 1;
        BasicHDU retval ;
        Header new_header = clone_header(header);
        /*
      new_header.setNaxis(1, new_naxis1);
      new_header.setNaxis(2, new_naxis2);
      */
        new_header.addValue("NAXIS1" , new_naxis1, null);
        new_header.addValue("NAXIS2" , new_naxis2, null);
        if (header.containsKey("PLTRAH"))
        {
            /* it's a PLATE projection */
            new_header.addValue("CNPIX1" , cnpix1 + min_x, null);
            new_header.addValue("CNPIX2" , cnpix2 + min_y, null);
        }
        else
        {
            /* it's some non-PLATE projection */
            float new_crpix1 = crpix1 - min_x;
            //System.out.println("setting CRPIX1 from " + crpix1 + "  to " + new_crpix1 );
            new_header.addValue("CRPIX1" , crpix1 - min_x, null);
            new_header.addValue("CRPIX2" , crpix2 - min_y, null);
        }

        switch (bitpix)
        {
            case 32:
                if (naxis4 == 1)
                {
                    data32x4 = (int[][][][]) hdu.getData().getData();
                    int[][][][] new_data32x4 = new int[1][1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data32x4[0][0][y_out][x_out] = data32x4[0][0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data32x4);
                }
                else if (naxis3 == 1)
                {
                    data32x3 = (int[][][]) hdu.getData().getData();
                    int[][][] new_data32x3 = new int[1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            //System.out.println("x = " + x + "  y = " + y +
                            //"  pixel = " + data32x3[0][y][x]);
                            new_data32x3[0][y_out][x_out] = data32x3[0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    //System.out.println("first pixel = " + new_data32x3[0][0][0]);
                    new_image_data =
                            new ImageData(new_data32x3);
                }
                else
                {
                    data32 = (int[][]) hdu.getData().getData();
                    int[][] new_data32 = new int[new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data32[y_out][x_out] = data32[y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data32);
                }
                break;
            case 16:
                if (naxis4 == 1)
                {
                    data16x4 = (short[][][][]) hdu.getData().getData();
                    short[][][][] new_data16x4 = new short[1][1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data16x4[0][0][y_out][x_out] = data16x4[0][0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data16x4);
                }
                else if (naxis3 == 1)
                {
                    data16x3 = (short[][][]) hdu.getData().getData();
                    short[][][] new_data16x3 = new short[1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            //System.out.println("x = " + x + "  y = " + y +
                            //"  pixel = " + data16x3[0][y][x]);
                            new_data16x3[0][y_out][x_out] = data16x3[0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data16x3);
                }
                else
                {
                    data16 = (short[][]) hdu.getData().getData();
                    short[][] new_data16 = new short[new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data16[y_out][x_out] = data16[y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data16);
                }
                break;
            case 8:
                if (naxis4 == 1)
                {

                    data8x4 = (byte[][][][]) hdu.getData().getData();
                    byte[][][][] new_data8x4 = new byte[1][1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data8x4[0][0][y_out][x_out] = data8x4[0][0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data8x4);
                }
                else if (naxis3 == 1)
                {

                    data8x3 = (byte[][][]) hdu.getData().getData();
                    byte[][][] new_data8x3 = new byte[1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data8x3[0][y_out][x_out] = data8x3[0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_data8x3);
                }
                else
                {
                    data8 = (byte[][]) hdu.getData().getData();
                    byte[][] new_data8 = new byte[new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_data8[y_out][x_out] = data8[y][x];
                            x_out++;
                        }
                        y_out++;
                    }

                    new_image_data =
                            new ImageData(new_data8);
                }
                break;
            case -32:
                if (naxis4 == 1)
                {
                    datam32x4 = (float[][][][]) hdu.getData().getData();
                    float[][][][] new_datam32x4 = new float[1][1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam32x4[0][0][y_out][x_out] = datam32x4[0][0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_datam32x4);
                }
                else if (naxis3 == 1)
                {

                    datam32x3 = (float[][][]) hdu.getData().getData();
                    float[][][] new_datam32x3 = new float[1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam32x3[0][y_out][x_out] = datam32x3[0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_datam32x3);
                }
                else
                {
                    datam32 = (float[][]) hdu.getData().getData();
                    float[][] new_datam32 = new float[new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam32[y_out][x_out] = datam32[y][x];
                            x_out++;
                        }
                        y_out++;
                    }

                    new_image_data =
                            new ImageData(new_datam32);
                }

                break;
            case -64:
                if (naxis4 == 1)
                {

                    datam64x4 = (double[][][][]) hdu.getData().getData();
                    double[][][][] new_datam64x4 = new double[1][1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam64x4[0][0][y_out][x_out] = datam64x4[0][0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_datam64x4);
                }
                else if (naxis3 == 1)
                {

                    datam64x3 = (double[][][]) hdu.getData().getData();
                    double[][][] new_datam64x3 = new double[1][new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam64x3[0][y_out][x_out] = datam64x3[0][y][x];
                            x_out++;
                        }
                        y_out++;
                    }
                    new_image_data =
                            new ImageData(new_datam64x3);
                }
                else
                {
                    datam64 = (double[][]) hdu.getData().getData();
                    double[][] new_datam64 = new double[new_naxis2][new_naxis1];

                    y_out = 0;
                    for (y = min_y; y <= max_y; y++)
                    {
                        x_out = 0;
                        for (x = min_x; x <= max_x; x++)
                        {
                            new_datam64[y_out][x_out] = datam64[y][x];
                            x_out++;
                        }
                        y_out++;
                    }

                    new_image_data =
                            new ImageData(new_datam64);
                }
                break;
            default:
                Assert.tst(false,
                        "FitsRead.split_FITS_cube:  Unimplemented bitpix = " +
                                bitpix);
        }
        retval = new ImageHDU(new_header, new_image_data);

        return retval;
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
            //System.out.println("RBH card.toString() = " + card.toString());
            cards[i] = card.toString();
            i++;
        }
        return(new Header(cards));
    }


    static void usage()
    {
        System.out.println(
                "Usage: java edu.caltech.ipac.visualize.plot.Crop source min_x min_y max_x max_y  destination");
        System.exit(0);
    }

    // main is for testing only
    public static void main(String[] args)
    {
        int i;
        int min_x = 0, min_y = 0, max_x = 0, max_y = 0;
        Fits inFits = null;
        Fits refFits = null;
        Fits newFits;
        Crop crop;

        if (false)
        {
            System.err.println("Enter a CR:");
            try
            {
                int c = System.in.read();
            }
            catch (IOException e)
            {
            }
        }



        if (args.length < 6)
            usage();

        String in_name = args[0];
        try
        {
            min_x = Integer.parseInt(args[1]);
            min_y = Integer.parseInt(args[2]);
            max_x = Integer.parseInt(args[3]);
            max_y = Integer.parseInt(args[4]);
        }
        catch (NumberFormatException nfe)
        {
            System.out.println("bad number for min_x or min_y or max_x or max_y");
            usage();
        }
        String out_name = args[5];

        try
        {
            inFits = new Fits(in_name);
        }
        catch (FitsException e)
        {
            System.out.println("got FitsException: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }


        try
        {
            newFits = Crop.do_crop(inFits, min_x, min_y, max_x, max_y);

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

