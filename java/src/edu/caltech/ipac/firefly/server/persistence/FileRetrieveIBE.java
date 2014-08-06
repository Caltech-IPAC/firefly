package edu.caltech.ipac.firefly.server.persistence;

import edu.caltech.ipac.astro.ibe.IBE;
import edu.caltech.ipac.astro.ibe.IbeDataParam;
import edu.caltech.ipac.astro.ibe.IbeDataSource;
import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.server.packagedata.FileInfo;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchProcessor;
import edu.caltech.ipac.firefly.server.query.SearchProcessorImpl;
import edu.caltech.ipac.firefly.server.query.URLFileInfoProcessor;
import edu.caltech.ipac.firefly.server.visualize.LockingVisNetwork;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * @author tatianag
 *         $Id: $
 */
@SearchProcessorImpl(id = "ibe_file_retrieve")
public class FileRetrieveIBE extends URLFileInfoProcessor implements SearchProcessor<FileInfo>  {
    public FileInfo getData(ServerRequest request) throws DataAccessException {
        try {
            String mission = request.getParam("mission");
            Map<String,String> paramMap = IBEUtils.getParamMap(request.getParams());

            IBE ibe = IBEUtils.getIBE(mission, paramMap);
            IbeDataSource ibeDataSource = ibe.getIbeDataSource();
            IbeDataParam dataParam= ibeDataSource.makeDataParam(paramMap);
            if (!dataParam.isDoCutout() && ibeDataSource.useFileSystem()) {
                File f = ibe.createDataFilePath(dataParam);
                if (f.exists()) {
                    return new FileInfo(f.toString(),
                            dataParam.getFileName(),
                            f.length());
                } else {
                    throw new DataAccessException(f.toString()+" does not exist");
                }
            } else {
                URL url = ibe.createDataUrl(dataParam);
                try {
                    return LockingVisNetwork.getFitsFile(url);
                } catch (FailedRequestException fre) {
                    throw new DataAccessException("Failed to get file from url "+url, fre);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Failed to get file info.", e);
        } catch (DataAccessException dae) {
            throw dae;
        }
    }

    @Override
    public URL getURL(ServerRequest sr) throws MalformedURLException {
        return null; /* not used */
    }

}
