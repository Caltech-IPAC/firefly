/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.plotdata;

import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.util.FitsValidation;
// import edu.caltech.ipac.visualize.plot.ImageHeader;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.ImageHDU;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by zhang on 1/6/17.
 * The Geom class has no public methods.  It has one package public method "open_in".
 */
public class GeomTest extends FitsValidation {

    private static Fits  inFits=null;
    // private static ImageHeader expectedImageHeader=null;


    /**
     * A one dimensional array is created and  used to run the unit test
     */
    @BeforeClass
    public static void setUp() throws FitsException, ClassNotFoundException, IOException {

        String fileName = "f3.fits";
        inFits = FileLoader.loadFits(GeomTest.class, fileName);

        //get the expected ImageHeader from calling it directly
        ImageHDU imageHdu = (ImageHDU) inFits.getHDU(0);
        Header header =imageHdu.getHeader();
        int planeNumber = header.getIntValue("SPOT_PL", 0); // keep for now, but SPOT_PL is no longer guaranteed
        int extension_number = header.getIntValue("SPOT_EXT", -1);
        long HDU_offset;
        if (extension_number == -1) {
            HDU_offset = imageHdu.getFileOffset();
        } else {
            HDU_offset = header.getIntValue("SPOT_OFF", 0);
        }

        if (HDU_offset < 0) HDU_offset = 0;
        // expectedImageHeader = new ImageHeader(header, HDU_offset,planeNumber);
    }

    /**
     * Release the memories
     */
    @AfterClass
    public static void tearDown() {
        inFits=null;
        // expectedImageHeader=null;
    }

//    @Test
//    public void testOpen_in() throws FitsException, IOException, GeomException, IllegalAccessException {
//        FitsRead fitsRead0 = FitsReadFactory.createFitsReadArray(inFits)[0];
//        ImageHeader calculatedImageHeader = geom.open_in(fitsRead0);
//        validateImageHeader(expectedImageHeader, calculatedImageHeader);
//    }
//    private void validateImageHeader(ImageHeader exptImageHeader, ImageHeader actImageHeader) throws IllegalAccessException {
//
//        Class<?> objClass =  exptImageHeader.getClass();
//        Field[] exptfields = objClass.getDeclaredFields();
//        objClass =  actImageHeader.getClass();
//        Field[] actfields = objClass.getDeclaredFields();
//
//        Assert.assertEquals(exptfields.length, actfields.length);
//
//
//        for (int i=0; i<exptfields.length; i++){
//            Object expObj = exptfields[i].get(exptImageHeader);
//            Object acuObj = actfields[i].get(actImageHeader);
//
//            if (exptImageHeader.cdelt2<0.0) { /* pixels are upside down - reverse them in y, so credet2 and crpix2 are updated */
//                if (exptfields[i].getName().equalsIgnoreCase("crpix2") || exptfields[i].getName().equalsIgnoreCase("cdelt2")) {
//                    Assert.assertNotEquals(expObj,acuObj);
//                }
//            }
//            else {
//                Assert.assertEquals( expObj,acuObj);
//            }
//        }
//
//    }

//    @Test
//    public void endToEndTest() throws FitsException, IOException, GeomException {
//        testGeomDefault();
//        testComputeTiePoints();
//        //testUsingBiLinear();  //TODO disable it due to the change in the FITs header
//        testUsingNearestNeighbor();
//        testDeriveOutput();
//        testOverrideNaxis();
//        testPixelFraction();
//        testOverridCdelt11();
//        testOverridCdelt12();
//        testOverridCrval1();
//        testOverridCrval2();
//        testOverridCtype1();
//        testOverridCrot2();
//
//        testGeomException();
//        testGeomWithRefFits();
//        testStgToSinReprojection();
//    }

    /**test default Geom case
     Input f3.fits
     Expected result Fits f3_Geom_default.fits which was created by GeomTestMain.java what Booth wrote.
     */
    @Test
    public void testGeomDefault() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_default.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test setting tie_skip = 15
     *  Input f3.fits
     *  Expected result file f3_Geom_skip15.fits which was created by GeomTestMain.java what Booth wrote.
     */
    @Test
    public void testComputeTiePoints()throws FitsException, IOException, GeomException {

        String fileName = "f3_Geom_skip15.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
        geom.tie_skip = 15;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }
    /**
     * Test setting interpolation to bi-linear
     *  Input f3.fits
     *  Expected result file f3_Geom_ib.fits which was created by GeomTestMain.java what Booth wrote.
     *  TODO: this test is failing - needs update (tests non-default interp_flag value)
     */
    public void testUsingBiLinear() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_ib.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
        geom.interp_flag= true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test setting interpolation to nearest neighbor
     *  Input f3.fits
     *  Expected result file f3_Geom_in.fits which was created by GeomTestMain.java what Booth wrote.
     */
    @Test
    public void testUsingNearestNeighbor() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_in.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
        geom.interp_flag= false;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test to derive output naxis and crpix values from the input.
     *  Input f3.fits
     *  Expected result file f3_Gemo_derived.fits which was created by GeomTestMain.java what Booth wrote.
     */
    @Test
    public void testDeriveOutput() throws FitsException, IOException, GeomException{
        String fileName = "f3_Gemo_derived.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testPixelFraction() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_pixelValue_0.7.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverrideNaxis() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideNaxis_100.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverridCdelt11() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCdelt1_0.03333.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverridCdelt12() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideCdelt2_0.03333.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverridCrval1() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_overrideCrval1_200.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverridCrval2() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCrval2_70.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
        geom.override_crval2 = 70;
        geom.n_override_crval2 = true;
        Fits acutualFits = geom.do_geom(inFits, null);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test that Geom catches the exception when the parameter is wrong
     */
    @Test
    public void testGeomException() throws FitsException, IOException{
        Geom geom = new Geom();
        setCommonParameters(geom);
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
     *  ctype1=GLON
     *  Input f3.fits
     *  Expected result file f3_Geom_overrideCtype1GLON.fits which was created by GeomTestMain.java what Booth wrote.
     */
    @Test
    public void testOverridCtype1() throws FitsException, IOException, GeomException {
        String fileName = "f3_Geom_overrideCtype1GLON.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testOverridCrot2() throws FitsException, IOException, GeomException{
        String fileName = "f3_Geom_Crota2_0.5.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, fileName);

        Geom geom = new Geom();
        setCommonParameters(geom);
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
    @Test
    public void testGeomWithRefFits() throws FitsException, IOException, GeomException {
        String fileName = "f3Ref.fits";
        Fits refFits =  FileLoader.loadFits(GeomTest.class, fileName);
        Geom geom = new Geom();
        Fits acutualFits = geom.do_geom(inFits, refFits);
        Assert.assertNotNull(acutualFits);

        String expcFileName = "f3_Geom_withRef.fits";
        Fits expectedFits =  FileLoader.loadFits(GeomTest.class, expcFileName);
        validateFits(expectedFits,acutualFits);
        validateFits(expectedFits,acutualFits);
    }

    /**
     * Test reprojecting wise-w1-SIN.fits to STG projection using wise-w1-STG.fits as reference
     * and validating that the reprojected data remains close
     */
    @Test
    public void testStgToSinReprojection() throws FitsException, IOException, GeomException {
        String sinFileName = "wise-w1-SIN.fits";
        String stgFileName = "wise-w1-STG.fits";

        // Load input SIN projection FITS
        Fits sinFits = FileLoader.loadFits(GeomTest.class, sinFileName);

        // Load STG projection FITS as reference
        Fits stgRefFits = FileLoader.loadFits(GeomTest.class, stgFileName);

        for (boolean interpFlag : new boolean[]{false, true}) {
            String interpMode = interpFlag ? "bilinear" : "nearest neighbor";

            Geom geom = new Geom();
            geom.interp_flag = interpFlag;

            // perform reprojection using STG as reference
            Fits reprojectedFits = geom.do_geom(sinFits, stgRefFits);

            Assert.assertNotNull("Reprojected FITS should not be null (" + interpMode + ")", reprojectedFits);

            // Get the image data from both reprojected result and reference
            ImageHDU reprojectedHDU = (ImageHDU) reprojectedFits.getHDU(0);
            ImageHDU referenceHDU = (ImageHDU) stgRefFits.getHDU(0);

            float[][] reprojectedData = (float[][]) reprojectedHDU.getKernel();
            float[][] referenceData = (float[][]) referenceHDU.getKernel();

            // Validate dimensions match
            Assert.assertEquals("Height should match (" + interpMode + ")",
                    referenceData.length, reprojectedData.length);
            Assert.assertEquals("Width should match (" + interpMode + ")",
                    referenceData[0].length, reprojectedData[0].length);

            // Check that pixel values are reasonably close
            int validPixelCount = 0;
            int closePixelCount = 0;
            // we don't expect exact matches if interpolation is used
            double tolerance = 0.01; // 1% tolerance for pixel value differences

            for (int y = 0; y < referenceData.length; y++) {
                for (int x = 0; x < referenceData[y].length; x++) {
                    float refValue = referenceData[y][x];
                    float reprojValue = reprojectedData[y][x];

                    // Skip NaN values
                    if (!Float.isNaN(refValue) && !Float.isNaN(reprojValue)) {
                        validPixelCount++;

                        // Check if values are close (within tolerance)
                        if (Math.abs(refValue) < 1e-6) {
                            // For very small values, use absolute difference
                            if (Math.abs(reprojValue - refValue) < 1e-6) {
                                closePixelCount++;
                            }
                        } else {
                            // For larger values, use relative difference
                            double relativeDiff = Math.abs((reprojValue - refValue) / refValue);
                            if (relativeDiff < tolerance) {
                                closePixelCount++;
                            }
                        }
                    }
                }
            }

            // require that valid pixels are close to reference values
            double closePixelRatio = (double) closePixelCount / validPixelCount;
            Assert.assertTrue(
                    String.format("Expected at least 99.9%% of pixels to be close to reference values (%s), but got %.2f%% (%d/%d)",
                            interpMode, closePixelRatio * 100, closePixelCount, validPixelCount),
                    closePixelRatio >= .999
            );
        }
    }

    private void setCommonParameters(Geom geom) {
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
