package edu.caltech.ipac.visualize;

import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.Zscale;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


/**
 * Created by ymei on 3/28/16.
 * 10/19/16
 *  DM-8028
 *    Use teh UnitTestUtility to load file
 */
public class ZscaleTest {

    private static String filename = "WISE-Band-4.fits";
    

    @Test
    public void testZscalRetval() throws FitsException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        //String inFitsName =  TEST_ROOT + filename;
        // For IntelliJ run:
        //String inFitsName =  filename;
        Fits fits = FileLoader.loadFits(ZscaleTest.class,filename );
        FitsRead[] fry = FitsRead.createFitsReadArray(fits);
        float[] float1d = fry[0].getDataFloat();
        ImageHeader imageHeader = fry[0].getImageHeader();

        //Make rangeValues:
        int    lowerWhich = 91;
        double lowerValue = 1.0;
        int    upperWhich = 91;
        double upperValue = 1.0;
        double betaValue = 1.0;
        double gammaValue = 2.0;
        int    algorithm = 45;
        int    zscale_contrast = 25;
        int    zscale_samples = 600;
        int    zscale_samples_per_line = 120;
        double bias = 0.5;
        double contrast = 1.0;
        RangeValues rangeValues = new RangeValues(lowerWhich, lowerValue, upperWhich, upperValue, betaValue, gammaValue,  algorithm, zscale_contrast, zscale_samples, zscale_samples_per_line, bias, contrast);


        //Make the method getZscaleValue accessible:
        Method m = FitsRead.class.getDeclaredMethod("getZscaleValue", float[].class, ImageHeader.class, RangeValues.class);
        m.setAccessible(true);
        Zscale.ZscaleRetval zscaleRetval = (Zscale.ZscaleRetval)m.invoke(fry[0], new Object[]{float1d, imageHeader, rangeValues});

        double z1 = zscaleRetval.getZ1();
        double z2 = zscaleRetval.getZ2();

        //Check the result:
        Assert.assertEquals("Should found z1 = " + z1, z1, 418.74435, 1E-5);
        Assert.assertEquals("Should found z2 = " + z2, z2, 2409.418945, 1E-5);

    }

}
