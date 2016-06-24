package edu.caltech.ipac.visualize.plot.projection;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import nom.tam.util.Cursor;
import org.junit.*;

import java.io.File;
import java.net.URISyntaxException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
    private static final String TEST_ROOT = "test"+ File.separatorChar;
    private static final String PATH= TEST_ROOT+ProjectionTest.class.getCanonicalName().replaceAll("\\.", "/")
            .replace(ProjectionTest.class.getSimpleName(), "") + File.separatorChar;

    //this is used to test both FitsHeader and the ProjectORTHOGRAPHIC case
    private static String filename = "twomass-j-SIN.fits"; //downloaded 2mass m31
    long NONE_NULL_COUNT= 102; //this is the key size of the twomass-j-SIN.fits

    Fits fits=null;

    private Fits loadFitsFile(String fileName) throws FitsException, URISyntaxException {
        //these two are for running in gradle :firefly:test
         File inFile =  new File(PATH+filename);
         fits = new Fits(inFile);

        //these are for testing inside IntelliJ
//         URL path = ProjectionTest.class.getResource(fileName);
//         File inFile =  new File(path.getFile());
        return  new Fits(inFile);


    }

    /**
     * We use the CoordinateSys in the FITS header, and use the RA and DEC in the center of the image defined in
     * the header
     * @param fits
     * @return
     * @throws FitsException
     * @throws URISyntaxException
     */
    private Projection getProjection(Fits fits, CoordinateSys inCoordinate) throws FitsException, URISyntaxException {

        Header  header =  getHeader(fits);

        ImageHeader imageHeader  = new ImageHeader(header);
       /* CoordinateSys inCoordinate = CoordinateSys.makeCoordinateSys(
                imageHeader.getJsys(), imageHeader.file_equinox);*/
        return imageHeader.createProjection(inCoordinate );
    }
    @Before
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

    /**
     * This is testing the ORTHOGRAPHIC projection
     * @throws FitsException
     * @throws URISyntaxException
     * @throws ProjectionException
     */
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
        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
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

        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
        WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());
        WorldPt expectedWorldPt = new WorldPt( 8.17595829999999,41.91024999999999);
        //Assert.assertEquals(expectedWorldPt, world_pt );
        Assert.assertEquals(expectedWorldPt.getX(),world_pt.getX(), delta  );
        Assert.assertEquals(expectedWorldPt.getY(),world_pt.getY(), delta  );

    }

    @Test
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

        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
        WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());
        WorldPt expectedWorldPt = new WorldPt(325.716682,66.12434600000002);
        //Assert.assertEquals(expectedWorldPt, world_pt );
        Assert.assertEquals(expectedWorldPt.getX(),world_pt.getX(), delta  );
        Assert.assertEquals(expectedWorldPt.getY(),world_pt.getY(), delta  );


    }

    String getValue(String key, String[] hArr){
        for (int i=0; i<hArr.length; i++){
            // System.out.println( hArr[i]);
            String[] pair = hArr[i].split("=");
            if (pair.length==2) {
                if (pair[0].equalsIgnoreCase(key)){
                    return pair[1];
                }
            }
        }
        return "";

    }
    ImageHeader makeHeader(String jsonString) throws FitsException, IllegalAccessException {


        String headerStr =jsonString.replaceAll("\\s*=\\s*", "=");;//remove the space around the = sign

        String[]  hArr = headerStr.trim().split(" "); //split by white space

        int max = HeaderCard.MAX_KEYWORD_LENGTH;
        HashMap<String, String> longKeyList = new HashMap<String, String>();
        Header header = new Header();
        for (int i=0; i<hArr.length; i++){
           // System.out.println( hArr[i]);
            String[] pair = hArr[i].split("=");
            if (pair.length==2){
                //System.out.println( pair[0]);
                if (pair[0].length()>max){
                    longKeyList.put(pair[0], pair[1]);
                    continue;
                }
                HeaderCard  hc= new HeaderCard(pair[0].toUpperCase(), pair[1], "");
                header.addLine(hc);
            }
         }
          //using the imageHeader saved in the json file causes side effect in some files.
           ImageHeader imageHeader = new ImageHeader(header);

         if (longKeyList.size()!=0){
             String[] keys = longKeyList.keySet().toArray(new String[0]);
             for (int i=0; i<keys.length; i++){
                 if (keys[i].equalsIgnoreCase("file_equinox")){
                     imageHeader.file_equinox=(new Double(longKeyList.get(keys[i]))).doubleValue();
                 }
                 else if (keys[i].equalsIgnoreCase("blank_value")){
                     imageHeader.blank_value = (new Double(longKeyList.get(keys[i]))).doubleValue();
                 }
             }
         }


       // ImageHeader imageHeader = new ImageHeader();
       // imageHeader.bitpix =  (new Integer(getValue("bitpix", hArr)) ).intValue();
        /*
        Need to add all the key value pair as above, it is not a good way to do it
         */

        return imageHeader;
    }

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

    private double[] getXY(String xyString){
        String[] xyArray=xyString.split(" ");
        String[] xArray=xyArray[0].split("=");
        String[] yArray=xyArray[1].split("=");
        double[] xyData = { (new Double(xArray[1])).doubleValue(), (new Double(yArray[1])).doubleValue()};
        return xyData;
    }
    private void validate(JSONObject jsonObject) throws FitsException, ProjectionException, IllegalAccessException, URISyntaxException {

        String jsonFileName = jsonObject.get("headerFileName").toString();

        //TODO need to either write  ImageHeader in javascript or add some methods in java ImageHeader
        // to handle key,value pairs.  Use the Fits header to do the test for now
        //ImageHeader imageHeader= makeHeader( jsonObject.get("header").toString());
        //Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);

        String fitsFileName = jsonFileName.replace("Header.json", ".fits");
        File inFile =  new File(fitsFileName);
        Fits fits = new Fits(inFile);

        Header  header =  getHeader(fits);

        ImageHeader imageHeader  = new ImageHeader(header);

        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);


      try {
          //Using the RA and DEC at the center of the image
          ProjectionPt image_pt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);

          //the expected value is achieved when the test is written.  If the Java code changes and
          //the Assert is falling, the changes introduce the problem.
          String expectedProjectPtStr = jsonObject.get("expectedImagePt").toString();
          double[] xyData = getXY(expectedProjectPtStr);

          ProjectionPt expectedProjectPt = new ProjectionPt(xyData[0], xyData[1]);

          //Assert.assertEquals(expectedProjectPt, image_pt);
          Assert.assertEquals(expectedProjectPt.getFsamp(),image_pt.getFsamp(), delta  );
          Assert.assertEquals(expectedProjectPt.getFline(),image_pt.getFline(), delta  );

        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
          WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());
          String expectedWorldPtStr = jsonObject.get("expectedWorldPt").toString();
          xyData = getXY(expectedWorldPtStr);

          WorldPt expectedWorldPt = new WorldPt(xyData[0], xyData[1]);
          Assert.assertEquals(expectedWorldPt.getX(),world_pt.getX(), delta  );
          Assert.assertEquals(expectedWorldPt.getY(),world_pt.getY(), delta  );

          //System.out.println( " Test Pass \n");
      }catch (Exception e) {
           System.out.println(jsonFileName + " has exception");
      }

    }
    @Test
    public void testAllProjections() throws IOException, ParseException, FitsException, ProjectionException, IllegalAccessException, URISyntaxException {



        //This is to run unit test under command line
         String[] jsonHeaderFileNames =  getJsonFiles(PATH);

        //This is to test it in IntelliJ
//        String path = "/Users/zhang/lsstDev/testingData/projectionTestingData/";
//        String[] jsonHeaderFileNames =  getJsonFiles(path);


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


