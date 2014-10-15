package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ReqConst;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.HealpixWrapper;
import edu.caltech.ipac.firefly.server.util.ImageGridSupport;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.hydra.data.PlanckCutoutRequest;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: tlau
 * Date: Jun 27, 2012
 * Time: 3:03:12 PM
 * To change this template use File | Settings | File Templates.
 */
@SearchProcessorImpl(id= "PlanckCutoutsDownload")
public class PlanckCutoutsFileGroupProcessor extends FileGroupsProcessor {
    private static final String PLANCK_FILE_PROP= "planck.filesystem_basepath";
    private static final String PLANCK_IRSA_DATA_BASE_DIR = AppProperties.getProperty(PLANCK_FILE_PROP);
    private static final String PLANCK_PSF_PROP= "planck.psf_basepath";
    private static final String PLANCK_PSF_DATA_BASE_DIR = AppProperties.getProperty(PLANCK_PSF_PROP);

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
        List<FileGroup> retval;
        ServerRequest searchR= request.getSearchRequest();
        SearchManager man= new SearchManager();
        SearchProcessor processor= man.getProcessor(searchR.getRequestId());

        DataGroupPart primaryData= (DataGroupPart)processor.getData(searchR);
        TableMeta meta= QueryUtil.getRawDataSet(primaryData).getMeta();
        processor.prepareTableMeta(meta, Collections.unmodifiableList(primaryData.getTableDef().getCols()), searchR);
        DataGroup dataGroup= primaryData.getData();

        List<FileInfo> retList;
        retList= retrieveFitsFiles(dataGroup, request);


        retval= new ArrayList<FileGroup>(1);
        retval.add(new FileGroup(retList,null,0,"PlanckImageCutouts"));

        WorldPt pt = WorldPt.parse(request.getSearchRequest().getParam(ReqConst.USER_TARGET_WORLD_PT));
        String suffix = "";
        if (pt.getCoordSys().equals(CoordinateSys.GALACTIC))
            suffix = "G";
        else if (pt.getCoordSys().equals(CoordinateSys.EQ_J2000))
            suffix = "C";
        suffix += FileGroupProcessorUtils.createRaDecString(pt, 3);
        request.setBaseFileName("PLCK_" + suffix);
        return retval;
    }

    private List<FileInfo> retrieveFitsFiles(DataGroup dataGroup, DownloadRequest request) {
        List<FileInfo> retList= new ArrayList<FileInfo>();
        WebPlotRequest wpReq;
        FileRetriever retrieve;
        FileInfo fi;
        String wpReqStr, filename, target;
        DataObject dObj;
        boolean downloadLFI = false;
        boolean downloadHFI = false;
        WorldPt wpt = null;
        String userDesc = null;
        File f;
        String releaseVersion = request.getParam(PlanckCutoutRequest.RELEASE_VERSION);
        for (int i: request.getSelectedRows()) {
            dObj = dataGroup.get(i);
            wpReqStr= (String) dObj.getDataElement(ImageGridSupport.COLUMN.THUMBNAIL.toString());
            wpReq= WebPlotRequest.parse(wpReqStr);
            retrieve= FileRetrieverFactory.getRetriever(wpReq);

            if (retrieve!=null) {
                try {
                    FileData fileData = retrieve.getFile(wpReq);
                    f= fileData.getFile();
                    if (f==null) continue;

                    wpt = WorldPt.parse(request.getSearchRequest().getParam("UserTargetWorldPt"));
                    target = FileGroupProcessorUtils.createRaDecString(wpt, 3);
                    if (wpt.getCoordSys().equals(CoordinateSys.GALACTIC)) {
                        target = "G"+target;
                    } else {
                        target = "C"+target;
                    }
                    String group= dObj.getDataElement(ImageGridSupport.COLUMN.GROUP.toString()).toString();
                    String band= dObj.getDataElement(ImageGridSupport.COLUMN.DESC.toString()).toString().
                            replaceAll(" ","").replaceAll("IRAS","");
                    group=group.replaceAll("\\-","").replaceAll("PLANCK","PLCK");
                    filename = group+"_"+target+"_"+band+".fits";
                    fi= new FileInfo(f.getPath(), filename, f.length());
                    retList.add(fi);
                    userDesc = wpReq.getParam("UserDesc");
                    if (userDesc!=null && userDesc.contains("PLANCK")) {
                        if (userDesc.contains("30") || userDesc.contains("44") ||  userDesc.contains("70")) {
                            if (downloadLFI==false) downloadLFI = true;
                        } else if (userDesc.contains("100") || userDesc.contains("143") || userDesc.contains("217") ||
                                userDesc.contains("353") || userDesc.contains("545") || userDesc.contains("857")) {
                            if (downloadHFI==false) downloadHFI = true;
                        }
                    }
                    if (releaseVersion.equals("release2")){
                        downloadHFI = false;
                        downloadLFI = false;
                    }
                } catch (Exception e) {
                    logger.warn(e,"Could not retrieve file for WebPlotRequest: " + wpReqStr);
                }
            } else {
                logger.warn("Could not a file retriever for WebPlotRequest: " + wpReqStr);
            }
        }

        //Add PSF files
        try {
            if (wpt!=null) {
                long healpix = -1;
                String fname;
                File CutoutDir = new File(PLANCK_PSF_DATA_BASE_DIR + request.getParam("PSF_subPath") + "/");
                WorldPt wpg= VisUtil.convert(wpt, CoordinateSys.GALACTIC);
                if (downloadLFI) {
                    healpix = HealpixWrapper.getHealPixelForPlanckImageCutout(
                            wpg.getLon(), wpg.getLat(), HealpixWrapper.FileType.LFI);
                    fname = getPSFPath(true, healpix);
                    f = new File(PLANCK_PSF_DATA_BASE_DIR + request.getParam("PSF_subPath") + "/"+fname);
                    fi= new FileInfo(f.getPath(), f.getName(), f.length());
                    retList.add(fi);
                }
                if (downloadHFI) {
                    healpix = HealpixWrapper.getHealPixelForPlanckImageCutout(
                            wpg.getLon(), wpg.getLat(), HealpixWrapper.FileType.HFI);
                    fname = getPSFPath(false, healpix);
                    f = new File(PLANCK_PSF_DATA_BASE_DIR + request.getParam("PSF_subPath") + "/"+fname);
                    fi= new FileInfo(f.getPath(), f.getName(), f.length());
                    retList.add(fi);
                }
            }
        } catch (Exception ex) {

        }
        return retList;
    }

    private static String getPSFPath(boolean isLFI, long healpix) {
        String filename = "PSF_";
        String base = "";
        String idx = "";
        if (healpix>0) {
            idx = "00000000"+Long.toString(healpix);
            idx = idx.substring(idx.length()-8);
            if (isLFI) {
                filename = "/LFI/"+idx.substring(0, 2)+"/"+idx.substring(2, 5)+"/PSF_LFI_"+idx+".fits.gz";
            } else {
                filename = "/HFI/"+idx.substring(0, 2)+"/"+idx.substring(2, 5)+"/PSF_HFI_"+idx+".fits.gz";
            }

        }
        return filename;
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
