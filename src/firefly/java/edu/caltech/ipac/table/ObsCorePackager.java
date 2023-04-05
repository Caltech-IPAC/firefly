package edu.caltech.ipac.table;

import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.server.db.EmbeddedDbUtil;
import edu.caltech.ipac.firefly.server.network.HttpServiceInput;
import edu.caltech.ipac.firefly.server.packagedata.FileGroup;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.FileGroupsProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.table.io.IpacTableException;
import edu.caltech.ipac.table.io.VoTableReader;
import edu.caltech.ipac.util.download.URLDownload;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.net.URL;


@SearchProcessorImpl(id = "ObsCorePackager")
public class ObsCorePackager extends FileGroupsProcessor {
    private static final Logger.LoggerImpl LOGGER = Logger.getLogger();

    public List<FileGroup> loadData(ServerRequest request) throws IOException, DataAccessException {
        assert (request instanceof DownloadRequest);
        try {
            return computeFileGroup((DownloadRequest) request);
        } catch (Exception e) {
            LOGGER.error(e);
            throw new DataAccessException(e.getMessage());
        }
    }


    private List<FileGroup> computeFileGroup(DownloadRequest request) throws IOException, IpacTableException, DataAccessException {
        ArrayList<Integer> selectedRows = new ArrayList<>(request.getSelectedRows());

        //todo: keeping these lines in for now, in case we decide to do folders, otherwise these can be removed
        String zipType = request.getParam("zipType"); //folder or flat
        boolean doFolders = false;//zipType != null && zipType.equalsIgnoreCase("folder");

        List<FileInfo> fileInfos = new ArrayList<>();
        MappedData dgDataUrl = EmbeddedDbUtil.getSelectedMappedData(request.getSearchRequest(), selectedRows); //returns all columns

        //todo: using naming scheme from ObsCoreConverter.js getObsCoreRowMetaInfo for now - use productTitleTemplate eventually
        //todo: for the above, get request.searchRequest.params.template? from the request

        for (int idx : selectedRows) {
            String access_url = (String) dgDataUrl.get(idx, "access_url");
            String access_format = (String) dgDataUrl.get(idx, "access_format");
            String obs_collection = (String) dgDataUrl.get(idx, "obs_collection");
            String instrument_name = (String) dgDataUrl.get(idx, "instrument_name");
            String obs_id = (String) dgDataUrl.get(idx, "obs_id");
            URL url = new URL(access_url);
            String prepend_file_name = obs_collection + "-" + instrument_name + "-" + obs_id;

            if (access_format.contains("datalink")) {
                List<FileInfo> tmpFileInfo = parseDatalink(url, prepend_file_name);
                fileInfos.addAll(tmpFileInfo);
                continue;
            }

            String dataPath = url.getFile();
            String fileName = dataPath;
            File f = new File(fileName);
            String extName = f.getName(); //doFolders ? fileName : f.getName();
            extName = prepend_file_name + "_" + extName;

            FileInfo fileInfo = new FileInfo(access_url, extName, 0);
            fileInfos.add(fileInfo);
        }

        HttpServiceInput authCreds = HttpServiceInput.createWithCredential();
        fileInfos.forEach(fileInfo -> fileInfo.setRequestInfo(authCreds));

        return Arrays.asList(new FileGroup(fileInfos, null, 0, "ObsCore Download Files"));
    }

    public static List<FileInfo> parseDatalink(URL url, String prepend_file_name) {
        List<FileInfo> fileInfos = new ArrayList<>();;
        try {
            File tmpFile = File.createTempFile("tmpFile", ".xml");
            URLDownload.getDataToFile(url, tmpFile);
            DataGroup[] groups = VoTableReader.voToDataGroups(tmpFile.getAbsolutePath(), false);

            for (DataGroup dg : groups) {
                for (int i=0; i < dg.size(); i++) {
                    Object accessUrl = dg.getData("access_url", i);
                    Object semantics = dg.getData("semantics", i);
                    String sem = String.valueOf(semantics);
                    if (sem.equalsIgnoreCase("#this") || sem.equalsIgnoreCase("#preview" ) || sem.equalsIgnoreCase("#thumbnail")) {
                        URL tmpURL = new URL(String.valueOf(accessUrl));
                        String dataPath = tmpURL.getFile();
                        String fileName = dataPath;
                        File f = new File(fileName);
                        String extName = f.getName();
                        extName = prepend_file_name + "/" + extName; //create folder and add all Datalink entries in here
                        FileInfo fileInfo = new FileInfo(String.valueOf(accessUrl), extName, 0);
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
}
