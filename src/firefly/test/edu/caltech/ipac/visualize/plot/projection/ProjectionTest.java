package edu.caltech.ipac.visualize.plot.projection;
import edu.caltech.ipac.firefly.util.FitsHeaderToJson;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import org.junit.*;
import java.io.File;
import java.net.URISyntaxException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * Created by zhang on 6/15/16.
 * We use the CoordinateSys defined in the FITS header, and also use the RA and DEC in the center of the image
 * defined in the header
 * 10/19/16
 *  DM-8028
 *    Use teh UnitTestUtility to load file
 */
public class ProjectionTest {

    private  double delta = 0.1E-10;
    /**
     * This method gets the .json files in the given directory
     * @param path - the directory that has the testing data
     * @return a String array of the testing files that include the abosolute path
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

    /**
     * Thie method pass a JSONObject that contains two fields into a Java HashMap
     * @param jsonObject
     * @return
     */
    private HashMap<String, Double> jsonObjectToMap(JSONObject jsonObject){
        HashMap<String, Double> map = new HashMap<String, Double>();

        String[] keys = (String[])  jsonObject.keySet().toArray(new String[0]);
        for (int i=0; i<keys.length; i++){
            map.put(keys[i], new Double((Double) jsonObject.get(keys[i])) );
        }

        return map;
    }


    /**
     * This method validate the projection results
     * @param jsonObject
     * @throws FitsException
     * @throws ProjectionException
     * @throws IllegalAccessException
     * @throws URISyntaxException
     */
    private void validate(JSONObject jsonObject) throws FitsException, ProjectionException, IllegalAccessException, URISyntaxException {

        String jsonFileName = jsonObject.get("headerFileName").toString();


        ImageHeader imageHeader = FitsHeaderToJson.jsonObjectToImageHeader((JSONObject) jsonObject.get("header"));

        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);


      try {
          //Using the RA and DEC at the center of the image
          ProjectionPt image_pt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);

          //the expected value is achieved when the test is written.  If the Java code changes and
          //the Assert is falling, the changes introduce the problem.
          Object  obj = jsonObject.get("expectedImagePt");
          HashMap<String, Double>  expectedImagePtMap = jsonObjectToMap((JSONObject) obj);


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
    public void testAllProjections() throws IOException, ParseException, FitsException, ProjectionException, IllegalAccessException, URISyntaxException, ClassNotFoundException {



        String dataPath = FileLoader.getDataPath(ProjectionTest.class);
        String[] jsonHeaderFileNames =  getJsonFiles(dataPath);

        JSONParser parser = new JSONParser();


         for (int i=0; i<jsonHeaderFileNames.length; i++){

            Object obj = parser.parse(new FileReader(jsonHeaderFileNames[i]));

            JSONObject jsonObject = (JSONObject) obj;
            validate(jsonObject);

        }

    }



    /**
     * This main method is used to create unit test reference files.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        String path="/Users/zhang/lsstDev/testingData/projectionTestingData/";
        String[] fNames = {
                "1904-66_SFL.fits",                                      //SFL or GLS  SANSON-FLAMSTEED
                "f3.fits",                                               //GNOMONIC
                "DssImage-10.68470841.26902815.015.0poss2ukstu_red.fits", //PLATE
                "GLM_01050+0000_mosaic_I2.fits",                        //CAR  CARTESIAN
                "m51.car.fits",                                         //CAR  CARTESIAN
                "field1.fits",                                          //SIN  ORTHOGRAPHIC
                "m51.sin.fits",                                         //SIN  ORTHOGRAPHIC
                // "file7.fits",                                           //Not FITS format
                //"file7Downloaded.fits",
                "SIP.fits",                                            //TAN--SIP  GNOMONIC
                //"lsstsample1.fits",                                   //CEA  CYLINDRICAL EQUAL AREA
                "allsky.fits",                                        //AIT (ATF is deprecated)   AITOFF
                "bird_2ha3-Booth.fits",                              //----  LINEAR
                "NED_M31.fits",                                          //ARC  ZENITH EQUIDISTANT
                "cong_12_smo.fits"                                    //ARC  ZENITH EQUIDISTANT
        };

        for (int i=0; i<fNames.length; i++) {
            try {
                FitsHeaderToJson.writeImageHeaderProjectionToJson(path + fNames[i]);
            }
            catch (FitsException e) {
                System.out.println("========ERROR=========="+fNames[i]+"===============");
                e.printStackTrace();
            }

        }

    }
}


