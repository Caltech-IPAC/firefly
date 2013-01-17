package edu.caltech.ipac.heritage.server.download;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.packagedata.PackageMaster;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupPart;
import edu.caltech.ipac.firefly.server.util.ipactable.IpacTableParser;
import edu.caltech.ipac.heritage.data.entity.DataType;
import edu.caltech.ipac.heritage.data.entity.download.PackageRequest;
import edu.caltech.ipac.heritage.server.persistence.FileInfoDao;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author tatianag
 *         $Id: IrsEnhancedFileGroupsProcessor.java,v 1.3 2011/10/24 19:17:59 tatianag Exp $
 */
@SearchProcessorImpl(id ="irsEnhancedDownload")
public class IrsEnhancedFileGroupsProcessor  extends FileGroupsProcessor {

    private static final String FILE_COL = "heritagefilename";
    private static final String REQKEY_COL = "reqkey";

    @Override
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {

        assert(request instanceof DownloadRequest);
        DownloadRequest req = (DownloadRequest)request;
        Collection<Integer> selectedRows= req.getSelectedRows();
        TableServerRequest searchRequest = req.getSearchRequest();
        searchRequest.setPageSize(0);
        DataGroupPart dgp = new SearchManager().getDataGroup(searchRequest);

        IpacTableParser.MappedData dgData = IpacTableParser.getData(new File(dgp.getTableDef().getSource()),
                selectedRows, FILE_COL, REQKEY_COL);

        // additional data to be downloaded (based on reqkey)
        String datatypes = request.getParam("datatypes");
        ArrayList<DataType> dTypes = new ArrayList<DataType>();
        if (datatypes != null && datatypes.length() > 0) {
            String[] dtArray = datatypes.split(",");
            for (String dt : dtArray) {
                DataType dType = DataType.parse(dt);
                if (!dType.equals(DataType.IRS_ENHANCED)) {
                    dTypes.add(dType);
                }
            }
        }
        boolean moreDataRequested = dTypes.size() > 0;

        List<FileGroup> fgs = new ArrayList<FileGroup>(1);
        Set<FileInfo> fi= new LinkedHashSet<FileInfo>();
        String filename;
        File f;
        long fsize;
        long totalSize = 0;
        String externalName;
        HashSet<Integer> reqkeys = new HashSet<Integer>();
        for(int rowIdx : selectedRows) {
            filename = (String)dgData.get(rowIdx, FILE_COL);

            f = new File(filename);

            if (f.exists()) {
                fsize = f.length();
            } else {
                Logger.warn("File does not exist: "+f.getAbsolutePath());
                fsize = 10000;
            }
            totalSize += fsize;

            int reqkey = QueryUtil.getInt(dgData.get(rowIdx, REQKEY_COL));
            if (moreDataRequested) { reqkeys.add(reqkey); }
            externalName =  "r"+reqkey+File.separator+"enhanced"+File.separator+f.getName();

           fi.add(new FileInfo(f.getAbsolutePath(), externalName, fsize));
        }
        FileGroup fg = new FileGroup(fi, null, totalSize, "Selected Irs Enhanced Products");
        fgs.add(fg);

        if (moreDataRequested) {
            for (FileGroup afg : computeFileGroup(req, dTypes, reqkeys)) {
                fgs.add(afg);
            }
        }
        return fgs;
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request, List<DataType> dTypes, Set<Integer> reqkeys) {
        ArrayList<PackageRequest.AorPackageUnit> aplist = new ArrayList<PackageRequest.AorPackageUnit>();
        for (int reqkey : reqkeys) {
            aplist.add(new PackageRequest.AorPackageUnit(reqkey));
        }
        long maxBundle= PackageMaster.getMaxBundleSize(request);
        PackageRequest req = new PackageRequest.AOR(aplist.toArray(new PackageRequest.AorPackageUnit[aplist.size()]),
                                         dTypes.toArray(new DataType[dTypes.size()]),
                                         request.getBaseFileName(), request.getTitle(), request.getEmail(),
                                         maxBundle);
        return FileInfoDao.computeFileGroup(req, HeritageFileGroupsProcessor.BASE_DIR);
    }
}
