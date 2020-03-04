package edu.caltech.ipac.firefly.util;

import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.DataType;
import edu.caltech.ipac.visualize.plot.*;
import edu.caltech.ipac.visualize.plot.projection.Projection;
import nom.tam.fits.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.List;

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
        //we only use the first HDU in the FITS for unit test, just for simplicity reason
        Header header = hdus[0].getHeader();
        ImageHeader imageHeader= new ImageHeader(header);

        return imageHeader;
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
//                    if(!field.getName().equals("using_tpv"))
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

    private static JSONObject addCardInfo( Header header, String[] headerKeys){
        JSONObject jsonObj = new JSONObject();
        for (int i=0; i< headerKeys.length; i++){
            JSONObject obj = new JSONObject();
            HeaderCard aCard= header.findCard( headerKeys[i]);
            if(aCard!=null && aCard.getComment()!=null) {
                obj.put("comment", aCard.getComment());
            }

            if(aCard!=null && aCard.getValue()!=null) {
                obj.put("value", aCard.getValue());
            }
            jsonObj.put(headerKeys[i], obj);
        }
        return jsonObj;
    }
    /**
     * This method processes the header and get the minimum set of key,value pairs out for wavelength calculation
     * and save to Json object format
     * @param header
     * @return
     * @throws Exception
     */
    public static JSONObject convertHeaderToWavelengthHeaderJson(Header header)  throws Exception {

        //Class<?> objClass = obj.getClass();

        int N = Math.max(header.getIntValue("naxis", -1), header.getIntValue("WCSAXES", -1));
        if (N==-1) {
            throw new IllegalArgumentException("This header does not contain proper N value for wavelength calcualtion");
        }
        String PCKey =  header.containsKey("CDELT3")?"PC3":"CD3";
        String[] wlIntParamsKeys = {
                "NAXIS",
                "WCSAXES",
                "WCSAXIS",
                "RESTWAV",
                "CRPIX1",
                "CRPIX2",
                "CRPIX3",
                "CDELT3",
                "CTYPE3",
                "CRVAL3",
                "CUNIT3",
                "PS3_0",
                "PS3_1",
                "PS3_2",
                "PV3_0",
                "PV3_1",
                "PV3_2",
                PCKey +"_"+ 1,
                PCKey +"_"+ 2,
                PCKey +"_"+ 3,

        };

        return addCardInfo( header, wlIntParamsKeys);

    }
    /**
     * This method convert the ImageHeader object to JSONObject
     * @param obj
     * @return
     * @throws Exception
     */
    public static JSONObject convertImageHeaderToJsonObject(Object obj)  throws Exception {

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
     * This method is create a JSONObject which contains a ImageHeader, Projection etc for any given Fits file.
     * @param inputFitsFile
     * @throws Exception
     */

    public static void writeImageHeaderAndProjectionToJson(String inputFitsFile) throws Exception {

        Fits fits = new Fits(inputFitsFile);
        JSONObject obj = new JSONObject();
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";

        obj.put("headerFileName", outJsonFile );
        ImageHeader imageHeader = getImageHeader(fits);

        //convert the ImageHeader object to jsonString
        JSONObject imageHeaderObj = convertImageHeaderToJsonObject(imageHeader);
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
     *
     * @param inputFitsFile
     * @throws Exception
     */
    public static void writeProjectionToJson(String inputFitsFile ) throws Exception {

        JSONObject obj = new JSONObject();

        Fits fits = new Fits(inputFitsFile);
        ImageHeader imageHeader =  getImageHeader(fits);
        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length() - 5) + "Projection.json";
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
     * This method is to write the required parameters for wavelength calculation in the header to json
     * @throws Exception
     */
    public static void writeHeaderToJson(String inputFitsFile, boolean isImageHeader) throws Exception {

        //use part of the input file to make an output file name
        JSONObject jsonObject = new JSONObject();
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";
        //add the headerFileName in the output json file
        jsonObject.put("headerFileName", outJsonFile );

        //read FITs and Headers
        Fits fits = new Fits(inputFitsFile);
        BasicHDU[] hdu = fits.read();

        //convert the ImageHeader object to jsonString
        if (isImageHeader) {
            writeHeaderToJson(jsonObject, new ImageHeader(hdu[0].getHeader()), outJsonFile, null);
        }
        else {
            String ctype= hdu[0].getHeader().getStringValue("CTYPE3");
            JSONObject wlTable=null;
            if ("WAVE-TAB".equals(ctype)) {
                wlTable = getWLTable(inputFitsFile, 1);
            }
            writeHeaderToJson(jsonObject, hdu[0].getHeader(), outJsonFile, wlTable );
        }
    }

    public static JSONArray getTableDataJsonArray(DataType[] columns, DataGroup dg){

        //NOTE: the TAB data stored as one row data array, so row index is 0.
        float[][] colData=new float[2][];
        for (int i=0; i<columns.length; i++){
            colData[i] = (float[]) dg.getData(columns[i].getKeyName(), 0);
        }

        JSONArray col1DataArrJson = new JSONArray();
        for (int i=0; i<colData[0].length; i++){
            col1DataArrJson.add(colData[0][i]);
        }

        JSONArray col2DataArrJson = new JSONArray();
        for (int i=0; i<colData[1].length; i++){
            col2DataArrJson.add(colData[1][i]);
        }

        JSONArray dataJsonAry = new JSONArray();
        dataJsonAry.add(col1DataArrJson);
        dataJsonAry.add(col2DataArrJson);

        return dataJsonAry;
    }

    public static JSONArray getColumnDefinitionAry(DataType[] columns){
        JSONArray columnAryJson = new JSONArray();

        for (int i=0; i<columns.length; i++){
            JSONObject colObj = new JSONObject();
            colObj.put("name", columns[i].getKeyName());
            colObj.put("arraySize", columns[i].getArraySize());
            colObj.put("type", columns[i].getTypeDesc());
            columnAryJson.add(colObj);
        }
        return columnAryJson;
    }
    /**
     *
     * @param filePath
     * @param tblIdx
     * @return
     */
    public static JSONObject getWLTable(String filePath, int tblIdx){

        DataGroup dg = FileLoader.loadIpacTable(filePath,  tblIdx);

        DataType[] columns =dg.getDataDefinitions();
        JSONArray columnAryJson = getColumnDefinitionAry(columns);


        JSONArray dataJsonAry= getTableDataJsonArray(columns, dg);
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("0", dataJsonAry);



        JSONObject tableDataJson = new JSONObject();
        tableDataJson.put("data", jsonObj);
        tableDataJson.put("columns",columnAryJson );

        JSONObject tableJsonObj = new JSONObject();
        tableJsonObj.put("tableData", tableDataJson );


        return tableJsonObj;

    }

    /**
     *
     * @param filePath
     * @param inputFitsFile
     * @param tblIdx
     * @return
     */
    public static JSONObject getWLTable(String filePath, String inputFitsFile, int tblIdx){
        String fitsFilePath = filePath + "/"+inputFitsFile;
        return getWLTable(fitsFilePath, tblIdx);

    }

    /**
     *
     * @param cls
     * @param inputFitsFile
     * @param tblIdx
     * @return
     */
    public static JSONObject getWLTable(Class cls, String inputFitsFile, int tblIdx){
        String fitsFilePath = FileLoader.getDataPath(cls) + "/"+inputFitsFile;
        return getWLTable(fitsFilePath, tblIdx);

    }
    /**
     *
     * @param aHeader-either an ImageHeader or a FITs header
     * @param outJsonFile - output file
     * @throws Exception
     */
    public static void writeHeaderToJson(JSONObject jsonObject, Object aHeader, String outJsonFile, JSONObject wlTable) throws Exception {


        //convert the ImageHeader object to jsonString
        JSONObject jsonHeader=null;
        if (aHeader instanceof ImageHeader ){
            jsonHeader = convertImageHeaderToJsonObject( aHeader);
        }
        else if (aHeader instanceof Header){
            jsonHeader = convertHeaderToWavelengthHeaderJson ( (Header) aHeader);



        }
        jsonObject.put("header" ,jsonHeader );

        if (wlTable !=null){
            jsonObject.put("wlTable", wlTable);
        }

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(jsonObject.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) throws Exception {

        String fileName = "wavelengthTable.fits";
        String filePath = "/Users/zhang/hydra/cm/firefly_test_data/edu/caltech/ipac/visualize/plot/projection/";//run in intelliJ
        //FileLoader.getDataPath(ProjectionTest.class); //run in command line, use this one
        FitsHeaderToJson.writeHeaderToJson(filePath + fileName, false);

    }

}