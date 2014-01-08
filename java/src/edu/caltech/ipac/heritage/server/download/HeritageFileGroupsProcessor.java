package edu.caltech.ipac.heritage.server.download;


import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.WaveLength;
import edu.caltech.ipac.heritage.data.entity.download.HeritageDownloadRequest;
import edu.caltech.ipac.heritage.data.entity.download.PackageRequest;
import edu.caltech.ipac.heritage.server.persistence.FileInfoDao;
import edu.caltech.ipac.util.AppProperties;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * User: roby
 * Date: Sep 24, 2008
 * Time: 1:17:59 PM
 */

/**
 * @author Trey Roby
 */
@SearchProcessorImpl(id ="heritageDownload")
public class HeritageFileGroupsProcessor extends FileGroupsProcessor {

   public static final File BASE_DIR = new File(AppProperties.getProperty("download.filesystem.base"));
   public static final String SM_BASE_PREFIX = AppProperties.getProperty("download.sm.base");


   public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert(request instanceof HeritageDownloadRequest);
        try {
            return  computeFileGroup((HeritageDownloadRequest)request);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(HeritageDownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        Collection<Integer> selectedRows= request.getSelectedRows();
        Collection<WaveLength> waveLengths= request.getWaveLengths();
        Collection<DataType> dataTypes= request.getDataTypes();
        DataGroupPart dgp = new SearchManager().getDataGroup(request.getSearchRequest());

        PackageRequest req;
        String searchRequestId = request.getSearchRequest().getRequestId();


        long maxBundle= PackageMaster.getMaxBundleSize(request);

        if (searchRequestId.startsWith("aor")) {
            ArrayList<PackageRequest.AorPackageUnit> aplist = new ArrayList<PackageRequest.AorPackageUnit>();
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                                                                        selectedRows, "reqkey", "reqmode");

            for(int rowIdx : selectedRows) {
                int reqkey = QueryUtil.getInt(dgData.get(rowIdx, "reqkey"));
                if (waveLengths != null) {
                    String reqmode = String.valueOf(dgData.get(rowIdx, "reqmode")).toUpperCase();
                    for(WaveLength w : waveLengths) {
                        if (w.isValidMode(reqmode)) {
                            aplist.add(new PackageRequest.AorPackageUnit(reqkey, w.chanNum));
                        }
                    }

                } else {
                    aplist.add(new PackageRequest.AorPackageUnit(reqkey));
                }
            }

            req = new PackageRequest.AOR(aplist.toArray(new PackageRequest.AorPackageUnit[aplist.size()]),
                                         dataTypes.toArray(new DataType[dataTypes.size()]),
                                         request.getBaseFileName(), request.getTitle(), request.getEmail(),
                                         maxBundle);
        } else if (searchRequestId.startsWith("bcd") || searchRequestId.startsWith("MOS")) {
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                                                                        selectedRows, "bcdid");
            int[] ids = new int[selectedRows.size()];
            int i = 0;
            for(int rowIdx : selectedRows) {
                ids[i++] = QueryUtil.getInt(dgData.get(rowIdx, "bcdid"));
            }

            req = new PackageRequest.BCD(ids, request.getDataTypes().toArray(new DataType[request.getDataTypes().size()]),
                                         request.getBaseFileName(), request.getTitle(), request.getEmail(),
                                         maxBundle);

        } else if (searchRequestId.startsWith("pbcd")) {
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                                                                        selectedRows, "pbcdid");
            int[] ids = new int[selectedRows.size()];
            int i = 0;
            for(int rowIdx : selectedRows) {
                ids[i++] = QueryUtil.getInt(dgData.get(rowIdx, "pbcdid"));
            }

            req = new PackageRequest.PBCD(ids, dataTypes.toArray(new DataType[dataTypes.size()]),
                                          request.getBaseFileName(), request.getTitle(), request.getEmail(),
                                          maxBundle );
        } else if (searchRequestId.startsWith("sm")) {
            boolean includeRelated = request.getDataTypes().contains(DataType.SM_ANCIL);
            // TODO: SM when db is ready, get info from db
            IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                                                                        selectedRows, "fname", "size");

            List<FileGroup> fgs = new ArrayList<FileGroup>(1);
            Set<FileInfo> fi= new LinkedHashSet<FileInfo>();
            String fname, urlPath, basename;
            int fsize, totalSize = 0, separatorIdx;
            for(int rowIdx : selectedRows) {
                fname = dgData.get(rowIdx, "fname").toString();
                // CR9673
                separatorIdx = fname.lastIndexOf(File.separator);
                if (separatorIdx >= 0 && separatorIdx < fname.length()-1) {
                    basename = fname.substring(separatorIdx+1);
                } else {
                    basename = fname;
                }
                fsize = QueryUtil.getInt(dgData.get(rowIdx, "size"));
                urlPath = SM_BASE_PREFIX+File.separator+fname;
                fi.add(new FileInfo(urlPath, basename, fsize));
                totalSize += fsize;

                if (includeRelated) {
                    String r_basename, r_urlPath;
                    String [] r_suffixes;
                    String primaryEnding;
                    if (basename.endsWith("median_mosaic.fits")) {
                        // This case should disappear - median mosaic will be an ancillary to mosaic
                        primaryEnding = "median_mosaic.fits";
                        r_suffixes = new String[]{"median_mosaic_unc"};

                    } else if (basename.endsWith("mosaic.fits")) {
                        primaryEnding = "mosaic.fits";
                        r_suffixes = new String[]{"unc", "cov", "std", "median_mosaic", "median_mosaic_unc"};
                    } else {
                        primaryEnding = "";
                        r_suffixes = new String[]{};
                    }
                    if (primaryEnding.length() > 0) {
                        for (String suffix : r_suffixes)  {
                            r_basename = basename.replace(primaryEnding, suffix+".fits");
                            r_urlPath = urlPath.replace(primaryEnding, suffix+".fits");
                            fi.add(new FileInfo(r_urlPath, r_basename, fsize));
                            totalSize += fsize;
                        }
                    }
                }
            }
            FileGroup fg = new FileGroup(fi, null, totalSize, "Super Mosaic products");
            fgs.add(fg);
            return fgs;
        } else {
            throw new IllegalArgumentException("Download Manager : unsupported type "+ request.getSearchRequest().getRequestId());
        }
        return FileInfoDao.computeFileGroup(req, BASE_DIR);

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

