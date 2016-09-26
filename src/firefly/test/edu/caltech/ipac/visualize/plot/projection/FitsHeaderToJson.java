package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by zhang on 6/21/16
 *
 */
public class FitsHeaderToJson {

    private static final String TEST_ROOT = "test"+ File.separatorChar;
    private static final String PATH= TEST_ROOT+FitsHeaderToJson.class.getCanonicalName().replaceAll("\\.", "/")
            .replace(FitsHeaderToJson.class.getSimpleName(), "") + File.separatorChar;


    /**
     * This is a good way to do it but don't figure out how to do it better.  It works for now.
     * @param obj
     * @return
     */
    private static int getDim(Object obj){

        try{
            //when this casting fail, it means the dimension is 1
            double[][] dArray=(double[][]) obj;
            return 2;
        }
        catch(Exception e){
            return 1;
        }
    }

    /**
     * This method to convert one or two dimensional object/double array to JSONArray
     * @param obj
     * @return
     */
    private static JSONArray objetArrayToJsonSring(Object obj) {
        JSONArray list2d = new JSONArray();
        JSONArray list1d = new JSONArray();

        if (getDim(obj)==2){
            double[][] dArray=(double[][]) obj;
            int len = dArray.length/2;
            for (int i=0; i<len; i++) {
                list1d.clear();
                for (int j = 0; j < len; j++) {
                    list1d.add(dArray[i][j]);
                }
                list2d.add(list1d);
            }
            return list2d;
        }
        else {
            double[] dArray=(double[]) obj;
            list1d.clear();

            for (int j = 0; j < dArray.length; j++) {
                list1d.add(dArray[j]);
            }
            return list1d;
        }


    }

    /**
     * This method put the ImageHeader to a JSONObject.  It
     * @param obj
     * @return
     * @throws Exception
     */
    public static JSONObject  ImageHeaderToJson(Object obj)  throws Exception {

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

    private   static  ImageHeader getImageHeader(Fits fits)throws FitsException {
        BasicHDU[] hdus = fits.read();

        if (hdus == null){

            throw new FitsException (" hdu is null");
        }
        Header header = hdus[0].getHeader();
        ImageHeader imageHeader= new ImageHeader(header);

        return imageHeader;
    }


    public static void writeToJson(String inputFitsFile) throws Exception {

        Fits fits = new Fits(inputFitsFile);
        JSONObject obj = new JSONObject();
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";

        obj.put("headerFileName", outJsonFile );
        ImageHeader imageHeader = getImageHeader(fits);

        //convert the ImageHeader object to jsonString
        JSONObject imageHeaderObj = ImageHeaderToJson(imageHeader);
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



    public static void main(String[] args) throws Exception {

        if (args.length<1){
            System.out.println("usage FitsHeaderToJson inputFile");
        }



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
                writeToJson(path + fNames[i]);
            }
            catch (FitsException e) {
                System.out.println("========ERROR=========="+fNames[i]+"===============");
                e.printStackTrace();
            }

        }

    }

}