package edu.caltech.ipac.visualize.plot.projection;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import org.json.simple.JSONArray;
import org.junit.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
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

    private  double delta = 0.0000000001;
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
     * This method convert a JSONArray which is one dimensional array to java double array
     * @param jsonArray1d
     * @return
     */
    private double[] jsonArray1dToDouble1d(JSONArray jsonArray1d){
        double[] double1d = new double[jsonArray1d.size()];
        for (int i=0; i<jsonArray1d.size(); i++){
            double1d[i]=(double)jsonArray1d.get(i);
        }
        return double1d;
    }

    /**
     * This method read the JSONObject which was converted by ImageHeader object and then convert it back to ImageHeader
     * @param jsonObject
     * @return
     * @throws IllegalAccessException
     */
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

            if (value != null) {
                if (value instanceof JSONArray){ //test if the value is an array
                    JSONArray jArray= (JSONArray) value;
                    if (jArray.get(0) instanceof JSONArray) { //test if it is a two dimensional array
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
                else { //none array
                    Type type = field.getType();
                    if (type.getTypeName().equalsIgnoreCase("int")) {
                        if (value instanceof Long) {
                            int v = ((Long) value).intValue();
                            field.set(imageHeader, v);
                        } else {
                            field.set(imageHeader, value);
                        }
                    } else {
                        field.set(imageHeader, value);
                    }
                }
            }
        }
        return imageHeader;
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


        ImageHeader imageHeader = jsonObjectToImageHeader((JSONObject) jsonObject.get("header"));

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
}


