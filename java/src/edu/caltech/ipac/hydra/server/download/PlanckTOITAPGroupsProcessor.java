package edu.caltech.ipac.hydra.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.hydra.data.PlanckTOITAPRequest;
import edu.caltech.ipac.hydra.server.query.PlanckTOITAPFileRetrieve;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: wmi
 * Date: July 16, 2014
 * Time: 1:40:11 PM
 * To change this template use File | Settings | File Templates.
 */


@SearchProcessorImpl(id = "planckTOITAPDownload")
public class PlanckTOITAPGroupsProcessor extends FileGroupsProcessor {

    private static final Logger.LoggerImpl logger = Logger.getLogger();

    
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }


    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        // create unique list of filesystem-based and url-based files


        Set<String> zipFiles = new HashSet<String>();
        boolean isSelectAll = request.isSelectAll();
        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
        long fgSize = 0;

        // values = folder or flat
        String zipType = request.getParam("zipType");
        boolean doFolders =  zipType != null && zipType.equalsIgnoreCase("folder");

        String type = request.getSafeParam("type");
        String ssoflag = request.getSafeParam("ssoflag");
        String Size = request.getSafeParam("radius");
        String optBand = request.getSafeParam("planckfreq");
        String detector = request.getSearchRequest().getParam("detector");
        String Type = "";
        String timeStr= "";
        String toiurl = "";

        if (!StringUtils.isEmpty(type)) {
            if (type.equals("circle")) {
                Type="CIRCLE";}
            else if (type.equals("box")) {
                Type="BOX";}
            else if (type.equals("polygon")) {
                Type="POLYGON";}
        }

        String dlTOImaps = request.getParam("TOImaps");
        String dlTOI = request.getParam("dlTOI");
        List<String> types = new ArrayList<String>();
        if (dlTOI.equals("yes")) {
            types.add("dlTOI");
        }
        if (dlTOImaps != null && dlTOImaps.length() > 0 && !dlTOImaps.equalsIgnoreCase("_none_")) {
            String[] mapArr = dlTOImaps.split(",");
            for (String map : mapArr) {
                if (map.equalsIgnoreCase("M")) {
                    types.add("dlMinimap");
                } else if (map.equalsIgnoreCase("H")) {
                    types.add("dlHires");
                }
            }
        }

        WorldPt pt;
        String pos = null;
        String gpos = null;

        String userTargetWorldPt = request.getParam(ReqConst.USER_TARGET_WORLD_PT);
        if (userTargetWorldPt != null) {
            pt = WorldPt.parse(userTargetWorldPt);
            if (pt != null) {
                pt = VisUtil.convertToJ2000(pt);
                pos = pt.getLon() + "," + pt.getLat();
                pt = VisUtil.convert(pt, CoordinateSys.GALACTIC);
                gpos = "G"+ String.format("%.2f",(Double)pt.getLon()) + "+" + String.format("%.2f",(Double)pt.getLat());
            }
        } else {
            throw new DataAccessException("No Name or Position found");
        }

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "rmjd");

        String baseUrl = PlanckTOITAPFileRetrieve.getBaseURL(request);
        String detectors[] = request.getSearchRequest().getParam(detector).split(",");
        String detc_constr;

        if (detectors[0].equals("_all_")){
            detc_constr ="";
        } else {
            detc_constr = "(detector='"+detectors[0]+"'";
            for(int j = 1; j < detectors.length; j++){
                detc_constr += "+or+detector='"+detectors[j]+"'";
            }
            detc_constr += ")+and+";
        }

        String sso_constr = "";
        if (ssoflag.equals("false")){
            sso_constr = "(sso='0')";
        }

        logger.briefInfo("detector constr=" +detc_constr);
        logger.briefInfo("sso constr=" +sso_constr);
        String baseFilename = "planck_toi_search_"+ gpos + "_" + optBand+"GHz";

        if (isSelectAll){
            timeStr="";
            toiurl = PlanckTOITAPFileRetrieve.createTOITAPURLString(baseUrl, pos, Type, Size, optBand, detc_constr, sso_constr, timeStr);
        }
        else{
            String rmjdSelt ="";
            for (int rowIdx : selectedRows) {
                rmjdSelt += String.format("%.0f",(Double)dgData.get(rowIdx, "rmjd")) +",";
            }

            String rmjdStrArr[] = rmjdSelt.split(",");
            timeStr = "+and+(";
            for (int j = 0; j < rmjdStrArr.length; j++) {
                double t1, t2;
                double t = Double.parseDouble(rmjdStrArr[j]);
                t1 = t - 0.5;
                t2 = t + 0.5;
                if (j != rmjdStrArr.length - 1) {
                    timeStr += "(mjd>=" + Double.toString(t1) + "+and+mjd<=" + Double.toString(t2) + ")+or+";
                } else {
                    timeStr += "(mjd>=" + Double.toString(t1) + "+and+mjd<=" + Double.toString(t2) + ")";

                }
            }
            timeStr += ")";

            toiurl = PlanckTOITAPFileRetrieve.createTOITAPURLString(baseUrl, pos, Type, Size, optBand, detc_constr, sso_constr, timeStr);
        }

        FileInfo fi = null;
        //String fName = String.format("%d",rowIdx);
        int estSize = 30000;
        File fileName = new File(baseFilename);

        logger.briefInfo("filename=" +fileName);

        //String extName = fName;

        // strip out filename when using file resolver
        logger.briefInfo("toi search url=" +toiurl);

        // strip out filename when using file resolver
//        if (doFolders) {
//            int idx = extName.lastIndexOf("/");
//            idx = idx < 0 ? 0 : idx;
//            extName = extName.substring(0, idx) + "/";
//        } else {
//            extName = null;
//        }

        String TOIFile = baseFilename + ".fits";

        fi = new FileInfo(toiurl, TOIFile, estSize);

        if (fi != null) {

            fiArr.add(fi);
            fgSize += fi.getSizeInBytes();
        }

        FileGroup fg = new FileGroup(fiArr, null, fgSize, "PlanckTOI Download Files");
        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        fgArr.add(fg);
        return fgArr;


    }

}


/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313)
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
