/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize.imageretrieve;

import edu.caltech.ipac.astro.ibe.datasource.AtlasIbeDataSource;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.visualize.net.SloanDssImageParams;

/**
 * @author Trey Roby
 */
public class ServiceDesc {


    public static String get(WebPlotRequest r) {
        return switch (r.getServiceType()) {
            case ISSA -> getIssaDesc(r);
            case IRIS -> getIrisDesc(r);
            case TWOMASS -> get2MassDesc(r);
            case MSX -> "MSX Image" + r.getServiceType();
            case DSS -> getDssDesc(r);
            case SDSS -> getSloanDssDesc(r);
            case WISE -> getWiseDesc(r);
            case AKARI, SEIP, ATLAS -> getAtlasDesc(r);
            case UNKNOWN -> r.getServiceTypeString();
            default -> r.getServiceType() + "";
        };
    }


    private static String getAtlasDesc(WebPlotRequest r) {

        String schema= r.getParam(AtlasIbeDataSource.DATASET_KEY);
        String table=r.getParam(AtlasIbeDataSource.TABLE_KEY);
        String band= r.getSurveyBand();
        return schema + " "+table+ " " + band;
    }


    private static String getDssDesc(WebPlotRequest r) {
        String survey= r.getSurveyKey().toLowerCase();
        String root = "DSS ";

        return switch (survey) {
            case "poss2ukstu_red" -> root + "POSS2/UKSTU Red";
            case "poss2ukstu_ir" -> root + "POSS2/UKSTU Infrared";
            case "poss2ukstu_blue" -> root + "POSS2/UKSTU Blue";
            case "poss1_red" -> root + "POSS1 Red";
            case "poss1_blue" -> root + "POSS1 Blue";
            case "quickv" -> root + "Quick-V Survey";
            case "phase2_gsc2" -> root + "HST Phase 2 Target Positioning(GSC 2)";
            case "phase2_gsc1" -> root + "HST Phase 1 Target Positioning(GSC 1)";
            default -> root;
        };
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
        String band= request.getSurveyBand().toLowerCase();
        String root = "2MASS ";
        return switch (band) {
            case "j" -> root + "J";
            case "h" -> root + "H";
            case "k" -> root + "K";
            default -> root;
        };
    }

    private static String getIrisDesc(WebPlotRequest request) {
        return getIrisDesc(request.getSurveyKey());
    }

    private static String getIrisDesc(String survey ) {
        String root = "IRAS: ";
        return switch (survey) {
            case "12" -> root + "IRIS 12 micron";
            case "25" -> root + "IRIS 25 micron";
            case "60" -> root + "IRIS 60 micron";
            case "100" -> root + "IRIS 100 micron";
            default -> root;
        };
    }

    private static String getIssaDesc(WebPlotRequest request) {
        String survey= request.getSurveyKey();
        String root = "ISSA ";
        return switch (survey) {
            case "12" -> root + "12 micron";
            case "25" -> root + "25 micron";
            case "60" -> root + "60 micron";
            case "100" -> root + "100 micron";
            default -> root;
        };
    }
}
