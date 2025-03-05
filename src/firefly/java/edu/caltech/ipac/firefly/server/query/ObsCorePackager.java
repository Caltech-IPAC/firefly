package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil.getSelectedData;
import static edu.caltech.ipac.util.FileUtil.getUniqueFileNameForGroup;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static edu.caltech.ipac.firefly.core.Util.Opt.ifNotNull;

@SearchProcessorImpl(id = "ObsCorePackager")
public class ObsCorePackager extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final List<String> acceptableExtensionTypes = Arrays.asList("png","jpg","jpeg","bmp","fits","tsv",
            "csv", "tbl", "fits", "json", "pdf", "tar", "html", "xml", "vot", "vot", "vot", "vot", "reg", "png", "xml");

    public static final String PRODUCTS = "productTypes";
    public static final String ACCESS_URL = "access_url";
    public static final String SERVICE_DEF = "service_def";
    public static final String ACCESS_FORMAT = "access_format";
    public static final String SEMANTICS = "semantics";
    public static final String CONTENT_TYPE = "content_type";

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

    private static final List<String> CUTOUT_UCDs= Arrays.asList("phys.size","phys.size.radius","phys.angSize", "pos.spherical.r");
    private static final List<String>  RA_UCDs= List.of("pos.eq.ra");
    private static final List<String>  DEC_UCDs= List.of("pos.eq.dec");

    record CenterCols(String lonCol, String latCol) {}

    record CenterColValues(String ra, String dec) {}

    public record Position(CenterCols centerColNames, CenterColValues centerColValues) {}

    record ServDescUrl(String partialUrl, List<MissingParam> missingParams) {
        public boolean isValid() {
            return !StringUtils.isEmpty(partialUrl);
        }
    }

    record MissingParam(String paramName, String refId) {}

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            LOGGER.error(e);
            throw e;
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) throws DataAccessException{
        var selectedRows = new ArrayList<>(request.getSelectedRows());

        List<FileInfo> fileInfos = new ArrayList<>();
        MappedData dgDataUrl = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(), selectedRows); //returns all columns

        DataGroup dg = getSelectedData(request.getSearchRequest(), selectedRows);
        String datalinkServDesc = request.getParam(DATALINK_SER_DEF); //check for a non obscore file containing a datalink service descriptor

        try {
            Map<String,Integer> dataLinkFolders = new HashMap<>();
            for (int idx : selectedRows) {

                String access_url = (String) dgDataUrl.get(idx, ACCESS_URL);

                if (access_url == null) {
                    //check if this could be a non-obscore table containing a datalink url
                    if (datalinkServDesc != null) {
                        //construct product url / access url here
                        ServDescUrl serDescUrl = createUrlFromServDesc(datalinkServDesc);

                        if (serDescUrl.isValid()) {
                            StringBuilder finalUrl = new StringBuilder(serDescUrl.partialUrl);

                            if (serDescUrl.missingParams.isEmpty()) {
                                access_url = serDescUrl.partialUrl;
                            }
                            else {
                                for (MissingParam missing : serDescUrl.missingParams) {
                                    String refId = missing.refId;
                                    String key = "";
                                    for (DataType data : dg.getDataDefinitions()) {
                                        if (data.getID().equalsIgnoreCase(refId)) {
                                            key = data.getKeyName();
                                            break;
                                        }
                                    }
                                    String resolvedValue = (String) dgDataUrl.get(idx, key);
                                    if (resolvedValue != null && !resolvedValue.isEmpty()) {
                                        if (serDescUrl.partialUrl.endsWith("?"))
                                            finalUrl.append(missing.paramName).append("=").append(resolvedValue);
                                        else
                                            finalUrl.append("&").append(missing.paramName).append("=").append(resolvedValue);
                                    }
                                }
                                access_url = finalUrl.toString(); //datalink url from service descriptor
                            }
                        }
                    }
                    else continue; //no file available to process
                }
                String access_format = (String) dgDataUrl.get(idx, ACCESS_FORMAT);
                URL url = new URL(access_url);

                String colNames = request.getSearchRequest().getParam(TEMPLATE_COL_NAMES);
                boolean useSourceUrlFileName= request.getSearchRequest().getBooleanParam(USE_SOURCE_FILE_NAME,false);
                String[] cols = colNames != null ? colNames.split(",") : null;
                String ext_file_name = getFileName(idx,cols,useSourceUrlFileName,dgDataUrl,url);

                if (access_format != null) {
                    if (access_format.contains(DATALINK)) {
                        ext_file_name = getUniqueFileNameForGroup(ext_file_name, dataLinkFolders);
                        List<FileInfo> tmpFileInfos = parseDatalink(url, request, dgDataUrl, idx, ext_file_name);
                        fileInfos.addAll(tmpFileInfos);
                    }
                    else { //non datalink entry -  such as fits,jpg etc.
                        if (!useSourceUrlFileName || FileUtil.getExtension(ext_file_name).isEmpty()) {
                            String extension = access_format.replaceAll(".*/", "");
                            ext_file_name += "." + extension;
                        }
                        FileInfo fileInfo = new FileInfo(access_url, ext_file_name, 0);
                        fileInfos.add(fileInfo);
                    }
                }
                else {
                    if (datalinkServDesc != null) { //parse the datalink url found in the non-obscore table
                        List<FileInfo> tmpFileInfos = parseDatalink(url, request, dgDataUrl, idx, ext_file_name);
                        fileInfos.addAll(tmpFileInfos);
                    }
                    else { //access_format & access_url are both null, so try and get it from the url's Content_Type
                        FileInfo fileInfo = new FileInfo(access_url, ext_file_name, 0);
                        fileInfos.add(fileInfo);
                    }
                }

            }
            fileInfos.forEach(fileInfo -> fileInfo.setRequestInfo(HttpServiceInput.createWithCredential(fileInfo.getInternalFilename())));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList(new FileGroup(fileInfos, null, 0, "ObsCore Download Files"));
    }

    public static List<FileInfo> parseDatalink(URL url, DownloadRequest request, MappedData dgDataUrl, int idx, String prepend_file_name) {
        List<FileInfo> fileInfos = new ArrayList<>();

        //products to download in datalink file
        String productTypes = request.getParam(PRODUCTS);
        String[] products = (productTypes != null && !productTypes.trim().isEmpty()) ? productTypes.split(",") : null;

        String cutoutValue = request.getParam(CUTOUT_VALUE);
        boolean useSourceUrlFileName= request.getSearchRequest().getBooleanParam(USE_SOURCE_FILE_NAME,false);

        try {
            DataGroup[] groups = VoTableReader.voToDataGroups(url.toString(), false);

            String pos = request.getParam(POSITION);
            Position position = getPosition(pos);
            String ra, dec;

            ra = ifNotNull(position.centerColValues().ra())
                    .get(v -> v.equals("null") ? null : v);  // Ensure "null" is treated as null

            dec = ifNotNull(position.centerColValues().dec())
                    .get(v -> v.equals("null") ? null : v);

            if (ra == null || dec == null) { //if ra dec are null, use center columns to get the lon & lat values from the file
                String lonCol = position.centerColNames.lonCol(); //for ra/lon
                String latCol = position.centerColNames.latCol(); //for dec/lat
                ra = Objects.toString(dgDataUrl.get(idx, lonCol), null);
                dec = Objects.toString(dgDataUrl.get(idx, latCol), null);
            }

            //to be used for cutout service descriptor url
            String serDefUrl = "";
            Map<String, ServDescUrl> serDefUrls = new HashMap<>();

            for (DataGroup dg : groups) {
                boolean makeDataLinkFolder = false;
                int countValidDLFiles = 0;
                //do one initial pass through semantics column to determine if we need to create a folder for datalink files
                for (int i=0; i < dg.size(); i++) {
                    String sem = Objects.toString(dg.getData(SEMANTICS, i), null);
                    if (sem == null) {
                        continue;
                    }

                    if (testSem(sem,"#this") || testSem(sem,"#thumbnail") ||testSem(sem,"#preview") ||
                            testSem(sem,"#preview-plot")) {
                        countValidDLFiles++;
                    }
                }
                if (countValidDLFiles > 1) {
                    makeDataLinkFolder = true;
                }

                for (int i=0; i < dg.size(); i++) {
                    String accessUrl = Objects.toString(dg.getData(ACCESS_URL, i), null);
                    String sem = Objects.toString(dg.getData(SEMANTICS, i), null);
                    String content_type = Objects.toString(dg.getData(CONTENT_TYPE, i), null);

                    String productUrl = accessUrl;

                    if (accessUrl == null || sem == null) {
                        //if only semantic (sem) is null, accessUrl may still be available, but we won't know which accessUrls ones to pick
                        String serviceDef = String.valueOf(dg.getData(SERVICE_DEF, i));

                        if (testSem(sem, "#cutout") && serviceDef != null && !serviceDef.isEmpty()) {
                            ServDescUrl result;

                            if (serDefUrls.containsKey(serviceDef)) { //if we have already found this service descriptor url
                                result = serDefUrls.get(serviceDef);
                            } else { //else, parse this service descriptor to create a product url using access_url and the inputParams
                                if (ra == null || dec == null || cutoutValue == null) continue; //cannot create service descriptor url
                                result = createCutoutSerDefUrl(groups, serviceDef, ra, dec, cutoutValue);
                                serDefUrls.put(serviceDef, result);
                            }

                            if (result != null && result.isValid()) {
                                if (result.missingParams.isEmpty()) {
                                    serDefUrl = result.partialUrl;
                                } else {
                                    //Resolve missing params from currentRow (these are inputParams with no value but a "ref" instead)
                                    StringBuilder finalUrl = new StringBuilder(result.partialUrl);
                                    for (MissingParam missing : result.missingParams) {
                                        String refId = missing.refId;
                                        String key = "";
                                        for (DataType data : dg.getDataDefinitions()) {
                                            if (data.getID().equalsIgnoreCase(refId)) {
                                                key = data.getKeyName();
                                                break;
                                            }
                                        }
                                        String resolvedValue = String.valueOf(dg.getData(key, i));
                                        if (!StringUtils.isEmpty(resolvedValue)) {
                                            finalUrl.append("&").append(missing.paramName).append("=").append(resolvedValue);
                                        }
                                    }
                                    serDefUrl = finalUrl.toString();
                                }
                                productUrl = serDefUrl;
                            }
                            else continue; //couldn't create a valid service descriptor product url for this cutout
                        }
                        else continue;
                    }

                    String ext_file_name= null;
                    if (useSourceUrlFileName) {
                        String fileName= new URL(productUrl).getFile();
                        String ext= FileUtil.getExtension(fileName);
                        if (!ext.isEmpty()) ext_file_name= fileName;
                    }
                    if (ext_file_name==null){
                        ext_file_name = prepend_file_name;
                        ext_file_name = testSem(sem, "#this") ? ext_file_name : ext_file_name + "-" + substringAfterLast(sem, "#");
                        String extension = "unknown";
                        extension = content_type != null ? getExtFromURL(content_type) : getExtFromURL(productUrl);
                        ext_file_name += "." + extension;
                        //create a folder only if makeDataLinkFolder is true
                        ext_file_name = makeDataLinkFolder ? prepend_file_name + "/" + ext_file_name : ext_file_name;
                    }

                    if (products != null) { // Check if products exist and contains sem
                        if (Arrays.asList(products).contains(sem) || Arrays.asList(products).contains("*")) { //* refers to all data products
                            FileInfo fileInfo = new FileInfo(productUrl, ext_file_name, 0);
                            fileInfos.add(fileInfo);
                        }
                    } else { //add all valid productUrls (accessUrls or cutout service descriptor urls)
                        FileInfo fileInfo = new FileInfo(productUrl, ext_file_name, 0);
                        fileInfos.add(fileInfo);
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return fileInfos;
    }

    private static boolean testSem(String sem, String val) {
        return (sem!=null && sem.toLowerCase().endsWith(val));
    }

    public static String getExtFromURL(String contentType) {
        String ext = "unknown"; //fallback, if no content type is found
        try {
            if (contentType != null) {
                for (String ct : acceptableExtensionTypes) {
                    if (contentType.contains(ct)) {
                        ext = ct;
                        break;
                    }
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return ext;
    }

    public static String makeValidString(String val) {
        //remove spaces and replace all non-(alphanumeric,period,underscore,hyphen) chars with underscore
        return val.replaceAll("\\s", "").replaceAll("[^a-zA-Z0-9._-]", "_");
    }
    public static String getFileName(int idx, String[] colNames, boolean useSourceUrlFileName, MappedData dgDataUrl, URL url) {
        String obs_collection = (String) dgDataUrl.get(idx, "obs_collection");
        String instrument_name = (String) dgDataUrl.get(idx, "instrument_name");
        String obs_id = (String) dgDataUrl.get(idx, "obs_id");
        String obs_title = (String) dgDataUrl.get(idx, "obs_title");
        String file_name = "";

        //option 0: if useSourceUrlFileName is true the try to get it from the URL
        if (useSourceUrlFileName) {
            String fileName= url.getFile();
            String ext= FileUtil.getExtension(fileName);
            if (!ext.isEmpty()) return fileName;
        }

        //option 1: use productTitleTemplate (colNames) if available to construct file name
        if (colNames != null) {
            for(String col : colNames) {
                String value = (String) dgDataUrl.get(idx, col);
                if (value != null) {
                    file_name += makeValidString(value) + "-";
                }
            }
        }
        if (file_name.length() > 0) {
            return file_name.substring(0, file_name.length()-1); //to get rid of last hyphen
        }

        else if (obs_title != null) { //option 2, if productTitleTemplate is not available, try and use obs_title
            file_name = makeValidString(obs_title);
        }

        else { //option 3: create file name using this manual template
            file_name = (obs_collection != null? makeValidString(obs_collection)+"-" : "") +
                    (instrument_name != null? makeValidString(instrument_name)+"-" : "") +
                    (obs_id != null? makeValidString(obs_id) : "") ;
        }

        if (file_name.length() == 0) file_name = "file"; //fallback option
        return file_name;
    }


    //the code below parses the service descriptors for cutouts to create a cutout product URL (and to check for a ServDesc containing datalink in non-obscore fileseu)

    private static Position getPosition(String pos) {
        JSONObject jsonObject = new JSONObject(pos);
        JSONObject centerColsJson = jsonObject.getJSONObject(CENTER_COL_NAMES);
        CenterCols centerColNames = new CenterCols(
                String.valueOf(centerColsJson.get(LON_COL)),
                String.valueOf(centerColsJson.get(LAT_COL))
        );
        JSONObject centerColValuesJson = jsonObject.getJSONObject(CENTER_COL_VALS);
        CenterColValues centerColValues = new CenterColValues(
                String.valueOf(centerColValuesJson.get(RA)),
                String.valueOf(centerColValuesJson.get(DEC))
        );
        return new Position(centerColNames, centerColValues);
    }

    private static ServDescUrl createCutoutSerDefUrl(DataGroup[] groups, String serviceDefId, String ra, String dec, String cutoutValue) {
        for (DataGroup dg : groups) {
            for (ResourceInfo ri : dg.getResourceInfos()) {
                if (ADHOC_SERVICE.equalsIgnoreCase(ri.getUtype()) && serviceDefId.equalsIgnoreCase(ri.getID())) {
                    ServDescUrl result = parseCutoutServiceDescriptor(ri, ra, dec, cutoutValue);
                    return result;
                }
            }
        }
        return null;
    }


    private static ServDescUrl parseCutoutServiceDescriptor(ResourceInfo serviceDescriptor, String ra, String dec, String cutoutValue) {
        String accessUrl = null;
        String primaryParamName = null;
        StringBuilder queryString = new StringBuilder();
        List<MissingParam> missingParams = new ArrayList<>(); //these are inputParams with no value but a "ref" instead

        List<GroupInfo> groups = serviceDescriptor.getGroups();
        List<ParamInfo> params = serviceDescriptor.getParams();

        //Extract accessURL
        for (ParamInfo param : params) {
            if (SER_DEF_ACCESS_URL.equalsIgnoreCase(param.getKeyName())) {
                accessUrl = param.getStringValue();
                break;
            }
        }
        if (accessUrl == null || accessUrl.isEmpty()) {
            return new ServDescUrl("", Collections.emptyList()); // Invalid service descriptor
        }

        //Locate the "inputParams" group
        for (GroupInfo group : groups) {
            if (!SER_DEF_INPUT_PARAMS.equalsIgnoreCase(group.getName())) continue;

            List<ParamInfo> inputParams = group.getParamInfos();

            //Check for xtype = "CIRCLE"
            for (ParamInfo param : inputParams) {
                if (CIRCLE.equalsIgnoreCase(param.getXType())) {
                    primaryParamName = param.getKeyName();
                    String formattedString = String.format("%s=%s+%s+%s", primaryParamName, ra, dec, cutoutValue);
                    queryString.append(formattedString);
                    break;
                }
            }

            //If no CIRCLE, check for separate RA, DEC, CUTOUT via UCDs
            if (primaryParamName == null) {
                String raName = null, decName = null, cutoutName = null;

                for (ParamInfo param : inputParams) {
                    String ucd = param.getUCD();
                    if (ucd == null) continue;

                    if (isMatch(ucd, CUTOUT_UCDs)) cutoutName = param.getKeyName();
                    else if (isMatch(ucd, RA_UCDs)) raName = param.getKeyName();
                    else if (isMatch(ucd, DEC_UCDs)) decName = param.getKeyName();
                }

                if (raName != null && decName != null && cutoutName != null) {
                    String formattedString = String.format("%s=%s&%s=%s&%s=%s", raName, ra, decName, dec, cutoutName, cutoutValue);
                    queryString.append(formattedString);
                } else {
                    return new ServDescUrl("", Collections.emptyList()); // Invalid service descriptor
                }
            }

            //if either one of ra/dec/cutoutValue is null, try and find them in inputParams (default value), else return invalid service descriptor
            boolean raResolved = (ra != null);
            boolean decResolved = (dec != null);
            boolean cutoutResolved = (cutoutValue != null);

            //Append other valid params (where value is not null or empty) (excluding RA, DEC, CUTOUT)
            for (ParamInfo param : inputParams) {
                String ucd = param.getUCD();
                String key = param.getKeyName();
                String value = param.getStringValue();

                //if ra, dec or cutoutValue is null, check if you can use default vals in the service descriptor
                if ((isMatch(ucd, RA_UCDs) && raResolved) ||
                        (isMatch(ucd, DEC_UCDs) && decResolved) ||
                        (isMatch(ucd, CUTOUT_UCDs) && cutoutResolved)) {
                    continue; //skip processing for RA, DEC, or CUTOUT if they were provided in the function arguments
                }

                if (StringUtils.isEmpty(key)) continue;

                if (param.getRef() != null && StringUtils.isEmpty(value)) {
                    missingParams.add(new MissingParam(key, param.getRef()));
                } else if (!StringUtils.isEmpty(value)) {
                    queryString.append("&").append(key).append("=").append(value);
                    //mark RA, DEC, or CUTOUT as resolved if we find a value for them
                    if (isMatch(ucd, RA_UCDs)) ra = value;
                    if (isMatch(ucd, DEC_UCDs)) dec = value;
                    if (isMatch(ucd, CUTOUT_UCDs)) cutoutValue = value;
                }
            }
        }

        if (ra == null || dec == null || cutoutValue == null) { //final check, if one of these values is null, cannot create service descriptor url
            return new ServDescUrl("", Collections.emptyList()); //invalid service descriptor
        }

        return new ServDescUrl(accessUrl + "?" + queryString.toString(), missingParams);
    }

    //Utility function to check if UCD matches a known UCD category
    private static boolean isMatch(String ucd, List<String> ucdArray) {
        if (ucd == null || ucdArray == null || ucdArray.isEmpty()) {
            return false;
        }

        String[] ucdParts = ucd.split(";");

        //check if any ucsd exists in ucdArray
        for (String part : ucdParts) {
            if (ucdArray.contains(part.trim())) {
                return true;
            }
        }
        return false;
    }

    private static ServDescUrl createUrlFromServDesc(String datalinkServDesc) {
        String accessUrl = null;
        StringBuilder queryString = new StringBuilder();
        List<MissingParam> missingParams = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(datalinkServDesc);

            //extract accessUrl
            accessUrl = jsonObject.getString(SER_DEF_ACCESS_URL);

            if (StringUtils.isEmpty(accessUrl)) {
                return new ServDescUrl("", Collections.emptyList());
            }

            //extract inputParams
            JSONObject inputParams = jsonObject.getJSONObject(SER_DEF_INPUT_PARAMS);

            for (String key : inputParams.keySet()) {
                JSONObject paramObject = inputParams.getJSONObject(key);

                //extract either "ref" or "value" (checking which exists)
                String ref = paramObject.optString(REF, null);
                String value = paramObject.optString(VALUE, null);

                if (ref != null) {
                    missingParams.add(new MissingParam(key, ref));
                }
                if (value != null) {
                    if (!queryString.isEmpty()) queryString.append("&");
                    queryString.append(key).append("=").append(value);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ServDescUrl(accessUrl + "?" + queryString.toString(), missingParams);
    }
}
