package edu.caltech.ipac.firefly.server.packagedata.obscorepackager;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.MappedData;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.getSelectedData;
import static edu.caltech.ipac.firefly.server.packagedata.obscorepackager.DatalinkUtil.*;
import static edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCoreUtil.*;

@SearchProcessorImpl(id = "ObsCorePackager")
public class ObsCorePackager extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();
    public static final List<String> acceptableExtensionTypes = Arrays.asList("png","jpg","jpeg","bmp","fits","tsv",
            "csv","tbl","json","pdf","tar","html","xml","vot","reg");
    public static final String PRODUCTS = "productTypes";
    public static final String ACCESS_URL = "access_url";
    public static final String SERVICE_DEF = "service_def";
    public static final String ACCESS_FORMAT = "access_format";
    public static final String SEMANTICS = "semantics";
    public static final String CONTENT_TYPE = "content_type";
    public static final String FILE = "file";
    public static final String SOURCE = "source";
    public static final String DATALINK_SER_DEF = "datalinkServiceDescriptor";
    public static final String ADHOC_SERVICE = "adhoc:service";
    public static final String DATALINK = "datalink";
    public static final String CENTER_COL_NAMES = "centerColNames";
    public static final String CENTER_COL_VALS = "centerColValues";
    public static final String LON_COL = "lonCol";
    public static final String LAT_COL = "latCol";
    public static final String RA = "ra";
    public static final String DEC = "dec";
    public static final String CUTOUT_VALUE = "cutoutValue";
    public static final String SER_DEF_ACCESS_URL = "accessURL";
    public static final String SER_DEF_INPUT_PARAMS = "inputParams";
    public static final String CIRCLE = "circle";
    public static final String REF = "ref";
    public static final String VALUE = "value";
    public static final String TEMPLATE_COL_NAMES = "templateColNames";
    public static final String USE_SOURCE_FILE_NAME = "useSourceUrlFileName";
    public static final String POSITION = "position";
    public static final String GENERATE_DOWNLOAD_FILE_NAME = "generateDownloadFileName";

    public static final List<String> CUTOUT_UCDs= Arrays.asList("phys.size","phys.size.radius","phys.angSize", "pos.spherical.r");
    public static final List<String>  RA_UCDs= List.of("pos.eq.ra");
    public static final List<String>  DEC_UCDs= List.of("pos.eq.dec");

    public static final List<String> DATALINK_COL_NAMES = List.of(
            "id", "access_url", "service_def", "error_message", "semantics",
            "description", "content_type", "content_length"
    );

    @Override
    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws DataAccessException {
        List<FileInfo> fileInfos = new ArrayList<>();
        try {
            var selectedRows = new ArrayList<>(request.getSelectedRows());
            MappedData dgDataUrl = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(), selectedRows); //returns all columns
            DataGroup dg = getSelectedData(request.getSearchRequest(), selectedRows);

            String pos = request.getParam(POSITION);
            Map<String, Integer> prefixCounter = new HashMap<>();

            //todo: remove datalinkServDesc from here, moved it down where it's in scope
            //String datalinkServDesc = request.getParam(DATALINK_SER_DEF); //check for a non-obscore table containing a Datalink Service Descriptor

            //todo: remove this: DataType[] dataDefs = dg.getDataDefinitions();
            boolean isLinksTable = isLinksTable(dg); //todo: remove this: isLinksTable(dataDefs);

            if (isLinksTable) { //to check if the incoming request is directly from a datalink table (in cases where user may extract a table from an existing obscore type table)
                List<String> positionColVals = getPositionColVals(pos, -1, null); //don't need index or dgDataUrl, pos should have ra/dec vals
                List<FileInfo> tmpFileInfos = resolveDatalink(null, request, "", positionColVals, dg);
                fileInfos.addAll(tmpFileInfos);
            }
            else {
                for (int idx : selectedRows) {
                    String accessUrl = (String) dgDataUrl.get(idx, ACCESS_URL);
                    //check for a non-obscore table containing a Datalink Service Descriptor
                    String datalinkServDesc = request.getParam(DATALINK_SER_DEF); //note: datalinkServDesc is a JSON string

                    if (accessUrl == null) {
                        //check if this could be a non-obscore table containing a datalink url
                        if (datalinkServDesc != null) {
                            //construct product url / access url here
                            DatalinkUtil.ServDescUrl serDescUrl = createUrlFromServDesc(datalinkServDesc);
                            accessUrl = getAccessUrlFromNonObsCore(serDescUrl, dg, dgDataUrl, idx);
                        } else continue; //no file available to process
                    }

                    String accessFormat = (String) dgDataUrl.get(idx, ACCESS_FORMAT);
                    URL url = new URI(accessUrl).toURL();

                    List<String> positionColVals = getPositionColVals(pos, idx, dgDataUrl);
                    String colNames = request.getSearchRequest().getParam(TEMPLATE_COL_NAMES);
                    String[] cols = colNames != null ? colNames.split(",") : null;
                    String fileNamePrefix = getFileNamePrefix(idx, cols, dgDataUrl, url, positionColVals); //this will be used as folder name, and as prefix for individual file names
                    fileNamePrefix = makeUniquePrefix(fileNamePrefix, prefixCounter);

                    boolean isTblContainsDatalink = (datalinkServDesc != null) || (accessFormat != null && accessFormat.contains(DATALINK));

                    if (isTblContainsDatalink) {
                        List<FileInfo> tmpFileInfos = resolveDatalink(url, request, fileNamePrefix, positionColVals, null);
                        fileInfos.addAll(tmpFileInfos);
                    } else {
                        String extName = null;
                        FileInfo fileInfo = new FileInfo(accessUrl, extName, 0);
                        fileInfos.add(fileInfo);
                    }
                }
            }
            fileInfos.forEach(fileInfo -> fileInfo.setRequestInfo(new HttpServiceInput(fileInfo.getInternalFilename())));
        }
        catch(Exception e) {
            LOGGER.error(e);
            throw new DataAccessException("Error while processing ObsCorePackager request: " + e.getMessage(), e);
        }
        return List.of(new FileGroup(fileInfos, null, 0, "ObsCore Download Files"));
    }
}
