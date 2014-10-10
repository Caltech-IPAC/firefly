package edu.caltech.ipac.hydra.server.query;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: Sept. 19, 2014
 * Time: 2:21:33 PM
 * To change this template use File | Settings | File Templates.
 */


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.astro.IpacTableWriter;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.DynQueryProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.ImageGridSupport;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.visualize.VisContext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.hydra.data.PlanckCutoutRequest;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.util.*;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.*;


@SearchProcessorImpl(id = "planckImagesCutoutQuery")
public class QueryPlanckImagesCutout extends DynQueryProcessor {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    //private static final String PLANCK_CUTOUT_HOST= "http://***REMOVED***:9072/cgi-bin/Heal2Tan/nph-heal2tan";
    private static final double FWHM [] ={32.65,27.00,13.01,9.94,7.04,4.66,4.41,4.47,4.23};
    private static final int planckBands[] = {30,44,70,100,143,217,353,545,857};
    private static final String wmapBands[] = {"K","Ka","Q","V","W"};
    private static final int irasBands[] = {100,60,25,12};
    private static final int width = 96;

    public static final String RA = "ra";
    public static final String DEC = "dec";
    public static final String OBJ_NAME = "name";
    public static final String MAX_SEARCH_TARGETS = "maxSearchTargets";

//    private int maxSearchTargets = Integer.MAX_VALUE;
//    private List<Target> targets = null;

    private Target curTarget = null;

    protected File loadDynDataFile(TableServerRequest request) throws IOException, DataAccessException {
        long start = System.currentTimeMillis();
        String name = request.getParam("name");
        String ra  = request.getParam("ra");
        String dec =request.getParam("dec");
        String baseUrl = request.getParam(PlanckCutoutRequest.CUTOUT_HOST);
        String releaseVersion = request.getParam(PlanckCutoutRequest.RELEASE_VERSION);
        String mType = "I";
        //WebPlotRequest wpReq;
        String ExpandedDesc,desc;
        DataObject row;
        String groupName, url, planckurl, sizeStr, pixsize;



        String pos = ra +"," +  dec;
        String subsizeStr = "2.0";
        Float subsize = new Float(subsizeStr);
        curTarget = new TargetFixedSingle(name, new PositionJ2000(new Double(ra), new Double(dec)));

        ArrayList<DataType> defs = ImageGridSupport.createBasicDataDefinitions();
        DataGroup table = ImageGridSupport.createBasicDataGroup(defs, true);

        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_HILITE", "F"));
        table.addAttributes(new DataGroup.Attribute("JPEG_SELECTION_DOUBLE_CLICK", "T"));

        WorldPt pt;
        pt = getTargetWorldPt(curTarget);
        String req;
        RangeValues rv= new RangeValues(RangeValues.PERCENTAGE,1.0,RangeValues.PERCENTAGE,99.0,RangeValues.STRETCH_LINEAR);
        WebPlotRequest wpReq;

        //For Planck
        url = createCutoutURLString(baseUrl, pos, releaseVersion, mType);
//        table.addAttributes(new DataGroup.Attribute("TitleDesc.PLANCK-"+mType, "(Planck Cutouts)"));
        for(int j = 0; j < planckBands.length; j++){
            row = new DataObject(table);

            //skip Q and U for 545 and 857
//            if (mType.equals("Q") && planckBands[j]==545){
//                continue;
//            } else if (mType.equals("U") && planckBands[j]==545){
//                continue;
//            } else if (mType.equals("Q") && planckBands[j]==857){
//                continue;
//            } else if (mType.equals("U") && planckBands[j]==857){
//                continue;
//            }

            if (planckBands[j]==30 || planckBands[j]==44 || planckBands[j]==70){
                pixsize ="2.0";
                sizeStr = "60";
            } else {
                pixsize = "1.0";
                sizeStr = "120";
            }

            planckurl= url+"&pixsize="+pixsize+"&size="+sizeStr+"&mission=planck&planckfreq="+planckBands[j]+"&wmapfreq=&submit=";

            groupName= "PLANCK";
            desc= planckBands[j] + "GHz";
            ExpandedDesc= groupName+"-"+desc;
            wpReq= WebPlotRequest.makeURLPlotRequest(planckurl,ExpandedDesc);
            wpReq.setWorldPt(pt);
            wpReq.setSizeInDeg(subsize);
            wpReq.setZoomType(ZoomType.TO_WIDTH);
            wpReq.setZoomToWidth(width);
            wpReq.setHasMaxZoomLevel(false);
            wpReq.setInitialColorTable(4);
            wpReq.setInitialRangeValues(rv);
            wpReq.setExpandedTitle(ExpandedDesc);
            wpReq.setHideTitleDetail(true);
            wpReq.setTitle(desc);
            req= wpReq.toString();

            addToRow(table, req, desc, groupName);
        }


        //for WMAP

        for (String band: wmapBands) {
            row = new DataObject(table);
            pixsize ="2.0";
            sizeStr = getSizeStr(pixsize,subsizeStr);
            String wmapurl = url + "&pixsize=" + pixsize + "&size=" + sizeStr + "&mission=wmap" + "&wmapfreq=" +
                            band + "&planckfreq=&submit=";
            groupName= "WMAP";
            desc= band;
            ExpandedDesc= groupName+"-"+desc;
            wpReq= WebPlotRequest.makeURLPlotRequest(wmapurl, ExpandedDesc);
            wpReq.setWorldPt(pt);
            wpReq.setSizeInDeg(subsize);
            wpReq.setZoomType(ZoomType.TO_WIDTH);
            wpReq.setZoomToWidth(width);
//            wpReq= WebPlotRequest.makeFilePlotRequest(listOfwmapFiles[i].getPath(),2.0F);
            wpReq.setInitialColorTable(4);
            wpReq.setInitialRangeValues(rv);
            wpReq.setExpandedTitle(ExpandedDesc);
            wpReq.setHideTitleDetail(true);
            wpReq.setTitle(desc);
            req= wpReq.toString();

            addToRow(table, req, desc, groupName);
        }


    //    if (map_scale.equals("yes")){
    //        table.addAttributes(new DataGroup.Attribute("TitleDesc.IRAS",
    //                "(IRAS Cutouts from ISSA plates: Cutout Image Size="+toDegString(subsizeStr)+")"));
    //    } else {
    //        table.addAttributes(new DataGroup.Attribute("TitleDesc.IRAS", "(IRAS Cutouts from ISSA plates)"));
    //    }
        //for IRAS
        //Padding the cutout size for glactic north up rotation first
        //and crop the image
        for (int band: irasBands) {
            row = new DataObject(table);
            groupName= "IRAS";
            ExpandedDesc= groupName+" "+band+" microns";
            desc= band+" microns";
            wpReq= WebPlotRequest.makeISSARequest(pt,Integer.toString(band),subsize);
            wpReq.setRotateNorth(true);
            wpReq.setRotateNorthType(CoordinateSys.GALACTIC);
            wpReq.setPostCropAndCenter(true);
            wpReq.setPostCropAndCenterType(CoordinateSys.GALACTIC);
            wpReq.setZoomType(ZoomType.TO_WIDTH);
            wpReq.setZoomToWidth(width);
            wpReq.setInitialColorTable(4);
            wpReq.setInitialRangeValues(rv);
            wpReq.setExpandedTitle(ExpandedDesc);
            wpReq.setTitle(desc);
            wpReq.setHideTitleDetail(true);
            req= wpReq.toString();
            addToRow(table, req, desc, groupName);

        }

        // write out table into ipac-table format..
        if (table.size()==0) {
            table.addAttributes(new DataGroup.Attribute("INFO", "Image data not found!"));
        }
        File f = createFile(request);
        table.shrinkToFitData();
        IpacTableWriter.save(f, table);
        return f;
    }


    private static void addToRow(DataGroup table, String req, String desc, String groupName) {
        DataObject row = new DataObject(table);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.TYPE.toString()), "req");
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.THUMBNAIL.toString()), req);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.DESC.toString()), desc);
        row.setDataElement(table.getDataDefintion(ImageGridSupport.COLUMN.GROUP.toString()), groupName);
        table.add(row);
    }

    private String getSizeStr (String pixsize, String subsizeStr) {

        Float pixelscale = new Float(pixsize);
        Float subsize = new Float(subsizeStr);
        int sizePix = (int)((subsize*60)/pixelscale);
        String sizeStr = Integer.toString(sizePix);
        return sizeStr;
    }

    public static String createCutoutURLString(String baseUrl,String pos, String version, String mtypes) {
        //String url = baseUrl + "?locstr=" + pos + "&pixsize=" + pixsize + "&version=" + version + "&hmap=" + mtypes;
        String url = baseUrl + "?locstr=" + pos + "&version=" + version + "&hmap=" + mtypes;
        return url;
    }


    @Override
    public void prepareTableMeta(TableMeta meta, List<DataType> columns, ServerRequest request) {
        meta.setAttribute("GRID_THUMBNAIL_COLUMN", "thumbnail");
        meta.setAttribute("PLOT_REQUEST_COLUMN", "req");
    }

    //------------------------------------ static methods ------------------------------------
    private static WorldPt getTargetWorldPt(Target t) throws IOException {
        WorldPt pt = null;
        if (!t.isFixed()) {
            throw new IOException("Table upload cannot support moving targets.");
        }
        Fixed ft = (Fixed) t;

        pt = new WorldPt(ft.getPosition().getRa(), ft.getPosition().getDec());

        return pt ;
    }

    private static String toDegString(String s) {
        float sv = StringUtils.getFloat(s);

        return NumberFormat.getNumberInstance().format(sv) + " deg";
    }

}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/

