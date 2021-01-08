/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;

import edu.caltech.ipac.firefly.data.HttpResultInfo;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Booth Hartley
 * @version $Id: HorizonsEphFile.java,v 1.8 2012/01/23 22:09:55 roby Exp $
 */
public class HorizonsEphFile {

    private static final String CGI_CMD= "/x/smb_spk.cgi";

    private static String suggested_filename;




    public static byte[] lowlevelGetEphFile(HorizonsFileParams params) throws FailedRequestException {

        String urlStr=  HorizonsEphPairs.horizonsServer + CGI_CMD;
        HttpResultInfo info;
        try {
            info = URLDownload.getDataFromURL(new URL(urlStr),buildParamString(params),null,null);
        } catch (MalformedURLException e) {
            throw new FailedRequestException("bad url: " + urlStr);
        }
        if (info.getContentType() != null && info.getContentType().startsWith("text/")) {
            throw new FailedRequestException(
                    info.getResultAsString(),
                    "The Horizons server is reporting an error- " +
                            "the Horizons error message was displayed to the user.");
        }
        suggested_filename = info.getExternalName();
        return info.getResult();
    }

    public static String getSuggestedFilename() { return suggested_filename; }

    private static Map<String,String> buildParamString(HorizonsFileParams params) throws FailedRequestException {

        Map<String,String> data=new HashMap<>();
        try {
            String qType=null;
            if(HorizonsFileParams.XSP_EXT.equals(params.getFileType())) {
                qType= "-T";
            }
            else if(HorizonsFileParams.BSP_EXT.equals(params.getFileType())) {
                qType= "-B";
            }
            else {
               throw new FailedRequestException("Could not get all parameters",
                     "parameter extension not "+
                      HorizonsFileParams.XSP_EXT + " or "+
                      HorizonsFileParams.BSP_EXT+
                      " - Caller sent extension: " +  params.getFileType());
            }

            data.put("OPTION", "Make SPK");
            data.put("OBJECT", params.getNaifID());
            data.put("START", params.getBeginDateStr());
            data.put("STOP", params.getEndDateStr());
            data.put("EMAIL", "spot@caltech.edu");
            data.put("TYPE", qType);

            if(!params.isStandard()) {
                data.put("EPOCH", params.getEpoch());
                data.put("EC", params.getE());
                data.put("QR", params.getQ());
                data.put("TP", params.getT());
                data.put("OM", params.getBigOmega());
                data.put("W", params.getLittleOmega());
                data.put("IN", params.getI());
            }
        } catch (Exception e) {
            throw new FailedRequestException("Could not encode query",
                                             null, e);
        }
        return data;
    }

    private static void addParam(String       param,
                                 String       value,
                                 Map<String,String> map) {
        map.put(param,value);
    }


//    public static void main(String args[]) {
//        try {
//            SimpleDateFormat df=  new SimpleDateFormat("dd-MMM-yyyy");
//            Date begin= df.parse("1-jan-2004");
//            Date end= df.parse("1-jan-2008");
//
//            //Ephemeris eph= new StandardEphemeris(1000036, "");
//            //Ephemeris eph= new StandardEphemeris(123, "");
//
//            Ephemeris eph= new StandardEphemeris(999,"Pluto");
//
////            Ephemeris eph= new NonStandardEphemeris("2452944.5","244",
////                                                    .9680055D,
////                                                    .5714061D, 1.62D,
////                                                    111.712D,
////                                                    58.89801D);
//
//
//            NonStandardEphemeris nonStdEph1= new NonStandardEphemeris("2452944.5","244",
//                                                          .9680055D,
//                                                          .5714061D, 1.62D,
//                                                          111.712D,
//                                                          58.89801D);
//
//            int code1 = Arrays.hashCode( new Object [] { nonStdEph1.getEpoch(),
//                                          nonStdEph1.getT(),
//                                          nonStdEph1.getE(),
//                                          nonStdEph1.getQ(),
//                                          nonStdEph1.getI(),
//                                          nonStdEph1.getBigOmega(),
//                                          nonStdEph1.getLittleOmega() } );
//
//            //NonStandardEphemeris nonStdEph2= new NonStandardEphemeris(new String("2452944.5"),"244",
//            NonStandardEphemeris nonStdEph2= new NonStandardEphemeris(
//                                          new String("2452944.5"),
//                                                                      "244",
//                                                                      .9680055D,
//                                                                      .5714061D, 1.62D,
//                                                                      111.712D,
//                                                                      58.89801D);
//
//            int code2 = Arrays.hashCode( new Object [] { nonStdEph2.getEpoch(),
//                                          nonStdEph2.getT(),
//                                          nonStdEph2.getE(),
//                                          nonStdEph2.getQ(),
//                                          nonStdEph2.getI(),
//                                          nonStdEph2.getBigOmega(),
//                                          nonStdEph2.getLittleOmega() } );
//
//
//            NonStandardEphemeris nonStdEph3= new NonStandardEphemeris("2452944.5","244",
//                                                                      .1680055D,
//                                                                      .5714061D, 1.62D,
//                                                                      111.712D,
//                                                                      58.89801D);
//
//            int code3 = Arrays.hashCode( new Object [] { nonStdEph3.getEpoch(),
//                                          nonStdEph3.getT(),
//                                          nonStdEph3.getE(),
//                                          nonStdEph3.getQ(),
//                                          nonStdEph3.getI(),
//                                          nonStdEph3.getBigOmega(),
//                                          nonStdEph3.getLittleOmega() } );
//
//
//
//
//            System.out.println("code1= "+ code1);
//            System.out.println("code2= "+ code2);
//            System.out.println("code3= "+ code3);
//
//            HorizonsFileParams params= new HorizonsFileParams(
//                                                eph, begin, end,
//                                                HorizonsFileParams.BSP_EXT);
//
//            byte results[]= lowlevelGetEphFile(params);
//            FileOutputStream out= new FileOutputStream(
//                                         new File(params.toString()));
//            out.write(results);
//        } catch (Exception e) {
//            System.out.println("e= " + e.toString());
//            e.printStackTrace();
//        }
//    }


}
