package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.util.FitsValidation;
import nom.tam.fits.*;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by zhang on 1/6/17.
 * The Geom class has no public methods.  It has one package public method "open_in".
 */
public class GeomTest extends FitsValidation {



    private static String fileName = "f3.fits";
    private  Fits  inFits=null;
    private ImageHeader expectedImageHeader=null;
    private  Geom geom=null;


    @Before
    /**
     * An one dimensional array is created and it is used to run the unit test
     */
    public void setUp() throws FitsException, ClassNotFoundException, IOException {

        inFits = FileLoader.loadFits(GeomTest.class, fileName);

        //get the expected ImageHeader from calling it directly
        ImageHDU imageHdu = (ImageHDU) inFits.getHDU(0);
        Header header =imageHdu.getHeader();
        int planeNumber = header.getIntValue("SPOT_PL", 0);
        int extension_number = header.getIntValue("SPOT_EXT", -1);
        long HDU_offset;
        if (extension_number == -1) {
            HDU_offset = imageHdu.getFileOffset();
        } else {
            HDU_offset = header.getIntValue("SPOT_OFF", 0);
        }

        if (HDU_offset < 0) HDU_offset = 0;
        expectedImageHeader = new ImageHeader(header, HDU_offset,planeNumber);

        geom = new Geom();
    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        expectedImageHeader=null;
        geom=null;

    }

    @Test
    public void testOpen_in() throws FitsException, IOException, GeomException, IllegalAccessException {
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        ImageHeader calculatedImageHeader = geom.open_in(fitsRead0);
        validateImageHeader(expectedImageHeader, calculatedImageHeader);
    }
    private void validateImageHeader(ImageHeader exptImageHeader, ImageHeader actImageHeader) throws IllegalAccessException {
        Class<?> objClass =  exptImageHeader.getClass();
        Field[] exptfields = objClass.getDeclaredFields();
        objClass =  actImageHeader.getClass();
        Field[] actfields = objClass.getDeclaredFields();

        Assert.assertEquals(exptfields.length, actfields.length);


        for (int i=0; i<exptfields.length; i++){
            Object expObj = exptfields[i].get(exptImageHeader);
            Object acuObj = actfields[i].get(actImageHeader);

            if (exptImageHeader.cdelt2<0.0) { /* pixels are upside down - reverse them in y, so credet2 and crpix2 are updated */
                if (exptfields[i].getName().equalsIgnoreCase("crpix2") || exptfields[i].getName().equalsIgnoreCase("cdelt2")) {
                    Assert.assertNotEquals(expObj,acuObj);
                }
            }
            else {
                Assert.assertEquals( expObj,acuObj);
            }
        }

    }

    @Test
    public void endToEndTest() throws FitsException, IOException, GeomException {


        testGeomDefault();
        testComputeTiePoints();
        testUsingBiLinear();
        testUsingNearestNeighbor();
        testDeriveOutput();
        testOverrideNaxis();
        testPixelFraction();
        testOverridCdelt11();
        testOverridCdelt12();
        testOverridCrval1();
        testOverridCrval2();
        testOverridCtype1();
        testOverridCrot2();


        testGeomException();
        testGeomWithRefFits();

    }

    /**test default Geom case
     Input f3.fits
     Expected result Fits f3_Geom_default.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testGeomDefault() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_default.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test setting tie_skip = 15
     *  Input f3.fits
     *  Expected result file f3_Geom_skip15.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testComputeTiePoints()throws FitsException, IOException, GeomException {

        String fileName = "f3_Geom_skip15.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.tie_skip = 15;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);


    }
    /**
     * Test setting interpolation to bi-linear
     *  Input f3.fits
     *  Expected result file f3_Geom_ib.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testUsingBiLinear()throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_ib.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.interp_flag= true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }
    /**
     * Test setting interpolation to nearest neighbor
     *  Input f3.fits
     *  Expected result file f3_Geom_in.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testUsingNearestNeighbor() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_in.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.interp_flag= false;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }
    /**
     * Test to derive output naxis and crpix values from input
     *
     *  Input f3.fits
     *  Expected result file f3_Gemo_derived.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testDeriveOutput() throws FitsException, IOException, GeomException{
        String fileName = "f3_Gemo_derived.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_naxis1 = 0;
        geom.n_override_naxis1 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }

    /**
     * Test setting fraction of live pixels
     *  min_wgt=0.7
     *  Input f3.fits
     *  Expected result file f3_Geom_pixelValue_0.7.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testPixelFraction() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_pixelValue_0.7.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.min_wgt=0.7;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }

    /**
     * Test setting overriding naxis values
     *  naxis1(2) = 100
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideNaxis_100.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testOverrideNaxis() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideNaxis_100.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_naxis1=100;
        geom.override_naxis2=100;
        geom.n_override_naxis1 = true;
        geom.n_override_naxis2 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }


    /**
     * Test setting overriding cdelt1 value
     *  cdelt1=0.03333;
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCdelt1_0.03333.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testOverridCdelt11() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCdelt1_0.03333.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_cdelt1 = 0.03333;
        geom.n_override_cdelt1 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }
    /**
     * Test setting overriding cdelt2 value
     *  cdelt2=0.03333;
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCdelt2_0.03333.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testOverridCdelt12() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideCdelt2_0.03333.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_cdelt2 = 0.03333;
        geom.n_override_cdelt2 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }

    /**
     * Test setting overriding crval1 value
     *  crval1=200;
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCrval1_200.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testOverridCrval1() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideCrval1_200.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_crval1 = 200.0;
        geom.n_override_crval1 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }
    /**
     * Test setting overriding crval2 value
     *  crval2=200;
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCrval2_200.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testOverridCrval2() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCrval2_70.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_crval2 = 70;
        geom.n_override_crval2 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * This method test if the the Geom catch the exception when the parameter is wrong
     * @throws FitsException
     * @throws IOException
     */
    public void testGeomException() throws FitsException, IOException{


        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_naxis1=2;
        geom.override_naxis2=2;
        geom.n_override_naxis1 = true;
        geom.n_override_naxis2 = true;

        try {
            Fits acutualFits = geom.do_geom(inFits, null);
            Assert.fail("If there is no exception is thrown");
        }
        catch (GeomException ge) {
            ge.getMessage();
        }
    }

    /**
     * Test setting overriding ctype1=GLON
     *  ctype1 = "GLON
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCtype1GLON.fits which was created by GeomTestMain.java what Booth wrote.
     */

    public void testOverridCtype1() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCtype1GLON.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_ctype1 = "GLON";
        geom.n_override_ctype1 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);

    }

    /**
     *  Test setting overriding crot2=0.5
     *  crota2 = 0.5
     *  Input f3.fits
     *  Expected result file f3_Geom_Crota2_0.5.fits which was created by GeomTestMain.java what Booth wrote.
     */

    public void testOverridCrot2() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_Crota2_0.5.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommomParametrs(geom);
        geom.override_crota2 = 0.5;
        geom.n_override_crota2 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);


    }

    /**test Geom case
     Input f3.fits
     reference f3Ref.fits (this is created by rotation f3.fits by 10 degree)
     Expected result Fits f3_withRef.fits which was created by GeomTestMain.java what Booth wrote.
     */
    public void testGeomWithRefFits() throws FitsException, IOException, GeomException {
        String fileName = "f3Ref.fits";
        Fits refFits =  FileLoader.loadFits(GeomTest.class, fileName);
        Fits acutualFits = geom.do_geom(inFits, refFits);
        Assert.assertNotNull(acutualFits);

        String expcFileName = "f3_Geom_withRef.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, expcFileName);
        validateFits(expectedFits,acutualFits);
        validateFits(expectedFits,acutualFits);

    }



    private void setCommomParametrs(Geom geom){

        geom.crpix1_base = 162.5;
        geom.crpix2_base = 485.5;
        geom.imageScaleFactor = 89;
        geom.need_crpix_adjusted = true;
        geom.n_ref_name = false;
        geom.tie_skip = 10;
        geom.min_wgt =  0.5;
        geom.interp_flag= false;
        geom.n_override_ctype1 = false;
        geom.n_override_naxis2 = false;
        geom.n_override_naxis1 = false;
        geom.n_override_cdelt1 = false;
        geom.n_override_cdelt2 = false;
        geom.n_override_crval1 = false;
        geom.n_override_crval2 = false;
        geom.n_override_crota2 = false;
    }

}
