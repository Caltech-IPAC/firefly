package edu.caltech.ipac.visualize.plot.projection;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.json.simple.JSONArray;
import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * Created by zhang on 6/15/16.
 * We use the CoordinateSys defined in the FITS header, and also use the RA and DEC in the center of the image
 * defined in the header
 */
public class ProjectionTest {

    private  double delta = 0.0000000001;
    //private static final String TEST_ROOT = "test"+ File.separatorChar;
    private static final String TEST_ROOT = "firefly_test_data/";//edu/caltech/ipac/visualize/plot/projection/
    //private static final String PATH= TEST_ROOT+ProjectionTest.class.getCanonicalName().replaceAll("\\.", "/")

    private static final String  TEST_DATA_PATH="firefly_test_data/edu/caltech/ipac/visualize/plot/projection/";

    //this is used to test both FitsHeader and the ProjectORTHOGRAPHIC case
    private static String filename = "twomass-j-SIN.fits"; //downloaded 2mass m31
    long NONE_NULL_COUNT= 102; //this is the key size of the twomass-j-SIN.fits

 /*   Fits fits=null;

    private Fits loadFitsFile(String fileName) throws FitsException, URISyntaxException {
        //these two are for running in gradle :firefly:test
        // File inFile =  new File(PATH+filename);
        // fits = new Fits(inFile);

        //these are for testing inside IntelliJ
         URL path = ProjectionTest.class.getResource(fileName);
         File inFile =  new File(path.getFile());
        return  new Fits(inFile);


    }

    *//**
     * We use the CoordinateSys in the FITS header, and use the RA and DEC in the center of the image defined in
     * the header
     * @param fits
     * @return
     * @throws FitsException
     * @throws URISyntaxException
     *//*
    private Projection getProjection(Fits fits, CoordinateSys inCoordinate) throws FitsException, URISyntaxException {

        Header  header =  getHeader(fits);

        ImageHeader imageHeader  = new ImageHeader(header);
       *//* CoordinateSys inCoordinate = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);*//*
        return imageHeader.createProjection(inCoordinate );
    }
    //@Before
    public void setMeUp() throws FitsException, URISyntaxException {

         fits = loadFitsFile(filename);

    }
    @After
    public void tearDown(){
        fits=null;
    }

    private Header getHeader(Fits fits)throws FitsException {
        BasicHDU[] hdus = fits.read();

        if (hdus == null)
        {
            throw new FitsException (" hdu is null");
        }
        return  hdus [0].getHeader();

    }
    @Test
    public void testFitsHeader() throws FitsException {
        Assert.assertNotNull(fits);

        Header header = getHeader(fits);

        Assert.assertNotNull(header);
        int count = 0;
        for (Cursor itr = header.iterator(); itr.hasNext(); ) {
            HeaderCard hc = (HeaderCard) itr.next();
            if (hc.isKeyValuePair()) {
                count++;
            }
        }

        Assert.assertEquals(NONE_NULL_COUNT, count);


    }

    *//**
     * This is testing the ORTHOGRAPHIC projection
     * @throws FitsException
     * @throws URISyntaxException
     * @throws ProjectionException
     *//*
    @Test
    public void testProjectORTHOGRAPHIC() throws FitsException, URISyntaxException, ProjectionException {

        Projection projection =   getProjection(fits, CoordinateSys.EQ_J2000);

        //those values are taken from the 2massj's header CRVAL1 (RA) and CRVAL2 (DEC),the ra and dec at the origin
        double input_ra = 10.6395000;
        double input_dec = 41.2638333;


        ProjectionPt image_pt = projection.getImageCoords( input_ra, input_dec);

        //the expected value is achieved when the test is written.  If the Java code changes and
        //the Assert is falling, the changes introduce the problem.
        ProjectionPt expectedProjectPt = new ProjectionPt( 372.15189358590766, 231.38265329482758);

        //Assert.assertEquals(expectedProjectPt, image_pt );

        Assert.assertEquals(expectedProjectPt.getX(),image_pt.getX(), delta );
        Assert.assertEquals(expectedProjectPt.getY(),image_pt.getY(), delta  );
        *//*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        *//*
        WorldPt world_pt = projection.getWorldCoords(image_pt.getFsamp(), image_pt.getFline());
        WorldPt expectedWorldPt = new WorldPt(10.639499999999998,41.263833300000016);

        Assert.assertEquals(expectedWorldPt, world_pt );

    }

    //@Test
    public void testProjectGNOMONIC() throws FitsException, URISyntaxException, ProjectionException {

        String fileName = "iris-25-GNOMONIC.fits";
        Fits fits = loadFitsFile(fileName);
        Projection projection = getProjection(fits,  CoordinateSys.EQ_J2000);

        //those values are taken from the iris-25-GNOMONIC.fits's, a white spot on the right hand side facing the image
        double input_ra = 8.1759583;
        double input_dec =41.9102500;


        ProjectionPt image_pt = projection.getImageCoords( input_ra, input_dec);

        //the expected value is achieved when the test is written.  If the Java code changes and
        //the Assert is falling, the changes introduce the problem.
        ProjectionPt expectedProjectPt = new ProjectionPt(2189.175045953085,4681.739079415915);

        System.out.println("\n expected="+expectedProjectPt.getFsamp());
        System.out.println("\n calculated="+image_pt.getFsamp() );
        System.out.println("\n expected="+expectedProjectPt.getFline());
        System.out.println("\n calculated="+image_pt.getFline() );


        Assert.assertEquals(expectedProjectPt.getFsamp(),image_pt.getFsamp(), delta  );
        Assert.assertEquals(expectedProjectPt.getFline(),image_pt.getFline(), delta  );

        //Assert.assertEquals(expectedProjectPt, image_pt );

        *//*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        *//*
        WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());
        WorldPt expectedWorldPt = new WorldPt( 8.17595829999999,41.91024999999999);
        //Assert.assertEquals(expectedWorldPt, world_pt );
        Assert.assertEquals(expectedWorldPt.getX(),world_pt.getX(), delta  );
        Assert.assertEquals(expectedWorldPt.getY(),world_pt.getY(), delta  );

    }

    //@Test
    public void testProjectNCP() throws FitsException, URISyntaxException, ProjectionException {

        String fileName = "file7.fits";//this file has a loading error
        Fits fits = loadFitsFile(fileName);
       // Projection projection = getProjection(fits,  CoordinateSys.EQ_J2000);


    }


    //@Test
    public void testProject_f3() throws FitsException, URISyntaxException, ProjectionException {

        String fileName = "f3.fits";
        Fits fits = loadFitsFile(fileName);
        Projection projection = getProjection(fits,  CoordinateSys.EQ_J2000);

        //those values are taken from the iris-25-GNOMONIC.fits's, a white spot on the right hand side facing the image
        double input_ra =  325.716682;
        double input_dec = 66.124346;


        ProjectionPt image_pt = projection.getImageCoords( input_ra, input_dec);

        //the expected value is achieved when the test is written.  If the Java code changes and
        //the Assert is falling, the changes introduce the problem.
        ProjectionPt expectedProjectPt = new ProjectionPt(370.3194758757113, 56.19610949182024);
        System.out.println("\n expected="+expectedProjectPt.getFsamp());
        System.out.println("\n calculated="+image_pt.getFsamp() );
        System.out.println("\n expected="+expectedProjectPt.getFline());
        System.out.println("\n calculated="+image_pt.getFline() );
        Assert.assertEquals(expectedProjectPt.getFsamp(),image_pt.getFsamp(), delta  );
        Assert.assertEquals(expectedProjectPt.getFline(),image_pt.getFline(), delta  );


       // Assert.assertEquals(expectedProjectPt, image_pt );

        *//*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        *//*
        WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());
        WorldPt expectedWorldPt = new WorldPt(325.716682,66.12434600000002);
        //Assert.assertEquals(expectedWorldPt, world_pt );
        Assert.assertEquals(expectedWorldPt.getX(),world_pt.getX(), delta  );
        Assert.assertEquals(expectedWorldPt.getY(),world_pt.getY(), delta  );


    }
*/
    private String[] getJsonFiles(String path){

        File folder = new File(path);
        ArrayList<String> fArray = new ArrayList<String>();
        String[] fileNames = folder.list();

        for (int i = 0; i < fileNames .length; i++) {
            if (fileNames[i].endsWith(".json")) {
                fArray.add(path+fileNames[i]);
             }
        }
        return fArray.toArray(new String[0]);
    }

    private HashMap<String, Double> jsonObjectToMap(JSONObject jsonObject){
        HashMap<String, Double> map = new HashMap<String, Double>();

        String[] keys = (String[])  jsonObject.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++){
            map.put(keys[i], new Double((Double) jsonObject.get(keys[i])) );
        }

        return map;
    }

    private double[] jsonArray1dToDouble1d(JSONArray jsonArray1d){
        double[] double1d = new double[jsonArray1d.size()];
        for (int i=0; i<jsonArray1d.size(); i++){
            double1d[i]=(double)jsonArray1d.get(i);
        }
        return double1d;
    }


    private ImageHeader jsonObjectToImageHeader(JSONObject jsonObject) throws IllegalAccessException {


        ImageHeader imageHeader = new ImageHeader();
        Class<?> objClass =  imageHeader.getClass();

        //array containing Field objects reflecting all the accessible public fields of the
        //class or interface represented by this Class object
        Field[] fields = objClass.getFields();
        for (Field field : fields) {

            if (field == null || field.toString().contains("final")) continue;
            String name = field.getName();
            Object value = jsonObject.get(name);
            Type type = field.getType();
            if (value != null) {
                if (value instanceof JSONArray){

                    JSONArray jArray= (JSONArray) value;

                    if (jArray.get(0) instanceof JSONArray) {

                        int len = jArray.size();

                        double[][] double2d = new double[len][((JSONArray) jArray.get(0)).size()];
                        for (int i=0; i<len; i++){
                            JSONArray a = (JSONArray) jArray.get(i);
                            double2d[i]= jsonArray1dToDouble1d((JSONArray) jArray.get(i));
                        }
                        field.set(imageHeader, double2d);
                    }
                    else {
                        double[] dArray = jsonArray1dToDouble1d(jArray);

                        field.set(imageHeader, dArray);
                    }

                }
                else if (type.getTypeName().equalsIgnoreCase("int")) {

                    if (value instanceof Long) {
                        int v = ((Long) value).intValue();

                        field.set(imageHeader, v);
                    }
                    else {
                         field.set(imageHeader, value);
                    }
                }
                else {
                    field.set(imageHeader, value);
                }
            }
        }
        return imageHeader;
    }
    private void validate(JSONObject jsonObject) throws FitsException, ProjectionException, IllegalAccessException, URISyntaxException {

        String jsonFileName = jsonObject.get("headerFileName").toString();


        ImageHeader imageHeader = jsonObjectToImageHeader((JSONObject) jsonObject.get("header"));

        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);


      try {
          //Using the RA and DEC at the center of the image
          ProjectionPt image_pt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);

          //the expected value is achieved when the test is written.  If the Java code changes and
          //the Assert is falling, the changes introduce the problem.
          Object  obj = jsonObject.get("expectedImagePt");
          HashMap<String, Double>  expectedImagePtMap = jsonObjectToMap((JSONObject) obj);


          //Assert.assertEquals(expectedProjectPt, image_pt);
          Assert.assertEquals( expectedImagePtMap.get("x").doubleValue(),image_pt.getX(), delta  );
          Assert.assertEquals( expectedImagePtMap.get("y").doubleValue(),image_pt.getY(), delta  );

        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
          WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());

          obj = jsonObject.get("expectedWorldPt");
          HashMap<String, Double> expectedWorldPtMap = jsonObjectToMap((JSONObject) obj);
          Assert.assertEquals(expectedWorldPtMap.get("x").doubleValue(), world_pt.getX(), delta  );
          Assert.assertEquals(expectedWorldPtMap.get("y").doubleValue(), world_pt.getY(), delta  );


      }catch (Exception e) {
           System.out.println(jsonFileName + " has exception");
      }

    }
    @Test
    public void testAllProjections() throws IOException, ParseException, FitsException, ProjectionException, IllegalAccessException, URISyntaxException {



        //This is to run unit test under command line
        String testTreePath = ProjectionTest.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String  rootPath = testTreePath.split("firefly")[0];
        String dataPath = rootPath+TEST_DATA_PATH;
        String[] jsonHeaderFileNames =  getJsonFiles(dataPath);


        JSONParser parser = new JSONParser();


         for (int i=0; i<jsonHeaderFileNames.length; i++){
            System.out.println("\n The testing file is "+ jsonHeaderFileNames[i] +"\n");
            Object obj = parser.parse(new FileReader(jsonHeaderFileNames[i]));

            JSONObject jsonObject = (JSONObject) obj;
            validate(jsonObject);

        }
        //System.out.println("===========Unit tests done===============");
    }
}


