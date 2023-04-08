package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.table.DataObject;
import edu.caltech.ipac.table.MappedData;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.download.URLDownload;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.URL;

import static org.apache.commons.lang.StringUtils.substringAfterLast;

@SearchProcessorImpl(id = "ObsCorePackager")
public class ObsCorePackager extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    private static final List<String> acceptableExtensionTypes = Arrays.asList("png","jpg","jpeg","bmp","fits","tsv",
            "csv", "tbl", "fits", "json", "pdf", "tar", "html", "xml", "vot", "vot", "vot", "vot", "reg", "png", "xml");

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            LOGGER.error(e);
            throw new DataAccessException(e.getMessage());
        }
    }

    private List<FileGroup> computeFileGroup(DownloadRequest request) {
        var selectedRows = new ArrayList<>(request.getSelectedRows());

        List<FileInfo> fileInfos = new ArrayList<>();
        MappedData dgDataUrl = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(), selectedRows); //returns all columns

        try {
            Set<String> uniqueFileNames = new HashSet<>();
            Map<String,Integer> fileTypeCounter = new HashMap<>(); //different counters based on file extension .fits, .jpeg, etc.
            for (int idx : selectedRows) {
                String access_url = (String) dgDataUrl.get(idx, "access_url");
                if (access_url == null) {
                    continue; //no file available to process
                }
                String access_format = (String) dgDataUrl.get(idx, "access_format");
                URL url = new URL(access_url);
                HttpResultInfo res = URLDownload.getDataFromURL(url, null, null, null);
                String contentType = res.getContentType();

                String colNames = request.getSearchRequest().getParam("templateColNames");
                String[] cols = colNames != null ? colNames.split(",") : null;

                String ext_file_name = getFileName(idx,cols,dgDataUrl);
                String extension;

                if (access_format != null) {
                    if (access_format.contains("datalink")) {
                        if (uniqueFileNames.contains(ext_file_name)) { //folder name to be used for datalink entries - check for uniqueness
                            Integer counter = fileTypeCounter.get("datalink");
                            if (counter == null) {
                                counter = 1;
                            }
                            ext_file_name = ext_file_name + "(" + counter + ")";
                            fileTypeCounter.put("datalink", ++counter);
                        }
                        uniqueFileNames.add(ext_file_name);
                        List<FileInfo> tmpFileInfo = parseDatalink(url, ext_file_name);
                        fileInfos.addAll(tmpFileInfo);
                    }
                    else { //non datalink entry -  such as fits,jpg etc.
                        extension = access_format.replaceAll(".*/", "");
                        ext_file_name = uniquenessCheck(uniqueFileNames,ext_file_name,extension,fileTypeCounter);
                        FileInfo fileInfo = new FileInfo(access_url, ext_file_name, 0);
                        fileInfos.add(fileInfo);
                    }
                }
                else { //access_format is null, so try and get it from the url's Content_Type
                    extension = getExtFromURL(contentType);
                    ext_file_name = uniquenessCheck(uniqueFileNames,ext_file_name,extension,fileTypeCounter);
                    FileInfo fileInfo = new FileInfo(access_url, ext_file_name, 0);
                    fileInfos.add(fileInfo);
                }

            }
            fileInfos.forEach(fileInfo -> fileInfo.setRequestInfo(HttpServiceInput.createWithCredential(fileInfo.getInternalFilename())));
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return Arrays.asList(new FileGroup(fileInfos, null, 0, "ObsCore Download Files"));
    }

    // uniquenessCheck checks for unique file names (appending (1), (2), etc. for duplicate file names)
    // It also appends the extension and returns the final file name created
    public static String uniquenessCheck(Set<String> uniqueFileNames,String extName,String extension,Map<String,Integer>fileTypeCounter) {
        if (uniqueFileNames.contains(extName + "." + extension)) {
            Integer counter = fileTypeCounter.get(extension);
            if (counter == null) {
                counter = 1;
            }
            extName += "("+counter+")." + extension;
            fileTypeCounter.put(extension, ++counter);
        }
        else {
            extName += "." + extension;
        }
        uniqueFileNames.add(extName);
        return extName;
    }

    public static List<FileInfo> parseDatalink(URL url, String prepend_file_name) {
        List<FileInfo> fileInfos = new ArrayList<>();;
        try {
            File tmpFile = File.createTempFile("datlink-", ".xml", ServerContext.getTempWorkDir());
            URLDownload.getDataToFile(url, tmpFile);
            DataGroup[] groups = VoTableReader.voToDataGroups(tmpFile.getPath(), false);
            for (DataGroup dg : groups) {
                Set<String> uniqueFileNames = new HashSet<>();
                Map<String,Integer> fileTypeCounter = new HashMap<>();
                boolean makeDataLinkFolder = false;
                int countValidDLFiles = 0;
                //do one initial pass through semantics column to determine if we need to create a folder for datalink files
                for (int i=0; i < dg.size(); i++) {
                    String sem = String.valueOf(dg.getData("semantics", i));
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
                    String accessUrl = String.valueOf(dg.getData("access_url", i));
                    String sem = String.valueOf(dg.getData("semantics", i));
                    String content_type = String.valueOf(dg.getData("content_type", i));

                    if (accessUrl == null || sem == null) {
                        //if only semantic (sem) is null, accessUrl may still be available, but we won't know which accessUrls ones to pick
                        continue;
                    }

                    if (testSem(sem,"#this") || testSem(sem,"#thumbnail") ||testSem(sem,"#preview") ||
                            testSem(sem,"#preview-plot")) {
                        //get rid of braces in the ext_file_name coming from the folder name (prepend_file_name)
                        String ext_file_name = prepend_file_name.replaceAll("\\([^()]*\\)", "");
                        ext_file_name = testSem(sem, "#this") ? ext_file_name : ext_file_name + "-" + substringAfterLast(sem, "#");
                        String extension = "unknown";
                        if (content_type != null ) {
                            extension = getExtFromURL(content_type);
                        }
                        else { //fallback
                            extension = getExtFromURL(accessUrl);
                        }
                        ext_file_name = uniquenessCheck(uniqueFileNames,ext_file_name,extension,fileTypeCounter);
                        //create a folder only if makeDataLinkFolder is true
                        ext_file_name = makeDataLinkFolder ? prepend_file_name + "/" + ext_file_name : ext_file_name;
                        FileInfo fileInfo = new FileInfo(accessUrl, ext_file_name, 0);
                        fileInfos.add(fileInfo);
                    }
                }
            }
            tmpFile.delete();
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
    public static String getFileName(int idx, String[] colNames, MappedData dgDataUrl) {
        String obs_collection = (String) dgDataUrl.get(idx, "obs_collection");
        String instrument_name = (String) dgDataUrl.get(idx, "instrument_name");
        String obs_id = (String) dgDataUrl.get(idx, "obs_id");
        String obs_title = (String) dgDataUrl.get(idx, "obs_title");
        String file_name = "";

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
}
