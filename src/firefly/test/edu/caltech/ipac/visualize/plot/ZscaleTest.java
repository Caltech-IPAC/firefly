package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.util.FileLoader;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;


/**
 * Created by ymei on 3/28/16.
 * 10/19/16
 *  DM-8028
 *    Use the UnitTestUtility to load file
 * 01/07/2017 DM-7188
 *    Added the setUp() and tearDown(); Directly called Zscale.cdl_zscale;
 */
public class ZscaleTest {

    //Input fits file:
    private static String filename = "WISE-Band-4.fits";

    //Prepare the parameters for calling cdl_zscale:
    private ImageHeader imageHeader = null;
    private float[] float1d = null;

    
    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test for Histogram's public methods
     */
    public void setUp() throws FitsException {

        Fits fits = FileLoader.loadFits(ZscaleTest.class,filename );
        FitsRead[] fry = FitsRead.createFitsReadArray(fits);
        float1d = fry[0].getDataFloat();
        imageHeader = fry[0].getImageHeader();

    }

    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        float1d = null;
        imageHeader = null;
    }


    @Test
    public void testZscalRetval() throws FitsException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {


        int nx = imageHeader.naxis1;
        int ny = imageHeader.naxis2;
        int bitpix = imageHeader.bitpix;
        double blank_value = imageHeader.blank_value;

        double contrast = 0.25;
        int opt_size = 600;
        int len_stdline = 120;


        Zscale.ZscaleRetval zscaleRetval = Zscale.cdl_zscale(float1d, nx, ny, bitpix, contrast, opt_size, len_stdline, blank_value);

        double z1 = zscaleRetval.getZ1();
        double z2 = zscaleRetval.getZ2();

        //Check the result:
        Assert.assertEquals("Should found z1 = " + z1, z1, 418.74435, 1E-5);
        Assert.assertEquals("Should found z2 = " + z2, z2, 2409.418945, 1E-5);

    }

}
