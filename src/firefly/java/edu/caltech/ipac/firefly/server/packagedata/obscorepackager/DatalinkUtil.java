package edu.caltech.ipac.firefly.server.packagedata.obscorepackager;

import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.*;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.StringUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCorePackager.*;
import static edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCoreUtil.getExtFromURL;
import static edu.caltech.ipac.firefly.server.packagedata.obscorepackager.ObsCoreUtil.testSem;
import static edu.caltech.ipac.util.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfterLast;

public class DatalinkUtil {

    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public record MissingParam(String paramName, String refId) {}

    public record ServDescUrl(String partialUrl, List<MissingParam> missingParams) {
        public boolean isValid() {
            return !isEmpty(partialUrl);
        }
    }

    public static String getCutoutSerDefUrl(Map<String, ServDescUrl> serDefUrls, String serviceDef,
                                            List<String> positionColVals, String cutoutValue, DataGroup[] groups, DataGroup dg,
                                            int idx) {

        String serDefUrl = "";
        ServDescUrl result;
        String ra = positionColVals.get(0);
        String dec = positionColVals.get(1);

        if (serDefUrls.containsKey(serviceDef)) {
            result = serDefUrls.get(serviceDef);
        } else {
            if (ra == null || dec == null || cutoutValue == null) return null;
            result = createCutoutSerDefUrl(groups, serviceDef, ra, dec, cutoutValue);
            serDefUrls.put(serviceDef, result);
        }

        if (result != null && result.isValid()) {
            if (result.missingParams().isEmpty()) {
                serDefUrl = result.partialUrl();
            } else {
                StringBuilder finalUrl = new StringBuilder(result.partialUrl());
                for (MissingParam missing : result.missingParams()) {
                    String refId = missing.refId();
                    if (StringUtils.isEmpty(refId)) continue;

                    String key = "";
                    for (DataType data : dg.getDataDefinitions()) {
                        String dataId = data.getID();
                        if (dataId != null && data.getID().equalsIgnoreCase(refId)) {
                            key = data.getKeyName();
                            break;
                        }
                    }

                    String resolvedValue = String.valueOf(dg.getData(key, idx));
                    if (!StringUtils.isEmpty(resolvedValue)) {
                        finalUrl.append("&").append(missing.paramName()).append("=").append(resolvedValue);
                    }
                }
                serDefUrl = finalUrl.toString();
            }
        }
        return serDefUrl;
    }

    public static String getAccessUrlFromNonObsCore(ServDescUrl serDescUrl, DataGroup dg, MappedData dgDataUrl, int idx) {
        String accessUrl = null;
        if (serDescUrl != null && serDescUrl.isValid()) {
            StringBuilder finalUrl = new StringBuilder(serDescUrl.partialUrl());

            if (serDescUrl.missingParams().isEmpty()) {
                accessUrl = serDescUrl.partialUrl();
            } else {
                for (MissingParam missing : serDescUrl.missingParams()) {
                    String refId = missing.refId();
                    String key = "";
                    for (DataType data : dg.getDataDefinitions()) {
                        String dataId = data.getID();
                        if (dataId != null && dataId.equalsIgnoreCase(refId)) {
                            key = data.getKeyName();
                            break;
                        }
                    }
                    String resolvedValue = Objects.toString(dgDataUrl.get(idx, key), null);
                    if (resolvedValue != null && !resolvedValue.isEmpty()) {
                        if (serDescUrl.partialUrl().endsWith("?"))
                            finalUrl.append(missing.paramName()).append("=").append(resolvedValue);
                        else
                            finalUrl.append("&").append(missing.paramName()).append("=").append(resolvedValue);
                    }
                }
                accessUrl = finalUrl.toString();
            }
        }
        return accessUrl;
    }

    public static ServDescUrl createUrlFromServDesc(String datalinkServDesc) {
        String accessUrl;
        StringBuilder queryString = new StringBuilder();
        List<MissingParam> missingParams = new ArrayList<>();

        try {
            JSONObject jsonObject = new JSONObject(datalinkServDesc);

            accessUrl = jsonObject.getString(ObsCorePackager.SER_DEF_ACCESS_URL);
            if (StringUtils.isEmpty(accessUrl)) {
                return new ServDescUrl("", Collections.emptyList());
            }

            JSONObject inputParams = jsonObject.getJSONObject(ObsCorePackager.SER_DEF_INPUT_PARAMS);

            for (String key : inputParams.keySet()) {
                JSONObject paramObject = inputParams.getJSONObject(key);

                String ref = paramObject.optString(ObsCorePackager.REF, null);
                String value = paramObject.optString(ObsCorePackager.VALUE, null);

                if (ref != null) {
                    missingParams.add(new MissingParam(key, ref));
                }
                if (value != null) {
                    if (!queryString.isEmpty()) queryString.append("&");
                    queryString.append(key).append("=").append(value);
                }
            }

        } catch (Exception e) {
            LOGGER.error(e);
            return new ServDescUrl("", Collections.emptyList());
        }

        String separator = accessUrl.endsWith("?") ? "" : "?";
        return new ServDescUrl(accessUrl + separator + queryString, missingParams);
    }

    private static ServDescUrl createCutoutSerDefUrl(DataGroup[] groups, String serviceDefId, String ra, String dec, String cutoutValue) {
        for (DataGroup dg : groups) {
            for (ResourceInfo ri : dg.getResourceInfos()) {
                if (ObsCorePackager.ADHOC_SERVICE.equalsIgnoreCase(ri.getUtype()) &&
                        serviceDefId.equalsIgnoreCase(ri.getID())) {
                    return parseCutoutServiceDescriptor(ri, ra, dec, cutoutValue);
                }
            }
        }
        return null;
    }

    private static ServDescUrl parseCutoutServiceDescriptor(ResourceInfo serviceDescriptor, String ra, String dec, String cutoutValue) {
        String accessUrl = null;
        String primaryParamName = null;
        StringBuilder queryString = new StringBuilder();
        List<MissingParam> missingParams = new ArrayList<>();

        List<GroupInfo> groups = serviceDescriptor.getGroups();
        List<ParamInfo> params = serviceDescriptor.getParams();

        for (ParamInfo param : params) {
            if (ObsCorePackager.SER_DEF_ACCESS_URL.equalsIgnoreCase(param.getKeyName())) {
                accessUrl = param.getStringValue();
                break;
            }
        }
        if (accessUrl == null || accessUrl.isEmpty()) {
            return new ServDescUrl("", Collections.emptyList());
        }

        for (GroupInfo group : groups) {
            if (!ObsCorePackager.SER_DEF_INPUT_PARAMS.equalsIgnoreCase(group.getName())) continue;

            List<ParamInfo> inputParams = group.getParamInfos();

            for (ParamInfo param : inputParams) {
                if (ObsCorePackager.CIRCLE.equalsIgnoreCase(param.getXType())) {
                    primaryParamName = param.getKeyName();
                    String formattedString = String.format("%s=%s+%s+%s", primaryParamName, ra, dec, cutoutValue);
                    queryString.append(formattedString);
                    break;
                }
            }

            if (primaryParamName == null) {
                String raName = null, decName = null, cutoutName = null;

                for (ParamInfo param : inputParams) {
                    String ucd = param.getUCD();
                    if (ucd == null) continue;

                    if (isMatch(ucd, ObsCorePackager.CUTOUT_UCDs)) cutoutName = param.getKeyName();
                    else if (isMatch(ucd, ObsCorePackager.RA_UCDs)) raName = param.getKeyName();
                    else if (isMatch(ucd, ObsCorePackager.DEC_UCDs)) decName = param.getKeyName();
                }

                if (raName != null && decName != null && cutoutName != null) {
                    String formattedString = String.format("%s=%s&%s=%s&%s=%s", raName, ra, decName, dec, cutoutName, cutoutValue);
                    queryString.append(formattedString);
                } else {
                    return new ServDescUrl("", Collections.emptyList());
                }
            }

            boolean raResolved = (ra != null);
            boolean decResolved = (dec != null);
            boolean cutoutResolved = (cutoutValue != null);

            for (ParamInfo param : inputParams) {
                String ucd = param.getUCD();
                String key = param.getKeyName();
                String value = param.getStringValue();

                if ((isMatch(ucd, ObsCorePackager.RA_UCDs) && raResolved) ||
                        (isMatch(ucd, ObsCorePackager.DEC_UCDs) && decResolved) ||
                        (isMatch(ucd, ObsCorePackager.CUTOUT_UCDs) && cutoutResolved)) {
                    continue;
                }

                if (StringUtils.isEmpty(key)) continue;

                if (param.getRef() != null && StringUtils.isEmpty(value)) {
                    missingParams.add(new MissingParam(key, param.getRef()));
                } else if (!StringUtils.isEmpty(value)) {
                    queryString.append("&").append(key).append("=").append(value);
                }
            }
        }

        if (ra == null || dec == null || cutoutValue == null) {
            return new ServDescUrl("", Collections.emptyList());
        }

        return new ServDescUrl(accessUrl + "?" + queryString, missingParams);
    }

    //Utility function to check if UCD matches a known UCD category
    private static boolean isMatch(String ucd, List<String> ucdArray) {
        if (ucd == null || ucdArray == null || ucdArray.isEmpty()) return false;

        String[] ucdParts = ucd.split(";");
        for (String part : ucdParts) {
            if (ucdArray.contains(part.trim())) return true;
        }
        return false;
    }

    public static List<FileInfo> resolveDatalink(URL url, DownloadRequest request, String prependFileName, List<String> positionColVals, DataGroup data) throws IOException {
        List<FileInfo> fileInfos = new ArrayList<>();
        boolean isFlattenedStructure = request.getBooleanParam("isFlattenedStructure"); //true if flattened, else false for structured logic
        boolean generateDownloadFileName = Boolean.parseBoolean(Objects.toString(request.getParam(GENERATE_DOWNLOAD_FILE_NAME), "false"));

        String productTypes = request.getParam(PRODUCTS); //products to download in datalink file
        String[] products = (productTypes != null && !productTypes.trim().isEmpty()) ? productTypes.split(",") : null;
        String cutoutValue = request.getParam(CUTOUT_VALUE);

        try {
            DataGroup[] groups;
            if (data != null) { //if the request is from a datalink table, we will get the DataGroup object created directly from the table's selected rows
                groups = new DataGroup[]{data};
            }
            else {
                groups = VoTableReader.voToDataGroups(url.toString(), false); //for obscore/non obscore tables containing datalink, we use the accessUrl of the datalink
            }

            //to be used for cutout service descriptor url
            Map<String, ServDescUrl> serDefUrls = new HashMap<>();

            for (DataGroup dg : groups) {
                for (int i=0; i < dg.size(); i++) {
                    String accessUrl = Objects.toString(dg.getData(ACCESS_URL, i), null);
                    String sem = Objects.toString(dg.getData(SEMANTICS, i), null);
                    String file = Objects.toString(dg.getData(FILE, i), null);
                    String contentType = Objects.toString(dg.getData(CONTENT_TYPE, i), null);

                    String productUrl = accessUrl;

                    String fileName = null;
                    String suffix = null;

                    if (accessUrl == null || sem == null) {
                        //if only semantic (sem) is null, accessUrl may still be available, but we won't know which accessUrls ones to pick
                        String serviceDef = String.valueOf(dg.getData(SERVICE_DEF, i));

                        if (testSem(sem, "#cutout") && serviceDef != null && !serviceDef.isEmpty()) {
                            productUrl = getCutoutSerDefUrl(serDefUrls, serviceDef, positionColVals, cutoutValue, groups, dg, i);//serDefUrl;
                            if (isEmpty(productUrl)) continue;
                            if (!isEmpty(file)) fileName = file.substring(file.lastIndexOf('/') + 1);
                            suffix = "cutout";
                        }
                        else continue;
                    }

                    String extFileName= null;

                    if (generateDownloadFileName) {
                        extFileName = prependFileName;
                        extFileName = testSem(sem, "#this") ? extFileName : extFileName + "-" + substringAfterLast(sem, "#");
                        String extension = "unknown";
                        extension = contentType != null ? getExtFromURL(contentType) : getExtFromURL(productUrl);
                        extFileName += "." + extension;
                        extFileName = (isFlattenedStructure)  ?
                                extFileName : prependFileName + "/" + extFileName;
                    } else {
                        extFileName = (isFlattenedStructure) ?
                                extFileName : (prependFileName + "/" + (fileName != null ? fileName : ""));
                    }

                    if (products != null) { // Check if products exist and contains sem
                        if (Arrays.asList(products).contains(sem) || Arrays.asList(products).contains("*")) { //* refers to all data products
                            FileInfo fileInfo = new FileInfo(productUrl, extFileName, 0);
                            fileInfo.setSuffix(suffix);
                            fileInfos.add(fileInfo);
                        }
                    } else { //add all valid productUrls (accessUrls or cutout service descriptor urls)
                        FileInfo fileInfo = new FileInfo(productUrl, extFileName, 0);
                        fileInfo.setSuffix(suffix);
                        fileInfos.add(fileInfo);
                    }
                }
            }
        }
        catch(Exception e) {
            LOGGER.error(e);
        }
        return fileInfos;
    }

    //this function checks if the incoming table is a datalink table (in cases where user may extract a table from an existing obscore type table)
    public static boolean isLinksTable(DataGroup dataGroup) {
        DataType[] dataDefs = dataGroup.getDataDefinitions();
        if (dataDefs == null || dataDefs.length == 0) return false;

        Set<String> columnNames = Arrays.stream(dataDefs)
                .map(attr -> attr.getKeyName().toLowerCase())
                .collect(Collectors.toSet());

        //check if all datalink desc column names are present - confirming whether this is a datalink table
        return DATALINK_COL_NAMES.stream()
                .allMatch(columnNames::contains);
    }

}
