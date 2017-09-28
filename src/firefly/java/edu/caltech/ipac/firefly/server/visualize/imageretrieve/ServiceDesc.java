/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;

/**
 * @author Trey Roby
 */
public class ServiceDesc {


    public static String get(WebPlotRequest r) {
        switch (r.getServiceType()) {
            case ISSA: return getIssaDesc(r);
            case IRIS: return getIrisDesc(r);
            case TWOMASS: return get2MassDesc(r);
            case MSX: return "MSX Image" + r.getServiceType();
            case DSS: return getDssDesc(r);
            case SDSS: return getSloanDssDesc(r);
            case WISE: return getWiseDesc(r);
            default: return r.getServiceType()+"";
        }
    }



    private static String getDssDesc(WebPlotRequest r) {

        String survey= r.getSurveyKey().toLowerCase();
        String root = "DSS ";

        switch (survey) {
            case "poss2ukstu_red": return root + "POSS2/UKSTU Red";
            case "poss2ukstu_ir": return root + "POSS2/UKSTU Infrared";
            case "poss2ukstu_blue": return root + "POSS2/UKSTU Blue";
            case "poss1_red": return root + "POSS1 Red";
            case "poss1_blue": return root + "POSS1 Blue";
            case "quickv": return root + "Quick-V Survey";
            case "phase2_gsc2": return root + "HST Phase 2 Target Positioning(GSC 2)";
            case "phase2_gsc1": return root + "HST Phase 1 Target Positioning(GSC 1)";
            default : return root;
        }
    }


    private static String getSloanDssDesc(WebPlotRequest r) {
        String bandStr = r.getSurveyKey();
        SloanDssImageParams.SDSSBand band;
        try {
            band = Enum.valueOf(SloanDssImageParams.SDSSBand.class,bandStr);
        } catch (Exception e) {
            band= SloanDssImageParams.SDSSBand.r;
        }
        return "Band: " + band;
    }



    private static String getWiseDesc(WebPlotRequest r) {
        return "WISE "+r.getSurveyKey()+ " " + r.getSurveyBand();
    }

    private static String get2MassDesc(WebPlotRequest request) {
        String survey= request.getSurveyKey().toLowerCase();
        String root = "2MASS ";
        switch (survey) {
            case "j": return root + "J";
            case "h": return root + "H";
            case "k": return root + "K";
            default : return root;
        }
    }

    private static String getIrisDesc(WebPlotRequest request) {
        return getIrisDesc(request.getSurveyKey());
    }

    private static String getIrisDesc(String survey ) {
        String root = "IRAS: ";
        switch (survey) {
            case "12": return root + "IRIS 12 micron";
            case "25": return root + "IRIS 25 micron";
            case "60": return root + "IRIS 60 micron";
            case "100": return root + "IRIS 100 micron";
            default : return root;
        }
    }

    private static String getIssaDesc(WebPlotRequest request) {
        String survey= request.getSurveyKey();
        String root = "ISSA ";
        switch (survey) {
            case "12": return root + "12 micron";
            case "25": return root + "25 micron";
            case "60": return root + "60 micron";
            case "100": return root + "100 micron";
            default : return root;
        }
    }
}
