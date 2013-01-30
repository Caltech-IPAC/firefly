package edu.caltech.ipac.hydra.server.download;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.HealpixIndex;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wmi
 * Date: 1/23/13
 * Time: 3:13 PM
 * To change this template use File | Settings | File Templates.
 */

@SearchProcessorImpl(id = "PlanckDownload2")
public class PlanckFileGroupsProcessor2 extends FileGroupsProcessor {

//    public static final String PLANCK_FILESYSTEM_BASEPATH = AppProperties.getProperty("planck.filesystem_basepath");
    private static final String PLANCK_FILE_PROP= "planck.filesystem_basepath";
    private static final String PLANCK_IRSA_DATA_BASE_DIR = AppProperties.getProperty(PLANCK_FILE_PROP);

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

        ArrayList<FileGroup> fgArr = new ArrayList<FileGroup>();
        long fgSize = 0;

//        String basePath = PLANCK_FILESYSTEM_BASEPATH;
        String basePath = "***REMOVED***irsa-data-planck-dev/data/2012_planck/";
//        if (!basePath.endsWith("/")) {
//            basePath += "/";
//        }

        basePath += "test-cutouts-20121218/";

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
                selectedRows, "name1", "glon", "glat"); //"name", "glon", "glat"
            long healpix = -1;
            String fname;
            FileInfo fi = null;
            File f;
            WorldPt wpt= null;
            double lon, lat;
            for (int rowIdx : selectedRows) {
                ArrayList<FileInfo> fiArr = new ArrayList<FileInfo>();
                String sname = (String) dgData.get(rowIdx, "name1");

                int dirStartIdx = sname.indexOf(" ") + 1;
                String sDir1 = sname.substring(dirStartIdx, dirStartIdx + 4);
                String sDir2 = sname.replace(" ","_");
                File cutoutDir = new File(basePath + sDir1 + "/" + sDir2 + "/");
                String outDir = sname.replace(' ', '_');

                for (int i = 0; i < cutoutTypes.length; i++) {
                    final String cutoutType = cutoutTypes[i];

                    // get selected cutout files based on filename prefix (PE, WMAP, IRIS, ...)
                    File[] files = cutoutDir.listFiles(new FilenameFilter() {
                        public boolean accept(File cutoutDir, String sname) {
                            if (cutoutType.equals("PE")){
                                return sname.startsWith("LFI") || sname.startsWith("HFI");
                            }
                            else {
                                return sname.startsWith(cutoutType);
                            }
                        }
                    });

                    if (files != null) {
                        for (int j = 0; j < files.length; j++) {
                            if (files[i] != null && files[i].exists()) {
                                fi = new FileInfo(files[j].getPath(), outDir + "/" + files[j].getName(), files[j].length());
                                fiArr.add(fi);
                                fgSize += files[j].length();
                            }
                        }
                    }

                    if (cutoutType.equals("PE")) {
                        try {
                            // Note: In "planck.xml", remember to add param key-value pair "PSF_subPath"
                            // inside download block
                            //
                            // e.g. <Param key="PSF_subPath" value="/2012_planck/beams/121218"/>

                            lon = (Double)dgData.get(rowIdx, "glon");
                            lat = (Double)dgData.get(rowIdx, "glat");
                            wpt = new WorldPt(lon, lat);

                            healpix = HealpixIndex.getHealPixelForPlanckImageCutout(
                                    wpt.getLon(), wpt.getLat(), HealpixIndex.FileType.LFI);
                            fname = getPSFPath(true, healpix);
                            f = new File(PLANCK_IRSA_DATA_BASE_DIR + request.getParam("PSF_subPath") + "/"+fname);
                            if (f.exists()) {
                                fi= new FileInfo(f.getPath(), outDir + "/" + f.getName(), f.length());
                                fiArr.add(fi);
                                fgSize += f.length();
                            }

                            healpix = HealpixIndex.getHealPixelForPlanckImageCutout(
                                        wpt.getLon(), wpt.getLat(), HealpixIndex.FileType.HFI);
                            fname = getPSFPath(false, healpix);
                            f = new File(PLANCK_IRSA_DATA_BASE_DIR + request.getParam("PSF_subPath") + "/"+fname);
                            if (f.exists()) {
                                fi= new FileInfo(f.getPath(), outDir + "/" + f.getName(), f.length());
                                fiArr.add(fi);
                                fgSize += f.length();
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
