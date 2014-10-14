package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.HealpixWrapper;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.firefly.server.visualize.FileData;
import edu.caltech.ipac.firefly.server.visualize.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.FileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.hydra.data.PlanckCutoutRequest;
import edu.caltech.ipac.hydra.server.query.QueryPlanckImagesCutout;
import edu.caltech.ipac.target.Fixed;
import edu.caltech.ipac.target.PositionJ2000;
import edu.caltech.ipac.target.Target;
import edu.caltech.ipac.target.TargetFixedSingle;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: wmi
 * Date: 1/23/13
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */

@SearchProcessorImpl(id = "PlancImagesCutoutkDownload")
public class PlanckImagesCutoutFileGroupsProcessor extends FileGroupsProcessor {

//    public static final String PLANCK_FILESYSTEM_BASEPATH = AppProperties.getProperty("planck.filesystem_basepath");
//    private static final String PLANCK_FILE_PROP= "planck.filesystem_basepath";
//    private static final String PLANCK_IRSA_DATA_BASE_DIR = AppProperties.getProperty(PLANCK_FILE_PROP);
//    private static final String PLANCK_PSF_PROP= "planck.psf_basepath";
//    private static final String PLANCK_PSF_DATA_BASE_DIR = AppProperties.getProperty(PLANCK_PSF_PROP);
//    private static final String BASE_DIR= AppProperties.getProperty(PLANCK_FILE_PROP) + "/cutouts/pccs1_cutouts/";
    private static final int planckBands[] = {30,44,70,100,143,217,353,545,857};
    private static final String wmapBands[] = {"K","Ka","Q","V","W"};
    private static final int irasBands[] = {100,60,25,12};
    private static final int width = 96;
    private Target curTarget = null;


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
        Collection<Integer> selectedRows = request.getSelectedRows();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());
        String baseUrl = request.getSearchRequest().getParam(PlanckCutoutRequest.CUTOUT_HOST);
        String releaseVersion = request.getSearchRequest().getParam(PlanckCutoutRequest.RELEASE_VERSION);
        String mType = "I";
        String groupName, url, planckurl, sizeStr, pixsize;
        String subsizeStr = "2.0";
        Float subsize = new Float(subsizeStr);
        WebPlotRequest wpReq;
        WorldPt pt = null;

        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        long fgSize = 0;

//        String basePath = BASE_DIR;

        // get selected cutoutTypes (PE, WMAP, IRIS)
        String[] cutoutTypes = null;
        String cutoutTypesStr = request.getParam("cutoutTypes");
        if (cutoutTypesStr != null && cutoutTypesStr.length() > 0 && !cutoutTypesStr.equalsIgnoreCase("_none_")) {
            cutoutTypes = cutoutTypesStr.split(",");
        }

        // add catalog table
        String tablePath = dgp.getTableDef().getSource();
        File file = new File(tablePath);
        if (file != null && file.exists()) {
            ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
            FileInfo fi = new FileInfo(file.getName(), file.getName(), file.length());
            fiArr.add(fi);
            fgSize += file.length();

            FileGroup fg = new FileGroup(fiArr, file.getParentFile(), fgSize, "PLANCK Download Files");
            fgArr.add(fg);
        }

        if (cutoutTypes != null && cutoutTypes.length > 0) {
            // use selected row "name" field to determine subdirectory path to cutout files
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, "name", "glon", "glat", "ra","dec"); //"name", "glon", "glat"

            Map<String, String> cookies = ServerContext.getRequestOwner().getIdentityCookies();

            long healpix = -1;
            String baseFilename, sname, sDir1, sDir2;

            FileInfo fi = null;
            File f;
            double glon, glat,ra,dec;
            String Ra, Dec, pos;

            for (int rowIdx : selectedRows) {
                ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
                sname = (String) dgData.get(rowIdx, "name");
                String outDir = sname.replace(' ', '_');

                for (int i = 0; i < cutoutTypes.length; i++) {
                    final String cutoutType = cutoutTypes[i];

                    ra = (Double)dgData.get(rowIdx, "ra");
                    dec = (Double)dgData.get(rowIdx, "dec");
                    Ra = Double.toString(ra);
                    Dec = Double.toString(dec);

                    pos = Ra + "," + Dec;
                    pt = new WorldPt(ra,dec, CoordinateSys.EQ_J2000);

                    url = createCutoutURLString(baseUrl, pos, releaseVersion, mType);


                    if (cutoutType.equals("PE")) {

                        for(int j = 0; j < planckBands.length; j++){
                            if (planckBands[j]==30 || planckBands[j]==44 || planckBands[j]==70){
                                pixsize ="2.0";
                                sizeStr = "60";
                                baseFilename = "LFI_" + planckBands[j] + "_" + sname;
                            } else {
                                pixsize = "1.0";
                                sizeStr = "120";
                                baseFilename = "HFI_" + planckBands[j] + "_" + sname;
                            }

                            planckurl= url+"&pixsize="+pixsize+"&size="+sizeStr+"&mission=planck&planckfreq="+planckBands[j]+"&wmapfreq=&submit=";
                            String PlanckFile = outDir + "/" + baseFilename + ".fits";
                            int estSize = 60000;
                            fi = new FileInfo(planckurl, PlanckFile, estSize);
                            if (fi != null) {
                                fi.setCookies(cookies);
                                fiArr.add(fi);
                                fgSize += fi.getSizeInBytes();
                            }
                        }
                    }

                    if(cutoutType.equals("WMAP")) {

                        for (String band: wmapBands) {
                            pixsize ="2.0";
                            sizeStr = "60";
                            baseFilename = "WMAP_" + band + "_" + sname;
                            String wmapurl = url + "&pixsize=" + pixsize + "&size=" + sizeStr + "&mission=wmap" + "&wmapfreq=" +
                                            band + "&planckfreq=&submit=";
                            String WMAPFile = outDir + "/" +baseFilename + ".fits";
                            int estSize = 17000;
                            fi = new FileInfo(wmapurl, WMAPFile, estSize);
                            fiArr.add(fi);
                            fgSize += fi.getSizeInBytes();
                        }

                    }

                    if (cutoutType.equals("IRIS")) {
                        try {
                            for (int band: irasBands) {
                                baseFilename = outDir + "/" + "IRIS_" + band + "_" + outDir + ".fits";
                                int estSize = 60000;

                                wpReq = WebPlotRequest.makeISSARequest(pt,Integer.toString(band),subsize);
                                FileRetriever retriever = FileRetrieverFactory.getRetriever(wpReq);
                                if (retriever!=null) {
                                     FileData fileData = retriever.getFile(wpReq);
                                     f = fileData.getFile();
                                    fi = new FileInfo(f.getPath(), baseFilename, estSize);
                                    fiArr.add(fi);
                                    fgSize += fi.getSizeInBytes();
                                }
                            }

                        } catch (Exception e) {
                            throw new IOException(e);
                        }
                    }
                }

                FileGroup fg = new FileGroup(fiArr, null, fgSize, "PLANCK Download Files");
                fgArr.add(fg);

            }
        }
        return fgArr;
    }


    private static String getPSFPath(boolean isLFI, long healpix) {
        String filename = "PSF_";
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

    public static String createCutoutURLString(String baseUrl,String pos, String version, String mtypes) {
        //String url = baseUrl + "?locstr=" + pos + "&pixsize=" + pixsize + "&version=" + version + "&hmap=" + mtypes;
        String url = baseUrl + "?locstr=" + pos + "&version=" + version + "&hmap=" + mtypes;
        return url;
    }

    private static WorldPt getTargetWorldPt(Target t) throws IOException {
        WorldPt pt = null;
        if (!t.isFixed()) {
            throw new IOException("Table upload cannot support moving targets.");
        }
        Fixed ft = (Fixed) t;

        pt = new WorldPt(ft.getPosition().getRa(), ft.getPosition().getDec());

        return pt ;
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
