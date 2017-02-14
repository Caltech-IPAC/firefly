package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.firefly.util.FitsHeaderToJson;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import edu.caltech.ipac.visualize.plot.projection.ProjectionParams;
import nom.tam.fits.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by zhang on 2/13/17.
 *
 * Since there is no alternative way (or using another application) to create an ImageHeader, it is difficult to
 * do a white-box unit test.  Thus, the block-box testing style is used.
 *
 * 
 * Using the f3.fits creates an ImageHeader object (named f3Header.json)  and stored in the test directory.
 * Use this f3Header.json as a reference to validate the newly created ImageHeader by the same input, f3.fits.
 *
 * In order to test more projections, more header json files are created as well.  All the headerJson files
 * are created in the main program.
 *
 * The projection json is created as well.
 *
 *
 */
public class ImageHeaderTest  extends ConfigTest {

    private Fits inFits;
    private static String fitsFileName = "f3.fits";
    private static ImageHeader expectedImageHeader;
    private Header header;
    private static double delta=1.E-10;
    private static String jsonHeaderFileName = "f3Header.json";

    /**
     * This method prepares the input most used.  There are some input which only is used once is not  created here.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    @Before
    public void setUp() throws FitsException, IOException, ClassNotFoundException, ParseException, IllegalAccessException {

        ConfigTest.LOG.info("load the expected data from json file");
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+jsonHeaderFileName ));
        expectedImageHeader = FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) ((JSONObject) obj).get("header"));


        ConfigTest.LOG.info("load input file");
        inFits = FileLoader.loadFits(ImageHeaderTest.class,fitsFileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        header = fitsRead0.getHeader();

        ConfigTest.LOG.info("done loading testing data");


    }
    @After
    /**
     * Release the memories
     */
    public void tearDown() {
        inFits=null;
        header=null;
        expectedImageHeader=null;
    }


    /**
     * This method uses the input from the setup to creata an ImageHeader and then compare it with the
     * referenced one created by the main program.
     * @throws FitsException
     * @throws IllegalAccessException
     */
    @Test
    public void createImageHeaderByOneArgumentTest() throws FitsException, IllegalAccessException {

        ImageHeader calculatedImageHeader = new ImageHeader(header);
        //validate it is not null
        Assert.assertNotNull(calculatedImageHeader );
        validate(expectedImageHeader,calculatedImageHeader);

   }

    /**
     * This method is to test the ImageHeader created with SPOT parameters.
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    @Test
    public void createImageHeaderConstructorWith3Arguments() throws FitsException, IOException, ClassNotFoundException, ParseException, IllegalAccessException{


        String jsonHeaderFileName = "fitsWithSpotExtHeader.json";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+jsonHeaderFileName ));
        ImageHeader expectedImageHeader = FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) ((JSONObject) obj).get("header"));


        String fitsFileName = "fitsWithSpotExt.fits";
        Fits inFits = FileLoader.loadFits(ImageHeaderTest.class,fitsFileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        Header header = fitsRead0.getHeader();
        ImageHDU imageHdu = (ImageHDU) fitsRead0.getHDU();
        int planeNumber = header.getIntValue("SPOT_PL", 0);
        int  extension_number = header.getIntValue("SPOT_EXT", -1);
        long HDUOffset = extension_number == -1? imageHdu.getFileOffset():header.getIntValue("SPOT_OFF", 0);

        ImageHeader calculatedImageHeader = new ImageHeader(header, HDUOffset, planeNumber);
        validate(expectedImageHeader,calculatedImageHeader);

    }

    /**
     * Since for each projection, the map type is different, and the ImageHaeder is different.  This is to
     * test different projection as the input (f3.fits);
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    @Test
    public void testUsingGNOMONICProjectionFITs() throws FitsException, IOException, ClassNotFoundException, ParseException, IllegalAccessException{

        String jsonHeaderFileName = "iris-25-GNOMONICHeader.json";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+jsonHeaderFileName ));
        ImageHeader expectedImageHeader = FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) ((JSONObject) obj).get("header"));


        ConfigTest.LOG.info("load input file");
        String  fitsFileName = "iris-25-GNOMONIC.fits";
        Fits inFits = FileLoader.loadFits(ImageHeaderTest.class,fitsFileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        Header header = fitsRead0.getHeader();
        ImageHeader calculatedImageHeader = new ImageHeader(header);
        validate(expectedImageHeader,calculatedImageHeader);


    }
    /**
     * Since for each projection, the map type is different, and the ImageHaeder is different.  This is to
     * test different projection as the input (f3.fits);
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    @Test
    public void testUsingSINProjectionFITs() throws FitsException, IOException, ClassNotFoundException, ParseException, IllegalAccessException{

        String jsonHeaderFileName = "twomass-j-SINHeader.json";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+jsonHeaderFileName ));
        ImageHeader expectedImageHeader = FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) ((JSONObject) obj).get("header"));


        ConfigTest.LOG.info("load input file");
        String  fitsFileName = "twomass-j-SIN.fits";
        Fits inFits = FileLoader.loadFits(ImageHeaderTest.class,fitsFileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        Header header = fitsRead0.getHeader();
        ImageHeader calculatedImageHeader = new ImageHeader(header);
        validate(expectedImageHeader,calculatedImageHeader);


    }

    /**
     * Since for each projection, the map type is different, and the ImageHaeder is different.  This is to
     * test different projection as the input (f3.fits);
     * @throws FitsException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IllegalAccessException
     */
    @Test
    public void testUsingSIPProjectionFITs() throws FitsException, IOException, ClassNotFoundException, ParseException, IllegalAccessException{

        String jsonHeaderFileName = "SIPHeader.json";
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+jsonHeaderFileName ));
        ImageHeader expectedImageHeader =FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) ((JSONObject) obj).get("header"));


        ConfigTest.LOG.info("load input file");
        String  fitsFileName = "SIP.fits";
        Fits inFits = FileLoader.loadFits(ImageHeaderTest.class,fitsFileName);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];
        Header header =fitsRead0.getHeader();
        ImageHeader calculatedImageHeader = new ImageHeader(header);
        validate(expectedImageHeader,calculatedImageHeader);
    }

    /**
     * Test if the param created by createProjectionParameters are the same as the input parameters
     * @throws FitsException
     * @throws IllegalAccessException
     */
    @Test
    public void testCreateProjectionParameters() throws FitsException, IllegalAccessException {
        ImageHeader imageHeader = new ImageHeader(header);
        ProjectionParams params = ImageHeader.createProjectionParams(imageHeader);

        Class<?> imageHeaderClass =  imageHeader.getClass();


        //the fields contain all the parameters needed
        Field[] eFields = imageHeaderClass.getFields();


        //created parameter fields
        Class<?> paramsClass = params.getClass();
        Field[] cFields = paramsClass.getFields();

        for (int i=0; i<cFields.length; i++){
            for (int j=0; j<eFields.length; j++) {
                if (cFields[i].getName().equalsIgnoreCase(eFields[j].getName())) {
                     Assert.assertEquals(eFields[j].get(imageHeader), cFields[i].get(params));
                    break;
                }
            }
        }

    }

    /**
     * Test the projection by comparing with the referenced projection saved in "f3Projection.json".
     * @throws FitsException
     * @throws IllegalAccessException
     * @throws ProjectionException
     * @throws ClassNotFoundException
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testCreateProjection() throws FitsException, IllegalAccessException, ProjectionException,
            ClassNotFoundException,ParseException,IOException{

        ConfigTest.LOG.info("load expected projection from the json file created previously");
        String expectedProjectionJsonFile= "f3Projection.json";
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = ( JSONObject) parser.parse(new FileReader( FileLoader.getDataPath(ImageHeaderTest.class)+expectedProjectionJsonFile ));

        JSONObject  obj = (JSONObject) jsonObject.get("expectedImagePt");
        HashMap<String, Double> expectedImagePtMap = new HashMap<>();
        String[] keys = (String[])  obj.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++){
            expectedImagePtMap.put(keys[i], new Double((Double) obj.get(keys[i])) );
        }
        obj = (JSONObject) jsonObject.get("expectedWorldPt");

        HashMap<String, Double> expectedWorldPtMap = new HashMap<>();
        keys = (String[])  obj.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++){
            expectedWorldPtMap.put(keys[i], Double.parseDouble( obj.get(keys[i]).toString()) );//new Double((Double) obj.get(keys[i])) );
        }


        ImageHeader imageHeader = new ImageHeader(header);
        Projection projection= imageHeader.createProjection(CoordinateSys.EQ_J2000);
        ProjectionPt imagePt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);
        WorldPt worldPt = projection.getWorldCoords(imagePt.getX(), imagePt.getY());


        Assert.assertEquals( expectedImagePtMap.get("x").doubleValue(),imagePt.getX(), delta  );
        Assert.assertEquals( expectedImagePtMap.get("y").doubleValue(),imagePt.getY(), delta  );

        Assert.assertEquals(expectedWorldPtMap.get("x").doubleValue(), worldPt.getX(), delta  );
        Assert.assertEquals(expectedWorldPtMap.get("y").doubleValue(), worldPt.getY(), delta  );

    }
    private void validate(ImageHeader expectedImageHeader, ImageHeader calculatedImageHeader) throws FitsException, IllegalAccessException{

        //validate projection
        Assert.assertEquals(expectedImageHeader.getProjectionName(),calculatedImageHeader.getProjectionName());

        //validate coordinate sys
        Assert.assertEquals(expectedImageHeader.getCoordSys(),calculatedImageHeader.getCoordSys());

        Assert.assertEquals(expectedImageHeader.getEquinox(),calculatedImageHeader.getEquinox(), delta);

        Assert.assertEquals(expectedImageHeader.getJsys(),calculatedImageHeader.getJsys());


        Class<?> calculatedObjClass =  calculatedImageHeader.getClass();

        //array containing Field objects reflecting all the accessible public fields of the
        //class or interface represented by this Class object
        Field[] cFields = calculatedObjClass.getFields();


        Class<?> expectedObjClass =  expectedImageHeader.getClass();
        Field[] eFields = expectedObjClass.getFields();

        Assert.assertEquals(eFields.length, cFields.length);


        for (int i=0; i<cFields.length;i++){

            if (eFields[i].getName().equalsIgnoreCase(cFields[i].getName())){
                Object cValue = cFields[i].get( calculatedImageHeader );

                Object eValue = cFields[i].get( expectedImageHeader );

                String  componentType = cFields[i].getType().getSimpleName();

                switch (componentType){
                    case "String":
                    case "int":
                    case "double":
                    case "long":
                    case "float":
                    case "boolean":
                        Assert.assertEquals(eValue, cValue);
                        break;
                    case "double[]":
                        double[] c1DArray = (double[]) cValue;
                        double[] e1DArray = (double[]) eValue;
                        Assert.assertArrayEquals(e1DArray,c1DArray, delta);
                        break;

                    case "float[]":
                        float[] c1DFArray = (float[]) cValue;
                        float[] e1DFArray = (float[]) eValue;
                        Assert.assertArrayEquals(e1DFArray,c1DFArray, (float) delta);
                        break;

                    case "double[][]":
                        double[][] c2DArray = (double[][]) cValue;
                        double[][] e2DArray = (double[][]) eValue;
                        for (int k=0; k<c2DArray.length; k++) {
                            Assert.assertArrayEquals(e2DArray[k], c2DArray[k], delta);
                        }
                        break;

                    case "float[][]":
                        float[][] c2DFArray = (float[][]) cValue;
                        float[][] e2DFArray = (float[][]) eValue;
                        for (int k=0; k<c2DFArray.length; k++) {
                            Assert.assertArrayEquals(e2DFArray[k], c2DFArray[k], (float) delta);
                        }
                        break;
                }

            }

        }

    }

    /**
     * This main program is for creating the output files to be saved
     * as references for the unit tests.
     *
     *
     *
     */

    public static void main(String[] args) throws Exception {

        fitsFileName = "f3.fits"; //"SIP.fits";//"twomass-j-SIN.fits"; //"iris-25-GNOMONIC.fits";//"f3.fits"; //
        String inputFitsFile= FileLoader.getDataPath(ImageHeaderTest.class)+fitsFileName;

        Fits fits = new Fits(inputFitsFile);
        FitsRead fitsRead0 = FitsRead.createFitsReadArray(fits)[0];
        Header header =fitsRead0.getHeader();
        ImageHeader imageHeader= new ImageHeader(header);
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";
        FitsHeaderToJson.writeImageHeaderToJson(imageHeader, outJsonFile);


        //create json file (using f3.fits) containing both the projection to save as a reference
        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);
        outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Projection.json";
        FitsHeaderToJson.writeProjectionToJson(imageHeader, projection, outJsonFile);




     /*   //for FITS with SPOT parameter
        String fName = "fitsWithSpotExt.fits";
        String inFitsFile = FileLoader.getDataPath(ImageHeaderTest.class)+fName;
        String outJsonFileSpot = inFitsFile.substring(0, inFitsFile.length()-5 ) + "Header.json";

        Fits inFits = new Fits(inFitsFile);
        fitsRead0 = FitsRead.createFitsReadArray(inFits)[0];


        ImageHDU imageHdu = (ImageHDU) fitsRead0.getHDU();
        Header headerSpot =fitsRead0.getHeader();
        int planeNumber = headerSpot.getIntValue("SPOT_PL", 0);
        int  extension_number = headerSpot.getIntValue("SPOT_EXT", -1);
        long HDUOffset = extension_number == -1? imageHdu.getFileOffset():headerSpot.getIntValue("SPOT_OFF", 0);
        ImageHeader imageHeaderWithSpot = new ImageHeader(headerSpot, HDUOffset, planeNumber);
        FitsHeaderToJson.writeImageHeaderToJson(imageHeaderWithSpot, outJsonFileSpot);*/


    }
}
