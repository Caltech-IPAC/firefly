package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.*;
import nom.tam.fits.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;

/**
 * Created by zhang on 6/21/16.N
 */
public class FitsHeaderToJson {

    private static final String TEST_ROOT = "test"+ File.separatorChar;
    private static final String PATH= TEST_ROOT+FitsHeaderToJson.class.getCanonicalName().replaceAll("\\.", "/")
            .replace(FitsHeaderToJson.class.getSimpleName(), "") + File.separatorChar;


    public static String toString(Object obj) throws Exception {
        Class<?> objClass = obj.getClass();
        StringBuffer sb = new StringBuffer();
        Field[] fields = objClass.getFields();
        for(Field field : fields) {
            String name = field.getName();
            Object value = field.get(obj);
            sb.append(name + ": " + value.toString() + " ");

        }
        return sb.toString();
    }
    public static void writeToJson(String inputFitsFile) throws Exception {

        Fits fits = new Fits(inputFitsFile);
        JSONObject obj = new JSONObject();
        String outJsonFile = inputFitsFile.substring(0, inputFitsFile.length()-5 ) + "Header.json";

        ImageHeader imageHeader = getImageHeader(fits);

        String imageHeaderStr = imageHeader.imageHeaderToString();

        obj.put("headerFileName", outJsonFile );



        //put origin header into json
        obj.put("header",imageHeaderStr.replace("\n", ""));


        //TODO need to finish this when I am back in Aug.
        double[] amd_x_coeff= imageHeader.getCoeffData("amd_x_coeff");
        double[] amd_y_coeff= imageHeader.getCoeffData("amd_y_coeff");
        double[] ppo_coeff= imageHeader.getCoeffData("ppo_coeff");



        if (amd_x_coeff!=null) {
           String s="[";
           for (int i=0; i<amd_x_coeff.length; i++){
             s=s+amd_x_coeff[i]+" ";
           }
            s=s+"]";
            obj.put("amd_x_coeff",s );
        }
        if (amd_y_coeff!=null) {
            String s="[";
            for (int i=0; i<amd_y_coeff.length; i++){
                s=s+amd_y_coeff[i]+" ";
            }
            s=s+"]";
            obj.put("amd_y_coeff",s );
        }
        if (ppo_coeff!=null) {
            String s="[";
            for (int i=0; i<ppo_coeff.length; i++){
                s=s+ppo_coeff[i]+" ";
            }
            s=s+"]";
            obj.put("po_coeff",s );
        }



        Projection projection = imageHeader.createProjection(CoordinateSys.EQ_J2000);
        ProjectionPt image_pt = projection.getImageCoords( imageHeader.crval1, imageHeader.crval2);//RA and DEC at the center of the image
        /*the expected value is achieved when the test is written.  If the Java code changes and
        the Assert is falling, the changes introduce the problem.
        */
        WorldPt world_pt = projection.getWorldCoords(image_pt.getX(), image_pt.getY());

        obj.put("expectedImagePt", new String("x="+image_pt.getX() + " y=" + image_pt.getY()));
        obj.put("expectedWorldPt", new String("x="+world_pt.getX() + " y=" + world_pt.getY()));

        try {
            FileWriter file = new FileWriter(outJsonFile);
            file.write(obj.toJSONString());
            file.flush();
            file.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private   static  ImageHeader getImageHeader(Fits fits)throws FitsException {
        BasicHDU[] hdus = fits.read();

        if (hdus == null)
        {
            throw new FitsException (" hdu is null");
        }
       // return  hdus [0].getHeader();


       return  new ImageHeader(hdus [0].getHeader());
    }

    private   static Header getHeader(Fits fits)throws FitsException {
        BasicHDU[] hdus = fits.read();

        if (hdus == null)
        {
            throw new FitsException (" hdu is null");
        }
        return  hdus [0].getHeader();


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