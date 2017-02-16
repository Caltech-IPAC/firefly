package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.visualize.plot.*;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Created by zhang on 6/21/16
 *
 */
public class FitsHeaderToJson {

    /**
     * This method is to create an ImageHeader for a given FITS file.
     * @param fits
     * @return
     * @throws FitsException
     */
    private static ImageHeader getImageHeader(Fits fits)throws FitsException {
        BasicHDU[] hdus = fits.read();

        if (hdus == null){

            throw new FitsException (" hdu is null");
        }
        Header header = hdus[0].getHeader();
        ImageHeader imageHeader= new ImageHeader(header);

        return imageHeader;
    }

    /**
     * This method is create a JSONObject which contains a ImageHeader, Projection etc for any given Fits file.
     * @param inputFitsFile
     * @throws Exception
     */

    public static void writeImageHeaderProjectionToJson(String inputFitsFile) throws Exception {

        Fits fits = new Fits(inputFitsFile);
        JSONObject obj = new JSONObject();
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";

        obj.put("headerFileName", outJsonFile );
        ImageHeader imageHeader = getImageHeader(fits);

        //convert the ImageHeader object to jsonString
        JSONObject imageHeaderObj = ConvertImageHeaderToJsonObject(imageHeader);
        obj.put("header",imageHeaderObj);

        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);

        ProjectionPt imagePt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image
        JSONObject imagePtJson = new JSONObject();
        imagePtJson.put("x",imagePt.getX() );
        imagePtJson.put("y",imagePt.getY() );
        obj.put("expectedImagePt", imagePtJson);

        WorldPt worldPt = projection.getWorldCoords(imagePt.getX(), imagePt.getY());
        JSONObject worldPJson = new JSONObject();
        worldPJson.put("x",worldPt.getX() );
        worldPJson.put("y",worldPt.getY() );
        obj.put("expectedImagePt", imagePtJson);
        obj.put("expectedWorldPt",worldPJson);

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method is to write both ImageHeader and Project information to one JSONObject and then save this JSONObject to a file.
     * @param imageHeader
     * @param projection
     * @param outJsonFile
     * @throws Exception
     */
    public static void writeImageHeaderProjectionToJson(ImageHeader imageHeader, Projection projection, String outJsonFile ) throws Exception {


        JSONObject obj = new JSONObject();

        obj.put("headerFileName", outJsonFile );

        //convert the ImageHeader object to jsonString
        JSONObject imageHeaderObj = ConvertImageHeaderToJsonObject(imageHeader);
        obj.put("header",imageHeaderObj);


        ProjectionPt imagePt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image
        JSONObject imagePtJson = new JSONObject();
        imagePtJson.put("x",imagePt.getX() );
        imagePtJson.put("y",imagePt.getY() );
        obj.put("expectedImagePt", imagePtJson);

        WorldPt worldPt = projection.getWorldCoords(imagePt.getX(), imagePt.getY());
        JSONObject worldPJson = new JSONObject();
        worldPJson.put("x",worldPt.getX() );
        worldPJson.put("y",worldPt.getY() );
        obj.put("expectedImagePt", imagePtJson);
        obj.put("expectedWorldPt",worldPJson);

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method is to write the some important fields in Projection to JSONObject and then save to an output file
     * @param imageHeader
     * @param projection
     * @param outJsonFile
     * @throws Exception
     */
    public static void writeProjectionToJson(ImageHeader imageHeader, Projection projection, String outJsonFile ) throws Exception {


        JSONObject obj = new JSONObject();

        ProjectionPt imagePt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image
        JSONObject imagePtJson = new JSONObject();
        imagePtJson.put("x",imagePt.getX() );
        imagePtJson.put("y",imagePt.getY() );
        obj.put("expectedImagePt", imagePtJson);

        WorldPt worldPt = projection.getWorldCoords(imagePt.getX(), imagePt.getY());
        JSONObject worldPJson = new JSONObject();
        worldPJson.put("x",worldPt.getX() );
        worldPJson.put("y",worldPt.getY() );
        obj.put("expectedImagePt", imagePtJson);
        obj.put("expectedWorldPt",worldPJson);

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    /**
     * This method convert a JSONArray which is one dimensional array to java double array
     * @param jsonArray1d
     * @return
     */
    private static double[] jsonArray1dToDouble1d(JSONArray jsonArray1d){
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
    public static ImageHeader jsonObjectToImageHeader(JSONObject jsonObject) throws IllegalAccessException {

        ImageHeader imageHeader = new ImageHeader();
        Class<?> objClass =  imageHeader.getClass();

        //array containing Field objects reflecting all the accessible public fields of the
        //class or interface represented by this Class object
        Field[] fields = objClass.getFields();
        for (Field field : fields) {

            if (field == null || field.toString().contains("final")) continue;
            String name = field.getName();
            if (name.equalsIgnoreCase("blank_value")){
                Logger.info("debug");
            }
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
            else {

                if (field.getType().isPrimitive()){
                    field.set(imageHeader, Double.NaN);
                }
                else {
                    field.set(imageHeader, null);
                }
            }
        }
        return imageHeader;
    }


    /**
     * This method to convert one or two dimensional object/double array to JSONArray
     * @param obj
     * @return
     */
    private static JSONArray objetArrayToJsonSring(Object obj) {
        JSONArray list2d = new JSONArray();

        int dim= (obj instanceof double[][])?2:1;
        if (dim==2){
            double[][] dArray=(double[][]) obj;
            for (int i=0; i<dArray.length; i++) {
                JSONArray list1d = new JSONArray();
                for (int j = 0; j < dArray[0].length; j++) {
                    list1d.add(dArray[i][j]);
                }
                list2d.add(list1d);
            }
            return list2d;
        }
        else {
            double[] dArray=(double[]) obj;
            JSONArray list1d = new JSONArray();
            list1d.clear();

            for (int j = 0; j < dArray.length; j++) {
                list1d.add(dArray[j]);
            }
            return list1d;
        }


    }

    /**
     * This method convert the ImageHeader object to JSONObject
     * @param obj
     * @return
     * @throws Exception
     */
    public static JSONObject ConvertImageHeaderToJsonObject(Object obj)  throws Exception {

        JSONObject jsonObj = new JSONObject();

        Class<?> objClass = obj.getClass();
        //array containing Field objects reflecting all the accessible public fields of the
        //class or interface represented by this Class object
        Field[] fields = objClass.getFields();
        //process the field's name and value pair and save to JSONObject
        for (Field field : fields) {
            if (field == null) continue;
            String name = field.getName();
            Object value = field.get(obj);
            if (value != null) {
                if (value.getClass().isArray() ) {
                    jsonObj.put(name, objetArrayToJsonSring(value));
                }
                else {
                    if (name.equalsIgnoreCase("bunit")){

                        jsonObj.put(name, value.toString());
                    }
                    else {
                        jsonObj.put(name, value);
                    }

                }

            }
        }
        return jsonObj;
    }

    /**
     * This method writes the ImageHeader object to a Json file.
     * @param imageHeader - an ImageHeader
     * @param outJsonFile - output file (path +fileName.json)
     * @throws Exception
     */
    public static void writeImageHeaderToJson(ImageHeader imageHeader, String outJsonFile) throws Exception {


        JSONObject obj = new JSONObject();
        //convert the ImageHeader object to jsonString
        JSONObject imageHeaderObj = ConvertImageHeaderToJsonObject(imageHeader);
        obj.put("header",imageHeaderObj);

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}